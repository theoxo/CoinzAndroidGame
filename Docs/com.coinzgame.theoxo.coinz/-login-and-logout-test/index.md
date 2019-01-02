[com.coinzgame.theoxo.coinz](../index.md) / [LoginAndLogoutTest](.)

# LoginAndLogoutTest

`class LoginAndLogoutTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `LoginAndLogoutTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [loginAndLogoutTest](login-and-logout-test.md) | `fun loginAndLogoutTest(): Unit`<br>Tests logging in and back out again.
For this test to run as expected a user with email "testcoincollector@test.test" and
password "testtest111" needs to be registered.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work. |
