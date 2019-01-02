[com.coinzgame.theoxo.coinz](../index.md) / [CollectCoinTest](index.md) / [setUpDatabase](.)

# setUpDatabase

`fun setUpDatabase(): Unit`

Set up the database entry for the test user before running the test so that it is repeatable.
Specifically this sets the user to have a "fresh" bank account and empty wallet.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.

