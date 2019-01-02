[com.coinzgame.theoxo.coinz](../index.md) / [SendAndViewMessageTest](index.md) / [sendAndViewMessageTest](.)

# sendAndViewMessageTest

`fun sendAndViewMessageTest(): Unit`

Tests logging in as one user, sending a message to another user, logging in as the second
user and viewing the message. Thereby tests the message passing system in completion.
For this test to run as expected two users need to be registered:
one with email "testcoincollector@test.test" and password "testtest111",
and one with email "testreceiver@test.test" and password "testtest111".
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work.

