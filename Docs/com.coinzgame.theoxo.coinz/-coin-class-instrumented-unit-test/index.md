[com.coinzgame.theoxo.coinz](../index.md) / [CoinClassInstrumentedUnitTest](.)

# CoinClassInstrumentedUnitTest

`class CoinClassInstrumentedUnitTest : Any`

Tests whether the [Coin](../-coin/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CoinClassInstrumentedUnitTest()`<br>Tests whether the [Coin](../-coin/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |

### Functions

| Name | Summary |
|---|---|
| [currencyIsCorrect](currency-is-correct.md) | `fun currencyIsCorrect(): Unit`<br>Check that the coin's currency matches in the JSON. |
| [idIsCorrect](id-is-correct.md) | `fun idIsCorrect(): Unit`<br>Check that the coin's id matches in the JSON. |
| [setUpCoinJson](set-up-coin-json.md) | `fun setUpCoinJson(): Unit`<br>Set up the [Coin](../-coin/index.md) and get its corresponding [JSONObject](#). |
| [valueIsCorrect](value-is-correct.md) | `fun valueIsCorrect(): Unit`<br>Check that the coin's value matches in the JSON. |
