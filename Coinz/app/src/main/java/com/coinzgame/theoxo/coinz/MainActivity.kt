package com.coinzgame.theoxo.coinz

import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode

import java.util.*

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

        val features = FeatureCollection.fromJson(geoJsonString).features()

        if (features == null) {
            Log.d(TAG, "[downloadComplete] features is null")
        } else if (this.mapboxMap == null) {
            Log.d(TAG, "[downloadComplete] mapboxMap is null, can't add markers")
        } else {
            for (feature in features) {
                val point = feature.geometry() as Point
                val lat = point.coordinates().get(1)
                val long = point.coordinates().get(0)

                this.mapboxMap?.addMarker(
                        MarkerOptions()
                                .title("Hi")
                                .position(LatLng(lat, long)))
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
            val year = Calendar.getInstance().get(Calendar.YEAR).toString()
            var month = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()  // Add one as 0-indexed
            var day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
            if (year != "2018" && year != "2019") {
                error("Unsupported date")
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
            val date = "$year/$month/$day"
            Log.d(TAG, date)
            val date_string = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"
            DownloadFileTask(this).execute(
                    date_string)
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
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
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

