[com.coinzgame.theoxo.coinz](../index.md) / [BankCoinsFromWalletThenInboxTest](.)

# BankCoinsFromWalletThenInboxTest

`class BankCoinsFromWalletThenInboxTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BankCoinsFromWalletThenInboxTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [bankSingleCoinTest](bank-single-coin-test.md) | `fun bankSingleCoinTest(): Unit`<br>Combines the tests for banking coins from the wallet and the inbox to make sure
that doing the latter does not overwrite anything from doing the former.
This was set up in response to a bug where depositing coins from the inbox would reset the
counter of coins deposited from the wallet to 0.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work. |
| [setUpDatabase](set-up-database.md) | `fun setUpDatabase(): Unit`<br>Set up the database entry for the test user before running the test so that it is repeatable.
Specifically this sets the user to have a "fresh" bank account and 30 valid coins in
their wallet, plus a message with 5 coins in it in their inbox.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service. |
