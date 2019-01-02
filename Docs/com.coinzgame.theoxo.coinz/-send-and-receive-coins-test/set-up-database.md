[com.coinzgame.theoxo.coinz](../index.md) / [SendAndReceiveCoinsTest](index.md) / [setUpDatabase](.)

# setUpDatabase

`fun setUpDatabase(): Unit`

Set up the database entry for the test users before running the test so that it is repeatable.
Specifically this sets the user "testcoincollector@test.test" and password "testtest111"
to have 30 coins in their wallet and enables them to send away coins in messages by
setting their counter for the day to 25 in their bank. It also sets the user
"tesetreceiver@test.test" with password "testtest111" to have a fresh bank account
and fresh inbox.
Both of these users thus need to be set up in the authentication service.
This test requires that this is the first time the app is being run on the system;
as such running it in a test suite will not work.

