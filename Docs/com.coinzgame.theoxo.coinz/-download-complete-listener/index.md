[com.coinzgame.theoxo.coinz](../index.md) / [DownloadCompleteListener](.)

# DownloadCompleteListener

`interface DownloadCompleteListener : Any`

Simple interface which defines the [downloadComplete](download-complete.md) function.
This allows the downloaded coin locations to be passed back to the caller.

### Functions

| Name | Summary |
|---|---|
| [downloadComplete](download-complete.md) | `abstract fun downloadComplete(result: String): Unit`<br>Handles the download having finished, dealing with the result appropriately. |

### Inheritors

| Name | Summary |
|---|---|
| [AncientCoinSpawner](../-ancient-coin-spawner/index.md) | `class AncientCoinSpawner : DownloadCompleteListener`<br>Provides means to set up and listen for alarms triggering ancient coins spawning.
Also provides functionality to download the day's map when such an alarm is triggered,
if it has not already been downloaded. |
| [MapFragment](../-map-fragment/index.md) | `class MapFragment : DownloadCompleteListener`<br>The app's main Activity.
Handles various aspects of loading and displaying the map,
tracking the user's location, marking the coins on the map and picking them up. |
