package com.coinzgame.theoxo.coinz

import android.content.*
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.coinzgame.theoxo.coinz.R.id.home_nav
import com.google.firebase.firestore.*
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import org.json.JSONObject

import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * The app's main Activity.
 * Handles various aspects of loading and displaying the map,
 * tracking the user's location, marking the coins on the map and picking them up.
 */
class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener,
        OnMapReadyCallback, DownloadCompleteListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    private val tag = "MainActivity"

    // Local variables related to the location tracking and displaying
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var originLocation : Location
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Map variables
    private var mapView : MapView? = null
    private var mapboxMap : MapboxMap? = null
    private val bankLocation : LatLng = LatLng(55.945459, -3.188707)

    // Downloaded data tracking
    private var currentDate : String? = null // FORMAT YYYY/MM/DD
    private val preferencesFile : String = "CoinzPrefsFile"
    private var lastDownloadDate : String? = null
    private var cachedMap : String? = null

    // Keep track of which coins are within range
    private lateinit var coinsInRange : MutableSet<String>
    // Keep track of data related to the coins
    private lateinit var coinIdToMarker : MutableMap<String, Marker>
    private lateinit var coinIdToFeature : MutableMap<String, Feature>

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var currentUserEmail : String? = null

    // Today's rates
    private var rates : JSONObject? = null

    /**
     * Initializes the necessary instances and event handlers upon activity creation.
     * Gets the [MapView] instance and and the [FirebaseFirestore] instance, sets up
     * click events for the buttons in the activity and finally invokes [enableLocation]
     *
     * @param[savedInstanceState] the previously saved instance state, if it exists
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)
        Log.d(tag, "[onCreate] Received user email $currentUserEmail")

        Mapbox.getInstance(this, MAPBOX_KEY)

        coinsInRange = HashSet()
        coinIdToMarker = HashMap()
        coinIdToFeature = HashMap()

        // Set up the click event for the button which allows the user to collect the coins
        collectButton.setOnClickListener { _ -> collectNearbyCoins() }

        // Set up click events for bottom nav bar
        bottom_nav_bar.setOnNavigationItemSelectedListener(this)

        // Set up Firestore
        firestore = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp instead of java.util.Date
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        val emailTag : String? = currentUserEmail
        if (emailTag == null) {
            Log.e(tag, "[onCreate] null user email")
        } else {
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)

            mapView = findViewById(R.id.mapView)
            mapView?.onCreate(savedInstanceState)

            enableLocation()
        }
    }

    /**
     * Upon activity start, invokes the activity's and [mapView]'s super functions.
     * Also calculates [currentDate] and fetches the preferences stored on the device, notably the
     * [lastDownloadDate] and [cachedMap] so that the coin locations do not need to be downloaded
     * unnecessarily.
     */

    override fun onStart() {
        super.onStart()

        // Default navigation bar item checked should be home
        bottom_nav_bar.selectedItemId = home_nav

        mapView?.onStart()

        // Restore preferences
        val storedPrefs = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        lastDownloadDate = storedPrefs.getString("lastDownloadDate", null)
        Log.d(tag, "[onStart] Fetched lastDownloadDate: $lastDownloadDate")

        // Need to get date in onStart() because app may have been left running overnight
        val year : String = Calendar.getInstance().get(Calendar.YEAR).toString()
        var month : String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()  // Add one as 0-indexed
        var day : String = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        if (year != "2018" && year != "2019") {
            Log.e(tag, "Unsupported date")
        }
        if (month.length < 2) {
            // Pad to 0M format
            month = "0$month"
        }
        if (day.length < 2) {
            // Pad to 0D format
            day = "0$day"
        }
        // TODO make above nicer
        currentDate = "$year/$month/$day"
        Log.d(tag, "[onStart] Today's date: $currentDate")

        if (currentDate == lastDownloadDate) {
            Log.d(tag, "[onStart] Dates match, fetching cached map")
            cachedMap = storedPrefs.getString("cachedMap", null)
            if (cachedMap == null) {
                Log.w(tag, "[onStart] Dates matched but fetched cachedMap is null! "
                                + "Map will be downloaded.")
            } else {
                Log.d(tag, "[onStart] Fetched cachedMap: ${cachedMap?.take(25)}")
            }
        }
    }

    /**
     * On activity being resumed, invoke the super functions for the activity and [mapView].
     */
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    /**
     * On stop, invoke the correct super functions (activity's and [mapView]'s) and save user prefs.
     * The user preferences are stored on the device only if needed, namely if [lastDownloadDate]
     * and [cachedMap] have changed since the last time they were stored.
     */
    override fun onStop() {
        super.onStop()
        mapView?.onStop()

        if (lastDownloadDate == currentDate) {
            Log.d(tag, "[onStop] Not storing date or map as no change has been made")
        } else {
            // Store preferences
            val settings: SharedPreferences = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = settings.edit()

            Log.d(tag, "[onStop] Storing lastDownloadDate as currentDate: $currentDate")
            editor.putString("lastDownloadDate", currentDate)

            val sneakpeak: String? = cachedMap?.take(25)
            Log.d(tag, "[onStop] Storing cachedMap: $sneakpeak...")
            editor.putString("cachedMap", cachedMap)

            editor.apply()
        }
    }

    /**
     * Invokes the activity's and [mapView]'s super functions.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    /**
     * Invokes the activity's and [mapView]'s super functions.
     */
    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    /**
     * Invokes the activity's and [mapView]'s super functions.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    /**
     * Handles user clicks on the [BottomNavigationView], starting the corresponding activities.
     *
     * @param item The [MenuItem] clicked
     */
    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.account_nav -> startAccountActivity()
            R.id.messaging_nav -> startInboxActivity()
            else -> return true //do nothing
        }
        return true
    }

    /**
     * Starts a new [AccountActivity].
     */
    private fun startAccountActivity() {
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
    }

    private fun startInboxActivity() {
        val intent = Intent(this, InboxActivity::class.java)
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
    }

    /**
     * Updates the [collectButton] text and sets its visibility as appropriate.
     */
    private fun updateCollectButton() {
        val coinsInRangeSize : Int = coinsInRange.size
        when {
            coinsInRangeSize > 1 -> {
                collectButton.text = "Collect ${coinsInRange.size} Coins"
                collectButton.visibility = View.VISIBLE
            }

            coinsInRangeSize == 1 -> {
                collectButton.text = "Collect one Coin"
                collectButton.visibility = View.VISIBLE
            }

            else -> {
                collectButton.visibility = View.GONE
            }
        }
    }

    /**
     * Listener for the AsyncTask marker map data download having finished.
     * Calls addMarkers to add the markers to the map.
     *
     * @param[result] the downloaded GeoJSON [String] which describes the location of the coins
     */
    override fun downloadComplete(result: String) {
        cachedMap = result
        val sneakpeak = result.take(25)
        Log.d(tag, "[downloadComplete] Result: $sneakpeak...")
        addMarkers(result)
    }

    /**
     * Adds the [Marker]s for the coins to the [MapboxMap] being displayed.
     * First check that the coin being added isn't already in the user's wallet (meaning it has
     * already been collected).
     *
     * @param[geoJsonString] The downloaded GeoJSON (as a [String]) which describes the location of the coins
     */
    private fun addMarkers(geoJsonString : String) {
        val features = FeatureCollection.fromJson(geoJsonString).features()
        rates = JSONObject(geoJsonString).get("rates") as? JSONObject
        Log.d(tag, "Rates: $rates")
        val iconFactory = IconFactory.getInstance(this)
        when {
            features == null -> {
                Log.e(tag, "[addMarkers] features is null")
            }

            this.mapboxMap == null -> {
                Log.e(tag, "[addMarkers] mapboxMap is null, can't add markers")
            }

            else -> {

                // Features are non-null and mapboxMap is too. Can safely loop over the features,
                // adding the markers to the map as we go along.

                // First, get snapshot of user wallet as it is
                firestoreWallet?.get()?.run {
                    addOnSuccessListener { docSnapshot ->
                        // Getting the snapshot succeeded. Add the markers and geofences iff
                        // they are not already in the snapshot (i.e. the user has collected
                        // them before)
                        for (feature in features) {

                            // Extract information from the feature
                            val point: Point = feature.geometry() as Point
                            val lat: Double = point.latitude()
                            val long: Double = point.longitude()
                            val properties: JsonObject? = feature.properties()
                            val id: String? = properties?.get("id")?.asString
                            val value: String? = properties?.get("value")?.asString
                            val currency: String? = properties?.get("currency")?.asString
                            //val symbol: String? = properties?.get("marker-symbol")?.asString
                            //val colour: String? = properties?.get("marker-color")?.asString

                            when {
                                id == null -> Log.e(tag, "[addMarkers] id of feature is null")
                                currency == null -> Log.e(tag, "[addMarkers] currency of feature is null")
                                else -> {
                                    if (docSnapshot["$currency|$id"] == null) {
                                        // Add the marker if coin is not in wallet already
                                        val addedMarker: Marker? = mapboxMap?.addMarker(
                                                MarkerOptions()
                                                        .title("~${value?.substringBefore('.')} $currency.")
                                                        .snippet("Currency: $currency.\nValue: $value.")
                                                        .position(LatLng(lat, long)))

                                        if (addedMarker != null) {
                                            // Add ID -> Marker and ID -> Feature to the maps so
                                            // we can identify and pick up nearby coins later
                                            coinIdToMarker[id] = addedMarker
                                            coinIdToFeature[id] = feature
                                        } else {
                                            Log.e(tag, "[addMarkers] Failed to add marker")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                val bankIcon : Icon = iconFactory.fromResource(R.mipmap.bank_drawable)

                // Also want to add a special marker for the bank
                val bank : Marker? = mapboxMap?.addMarker(
                        MarkerOptions()
                                .title("BANK")
                                .snippet("The bank!")
                                .position(bankLocation)
                                .icon(bankIcon)
                )
            }
        }
    }

    /**
     * Listener function for the async call to receive the [MapboxMap].
     * Sets up the local [MapboxMap] instance ([mapboxMap]), and then begins to fetch today's coins
     * if they are not already cached.
     * Also invokes [initializeLocationLayer] and [initializeLocationEngine] which initialize
     * the location tracking.
     *
     * @param mapboxMap the received [MapboxMap]
     */
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.e(tag, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true
            this.mapboxMap?.setOnMarkerClickListener { marker ->
                if (marker.title == "BANK") {
                    val intent = Intent(this, BankActivity::class.java)
                    intent.putExtra(USER_EMAIL, currentUserEmail)
                    intent.putExtra(EXCHANGE_RATES, rates.toString())
                    startActivity(intent)
                    true
                }
                else {
                    false
                }
            }

            initializeLocationEngine()
            initializeLocationLayer()

            // Start download from here to make sure that the mapboxMap isn't null when
            // it's time to add markers
            if (cachedMap == null) {
                Log.d(tag, "[onMapReady] Downloading coins location map")
                val dateString = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
                DownloadFileTask(this).execute(dateString)
            } else {
                Log.d(tag, "[onMapReady] Adding markers for cached map")
                addMarkers(cachedMap!!)
            }
        }
    }


    /**
     * Checks for permisions before making further calls to set up the location tracking.
     * If the necessary location permissions have been granted, invokes the
     * async call to get the [mapboxMap] ([MapboxMap]) instance.
     * If not, instantiates a [PermissionsManager] and requests the location permissions.
     */
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "[enableLocation] Permissions granted")
            mapView?.getMapAsync(this)
        } else {
            Log.d(tag, "[enableLocation] Permissions not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    /**
     * Instantiates and sets up the [LocationEngine].
     */
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    /**
     * Instantiates and sets up the [LocationLayerPlugin].
     */
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        when {
            mapView == null -> {
                Log.d(tag, "[initializeLocationLayer] mapView is null")
            }

            mapboxMap == null -> {
                Log.d(tag, "[initializeLocationLayer] mapboxMap is null")
            }

            else -> {
                locationLayerPlugin = LocationLayerPlugin(mapView!!, mapboxMap!!, locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    /**
     * Updates the current camera position for the displayed [MapboxMap].
     *
     * @param[location] the new [Location] to focus the camera on
     */
    private fun setCameraPosition(location : Location) {
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 15.0))
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // TODO present dialog stating why permissions are needed
    }

    /**
     * Listener for location permission results. If granted, invokes [enableLocation].
     *
     * @param[granted] the truth value of the sentence "the permission was granted"
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            Log.e(tag, "[onPermissionResult] Permissions not granted")
            // TODO explain to user why necessary
        }
    }

    /**
     * Simply passes the result on to the [permissionsManager].
     */
    // TODO understand this
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Listener for the user's [Location] changing, updating the recorded and displayed location.
     * Also invokes [checkCoinsNearby]
     *
     * @param[location] the new [Location] found, or null
     */
    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            originLocation = location
            setCameraPosition(location)
            checkCoinsNearby(location)
        }
    }

    /**
     * Calculate distance between user and each coin on map, adding nearby ones to [coinsInRange].
     * Currently "nearby" means ~25m, though the distance calculation is not exact.
     * Invokes [updateCollectButton] before finishing to notify the user of newly added nearby
     * coins.
     * 
     * @param location The user's current [Location]
     */
    private fun checkCoinsNearby(location : Location) {
        for (coinID : String in coinIdToFeature.keys) {
            val coinPoint : Point? = coinIdToFeature[coinID]?.geometry() as? Point
            val fromLat : Double? = coinPoint?.latitude()
            val fromLong : Double? = coinPoint?.longitude()

            val toLat : Double = location.latitude
            val toLong : Double = location.longitude

            when {
                fromLat == null -> Log.e(tag, "[onLocationChanged] Lat of $coinID is null")
                fromLong == null -> Log.e(tag, "[onLocationChanged] Long of $coinID is null")
                else -> {
                    val dist = flatEarthDist(fromLat, toLat, fromLong, toLong)
                    if (dist <= 25) {
                        coinsInRange.add(coinID)
                    }
                }
            }
        }

        // Update the collect button now that coinsInRange may have changed
        updateCollectButton()
    }

    /**
     * Approximates the distance between to latitude/longitude positions cheaply.
     * Sourced from 
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * along with [distPerLat] and [distPerLong].
     * 
     * @param fromLat The latitude (as a [Double]) of the first point
     * @param fromLong The longitude (as a [Double]) of the first point
     * @param toLat The latitude (as a [Double]) of the second point
     * @param toLong The longitude (as a [Double]) of the second point
     * 
     * @return The approximate distance between the points in meters ([Double])
     */
    private fun flatEarthDist(
            fromLat : Double, toLat : Double, fromLong : Double, toLong : Double) : Double {
        val a = (fromLat-toLat) * distPerLat(fromLat)
        val b = (fromLong-toLong) * distPerLong(fromLat)
        return Math.sqrt(a*a + b*b)
    }

    /**
     * Calculates the approximate distance of "one latitude" at the given latitude.
     * Sourced from
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * along with [distPerLong] and [flatEarthDist].
     * 
     * @param lat The current latitude ([Double])
     * 
     * @return The approximate length of "one latitude at [lat]", in metres ([Double])
     */
    private fun distPerLat(lat : Double) : Double {
        return (-0.000000487305676*Math.pow(lat, 4.0)
                -0.0033668574*Math.pow(lat, 3.0)
                +0.4601181791*lat*lat
                -1.4558127346*lat+110579.25662316)
    }

    /**
     * Calculates the approximate distance of "one longitude" at the given latitude.
     * Sourced from
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * along with [distPerLat] and [flatEarthDist].
     *
     * @param lat The current latitude ([Double])
     *
     * @return The approximate length of "one longitude at [lat]", in metres ([Double])
     */
    private fun distPerLong(lat : Double) : Double {
        return (0.0003121092*Math.pow(lat, 4.0)
                +0.0101182384*Math.pow(lat, 3.0)
                -17.2385140059*lat*lat
                +5.5485277537*lat+111301.967182595)
    }
    
    /**
     * Requests location updates from the [locationEngine] upon the activity being connected.
     */
    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] Requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    /**
     * Collects all the nearby coins, i.e. those currently in [coinsInRange].
     */
    private fun collectNearbyCoins() {
        val coinsToAddToWallet : MutableMap<String, Any> = HashMap()
        val markersToRemove : MutableMap<String, Marker> = HashMap()  // id is needed to remove geofences

        val coins = coinsInRange
        for (id in coins) {
            val marker: Marker? = coinIdToMarker[id]
            val coinProperties: JsonObject? = coinIdToFeature[id]?.properties()
            val value : String? = coinProperties?.get("value")?.asString
            val currency : String? = coinProperties?.get("currency")?.asString

            when {
                value == null -> {
                    Log.e(tag, "[collectNearbyCoins] Coin value is null")
                }
                currency == null -> {
                    Log.e(tag, "[collectNearbyCoins] Coin currency is null")
                }
                marker == null -> {
                    Log.e(tag, "[collectNearbyCoins] marker is null")
                }
                else -> {
                    coinsToAddToWallet["$currency|$id"] = value
                    markersToRemove[id] = marker
                }
            }
        }

        // Note that we do not wait for the firestore update to complete successfully before
        // removing the marker. This is safe because even if it fails the marker will simply
        // be added to the map again the next time this activity is started, and so the user
        // will be able to try again.
        // This means we can remove the marker before waiting for the async call to the database
        // to finish, making for a smoother user experience.
        if (coinsToAddToWallet.size > 0) {
            updateWallet(coinsToAddToWallet)
        }

        if (markersToRemove.size > 0) {
            removeMarkers(markersToRemove)
        }

    }

    /**
     * Collect a given coin, adding it to the user's wallet on Firestore (i.e. [firestoreWallet]).
     *
     * @param coins A [MutableMap] of "currency|id" -> value, i.e. the form expected by the database.
     */
    private fun updateWallet(coins : MutableMap<String, Any>) {
        firestoreWallet?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    // Doc exists, update values
                    firestoreWallet?.update(coins)?.run {
                        addOnSuccessListener {
                            Log.d(tag,
                                    "[updateWallet] Succeeded with ${coins.size} coins")
                            toast("Added ${coins.size} coin(s) to your wallet")
                        }
                        addOnFailureListener { e ->
                            Log.e(tag, "[updateWallet] Doc exists but update failed: $e")
                        }
                    }
                } else {
                    // Doc doesn't exist, create it
                    Log.d(tag, "[updateWallet] Setting up new doc")
                    firestoreWallet?.set(coins)?.run {
                        addOnSuccessListener {
                            Log.d(tag,
                                    "[updateWallet] Created wallet and set ${coins.size} coins")
                            toast("Added ${coins.size} coin(s) to your wallet")
                        }
                        addOnFailureListener { e ->
                            Log.e(tag, "[updateWallet] Failed to create doc: $e")
                        }
                    }
                }
            }
            addOnFailureListener { e ->
                Log.e(tag, "[collectButton] Wallet get failed: $e")
            }
        }
    }

    /**
     * Removes the requested Markers from the map upon coin collection.
     *
     * @param coins A [MutableMap] of "coin id -> marker" containing the markers to be removed
     */
    private fun removeMarkers(coins : MutableMap<String, Marker>) {
        for ((id, marker) in coins) {
            mapboxMap?.removeMarker(marker)
            Log.d(tag, "[removeMarkers] Successfully removed marker of $id")

            // Remove the coin id from the maps as it is no longer needed, and we do not want
            // to check for its location again
            coinIdToFeature.remove(id)
            coinIdToMarker.remove(id)
        }

        coinsInRange.removeAll(coins.keys)
        updateCollectButton()
    }
}

