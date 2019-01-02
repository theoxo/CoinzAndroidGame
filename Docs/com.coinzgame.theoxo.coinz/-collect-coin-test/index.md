[com.coinzgame.theoxo.coinz](../index.md) / [CollectCoinTest](.)

# CollectCoinTest

`class CollectCoinTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CollectCoinTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [collectCoinTest](collect-coin-test.md) | `fun collectCoinTest(): Unit`<br>Tests collecting a dummy coin and depositing it into the bank.
Also checks that the combo timer bonus feature updates the UI as expected.
Depositing the coin into the bank is necessary to check if it was collected appropriately.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work. |
| [setUpDatabase](set-up-database.md) | `fun setUpDatabase(): Unit`<br>Set up the database entry for the test user before running the test so that it is repeatable.
Specifically this sets the user to have a "fresh" bank account and empty wallet.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service. |
