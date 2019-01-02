[com.coinzgame.theoxo.coinz](../index.md) / [BankCoinsFromInboxTest](index.md) / [bankCoinsFromInboxTest](.)

# bankCoinsFromInboxTest

`fun bankCoinsFromInboxTest(): Unit`

This tests logging in, starting the bank, and depositing several coins from the user's inbox.
Checks that the bank credit is updated appropriately, that the counter does not change
(since the coin is not coming from the user's wallet), and that the coin is
removed from the ListView. Then restarts the [BankActivity](../-bank-activity/index.md) and makes sure the coin is still
not in the ListView.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work.

