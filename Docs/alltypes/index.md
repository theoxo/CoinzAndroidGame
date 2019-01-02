

### All Types

| Name | Summary |
|---|---|
| [com.coinzgame.theoxo.coinz.AccountFragment](../com.coinzgame.theoxo.coinz/-account-fragment/index.md) | A simple fragment allowing the user access to account-specific features such as signing out. |
| [com.coinzgame.theoxo.coinz.AncientCoinSpawner](../com.coinzgame.theoxo.coinz/-ancient-coin-spawner/index.md) | Provides means to set up and listen for alarms triggering ancient coins spawning.
Also provides functionality to download the day's map when such an alarm is triggered,
if it has not already been downloaded. |
| [com.coinzgame.theoxo.coinz.BankActivity](../com.coinzgame.theoxo.coinz/-bank-activity/index.md) | The screen which allows the user to deposit their coins into the bank.
Pops up when the user engages with the bank. |
| [com.coinzgame.theoxo.coinz.BankCoinFromWalletTest](../com.coinzgame.theoxo.coinz/-bank-coin-from-wallet-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.BankCoinsFromInboxTest](../com.coinzgame.theoxo.coinz/-bank-coins-from-inbox-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.BankCoinsFromWalletThenInboxTest](../com.coinzgame.theoxo.coinz/-bank-coins-from-wallet-then-inbox-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.BankMoreCoinsThanIsAllowedTest](../com.coinzgame.theoxo.coinz/-bank-more-coins-than-is-allowed-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.Coin](../com.coinzgame.theoxo.coinz/-coin/index.md) | Represents a Coin in the game by specifying its parameters. |
| [com.coinzgame.theoxo.coinz.CoinAdapter](../com.coinzgame.theoxo.coinz/-coin-adapter/index.md) | Provides a specialized [ArrayAdapter](#) to dynamically list [Coin](../com.coinzgame.theoxo.coinz/-coin/index.md)s in a list view. |
| [com.coinzgame.theoxo.coinz.CoinClassInstrumentedUnitTest](../com.coinzgame.theoxo.coinz/-coin-class-instrumented-unit-test/index.md) | Tests whether the [Coin](../com.coinzgame.theoxo.coinz/-coin/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |
| [com.coinzgame.theoxo.coinz.CoinIconFactory](../com.coinzgame.theoxo.coinz/-coin-icon-factory/index.md) | Provides means to generate appropriate [Icon](#)s for coins. |
| [com.coinzgame.theoxo.coinz.CollectCoinTest](../com.coinzgame.theoxo.coinz/-collect-coin-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.DownloadCompleteListener](../com.coinzgame.theoxo.coinz/-download-complete-listener/index.md) | Simple interface which defines the [downloadComplete](../com.coinzgame.theoxo.coinz/-download-complete-listener/download-complete.md) function.
This allows the downloaded coin locations to be passed back to the caller. |
| [com.coinzgame.theoxo.coinz.DownloadFileTask](../com.coinzgame.theoxo.coinz/-download-file-task/index.md) | [AsyncTask](#) which downloads a file from a remote server using a [HttpURLConnection](http://docs.oracle.com/javase/6/docs/api/java/net/HttpURLConnection.html).
In this case, used to download the coin locations.
Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete](../com.coinzgame.theoxo.coinz/-download-complete-listener/download-complete.md) method,
passing the downloaded data as a string. |
| [com.coinzgame.theoxo.coinz.InboxFragment](../com.coinzgame.theoxo.coinz/-inbox-fragment/index.md) | A fragment which shows the user their current inbox and allows them to craft new messages.
Crafting new messages starts a [MessageCreationActivity](../com.coinzgame.theoxo.coinz/-message-creation-activity/index.md). |
| [com.coinzgame.theoxo.coinz.LoginActivity](../com.coinzgame.theoxo.coinz/-login-activity/index.md) | A login screen that offers login via email/password, authenticating via Firebase. |
| [com.coinzgame.theoxo.coinz.LoginAndLogoutTest](../com.coinzgame.theoxo.coinz/-login-and-logout-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.MainActivity](../com.coinzgame.theoxo.coinz/-main-activity/index.md) | The app's main Activity.
Handles switching between and containing the three main fragments,
[MapFragment](../com.coinzgame.theoxo.coinz/-map-fragment/index.md), [InboxFragment](../com.coinzgame.theoxo.coinz/-inbox-fragment/index.md), and [AccountFragment](../com.coinzgame.theoxo.coinz/-account-fragment/index.md).
Also stores references to the user's email and firestore documents
for easy access by the fragments. |
| [com.coinzgame.theoxo.coinz.MapFragment](../com.coinzgame.theoxo.coinz/-map-fragment/index.md) | The app's main Activity.
Handles various aspects of loading and displaying the map,
tracking the user's location, marking the coins on the map and picking them up. |
| [com.coinzgame.theoxo.coinz.Message](../com.coinzgame.theoxo.coinz/-message/index.md) | A class representing messages sent between users. |
| [com.coinzgame.theoxo.coinz.MessageAdapter](../com.coinzgame.theoxo.coinz/-message-adapter/index.md) | Provides a specialized [ArrayAdapter](#) to dynamically list [Message](../com.coinzgame.theoxo.coinz/-message/index.md)s in a list view. |
| [com.coinzgame.theoxo.coinz.MessageClassInstrumentedUnitTest](../com.coinzgame.theoxo.coinz/-message-class-instrumented-unit-test/index.md) | Tests whether the [Message](../com.coinzgame.theoxo.coinz/-message/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |
| [com.coinzgame.theoxo.coinz.MessageCreationActivity](../com.coinzgame.theoxo.coinz/-message-creation-activity/index.md) | A pop-up screen which allows the user to craft and send a new [Message](../com.coinzgame.theoxo.coinz/-message/index.md). |
| [com.coinzgame.theoxo.coinz.MessageViewActivity](../com.coinzgame.theoxo.coinz/-message-view-activity/index.md) | A pop-up screen allowing the user to view a message in its inbox. |
| [com.coinzgame.theoxo.coinz.NewMessageAvailableTest](../com.coinzgame.theoxo.coinz/-new-message-available-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.SendAndReceiveCoinsTest](../com.coinzgame.theoxo.coinz/-send-and-receive-coins-test/index.md) |  |
| [com.coinzgame.theoxo.coinz.SendAndViewMessageTest](../com.coinzgame.theoxo.coinz/-send-and-view-message-test/index.md) |  |
