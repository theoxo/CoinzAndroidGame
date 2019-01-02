[com.coinzgame.theoxo.coinz](.)

## Package com.coinzgame.theoxo.coinz

### Types

| Name | Summary |
|---|---|
| [AccountFragment](-account-fragment/index.md) | `class AccountFragment : Any`<br>A simple fragment allowing the user access to account-specific features such as signing out. |
| [AncientCoinSpawner](-ancient-coin-spawner/index.md) | `class AncientCoinSpawner : `[`DownloadCompleteListener`](-download-complete-listener/index.md)<br>Provides means to set up and listen for alarms triggering ancient coins spawning.
Also provides functionality to download the day's map when such an alarm is triggered,
if it has not already been downloaded. |
| [BankActivity](-bank-activity/index.md) | `class BankActivity : Any`<br>The screen which allows the user to deposit their coins into the bank.
Pops up when the user engages with the bank. |
| [BankCoinFromWalletTest](-bank-coin-from-wallet-test/index.md) | `class BankCoinFromWalletTest : Any` |
| [BankCoinsFromInboxTest](-bank-coins-from-inbox-test/index.md) | `class BankCoinsFromInboxTest : Any` |
| [BankCoinsFromWalletThenInboxTest](-bank-coins-from-wallet-then-inbox-test/index.md) | `class BankCoinsFromWalletThenInboxTest : Any` |
| [BankMoreCoinsThanIsAllowedTest](-bank-more-coins-than-is-allowed-test/index.md) | `class BankMoreCoinsThanIsAllowedTest : Any` |
| [Coin](-coin/index.md) | `class Coin : Any`<br>Represents a Coin in the game by specifying its parameters. |
| [CoinAdapter](-coin-adapter/index.md) | `class CoinAdapter : Any`<br>Provides a specialized [ArrayAdapter](#) to dynamically list [Coin](-coin/index.md)s in a list view. |
| [CoinClassInstrumentedUnitTest](-coin-class-instrumented-unit-test/index.md) | `class CoinClassInstrumentedUnitTest : Any`<br>Tests whether the [Coin](-coin/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |
| [CoinIconFactory](-coin-icon-factory/index.md) | `class CoinIconFactory : Any`<br>Provides means to generate appropriate [Icon](#)s for coins. |
| [CollectCoinTest](-collect-coin-test/index.md) | `class CollectCoinTest : Any` |
| [DownloadCompleteListener](-download-complete-listener/index.md) | `interface DownloadCompleteListener : Any`<br>Simple interface which defines the [downloadComplete](-download-complete-listener/download-complete.md) function.
This allows the downloaded coin locations to be passed back to the caller. |
| [DownloadFileTask](-download-file-task/index.md) | `class DownloadFileTask : Any`<br>[AsyncTask](#) which downloads a file from a remote server using a [HttpURLConnection](http://docs.oracle.com/javase/6/docs/api/java/net/HttpURLConnection.html).
In this case, used to download the coin locations.
Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete](-download-complete-listener/download-complete.md) method,
passing the downloaded data as a string. |
| [InboxFragment](-inbox-fragment/index.md) | `class InboxFragment : Any`<br>A fragment which shows the user their current inbox and allows them to craft new messages.
Crafting new messages starts a [MessageCreationActivity](-message-creation-activity/index.md). |
| [LoginActivity](-login-activity/index.md) | `class LoginActivity : Any`<br>A login screen that offers login via email/password, authenticating via Firebase. |
| [LoginAndLogoutTest](-login-and-logout-test/index.md) | `class LoginAndLogoutTest : Any` |
| [MainActivity](-main-activity/index.md) | `class MainActivity : Any`<br>The app's main Activity.
Handles switching between and containing the three main fragments,
[MapFragment](-map-fragment/index.md), [InboxFragment](-inbox-fragment/index.md), and [AccountFragment](-account-fragment/index.md).
Also stores references to the user's email and firestore documents
for easy access by the fragments. |
| [MapFragment](-map-fragment/index.md) | `class MapFragment : `[`DownloadCompleteListener`](-download-complete-listener/index.md)<br>The app's main Activity.
Handles various aspects of loading and displaying the map,
tracking the user's location, marking the coins on the map and picking them up. |
| [Message](-message/index.md) | `class Message : Any`<br>A class representing messages sent between users. |
| [MessageAdapter](-message-adapter/index.md) | `class MessageAdapter : Any`<br>Provides a specialized [ArrayAdapter](#) to dynamically list [Message](-message/index.md)s in a list view. |
| [MessageClassInstrumentedUnitTest](-message-class-instrumented-unit-test/index.md) | `class MessageClassInstrumentedUnitTest : Any`<br>Tests whether the [Message](-message/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |
| [MessageCreationActivity](-message-creation-activity/index.md) | `class MessageCreationActivity : Any`<br>A pop-up screen which allows the user to craft and send a new [Message](-message/index.md). |
| [MessageViewActivity](-message-view-activity/index.md) | `class MessageViewActivity : Any`<br>A pop-up screen allowing the user to view a message in its inbox. |
| [NewMessageAvailableTest](-new-message-available-test/index.md) | `class NewMessageAvailableTest : Any` |
| [SendAndReceiveCoinsTest](-send-and-receive-coins-test/index.md) | `class SendAndReceiveCoinsTest : Any` |
| [SendAndViewMessageTest](-send-and-view-message-test/index.md) | `class SendAndViewMessageTest : Any` |

### Properties

| Name | Summary |
|---|---|
| [ALARM_ACTION](-a-l-a-r-m_-a-c-t-i-o-n.md) | `const val ALARM_ACTION: String` |
| [ANCIENT_COIN_SPAWN_CHANCE](-a-n-c-i-e-n-t_-c-o-i-n_-s-p-a-w-n_-c-h-a-n-c-e.md) | `const val ANCIENT_COIN_SPAWN_CHANCE: Double` |
| [ANCIENT_DOLR](-a-n-c-i-e-n-t_-d-o-l-r.md) | `const val ANCIENT_DOLR: String` |
| [ANCIENT_PENY](-a-n-c-i-e-n-t_-p-e-n-y.md) | `const val ANCIENT_PENY: String` |
| [ANCIENT_QUID](-a-n-c-i-e-n-t_-q-u-i-d.md) | `const val ANCIENT_QUID: String` |
| [ANCIENT_SHIL](-a-n-c-i-e-n-t_-s-h-i-l.md) | `const val ANCIENT_SHIL: String` |
| [BANK_DOCUMENT](-b-a-n-k_-d-o-c-u-m-e-n-t.md) | `const val BANK_DOCUMENT: String` |
| [BANK_MARKER_LATITUDE](-b-a-n-k_-m-a-r-k-e-r_-l-a-t-i-t-u-d-e.md) | `const val BANK_MARKER_LATITUDE: Double` |
| [BANK_MARKER_LONGITUDE](-b-a-n-k_-m-a-r-k-e-r_-l-o-n-g-i-t-u-d-e.md) | `const val BANK_MARKER_LONGITUDE: Double` |
| [BANK_MARKER_TITLE](-b-a-n-k_-m-a-r-k-e-r_-t-i-t-l-e.md) | `const val BANK_MARKER_TITLE: String` |
| [COINZ_CHANNEL_ID](-c-o-i-n-z_-c-h-a-n-n-e-l_-i-d.md) | `const val COINZ_CHANNEL_ID: String` |
| [COINZ_CHANNEL_NAME](-c-o-i-n-z_-c-h-a-n-n-e-l_-n-a-m-e.md) | `const val COINZ_CHANNEL_NAME: String` |
| [COINZ_DOWNLOAD_NOTIFICATION_ID](-c-o-i-n-z_-d-o-w-n-l-o-a-d_-n-o-t-i-f-i-c-a-t-i-o-n_-i-d.md) | `const val COINZ_DOWNLOAD_NOTIFICATION_ID: Int` |
| [COINZ_SPAWN_NOTIFICATION_ID](-c-o-i-n-z_-s-p-a-w-n_-n-o-t-i-f-i-c-a-t-i-o-n_-i-d.md) | `const val COINZ_SPAWN_NOTIFICATION_ID: Int` |
| [COIN_DEPOSITED](-c-o-i-n_-d-e-p-o-s-i-t-e-d.md) | `const val COIN_DEPOSITED: String` |
| [CURRENCY](-c-u-r-r-e-n-c-y.md) | `const val CURRENCY: String` |
| [EXCHANGE_RATES](-e-x-c-h-a-n-g-e_-r-a-t-e-s.md) | `const val EXCHANGE_RATES: String` |
| [FIRST_RUN_ACTION](-f-i-r-s-t_-r-u-n_-a-c-t-i-o-n.md) | `const val FIRST_RUN_ACTION: String` |
| [FIRST_TIME_RUNNING](-f-i-r-s-t_-t-i-m-e_-r-u-n-n-i-n-g.md) | `const val FIRST_TIME_RUNNING: String` |
| [GOLD_FIELD_TAG](-g-o-l-d_-f-i-e-l-d_-t-a-g.md) | `const val GOLD_FIELD_TAG: String` |
| [ID](-i-d.md) | `const val ID: String` |
| [INBOX_DOCUMENT](-i-n-b-o-x_-d-o-c-u-m-e-n-t.md) | `const val INBOX_DOCUMENT: String` |
| [LAST_DOWNLOAD_DATE](-l-a-s-t_-d-o-w-n-l-o-a-d_-d-a-t-e.md) | `const val LAST_DOWNLOAD_DATE: String` |
| [LOGOUT_FLAG](-l-o-g-o-u-t_-f-l-a-g.md) | `const val LOGOUT_FLAG: String` |
| [MESSAGE_ATTACHMENTS](-m-e-s-s-a-g-e_-a-t-t-a-c-h-m-e-n-t-s.md) | `const val MESSAGE_ATTACHMENTS: String` |
| [MESSAGE_JSON_STRING](-m-e-s-s-a-g-e_-j-s-o-n_-s-t-r-i-n-g.md) | `const val MESSAGE_JSON_STRING: String` |
| [MESSAGE_TEXT](-m-e-s-s-a-g-e_-t-e-x-t.md) | `const val MESSAGE_TEXT: String` |
| [NETWORK_ERROR](-n-e-t-w-o-r-k_-e-r-r-o-r.md) | `const val NETWORK_ERROR: String` |
| [OVERWRITE_ALARM_ACTION](-o-v-e-r-w-r-i-t-e_-a-l-a-r-m_-a-c-t-i-o-n.md) | `const val OVERWRITE_ALARM_ACTION: String` |
| [PREFERENCES_FILE](-p-r-e-f-e-r-e-n-c-e-s_-f-i-l-e.md) | `const val PREFERENCES_FILE: String` |
| [SAVED_MAP_JSON](-s-a-v-e-d_-m-a-p_-j-s-o-n.md) | `const val SAVED_MAP_JSON: String` |
| [SENDER](-s-e-n-d-e-r.md) | `const val SENDER: String` |
| [TIMESTAMP](-t-i-m-e-s-t-a-m-p.md) | `const val TIMESTAMP: String` |
| [UOE_MAX_LATITUDE](-u-o-e_-m-a-x_-l-a-t-i-t-u-d-e.md) | `const val UOE_MAX_LATITUDE: Double` |
| [UOE_MAX_LONGITUDE](-u-o-e_-m-a-x_-l-o-n-g-i-t-u-d-e.md) | `const val UOE_MAX_LONGITUDE: Double` |
| [UOE_MIN_LATITUDE](-u-o-e_-m-i-n_-l-a-t-i-t-u-d-e.md) | `const val UOE_MIN_LATITUDE: Double` |
| [UOE_MIN_LONGITUDE](-u-o-e_-m-i-n_-l-o-n-g-i-t-u-d-e.md) | `const val UOE_MIN_LONGITUDE: Double` |
| [USER_EMAIL](-u-s-e-r_-e-m-a-i-l.md) | `const val USER_EMAIL: String` |
| [VALUE](-v-a-l-u-e.md) | `const val VALUE: String` |
| [WALLET_DOCUMENT](-w-a-l-l-e-t_-d-o-c-u-m-e-n-t.md) | `const val WALLET_DOCUMENT: String` |
