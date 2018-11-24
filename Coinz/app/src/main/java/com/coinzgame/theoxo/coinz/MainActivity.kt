package com.coinzgame.theoxo.coinz

import android.content.*
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
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
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import org.json.JSONException
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
    private var originLocation : Location? = null
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Map variables
    private var mapView : MapView? = null
    private var mapboxMap : MapboxMap? = null
    private val bankLocation : LatLng = LatLng(55.945459, -3.188707)

    // Locally saved data tracking
    private var currentDate : String? = null // FORMAT YYYY/MM/DD
    private var lastDownloadDate : String? = null
    private var cachedMap : String? = null
    private var ancientCoins = ArrayList<Feature>()

    // Keep track of which coins are within range
    private lateinit var coinsInRange : MutableSet<String>
    // Keep track of data related to the coins
    private lateinit var coinIdToMarker : MutableMap<String, Marker>
    private lateinit var coinIdToFeature : MutableMap<String, Feature>
    private lateinit var markerIdToCoinId : MutableMap<Long, String>

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var currentUserEmail : String? = null

    // Today's rates
    private var rates : JSONObject? = null

    // Combo bonus feature objects
    private var comboTimer: CountDownTimer? = null
    private var comboTimeRemaining: Long? = null
    private var comboFactor: Double? = null

    private var modeIsPickup: Boolean = false

    /**
     * Initializes the necessary instances and event handlers upon activity creation.
     * Begins the process to set up the map and location tracking.
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
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
        markerIdToCoinId = HashMap()

        fab_inspect.setOnClickListener { _ -> switchMode() }
        fab_pickup.setOnClickListener {_ -> switchMode() }

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
     * Switches the user mode between coin pick up and coin inspection.
     */
    private fun switchMode() {
        if (modeIsPickup) {
            // In pick up mode, want to switch to inspect mode.
            fab_inspect.visibility = View.INVISIBLE
            fab_pickup.visibility = View.VISIBLE
            modeIsPickup = false
        } else {
            // In inspect mode, want to switch to pick up mode.
            fab_inspect.visibility = View.VISIBLE
            fab_pickup.visibility = View.INVISIBLE
            modeIsPickup = true
        }
    }

    /**
     * Fetches the preferences stored on the device if necessary.
     */
    override fun onStart() {
        super.onStart()

        // Default navigation bar item checked should be home
        bottom_nav_bar.selectedItemId = home_nav

        mapView?.onStart()

        // Restore preferences
        val storedPrefs = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        lastDownloadDate = storedPrefs.getString(LAST_DOWNLOAD_DATE, null)
        Log.d(tag, "[onStart] Fetched lastDownloadDate: $lastDownloadDate")

        // Need to get date in onStart() because app may have been left running overnight
        val year : String = Calendar.getInstance().get(Calendar.YEAR).toString()
        // Add one to the month as it is 0-indexed
        var month : String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
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

        currentDate = "$year/$month/$day"
        Log.d(tag, "[onStart] Today's date: $currentDate")

        if (currentDate == lastDownloadDate) {
            Log.d(tag, "[onStart] Dates match, fetching cached map")
            cachedMap = storedPrefs.getString(SAVED_MAP_JSON, null)
            if (cachedMap == null) {
                Log.w(tag, "[onStart] Dates matched but fetched cachedMap is null! "
                                + "Map will be downloaded.")
            } else {
                Log.d(tag, "[onStart] Fetched cachedMap: ${cachedMap?.take(25)}")
            }
        }

        // Also get the current ancient coins
        val ancientShilCoinString = storedPrefs.getString(ANCIENT_SHIL, null)
        if (ancientShilCoinString != null && ancientShilCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient shil coin saved")
            ancientCoins.add(Feature.fromJson(ancientShilCoinString))
        }

        val ancientQuidCoinString = storedPrefs.getString(ANCIENT_QUID, null)
        if (ancientQuidCoinString != null  && ancientQuidCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient quid coin saved")
            ancientCoins.add(Feature.fromJson(ancientQuidCoinString))
        }

        val ancientDolrCoinString = storedPrefs.getString(ANCIENT_DOLR, null)
        if (ancientDolrCoinString != null  && ancientDolrCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient dolr coin saved")
            ancientCoins.add(Feature.fromJson(ancientDolrCoinString))
        }

        val ancientPenyCoinString = storedPrefs.getString(ANCIENT_PENY, null)
        if (ancientPenyCoinString != null  && ancientPenyCoinString.isNotEmpty()) {
            Log.d(tag, "[onStart] Found an ancient peny coin saved")
            ancientCoins.add(Feature.fromJson(ancientPenyCoinString))
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    /**
     * Saves the user preferences if needed.
     * The user preferences are stored on the device only if [lastDownloadDate]
     * has changed since the last time they were stored.
     */
    override fun onStop() {
        super.onStop()
        mapView?.onStop()

        if (lastDownloadDate == currentDate) {
            Log.d(tag, "[onStop] Not storing date or map")
        } else {
            // Store preferences
            val settings: SharedPreferences = getSharedPreferences(PREFERENCES_FILE,
                    Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = settings.edit()

            Log.d(tag, "[onStop] Storing lastDownloadDate as currentDate: $currentDate")
            editor.putString(LAST_DOWNLOAD_DATE, currentDate)

            val sneakpeak: String? = cachedMap?.take(25)
            Log.d(tag, "[onStop] Storing cachedMap: $sneakpeak...")
            editor.putString(SAVED_MAP_JSON, cachedMap)

            editor.apply()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    /**
     * Handles user clicks on the [BottomNavigationView], starting the corresponding activities.
     *
     * @param item the menu item clicked
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
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Starts a new [InboxActivity].
     */
    private fun startInboxActivity() {
        val intent = Intent(this, InboxActivity::class.java)
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Listener for the AsyncTask marker map data download having finished.
     * Begins the process of adding the downloaded coins to the map.
     *
     * @param result the downloaded GeoJSON which describes today's coins
     */
    override fun downloadComplete(result: String) {
        val sneakpeak = result.take(25)
        Log.d(tag, "[downloadComplete] Result: $sneakpeak...")

        if (result == NETWORK_ERROR) {
            toast(NETWORK_ERROR)
            finish()
        } else {
            cachedMap = result  // store the cachedMap so we can save it onStop
            addMarkers(result)
        }
    }

    /**
     * Adds the [Marker]s for the coins to the [MapboxMap] being displayed.
     * First checks that the coin being added isn't already in the user's wallet (meaning it has
     * already been collected).
     *
     * @param geoJsonString The downloaded GeoJSON which describes the coins.
     */
    private fun addMarkers(geoJsonString : String) {
        val features : MutableList<Feature>? = FeatureCollection.fromJson(geoJsonString).features()
        // Also add any and all currently active ancient coins
        features?.addAll(ancientCoins)

        rates = JSONObject(geoJsonString).get("rates") as? JSONObject
        Log.d(tag, "Rates: $rates")
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

                val iconFactory: IconFactory = IconFactory.getInstance(this)
                val coinIconFactory = CoinIconFactory(iconFactory)

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

                            when {
                                id == null -> {
                                    Log.e(tag, "[addMarkers] id of feature is null")
                                }
                                currency == null -> {
                                    Log.e(tag, "[addMarkers] currency of feature is null")
                                }
                                value == null -> {
                                    Log.e(tag, "[addMarkers] value of feature is null")
                                }
                                docSnapshot["$currency|$id"] != null -> {
                                    // Coin has already been collected by the user. skip it
                                }
                                else -> {

                                    val valueDouble: Double = try {
                                        value.toDouble()
                                    } catch (e: NumberFormatException) {
                                        Log.d(tag, "[addMarkers] Casting value to double "
                                                + "failed. Setting it to -1.0")
                                        // Setting the value to be negative will cause the icon
                                        // to be null, meaning the marker will not be added.
                                        -1.0
                                    }

                                    val roundedValueString = String.format("%.2f", valueDouble)

                                    val icon : Icon? = coinIconFactory.getIconForCoin(id, currency,
                                            valueDouble)

                                    val addedMarker: Marker? = if (icon != null) {
                                        mapboxMap?.addMarker(
                                                MarkerOptions()
                                                        .title("~${value
                                                                .substringBefore('.')}" +
                                                                " $currency.")
                                                        .snippet("Currency: $currency." +
                                                                "\nValue: $roundedValueString.")
                                                        .position(LatLng(lat, long))
                                                        .icon(icon))
                                    } else {
                                        null
                                    }

                                    if (addedMarker != null) {
                                        // Add ID -> Marker and ID -> Feature to the maps so
                                        // we can identify and pick up nearby coins later
                                        coinIdToMarker[id] = addedMarker
                                        coinIdToFeature[id] = feature
                                        markerIdToCoinId[addedMarker.id] = id
                                    } else {
                                        Log.e(tag, "[addMarkers] Failed to add marker")
                                    }
                                }
                            }
                        }
                    }
                }


                val bankIcon : Icon = iconFactory.fromResource(R.mipmap.bank_icon)

                // Also want to add a special marker for the bank
                val bank : Marker? = mapboxMap?.addMarker(
                        MarkerOptions()
                                .title("BANK")
                                .snippet("The bank!")
                                .position(bankLocation)
                                .icon(bankIcon)
                )

                if (bank == null) {
                    Log.e(tag, "[addMarkers] bank marker is null")
                }
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
     * @param mapboxMap the received mapbox map.
     */
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.e(tag, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true
            this.mapboxMap?.setOnMarkerClickListener { marker ->
               onMarkerClick(marker)
            }

            initializeLocationEngine()
            initializeLocationLayer()

            // Copy cachedMap for thread safety
            val localCachedMap: String? = cachedMap
            // Start download from here to make sure that the mapboxMap isn't null when
            // it's time to add markers
            if (localCachedMap == null) {
                // Will need to download the map first before adding the markers.
                Log.d(tag, "[onMapReady] Downloading coins location map")
                val dateString = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
                DownloadFileTask(this).execute(dateString)
            } else {
                Log.d(tag, "[onMapReady] Adding markers for cached map")
                addMarkers(localCachedMap)
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
     * Instantiates and sets up the [locationEngine].
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
     * Instantiates and sets up the [locationLayerPlugin].
     */
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        // Copy the fields to ensure safe multithreading
        val localMapView = mapView
        val localMapBoxMap = mapboxMap
        val localLocationEngine = locationEngine
        when {
            localMapView == null -> {
                Log.d(tag, "[initializeLocationLayer] mapView is null")
            }

            localMapBoxMap == null -> {
                Log.d(tag, "[initializeLocationLayer] mapboxMap is null")
            }

            else -> {
                locationLayerPlugin = LocationLayerPlugin(
                        localMapView, localMapBoxMap, localLocationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    /**
     * Updates the current camera position.
     *
     * @param[location] the new location to focus the camera on.
     */
    private fun setCameraPosition(location : Location) {
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLng(
                LatLng(location.latitude, location.longitude)))
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // TODO present dialog stating why permissions are needed
    }

    /**
     * Listener for location permission results.
     * If granted, invokes [enableLocation].
     *
     * @param granted whether the permission was granted.
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            Log.e(tag, "[onPermissionResult] Permissions not granted")
            // TODO explain to user why necessary
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Listener for the user's [Location] changing, updating the recorded and displayed location.
     *
     * @param location the new location found, or null.
     */
    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            originLocation = location
            setCameraPosition(location)
        }
    }

    /**
     * Approximates the distance between to latitude/longitude positions cheaply.
     * Sourced from 
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * along with [distPerLat] and [distPerLong].
     * 
     * @param fromLat The latitude of the first point.
     * @param fromLong The longitude of the first point.
     * @param toLat The latitude of the second point.
     * @param toLong The longitude of the second point.
     * @return The approximate distance between the points in meters.
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
     * @param lat The current latitude.
     * @return The approximate length of one latitude at latitude [lat], in metres.
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
     * @param lat The current latitude.
     * @return The approximate length of one longitude at latitude [lat], in metres.
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
     * Collects a desired coin, adding it to the users wallet and updating the map.
     *
     * @param coinId the coin-to-be-removed's id
     */
    private fun collectCoin(coinId: String) {

        // Copy the current combo for thread-safety
        var localComboTimer = comboTimer
        var localComboFactor = comboFactor
        var localComboTimeRemaining = comboTimeRemaining


        if (localComboTimer == null) {
            // There is currently no timer active. Start one!
            Log.d(tag, "[collectCoin] No combo active")
            localComboTimeRemaining = 30000
            localComboTimer = getComboTimerInstance(localComboTimeRemaining)
        } else {
            if (localComboTimeRemaining == null) {
                Log.e(tag, "[collectCoin] Combo timer is non-null but remaining time is")
            } else {
                Log.d(tag, "[collectCoin] Combo found with comboTimer $localComboTimer"
                        + ", time remaining $localComboTimeRemaining and factor $localComboFactor")
                // There's a combo active -- extend it by fifteen seconds!
                localComboTimer.cancel()
                localComboTimeRemaining += 20000
                if (localComboTimeRemaining > 120000) {
                    localComboTimeRemaining = 120000
                }
                localComboTimer = getComboTimerInstance(localComboTimeRemaining)
            }
        }

        val marker: Marker? = coinIdToMarker[coinId]
        val coinProperties: JsonObject? = coinIdToFeature[coinId]?.properties()
        var value : Double? = coinProperties?.get("value")?.asDouble
        val currency : String? = coinProperties?.get("currency")?.asString

        when {
            value == null -> {
                Log.e(tag, "[collectCoin] Coin value is null")
            }
            currency == null -> {
                Log.e(tag, "[collectCoin] Coin currency is null")
            }
            marker == null -> {
                Log.e(tag, "[collectCoin] marker is null")
            }
            else -> {
                // Everything looks good. Check if there's a combo active
                if (localComboFactor == null) {
                    // No combo currently on but one will have be started above.
                    localComboFactor = 1.05
                } else {
                    value *= localComboFactor
                    localComboFactor += 0.025
                }

                // Update the user's wallet on firebase
                updateWallet(coinId, currency, value)
                // Remove the marker. This is safe because even if it fails the marker will simply
                // be added to the map again the next time this activity is started, and so the user
                // will be able to try again.
                // This means we can remove the marker before waiting for the async call to the
                // database to finish, making for a smoother user experience.
                removeMarker(coinId, marker)

                // Start the combo timer we've set up
                comboTimer = localComboTimer
                comboTimer?.start()
                comboFactorText.text = "${String.format("%.1f", (localComboFactor-1)*100)}%"
                comboFactor = localComboFactor
                comboTimerText.text = "$localComboTimeRemaining"
            }
        }
    }

    /**
     * Update the user's wallet on Firestore by adding the newly collected coin.
     *
     * @param coinId the coin's id
     * @param currency the coin's currency
     * @param value the coin's value
     */
    private fun updateWallet(coinId: String, currency: String, value: Double) {

        val coin = Coin(coinId, currency, value)
        val roundedValue: String = String.format("%.2f", value)
        val coinJsonString = try {
            coin.toJSON().toString()
        } catch (e: JSONException) {
            Log.e(tag, "[updateWallet] Encountered JSON exception: $e")
            null
        }

        if (coinJsonString == null) {
            Log.e(tag, "[updateWallet] Failed to get JSON-string for coin with id $coinId")
            // We don't want to push this to the database. Return early
            return
        }
        // Generate a map of currency|id -> json-string as expected by the database
        val coinMap: Map<String, String> = mapOf("$currency|$coinId" to coinJsonString)

        firestoreWallet?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    // The wallet already exists, add or update this coin's values
                    firestoreWallet?.update(coinMap)?.run {
                        addOnSuccessListener {
                            Log.d(tag,
                                    "[updateWallet] Found wallet, added coin $coinId of " +
                                            "currency $currency with value $value")
                            snackbarLayout.snackbar("Collected $roundedValue $currency")
                        }
                        addOnFailureListener { e ->
                            Log.e(tag, "[updateWallet] Doc exists but update failed: $e")
                        }
                    }
                } else {
                    // Doc doesn't exist, create it
                    Log.d(tag, "[updateWallet] Setting up new doc")
                    firestoreWallet?.set(coin)?.run {
                        addOnSuccessListener {
                            Log.d(tag,"[updateWallet] Created wallet, added coin $coinId of" +
                                    " currency $currency with value $value")
                            snackbarLayout.snackbar("Collected $roundedValue $currency")
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
     * Removes the requested [Marker] from the map.
     *
     * @param marker the marker to be removed
     */
    private fun removeMarker(id: String, marker: Marker) {
        mapboxMap?.removeMarker(marker)
        Log.d(tag, "[removeMarkers] Removed marker of $id")

        // Remove the coin id from the maps as it is no longer needed, and we do not want
        // to check for its location again
        coinIdToFeature.remove(id)
        coinIdToMarker.remove(id)

    }

    /**
     * Handles a [Marker] click event appropriately depending on the mode and distance.
     *
     * @param marker the marker which was clicked
     * @return whether to consume the click event or not
     */
    private fun onMarkerClick(marker: Marker) : Boolean {
        val userLocation = originLocation // copy for thread safety
        val markerPos = marker.position
        if (userLocation == null) {
            toast("Could not find your location")
            return false
        } else {
            val distance = flatEarthDist(userLocation.latitude, markerPos.latitude,
                    userLocation.longitude, markerPos.longitude)
            when {
                marker.title == "BANK" -> {
                    if (distance <= 25.0) {
                        val intent = Intent(this, BankActivity::class.java)
                        intent.putExtra(USER_EMAIL, currentUserEmail)
                        intent.putExtra(EXCHANGE_RATES, rates.toString())
                        startActivity(intent)
                    } else {
                        toast("You're too far away from the bank")
                    }

                    // Either way consume the event as don't want to show a default
                    // pop-up box for the bank
                    return true
                }

                modeIsPickup -> {
                    // Pick up the coin
                    if (distance <= 25.0) {
                        val coinId = markerIdToCoinId[marker.id]
                        if (coinId == null) {
                            Log.e(tag, "[OnMarkerClick] null coin id for ${marker.id}")
                        } else {
                            collectCoin(coinId)
                        }
                    } else {
                        toast("Too far away from coin")
                    }
                    return true
                }
                else -> {
                    return false
                }
            }
        }
    }

    /**
     * Gets a singleton [CountDownTimer] instance with the desired duration and behaviour.
     *
     * @param millisInFuture the millisecond to count down to
     * @return the set up combo timer
     */
    private fun getComboTimerInstance(millisInFuture: Long) : CountDownTimer {

        comboTimeRemaining = millisInFuture

        return object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisRemaining: Long) {
                try {
                   val timerText: Int = (millisRemaining / 1000).toInt()
                   comboTimerText.text = "$timerText"
                } catch (e: NumberFormatException) {
                    Log.e(tag, "[getComboTimerInstance][onTick] Exception: $e")
                }
                comboTimeRemaining = millisRemaining
            }

            override fun onFinish() {
                comboTimerText.text = "No combo active"
                comboFactorText.text = "No combo active"
                comboTimeRemaining = null
                comboTimer = null
                comboFactor = null
            }
        }
    }
}
