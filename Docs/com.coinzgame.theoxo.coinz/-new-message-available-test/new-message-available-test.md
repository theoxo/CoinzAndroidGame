[com.coinzgame.theoxo.coinz](../index.md) / [NewMessageAvailableTest](index.md) / [newMessageAvailableTest](.)

# newMessageAvailableTest

`fun newMessageAvailableTest(): Unit`

Tests logging in, switching to the [InboxFragment](../-inbox-fragment/index.md) and checking if the new message button
is available.
The purpose of this test is really to make sure navigating to and loading the [InboxFragment](../-inbox-fragment/index.md)
works as expected. It requires a fresh install of the app, so before running the test make
sure the app is not already installed on the device. Running this test inside of a test
suite will therefore not work.
The test also requires a use with email "testcoincollector@test.test" and password
"testtest111" to be set up in the authentication system.

