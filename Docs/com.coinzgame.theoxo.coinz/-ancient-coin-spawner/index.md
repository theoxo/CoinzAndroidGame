[com.coinzgame.theoxo.coinz](../index.md) / [AncientCoinSpawner](.)

# AncientCoinSpawner

`class AncientCoinSpawner : `[`DownloadCompleteListener`](../-download-complete-listener/index.md)

Provides means to set up and listen for alarms triggering ancient coins spawning.
Also provides functionality to download the day's map when such an alarm is triggered,
if it has not already been downloaded.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AncientCoinSpawner()`<br>Provides means to set up and listen for alarms triggering ancient coins spawning.
Also provides functionality to download the day's map when such an alarm is triggered,
if it has not already been downloaded. |

### Functions

| Name | Summary |
|---|---|
| [downloadComplete](download-complete.md) | `fun downloadComplete(result: String): Unit`<br>Listens for the background map download to finish. |
| [onReceive](on-receive.md) | `fun onReceive(context: <ERROR CLASS>?, intent: <ERROR CLASS>?): Unit`<br>Handles the received intent, either setting up the alarms or handling one of them triggering. |
