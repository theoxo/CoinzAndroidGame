[com.coinzgame.theoxo.coinz](../index.md) / [SendAndViewMessageTest](.)

# SendAndViewMessageTest

`class SendAndViewMessageTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SendAndViewMessageTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [sendAndViewMessageTest](send-and-view-message-test.md) | `fun sendAndViewMessageTest(): Unit`<br>Tests logging in as one user, sending a message to another user, logging in as the second
user and viewing the message. Thereby tests the message passing system in completion.
For this test to run as expected two users need to be registered:
one with email "testcoincollector@test.test" and password "testtest111",
and one with email "testreceiver@test.test" and password "testtest111".
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work. |
