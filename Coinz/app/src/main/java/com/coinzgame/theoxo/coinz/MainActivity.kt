package com.coinzgame.theoxo.coinz

import android.app.PendingIntent
import android.content.*
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.coinzgame.theoxo.coinz.R.id.home_nav
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.*
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
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

import java.util.*
import kotlin.collections.ArrayList
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

    // Downloaded data tracking
    private var currentDate : String? = null // FORMAT YYYY/MM/DD
    private val preferencesFile : String = "CoinzPrefsFile"
    private var lastDownloadDate : String? = null
    private var cachedMap : String? = null

    // Geofencing
    private var geofencingClient : GeofencingClient? = null
    private var geofenceList : ArrayList<Geofence>? = null
    private val geofencingPendingIntent : PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Keep track of which coins are within range
    private lateinit var coinsInRange : MutableSet<String>
    private lateinit var coinIdToMarker : MutableMap<String, Marker>
    private lateinit var coinIdToJSON : MutableMap<String, JsonObject>

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var currentUserEmail : String? = null

    /**
     * Initializes the necessary instances and event handlers upon activity creation.
     * Gets the [MapView] instance and the [GeofencingClient]
     * instance. Initializes the necessary fields, and invokes [collectNearbyCoins] and
     * [setUpLocalBroadCastManager] to set up the click events and [BroadcastReceiver] for
     * [Geofence] events. Finally, invokes [enableLocation].
     *
     * @param[savedInstanceState] the previously saved instance state, if it exists
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)
        Log.d(tag, "[onCreate] Received user email $currentUserEmail")

        Mapbox.getInstance(this,
                "***REMOVED***")

        coinsInRange = HashSet()
        coinIdToMarker = HashMap()
        coinIdToJSON = HashMap()

        // Set up the click event for the button which allows the user to collect the coins
        collectButton.setOnClickListener { _ -> collectNearbyCoins() }

        // Set up the broadcast manager which listens for Geofence events
        setUpLocalBroadCastManager()

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
            Log.e(tag, "[onCreate] Running with null user email")
        } else {
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)

            geofencingClient = LocationServices.getGeofencingClient(this)
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
     * Adds the [Marker]s for the coins to the [MapboxMap] being displayed and sets up [Geofence]s.
     * First check that the coin being added isn't already in the user's wallet (meaning it has
     * already been collected).
     *
     * @param[geoJsonString] the downloaded GeoJSON (as a [String]) which describes the location of the coins
     */
    private fun addMarkers(geoJsonString : String) {
        geofenceList = ArrayList()
        val geofenceRadius: Float = 25.toFloat()  // TODO unhack this maybe
        val features = FeatureCollection.fromJson(geoJsonString).features()

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
                            val lat: Double = point.coordinates()[1]
                            val long: Double = point.coordinates()[0]
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
                                        // Add the marker
                                        val addedMarker: Marker? = mapboxMap?.addMarker(
                                                MarkerOptions()
                                                        .title("~${value?.substringBefore('.')} $currency.")
                                                        .snippet("Currency: $currency.\nValue: $value.")
                                                        .position(LatLng(lat, long)))

                                        if (addedMarker != null) {
                                            // Ad id -> location to the hashmap so we can identify markers by
                                            // id later
                                            coinIdToMarker[id] = addedMarker
                                            coinIdToJSON[id] = properties
                                        } else {
                                            Log.e(tag, "[addMarkers] Failed to add marker")
                                        }

                                        // Add the corresponding Geofence.
                                        geofenceList?.add(Geofence.Builder()
                                                .setRequestId(id)
                                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                                .setCircularRegion(
                                                        lat,
                                                        long,
                                                        geofenceRadius)
                                                .setTransitionTypes(
                                                        Geofence.GEOFENCE_TRANSITION_ENTER
                                                                or Geofence.GEOFENCE_TRANSITION_EXIT)
                                                .build())
                                    }
                                }
                            }
                        }

                        // Have added all the geofences and markers, now add listener events for them
                        val geofencingRequest: GeofencingRequest = GeofencingRequest.Builder().apply {
                            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                            addGeofences(geofenceList)
                        }.build()

                        geofencingClient?.addGeofences(geofencingRequest, geofencingPendingIntent)?.run {
                            addOnSuccessListener {
                                Log.d(tag, "[addGeofences] Sucessfully added the geofences")
                            }

                            addOnFailureListener {
                                Log.e(tag, "[addGeofences] FAILED")
                            }
                        }
                    }
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
     * @param mapboxMap the received [MapboxMap]
     */
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.e(tag, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true

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
     *
     * @param[location] the new [Location] found, or null
     */
    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            originLocation = location
            setCameraPosition(location)
        }
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
     * Collects all the nearby coins by invoking [collectCoin] on them.
     */
    private fun collectNearbyCoins() {
        for (id in coinsInRange) {
            val marker: Marker? = coinIdToMarker[id]
            val coinProperties: JsonObject? = coinIdToJSON[id]

            when {
                coinProperties == null -> {
                    Log.e(tag, "[collectNearbyCoins] coinProperties is null")
                }
                marker == null -> {
                    Log.e(tag, "[collectNearbyCoins] marker is null")
                }
                else -> {
                    collectCoin(coinProperties, id, marker)
                }
            }
        }
    }

    /**
     * Collect a given coin, adding it to the user's wallet on Firestore.
     * Then invokes [updateCollectButton] and [removeMarkerAndGeofence].
     *
     * @param coinProperties The properties of the given coin as as a [JsonObject]
     * @param id The coin's id (a [String])
     * @param marker The [Marker] corresponding to the coin
     */
    private fun collectCoin(coinProperties : JsonObject, id : String, marker : Marker) {
        val value: String? = coinProperties.get("value")?.asString
        val currency: String? = coinProperties.get("currency")?.asString
        when {
            value == null -> Log.e(tag,"[collectCoin] Value of coin is null")
            currency == null -> Log.e(tag,
                    "[collectCoin] Currency of coin is null")
            else -> {
                firestoreWallet?.get()?.run {
                    addOnSuccessListener { docSnapshot ->
                        val valueToSet = mapOf("$currency|$id" to value)
                        if (docSnapshot.exists()) {
                            // Doc exists, update values
                            firestoreWallet?.update(valueToSet)?.run {
                                        addOnSuccessListener {
                                            Log.d(tag,
                                                    "[collectCoin] Added coin $id of "
                                                            + "currency $currency with value $value")
                                            toast("Collected $value $currency")
                                            coinsInRange.remove(id)
                                            updateCollectButton()
                                            removeMarkerAndGeofence(id, marker)
                                        }

                                        addOnFailureListener { e ->
                                            Log.e(tag, "[collectCoin] Doc exists but update failed: $e")
                                        }
                                    }
                        } else {
                            // Doc doesn't exist, create it
                            Log.d(tag, "[collectCoin] Setting up new doc")
                            firestoreWallet?.set(valueToSet)?.run {
                                        addOnSuccessListener {
                                            Log.d(tag,
                                                    "[collectCoin] Added coin $id of currency "
                                                            + "$currency with value $value to user's freshly created wallet")
                                            toast("Collected $value $currency")
                                            coinsInRange.remove(id)
                                            updateCollectButton()
                                            removeMarkerAndGeofence(id, marker)
                                        }

                                        addOnFailureListener { e ->
                                            Log.e(tag, "[collectCoin] Failed to create doc $e")
                                        }
                                    }
                        }
                    }

                    addOnFailureListener { e ->
                        Log.e(tag, "[collectButton] Wallet get failed: $e")
                    }
                }
            }
        }
    }

    /**
     * Removes a [Marker] and its corresponding [Geofence].
     *
     * @param id the ID of the Coin whose marker is being removed ([String])
     * @param marker the [Marker] to remove
     */
    private fun removeMarkerAndGeofence(id : String, marker : Marker) {
        geofencingClient?.removeGeofences(arrayListOf(id))?.run {
            addOnSuccessListener {
                mapboxMap?.removeMarker(marker)
                Log.d(tag, "[removeMarkerAndGeofence] Successfully removed $id")
            }

            addOnFailureListener { err ->
                Log.e(tag, "[collectButton.onClick][removeGeofences] FAILED: $err")
            }
        }
    }

    /**
     * Sets up the [LocalBroadcastManager] which listens for [Geofence] events.
     * This is so that the main thread can be notified about the type of [Geofence] transitions
     * which occurred, adding and removing coins from [coinsInRange].
     */
    private fun setUpLocalBroadCastManager() {
        // Settle up the broadcast managed and receiver to handle geofence transitions being
        // passed back
        val lbm : LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val receiver : BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle the incoming message
                Log.d(tag, "[onReceive] BroadcastReceiver has received an intent")

                // Get the passed extras
                val ids : ArrayList<String>? = intent?.getStringArrayListExtra(LBM_ID_TAG)
                val type = intent?.getIntExtra(LBM_TYPE_TAG, -1)
                Log.d(tag, "[onReceive] Type of intent received: $type")

                // Either add or remove coins to set keeping track of what is in range,
                // depending on the type of the transition seen
                if (type == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    if (ids == null) {
                        Log.e(tag, "[onReceive] GEOFENCE_TRANSITION_ENTER without any IDs")
                    } else {
                        for (id in ids) {
                            Log.d(tag, "[onReceive] Adding id \"$id\" to coinsInRange")
                            coinsInRange.add(id)
                        }
                    }
                } else if (type == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    if (ids == null) {
                        Log.e(tag, "[onReceive] GEOFENCE_TRANSITION_EXIT without any IDs")
                    } else {
                        for (id in ids) {
                            Log.d(tag, "[onReceive] Removing id \"$id\" from coinsInRange")
                            coinsInRange.remove(id)
                        }
                    }
                } else {
                    Log.w(tag, "[onReceive] Geofence transition type $type not expected")
                }

                updateCollectButton()
            }
        }

        // Register the receiver to the manager
        lbm.registerReceiver(receiver, IntentFilter(LBM_LISTENER))
    }

}

