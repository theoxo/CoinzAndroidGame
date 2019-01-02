[com.coinzgame.theoxo.coinz](../index.md) / [BankCoinsFromWalletThenInboxTest](index.md) / [bankSingleCoinTest](.)

# bankSingleCoinTest

`fun bankSingleCoinTest(): Unit`

Combines the tests for banking coins from the wallet and the inbox to make sure
that doing the latter does not overwrite anything from doing the former.
This was set up in response to a bug where depositing coins from the inbox would reset the
counter of coins deposited from the wallet to 0.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work.

