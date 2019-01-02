[com.coinzgame.theoxo.coinz](../index.md) / [SendAndReceiveCoinsTest](.)

# SendAndReceiveCoinsTest

`class SendAndReceiveCoinsTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SendAndReceiveCoinsTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [sendAndReceiveCoinsTest](send-and-receive-coins-test.md) | `fun sendAndReceiveCoinsTest(): Unit`<br>This test sends two coins from one user to another through the messaging system and
then logs into the other user's account and deposits the coins into the bank to
ensure they were sent correctly.
It requires that users "testcoincollector@test.test" with password "testtest111"
and "testreceiver@test.test" with password "testtest111" are set up in the authentication
service. It also requires that this is the first time the app is being run on the system;
as such running it in a test suite will not work. |
| [setUpDatabase](set-up-database.md) | `fun setUpDatabase(): Unit`<br>Set up the database entry for the test users before running the test so that it is repeatable.
Specifically this sets the user "testcoincollector@test.test" and password "testtest111"
to have 30 coins in their wallet and enables them to send away coins in messages by
setting their counter for the day to 25 in their bank. It also sets the user
"tesetreceiver@test.test" with password "testtest111" to have a fresh bank account
and fresh inbox.
Both of these users thus need to be set up in the authentication service.
This test requires that this is the first time the app is being run on the system;
as such running it in a test suite will not work. |
