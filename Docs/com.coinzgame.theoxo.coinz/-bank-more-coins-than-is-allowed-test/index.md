[com.coinzgame.theoxo.coinz](../index.md) / [BankMoreCoinsThanIsAllowedTest](.)

# BankMoreCoinsThanIsAllowedTest

`class BankMoreCoinsThanIsAllowedTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BankMoreCoinsThanIsAllowedTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [bankMoreCoinsThanIsAllowedTest](bank-more-coins-than-is-allowed-test.md) | `fun bankMoreCoinsThanIsAllowedTest(): Unit` |
| [setUpDatabase](set-up-database.md) | `fun setUpDatabase(): Unit`<br>Set up the database entry for the test user before running the test so that it is repeatable.
Specifically this sets the user to have a "fresh" bank account and 30 valid coins in
their wallet.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service. |
