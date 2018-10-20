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
import com.mapbox.mapboxsdk.annotations.Icon
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
 * The app's main Activity, handling various aspects of loading and displaying the map,
 * tracking the user's location, marking the coins on the map and picking them up.
 */
class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener,
        OnMapReadyCallback, DownloadCompleteListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var permissionsManager : PermissionsManager
    private lateinit var originLocation : Location
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    private var mapView : MapView? = null
    private var mapboxMap : MapboxMap? = null

    private val TAG = "MainActivity"
    private var currentDate : String? = null // FORMAT YYYY/MM/DD
    private val preferencesFile : String = "CoinzPrefsFile"
    private var lastDownloadDate : String? = null
    private var cachedMap : String? = null

    // Geofencing
    private var geofencingClient : GeofencingClient? = null
    private var geofenceList : ArrayList<Geofence>? = null
    val geofencingPendingIntent : PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Keep track of which coins are within range
    private lateinit var coinsInRange : HashSet<String>
    private lateinit var coinIdToMarker : HashMap<String, Marker>

    /**
     * First set up method called, getting the [Mapbox] instance and requesting the [MapboxMap].
     * Also sets up the necessary fields, onClick listeners and the geofence broadcast manager.
     *
     * @param[savedInstanceState] the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Mapbox.getInstance(this,
                "***REMOVED***")

        coinsInRange = HashSet<String>()
        coinIdToMarker = HashMap<String, Marker>()

        // Set up click event for the button
        collectButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                // Collect all the coins in range
                var numRemovedCoins : Int = 0
                for (id in coinsInRange) {
                    val marker : Marker? = coinIdToMarker.get(id)
                    if (marker != null) {
                        mapboxMap?.removeMarker(marker)
                        Log.d(TAG, "[collectButton.onClick] Removed marker with id $id")
                        numRemovedCoins++
                    } else {
                        Log.e(TAG, "[collectButton.onClick] Could not find marker for id $id")
                    }
                }

                geofencingClient?.removeGeofences(ArrayList(coinsInRange))?.run {
                    addOnSuccessListener {
                        Log.d(TAG, "[collectButton.onClick][removeGeofences] Successful")
                    }

                    addOnFailureListener() {
                        Log.e(TAG, "[collectButton.onClick][removeGeofences] FAILED")
                    }
                }

                // Once done looping over coinsInRange, destroy it
                coinsInRange = HashSet()

                // Update the button text and visibility
                updateCollectButton()

                // Finally let the user know how many coins were collected
                if (numRemovedCoins == 0) {
                    Log.w(TAG, "[collectButton.onClick] numRemoved coins is 0")
                } else if (numRemovedCoins == 1) {
                    toast("Collected one coin")
                } else {
                    toast("Collected $numRemovedCoins coins")
                }
            }
        })

        // Settle up the broadcast managed and receiver to handle geofence transitions being
        // passed back
        val lbm : LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val receiver : BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle the incoming message
                Log.d(TAG, "[onReceive] BroadcastReceiver has received an intent")

                // Get the passed extras
                val ids : ArrayList<String>? = intent?.getStringArrayListExtra(LBM_ID_TAG)
                val type = intent?.getIntExtra(LBM_TYPE_TAG, -1)
                Log.d(TAG, "[onReceive] Type of intent received: $type")

                // Either add or remove coins to set keeping track of what is in range,
                // depending on the type of the transition seen
                if (type == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    if (ids == null) {
                        Log.e(TAG, "[onReceive] GEOFENCE_TRANSITION_ENTER without any IDs")
                    } else {
                        for (id in ids) {
                            Log.d(TAG, "[onReceive] Adding id \"$id\" to coinsInRange")
                            coinsInRange.add(id)
                        }
                    }
                } else if (type == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    if (ids == null) {
                        Log.e(TAG, "[onReceive] GEOFENCE_TRANSITION_EXIT without any IDs")
                    } else {
                        for (id in ids) {
                            Log.d(TAG, "[onReceive] Removing id \"$id\" from coinsInRange")
                            coinsInRange.remove(id)
                        }
                    }
                } else {
                    Log.w(TAG, "[onReceive] Geofence transition type $type not expected")
                }

                updateCollectButton()
            }
        }

        // Register the receiver to the manager
        lbm.registerReceiver(receiver, IntentFilter(LBM_LISTENER))

        // Set up click events for bottom nav bar
        bottom_nav_bar.setOnNavigationItemSelectedListener(this)
        // Set default to home
        bottom_nav_bar.selectedItemId = R.id.home_nav

        geofencingClient = LocationServices.getGeofencingClient(this)
        mapView = findViewById<MapView>(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        enableLocation()
    }

    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.account_nav -> startAccountActivity()
            else -> return true //do nothing
        }

        return true
    }

    private fun startAccountActivity() {
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)

    }

    /**
     * Updates the collectButton text and sets its visibility, as appropriate given the
     * number of nearby coins
     */
    private fun updateCollectButton() {
        val coinsInRangeSize : Int = coinsInRange.size
        if (coinsInRangeSize > 1) {
            collectButton.text = "Collect ${coinsInRange.size} Coins"
            collectButton.visibility = View.VISIBLE
        } else if (coinsInRangeSize == 1) {
            collectButton.text = "Collect one Coin"
            collectButton.visibility = View.VISIBLE
        } else {
            collectButton.visibility = View.GONE
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
        Log.d(TAG, "[downloadComplete] Result: $sneakpeak...")
        addMarkers(result)
    }

    /**
     * Adds the [Marker]s for the coins to the [MapboxMap] being displayed. Also sets up the
     * corresponding [Geofence]s.
     *
     * @param[geoJsonString] the downloaded GeoJSON (as a [String]) which describes the location of the coins
     */
    private fun addMarkers(geoJsonString : String) {
        geofenceList = ArrayList<Geofence>()
        val geofence_radius : Float = 25.toFloat()
        val features = FeatureCollection.fromJson(geoJsonString).features()

        if (features == null) {
            Log.e(TAG, "[downloadComplete] features is null")
        } else if (this.mapboxMap == null) {
            Log.e(TAG, "[downloadComplete] mapboxMap is null, can't add markers")
        } else {
            for (feature in features) {
                val point: Point = feature.geometry() as Point
                val lat: Double = point.coordinates().get(1)
                val long: Double = point.coordinates().get(0)
                val properties: JsonObject? = feature.properties()
                val id: String? = properties?.get("id")?.asString
                val value: String? = properties?.get("value")?.asString
                val currency: String? = properties?.get("currency")?.asString
                //val symbol: String? = properties?.get("marker-symbol")?.asString
                //val colour: String? = properties?.get("marker-color")?.asString

                // Ad id -> location to the hashmap so we can identify markers by id later
                val addedMarker : Marker? = this.mapboxMap?.addMarker(
                                    MarkerOptions()
                                    .title(id)
                                    .snippet("Currency: $currency.\nValue: $value.")
                                    .position(LatLng(lat, long)))
                if (addedMarker != null) {
                    if (id != null) {
                        coinIdToMarker[id] = addedMarker
                    } else {
                        Log.e(TAG, "[addMarkers] successfully added marker but ID was null")
                    }
                } else {
                    Log.e(TAG, "[addMarkers] addedMarker is null; FAILED to add marker?")
                }


                geofenceList?.add(Geofence.Builder()
                        .setRequestId(id)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setCircularRegion(
                                lat,
                                long,
                                geofence_radius)
                        .setTransitionTypes(
                                Geofence.GEOFENCE_TRANSITION_ENTER
                                        or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build())
            }

            val geofencingRequest: GeofencingRequest = GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                addGeofences(geofenceList)
            }.build()

            geofencingClient?.addGeofences(geofencingRequest, geofencingPendingIntent)?.run {
                addOnSuccessListener {
                    Log.d(TAG, "[addGeofences] Sucessfully added the geofences")
                }

                addOnFailureListener {
                    Log.e(TAG, "[addGeofences] FAILED")
                }
            }

        }
    }

    /**
     * Listener function for the async call to receive the [MapboxMap]. Sets up the local
     * MapboxMap instance, and then begins to fetch today's coins if they are not already cached.
     *
     * @param[mapboxMap] the received MapboxMap
     */
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.e(TAG, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true

            initializeLocationEngine()
            initializeLocationLayer()

            // Start download from here to make sure that the mapboxMap isn't null when
            // it's time to add markers
            if (cachedMap == null) {
                Log.d(TAG, "[onMapReady] Downloading coins location map")
                val date_string = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
                DownloadFileTask(this).execute(date_string)
            } else {
                Log.d(TAG, "[onMapReady] Adding markers for cached map")
                addMarkers(cachedMap!!)
            }
        }
    }


    /**
     * Checks if the necessary location permissions have been granted. If so, invokes the
     * methods which initialize the [LocationEngine] and [LocationLayerPlugin].
     * If not, instantiates a [PermissionsManager] and requests the location permissions.
     */
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "[enableLocation] Permissions granted")
            mapView?.getMapAsync(this)
        } else {
            Log.d(TAG, "[enableLocation] Permissions not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    /**
     * Instantiates and sets up the [LocationEngine]
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
     * Instantiates and sets up the [LocationLayerPlugin]
     */
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        if (mapView == null) {
            Log.d(TAG, "[initializeLocationLayer] mapView is null")
        } else if (mapboxMap == null) {
            Log.d(TAG, "[initializeLocationLayer] mapboxMap is null")
        } else {
            locationLayerPlugin = LocationLayerPlugin(mapView!!, mapboxMap!!, locationEngine)
            locationLayerPlugin.apply {
                setLocationLayerEnabled(true)
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.NORMAL
            }
        }
    }

    /**
     * Updates the current camera position for the displayed [MapboxMap]
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
     * Listener for location listener results. If granted, invokes [enableLocation].
     *
     * @param[granted] the truth value of the sentence "the permission was granted"
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            Log.e(TAG, "[onPermissionResult] Permissions not granted")
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
        Log.d(TAG, "[onConnected] Requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    /**
     * Upon activity start, invokes the activity's and [mapView]'s super functions.
     * Calculates [currentDate] and also fetches the preferences stored on the device, notably the
     * [lastDownloadDate] and [cachedMap] so that the coin locations do not need to be downloaded
     * uneccesarily.
     */
    override fun onStart() {
        super.onStart()

        // Default navigation bar item checked should be home
        bottom_nav_bar.selectedItemId = home_nav

        mapView?.onStart()

        // Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        lastDownloadDate = settings.getString("lastDownloadDate", null)
        Log.d(TAG, "[onStart] Fetched lastDownloadDate: $lastDownloadDate")

        // Need to get date in onStart() because app may have been left running overnight
        val year : String = Calendar.getInstance().get(Calendar.YEAR).toString()
        var month : String = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()  // Add one as 0-indexed
        var day : String = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        if (year != "2018" && year != "2019") {
            Log.e(TAG, "Unsupported date")
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
        Log.d(TAG, "[onStart] Today's date: $currentDate")

        if (currentDate == lastDownloadDate) {
            Log.d(TAG, "[onStart] Dates match, fetching cached map")
            cachedMap = settings.getString("cachedMap", null)
            if (cachedMap == null) {
                Log.w(TAG, "[onStart] Dates matched but fetched cachedMap is null!")
            } else {
                Log.d(TAG, "[onStart] Fetched cachedMap: ${cachedMap?.take(25)}")
            }
        }
    }

    /**
     * On activity being resumed, simply makes sure that the super functions are invoked for
     * the activity and [mapView].
     */
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    /**
     * On stop, invokes the correct super functions (activity's and [mapView]'s) and saves the
     * user preferences to the device, namely the (potentially updated) [lastDownloadDate] and
     * [cachedMap].
     */
    override fun onStop() {
        super.onStop()
        mapView?.onStop()

        if (lastDownloadDate == currentDate) {
            Log.d(TAG, "[onStop] Not storing prefs as no change has been made")
        } else {
            // Store preferences
            val settings: SharedPreferences = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = settings.edit()

            Log.d(TAG, "[onStop] Storing lastDownloadDate as currentDate: $currentDate")
            editor.putString("lastDownloadDate", currentDate)

            val sneakpeak: String? = cachedMap?.take(25)
            Log.d(TAG, "[onStop] Storing cachedMap: $sneakpeak...")
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

}

