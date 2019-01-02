[com.coinzgame.theoxo.coinz](../index.md) / [MapFragment](.)

# MapFragment

`class MapFragment : `[`DownloadCompleteListener`](../-download-complete-listener/index.md)

The app's main Activity.
Handles various aspects of loading and displaying the map,
tracking the user's location, marking the coins on the map and picking them up.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MapFragment()`<br>The app's main Activity.
Handles various aspects of loading and displaying the map,
tracking the user's location, marking the coins on the map and picking them up. |

### Properties

| Name | Summary |
|---|---|
| [markerIdToCoin](marker-id-to-coin.md) | `lateinit var markerIdToCoin: MutableMap<Long, `[`Coin`](../-coin/index.md)`>` |
| [rates](rates.md) | `var rates: <ERROR CLASS>?` |

### Functions

| Name | Summary |
|---|---|
| [collectCoinFromMarker](collect-coin-from-marker.md) | `fun collectCoinFromMarker(marker: <ERROR CLASS>): Unit`<br>Fetches the coin corresponding to the marker and collects it, adding it to the wallet. |
| [downloadComplete](download-complete.md) | `fun downloadComplete(result: String): Unit`<br>Listener for the AsyncTask marker map data download having finished.
Begins the process of adding the downloaded coins to the map. |
| [onAttach](on-attach.md) | `fun onAttach(context: <ERROR CLASS>?): Unit`<br>Save the MainActivity which invoked this Fragment. |
| [onConnected](on-connected.md) | `fun onConnected(): Unit`<br>Requests location updates from the [locationEngine](#) upon the activity being connected. |
| [onCreateView](on-create-view.md) | `fun onCreateView(inflater: <ERROR CLASS>, container: <ERROR CLASS>?, savedInstanceState: <ERROR CLASS>?): <ERROR CLASS>?`<br>Inflate the layout corresponding to this fragment. |
| [onDestroy](on-destroy.md) | `fun onDestroy(): Unit` |
| [onLocationChanged](on-location-changed.md) | `fun onLocationChanged(location: <ERROR CLASS>?): Unit`<br>Listener for the user's [Location](#) changing, updating the recorded and displayed location. |
| [onLowMemory](on-low-memory.md) | `fun onLowMemory(): Unit` |
| [onMapReady](on-map-ready.md) | `fun onMapReady(mapboxMap: <ERROR CLASS>?): Unit`<br>Listener function for the async call to receive the [MapboxMap](#).
Sets up the local [MapboxMap](#) instance ([mapboxMap](on-map-ready.md#com.coinzgame.theoxo.coinz.MapFragment$onMapReady()/mapboxMap)), and then begins to fetch today's coins
if they are not already cached.
Also invokes [initializeLocationLayer](#) and [initializeLocationEngine](#) which initialize
the location tracking. |
| [onResume](on-resume.md) | `fun onResume(): Unit` |
| [onSaveInstanceState](on-save-instance-state.md) | `fun onSaveInstanceState(outState: <ERROR CLASS>): Unit` |
| [onStart](on-start.md) | `fun onStart(): Unit`<br>Fetches the preferences stored on the device if necessary. |
| [onStop](on-stop.md) | `fun onStop(): Unit` |
| [onViewCreated](on-view-created.md) | `fun onViewCreated(view: <ERROR CLASS>, savedInstanceState: <ERROR CLASS>?): Unit`<br>Sets up the local fields and button click events. Starts loading the MapView. |
| [startBank](start-bank.md) | `fun startBank(): Unit` |
