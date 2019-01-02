[com.coinzgame.theoxo.coinz](../index.md) / [BankActivity](.)

# BankActivity

`class BankActivity : Any`

The screen which allows the user to deposit their coins into the bank.
Pops up when the user engages with the bank.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BankActivity()`<br>The screen which allows the user to deposit their coins into the bank.
Pops up when the user engages with the bank. |

### Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: <ERROR CLASS>?): Unit`<br>Sets up the local fields and invokes [pullFromDatabase](#).
This includes getting the [currentUserEmail](#) from the intent
and setting up the [firestore](#) related instances. |
| [onStart](on-start.md) | `fun onStart(): Unit` |
