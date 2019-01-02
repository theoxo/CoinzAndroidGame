[com.coinzgame.theoxo.coinz](../index.md) / [MainActivity](.)

# MainActivity

`class MainActivity : Any`

The app's main Activity.
Handles switching between and containing the three main fragments,
[MapFragment](../-map-fragment/index.md), [InboxFragment](../-inbox-fragment/index.md), and [AccountFragment](../-account-fragment/index.md).
Also stores references to the user's email and firestore documents
for easy access by the fragments.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MainActivity()`<br>The app's main Activity.
Handles switching between and containing the three main fragments,
[MapFragment](../-map-fragment/index.md), [InboxFragment](../-inbox-fragment/index.md), and [AccountFragment](../-account-fragment/index.md).
Also stores references to the user's email and firestore documents
for easy access by the fragments. |

### Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: <ERROR CLASS>?): Unit`<br>Sets up the local Firestore references and greets the user if they are new.
Also sets up the Mapbox instance used for [MapFragment](../-map-fragment/index.md). |
| [onExplanationNeeded](on-explanation-needed.md) | `fun onExplanationNeeded(permissionsToExplain: MutableList<String>?): Unit`<br>Presents a dialog explaining to the user why the location permission is needed. |
| [onNavigationItemSelected](on-navigation-item-selected.md) | `fun onNavigationItemSelected(item: <ERROR CLASS>): Boolean`<br>Handles user clicks on the [BottomNavigationView](#), starting the corresponding activities. |
| [onPermissionResult](on-permission-result.md) | `fun onPermissionResult(granted: Boolean): Unit`<br>Listener for location permission results.
If granted, invokes sets up [mapFragment](#). |
| [onRequestPermissionsResult](on-request-permissions-result.md) | `fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Unit` |
| [onStart](on-start.md) | `fun onStart(): Unit`<br>Fetches the preferences stored on the device and invokes [checkLocationPermission](#). |
