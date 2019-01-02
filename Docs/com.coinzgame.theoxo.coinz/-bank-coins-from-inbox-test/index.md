[com.coinzgame.theoxo.coinz](../index.md) / [BankCoinsFromInboxTest](.)

# BankCoinsFromInboxTest

`class BankCoinsFromInboxTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BankCoinsFromInboxTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [bankCoinsFromInboxTest](bank-coins-from-inbox-test.md) | `fun bankCoinsFromInboxTest(): Unit`<br>This tests logging in, starting the bank, and depositing several coins from the user's inbox.
Checks that the bank credit is updated appropriately, that the counter does not change
(since the coin is not coming from the user's wallet), and that the coin is
removed from the ListView. Then restarts the [BankActivity](../-bank-activity/index.md) and makes sure the coin is still
not in the ListView.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work. |
| [setUpDatabase](set-up-database.md) | `fun setUpDatabase(): Unit`<br>Set up the database entry for the test user before running the test so that it is repeatable.
Specifically this sets the user to have a "fresh" bank account and a single message
in their inbox.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service. |
