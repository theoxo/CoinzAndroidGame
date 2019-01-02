[com.coinzgame.theoxo.coinz](../index.md) / [SendAndReceiveCoinsTest](index.md) / [sendAndReceiveCoinsTest](.)

# sendAndReceiveCoinsTest

`fun sendAndReceiveCoinsTest(): Unit`

This test sends two coins from one user to another through the messaging system and
then logs into the other user's account and deposits the coins into the bank to
ensure they were sent correctly.
It requires that users "testcoincollector@test.test" with password "testtest111"
and "testreceiver@test.test" with password "testtest111" are set up in the authentication
service. It also requires that this is the first time the app is being run on the system;
as such running it in a test suite will not work.

