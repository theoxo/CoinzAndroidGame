[com.coinzgame.theoxo.coinz](../index.md) / [MessageClassInstrumentedUnitTest](.)

# MessageClassInstrumentedUnitTest

`class MessageClassInstrumentedUnitTest : Any`

Tests whether the [Message](../-message/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MessageClassInstrumentedUnitTest()`<br>Tests whether the [Message](../-message/index.md) class functions as expected.
This unit test needs to be run as an instrumented test since it relies on [JSONObject](#). |

### Functions

| Name | Summary |
|---|---|
| [coinsAreCorrect](coins-are-correct.md) | `fun coinsAreCorrect(): Unit`<br>Check that the coins attached to the message equal to the ones we passed to it. |
| [senderIsCorrect](sender-is-correct.md) | `fun senderIsCorrect(): Unit`<br>Test that the sender email saved in the message is as expected. |
| [setUpMessage](set-up-message.md) | `fun setUpMessage(): Unit`<br>Set up an example [Message](../-message/index.md) and the fields to compare it to. |
| [textIsCorrect](text-is-correct.md) | `fun textIsCorrect(): Unit`<br>Check that the message's main text body is as expected. |
| [timestampIsCorrect](timestamp-is-correct.md) | `fun timestampIsCorrect(): Unit`<br>Check that the message's timestamp is as expected. |
| [toJSONStringIsCorrect](to-j-s-o-n-string-is-correct.md) | `fun toJSONStringIsCorrect(): Unit`<br>Check that the JSONObject -&gt; Message -&gt; JSONObject process is invertible (i.e. the
first and last JSONObjects are equivalent). This tests [Message.toJSONString](../-message/to-j-s-o-n-string.md) in
unison with the constructor. |
