[com.coinzgame.theoxo.coinz](../index.md) / [BankCoinFromWalletTest](index.md) / [bankSingleCoinTest](.)

# bankSingleCoinTest

`fun bankSingleCoinTest(): Unit`

This tests logging in, starting the bank, and depositing a coin from the wallet.
Checks that the bank credit and counter is updated appropriately and that the coin is
removed from the ListView. Then restarts the [BankActivity](../-bank-activity/index.md) and makes sure the coin is still
not in the ListView.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work.

