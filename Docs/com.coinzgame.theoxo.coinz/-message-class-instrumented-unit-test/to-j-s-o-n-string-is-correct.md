[com.coinzgame.theoxo.coinz](../index.md) / [MessageClassInstrumentedUnitTest](index.md) / [toJSONStringIsCorrect](.)

# toJSONStringIsCorrect

`fun toJSONStringIsCorrect(): Unit`

Check that the JSONObject -&gt; Message -&gt; JSONObject process is invertible (i.e. the
first and last JSONObjects are equivalent). This tests [Message.toJSONString](../-message/to-j-s-o-n-string.md) in
unison with the constructor.

