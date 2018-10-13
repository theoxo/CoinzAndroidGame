package com.coinzgame.theoxo.coinz

import android.app.PendingIntent
import android.content.*
import android.location.Location
import android.os.Bundle
import android.os.ResultReceiver
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
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
import kotlin.collections.HashSet

/**
 * The app's main Activity, handling loading and displaying the map,
 * tracking the user's location, and marking the coins on the map.
 */
class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener,
        OnMapReadyCallback, DownloadCompleteListener {

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

    // Trying to add geofencing
    private var geofencingClient : GeofencingClient? = null
    private var geofenceList : ArrayList<Geofence>? = null
    val geofencingPendingIntent : PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Keep track of which coins are within range
    private lateinit var coinsInRange : HashSet<String>

    /**
     * First set up method called, getting the [Mapbox] instance and requesting the [MapboxMap].
     *
     * @param[savedInstanceState] the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Mapbox.getInstance(this,
                "***REMOVED***")

        coinsInRange = HashSet<String>()

        val lbm : LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val receiver : BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "[onReceive] BroadcastReceiver has received an intent")

                val ids : ArrayList<String>? = intent?.getStringArrayListExtra("ids")
                val type = intent?.getIntExtra("type", -1)
                Log.d(TAG, "[onReceive] type of intent received: $type")
                // TODO make the above change depending on the coin and so on, ultimately to allow
                // for picking the coin up
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
                }
            }
        }

        lbm.registerReceiver(receiver, IntentFilter(LBM_LISTENER))

        geofencingClient = LocationServices.getGeofencingClient(this)
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    /**
     * Listener for the AsyncTask marker map data download having finished.
     * Calls addMarkers to add the markers to the map.
     *
     * @param[result] the downloaded GeoJSON (as a [String]) which describes the location of the coins
     */
    override fun downloadComplete(result: String) {
        cachedMap = result
        lastDownloadDate = currentDate
        val sneakpeak = result.take(25)
        Log.d(TAG, "[downloadComplete]: $sneakpeak...")
        addMarkers(result)
    }

    /**
     * Adds the markers for the coins to the [MapboxMap] being displayed.
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
                val symbol: String? = properties?.get("marker-symbol")?.asString
                val colour: String? = properties?.get("marker-color")?.asString

                val x: Icon? = null
                this.mapboxMap?.addMarker(
                        MarkerOptions()
                                .title(id)
                                .snippet("Currency: $currency.\nValue: $value.")
                                .position(LatLng(lat, long)))


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
     * MapboxMap instance, and then begins to fetch today's coins.
     *
     * @param[mapboxMap] the received MapboxMap
     */
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(TAG, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true
            this.mapboxMap?.uiSettings?.isZoomControlsEnabled = true

            enableLocation()

            // Start download from here to make sure that the mapboxMap isn't null when
            // it's time to add markers

            if (lastDownloadDate != currentDate) {
                Log.d(TAG, "[onMapReady] downloading coins location map")
                val date_string: String = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
                DownloadFileTask(this).execute(date_string)
            } else {
                if (cachedMap == null) {
                    Log.e(TAG, "[onMapReady] cached map is null but last seen download was today")
                } else {
                    Log.d(TAG, "[onMapReady] adding markers for cached map")
                    addMarkers(cachedMap!!)
                }
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
            initializeLocationEngine()
            initializeLocationLayer()
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
            Log.d(TAG, "[onPermissionResult] Permissions not granted")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Listener for the user's [Location] changing, updating the recorded and displayed location
     *
     * @param[location] the new [Location] found, or null
     */
    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            originLocation = location
            setCameraPosition(location)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(TAG, "[onConnected] Requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()

        // Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        lastDownloadDate = settings.getString("lastDownloadDate", null)
        Log.d(TAG, "[onStart] Fetched lastDownloadDate: $lastDownloadDate")
        cachedMap = settings.getString("cachedMap", null)
        Log.d(TAG, "[onStart] Fetched cachedMap: ${cachedMap?.take(25)}")

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
        Log.d(TAG, "[onStart] today's date: $currentDate")

        // Need to get date in onStart() because app may have been left running overnight
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()

        // Store preferences
        val settings : SharedPreferences = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor : SharedPreferences.Editor = settings.edit()

        Log.d(TAG, "[onStop] storing lastDownloadDate: $lastDownloadDate")
        editor.putString("lastDownloadDate", lastDownloadDate)

        val sneakpeak : String? = cachedMap?.take(25)
        Log.d(TAG, "[onStop] storing cachedMap: $sneakpeak...")
        editor.putString("cachedMap", cachedMap)

        editor.apply()

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

    /*
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }*/
}

