package com.coinzgame.theoxo.coinz

import android.content.*
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
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
import kotlinx.android.synthetic.main.fragment_map.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.HashMap

/**
 * The app's main Activity.
 * Handles various aspects of loading and displaying the map,
 * tracking the user's location, marking the coins on the map and picking them up.
 */
class MapFragment : Fragment(), OnMapReadyCallback, LocationEngineListener,
        DownloadCompleteListener {

    private val fragTag = "MapFragment"

    // Local variables related to the location tracking and displaying
    private var originLocation : Location? = null
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Map variables
    private var mapView : MapView? = null
    private var mapboxMap : MapboxMap? = null
    private val bankLocation : LatLng = LatLng(BANK_MARKER_LATITUDE, BANK_MARKER_LONGITUDE)

    // Keep track of data related to the coins
    private lateinit var markerIdToCoin : MutableMap<Long, Coin>

    // Today's rates
    private var rates : JSONObject? = null

    // Combo bonus feature objects
    private var comboTimer: CountDownTimer? = null
    private var comboTimeRemaining: Long? = null
    private var comboFactor: Double? = null
    private var mainActivity: MainActivity? = null

    private var modeIsPickup: Boolean = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainActivity = context as? MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "Fragment created")
        return inflater.inflate(R.layout.fragment_map, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        fab_inspect.setOnClickListener { _ -> switchMode() }
        fab_pickup.setOnClickListener {_ -> switchMode() }

        markerIdToCoin = HashMap()

        mapView?.getMapAsync(this)

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


    /**
     * Listener for the AsyncTask marker map data download having finished.
     * Begins the process of adding the downloaded coins to the map.
     *
     * @param result the downloaded GeoJSON which describes today's coins
     */
    override fun downloadComplete(result: String) {
        val sneakpeak = result.take(25)
        Log.d(fragTag, "Fragment [downloadComplete] Result: $sneakpeak...")

        if (result == NETWORK_ERROR) {
            mainActivity?.toast(NETWORK_ERROR)
            mainActivity?.finish()
        } else {
            mainActivity?.cachedMap = result  // store the cachedMap so we can save it onStop
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
        Log.d(fragTag, "addMarkers invoked")

        // First of all ensure we have a non-null copy of the activity we are attached to
        // as this will be needed to access firestore data etc
        val mainActivityCp = mainActivity // copy field for thread safety
        if (mainActivityCp == null) {
            Log.w(fragTag, "[addMarkers] mainActivity is null, returning early")
            return
        }

        val features : MutableList<Feature>? = FeatureCollection.fromJson(geoJsonString).features()
        // Also add any and all currently active ancient coins
        features?.addAll(mainActivityCp.ancientCoins)

        rates = JSONObject(geoJsonString).get("rates") as? JSONObject
        Log.d(fragTag, "Rates: $rates")
        when {
            features == null -> {
                Log.e(fragTag, "[addMarkers] features is null")
            }

            mapboxMap == null -> {
                Log.e(fragTag, "[addMarkers] mapboxMap is null, can't add markers")
            }

            else -> {

                // Features are non-null and mapboxMap is too. Can safely loop over the features,
                // adding the markers to the map as we go along.

                val iconFactory: IconFactory = IconFactory.getInstance(mainActivityCp)
                val coinIconFactory = CoinIconFactory(iconFactory)

                // First, get snapshot of user wallet as it is
                mainActivityCp.firestoreWallet?.get()?.run {
                    addOnSuccessListener { docSnapshot ->
                        Log.d(fragTag, "MapFragment wallet get succ")
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
                                    Log.e(fragTag, "[addMarkers] id of feature is null")
                                }
                                currency == null -> {
                                    Log.e(fragTag, "[addMarkers] currency of feature is null")
                                }
                                value == null -> {
                                    Log.e(fragTag, "[addMarkers] value of feature is null")
                                }
                                docSnapshot["$currency|$id"] != null -> {
                                    // Coin has already been collected by the user. skip it
                                }
                                else -> {

                                    val valueDouble: Double = try {
                                        value.toDouble()
                                    } catch (e: NumberFormatException) {
                                        Log.d(fragTag, "[addMarkers] Casting value to double "
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
                                        // Add marker ID -> Coin so we can recognize which
                                        // coin is being collected later
                                        markerIdToCoin[addedMarker.id] = Coin(id, currency,
                                                valueDouble)
                                    } else {
                                        Log.e(fragTag, "[addMarkers] Failed to add marker")
                                    }
                                }
                            }
                        }
                    }
                }


                val bankIcon: Icon = iconFactory.fromResource(R.mipmap.bank_icon)

                // Also want to add a special marker for the bank
                val bank: Marker? = mapboxMap?.addMarker(
                        MarkerOptions()
                                .title(BANK_MARKER_TITLE)
                                .snippet("The bank!")
                                .position(bankLocation)
                                .icon(bankIcon)
                )

                if (bank == null) {
                    Log.e(fragTag, "[addMarkers] bank marker is null")
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
            Log.e(fragTag, "[onMapReady] mapboxMap is null")
        } else {
            this.mapboxMap = mapboxMap
            this.mapboxMap?.uiSettings?.isCompassEnabled = true
            this.mapboxMap?.setOnMarkerClickListener { marker ->
               onMarkerClick(marker)
            }

            initializeLocationEngine()
            initializeLocationLayer()

            // Copy cachedMap for thread safety
            val localCachedMap: String? = mainActivity?.cachedMap
            // Start download from here to make sure that the mapboxMap isn't null when
            // it's time to add markers

            if (localCachedMap == null) {
                // Will need to download the map first before adding the markers.
                Log.d(fragTag, "[onMapReady] Downloading coins location map")
                val dateString = "http://homepages.inf.ed.ac.uk/stg/coinz/" +
                        "${mainActivity?.currentDate}/coinzmap.geojson"
                Log.d(fragTag, "Fragment downloading from $dateString" )
                DownloadFileTask(this).execute(dateString)
            } else {
                Log.d(fragTag, "[onMapReady] Adding markers for cached map")
                addMarkers(localCachedMap)
            }
        }
    }/*


    /**
     * Instantiates and sets up the [locationEngine].
     */*/
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(mainActivity).obtainBestLocationEngineAvailable()
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
                Log.d(fragTag, "[initializeLocationLayer] mapView is null")
            }

            localMapBoxMap == null -> {
                Log.d(fragTag, "[initializeLocationLayer] mapboxMap is null")
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
    }/*
*/
    /**
     * Updates the current camera position.
     *
     * @param[location] the new location to focus the camera on.
     */
    private fun setCameraPosition(location : Location) {
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLng(
                LatLng(location.latitude, location.longitude)))
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
        Log.d(fragTag, "[onConnected] Requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    /**
     * Fetches the coin corresponding to the marker and collects it, adding it to the wallet.
     *
     * @param marker the marker which corresponds to the coin
     */
    private fun collectCoinFromMarker(marker: Marker) {

        // Copy the current combo for thread-safety
        var localComboTimer = comboTimer
        var localComboFactor = comboFactor
        var localComboTimeRemaining = comboTimeRemaining

        // Get and update the current timer, or start a new one if there isn't one active
        if (localComboTimer == null) {
            // There is currently no timer active. Start one!
            Log.d(fragTag, "[collectCoinFromMarker] No combo active")
            localComboTimeRemaining = 30000
            localComboTimer = getComboTimerInstance(localComboTimeRemaining)
        } else {
            if (localComboTimeRemaining == null) {
                Log.e(fragTag, "[collectCoinFromMarker] Combo timer is non-null but" +
                        "remaining time is")
            } else {
                Log.d(fragTag, "[collectCoinFromMarker] Combo found with comboTimer " +
                        "$localComboTimer, time remaining $localComboTimeRemaining and factor " +
                        "$localComboFactor")
                // There's a combo active -- extend it!
                localComboTimer.cancel()
                localComboTimeRemaining += 20000
                if (localComboTimeRemaining > 120000) {
                    // Cap combo lengths to 120 secs
                    localComboTimeRemaining = 120000
                }
                localComboTimer = getComboTimerInstance(localComboTimeRemaining)
            }
        }

        // Attempt to fetch the coin
        var value: Double? = markerIdToCoin[marker.id]?.value
        val currency: String? = markerIdToCoin[marker.id]?.currency
        val coinId: String? = markerIdToCoin[marker.id]?.id

        when {
            value == null -> {
                Log.e(fragTag, "[collectCoinFromMarker] Coin value is null")
            }
            currency == null -> {
                Log.e(fragTag, "[collectCoinFromMarker] Coin currency is null")
            }
            coinId == null -> {
                Log.e(fragTag, "[collectCoinFromMarker] Coin id is null")
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
                removeMarker(marker)

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
            Log.e(fragTag, "[updateWallet] Encountered JSON exception: $e")
            null
        }

        if (coinJsonString == null) {
            Log.e(fragTag, "[updateWallet] Failed to get JSON-string for coin with id $coinId")
            // We don't want to push this to the database. Return early
            return
        }
        // Generate a map of currency|id -> json-string as expected by the database
        val coinMap: Map<String, String> = mapOf("$currency|$coinId" to coinJsonString)

        mainActivity?.firestoreWallet?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    // The wallet already exists, add or update this coin's values
                    mainActivity?.firestoreWallet?.update(coinMap)?.run {
                        addOnSuccessListener {
                            Log.d(fragTag,
                                    "[updateWallet] Found wallet, added coin $coinId of " +
                                            "currency $currency with value $value")
                            snackbarLayout.snackbar("Collected $roundedValue $currency")
                        }
                        addOnFailureListener { e ->
                            Log.e(fragTag, "[updateWallet] Doc exists but update failed: $e")
                        }
                    }
                } else {
                    // Doc doesn't exist, create it
                    Log.d(fragTag, "[updateWallet] Setting up new doc")
                    mainActivity?.firestoreWallet?.set(coin)?.run {
                        addOnSuccessListener {
                            Log.d(fragTag,"[updateWallet] Created wallet, added coin " +
                                    "$coinId of currency $currency with value $value")
                            snackbarLayout.snackbar("Collected $roundedValue $currency")
                        }
                        addOnFailureListener { e ->
                            Log.e(fragTag, "[updateWallet] Failed to create doc: $e")
                        }
                    }
                }
            }
            addOnFailureListener { e ->
                Log.e(fragTag, "[collectButton] Wallet get failed: $e")
            }
        }
    }

    /**
     * Removes the requested [Marker] from the map.
     *
     * @param marker the marker to be removed
     */
    private fun removeMarker(marker: Marker) {
        Log.d(fragTag, "[removeMarkers] Removing marker ${marker.id}")
        mapboxMap?.removeMarker(marker)
        markerIdToCoin.remove(marker.id)

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
            mainActivity?.toast("Could not find your location")
            return false
        } else {
            val distance = flatEarthDist(userLocation.latitude, markerPos.latitude,
                    userLocation.longitude, markerPos.longitude)
            when {
                marker.title == BANK_MARKER_TITLE -> {
                    if (distance <= 25.0) {
                        val intent = Intent(mainActivity, BankActivity::class.java)
                        intent.putExtra(USER_EMAIL, mainActivity?.currentUserEmail)
                        intent.putExtra(EXCHANGE_RATES, rates.toString())
                        startActivity(intent)
                    } else {
                        mainActivity?.toast("You're too far away from the bank")
                    }

                    // Either way consume the event as don't want to show a default
                    // pop-up box for the bank
                    return true
                }

                modeIsPickup -> {
                    // Pick up the coin
                    if (distance <= 25.0) {
                        collectCoinFromMarker(marker)
                    } else {
                        mainActivity?.toast("Too far away from coin")
                    }
                    return true
                }

                else -> {
                    // Otherwise the user is interacting with a coin in inspection mode.
                    // Simply return false to let Mapbox know that it should display
                    // the default info box for the marker
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
                    Log.e(fragTag, "[getComboTimerInstance][onTick] Exception: $e")
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
