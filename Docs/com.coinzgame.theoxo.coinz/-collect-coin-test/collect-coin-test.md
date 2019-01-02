[com.coinzgame.theoxo.coinz](../index.md) / [CollectCoinTest](index.md) / [collectCoinTest](.)

# collectCoinTest

`fun collectCoinTest(): Unit`

Tests collecting a dummy coin and depositing it into the bank.
Also checks that the combo timer bonus feature updates the UI as expected.
Depositing the coin into the bank is necessary to check if it was collected appropriately.
This requires that a user with email "testcoincollector@test.test" and
password "testtest111" is registered in the authentication service.
It also requires a fresh install of the app, so before running the test make sure
the app is not already installed on the device. Running this test inside of a test suite
will therefore not work.

