[com.coinzgame.theoxo.coinz](../index.md) / [NewMessageAvailableTest](.)

# NewMessageAvailableTest

`class NewMessageAvailableTest : Any`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `NewMessageAvailableTest()` |

### Properties

| Name | Summary |
|---|---|
| [mActivityTestRule](m-activity-test-rule.md) | `var mActivityTestRule: <ERROR CLASS>` |
| [mGrantPermissionRule](m-grant-permission-rule.md) | `var mGrantPermissionRule: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [newMessageAvailableTest](new-message-available-test.md) | `fun newMessageAvailableTest(): Unit`<br>Tests logging in, switching to the [InboxFragment](../-inbox-fragment/index.md) and checking if the new message button
is available.
The purpose of this test is really to make sure navigating to and loading the [InboxFragment](../-inbox-fragment/index.md)
works as expected. It requires a fresh install of the app, so before running the test make
sure the app is not already installed on the device. Running this test inside of a test
suite will therefore not work.
The test also requires a use with email "testcoincollector@test.test" and password
"testtest111" to be set up in the authentication system. |
