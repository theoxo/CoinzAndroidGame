[com.coinzgame.theoxo.coinz](../index.md) / [BankActivity](index.md) / [onCreate](.)

# onCreate

`fun onCreate(savedInstanceState: <ERROR CLASS>?): Unit`

Sets up the local fields and invokes [pullFromDatabase](#).
This includes getting the [currentUserEmail](#) from the intent
and setting up the [firestore](#) related instances.

### Parameters

`savedInstanceState` - the previously saved instance state, if it exists.