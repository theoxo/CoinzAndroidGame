package com.coinzgame.theoxo.coinz

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom

/**
 * Tests whether the [Message] class functions as expected.
 * This unit test needs to be run as an instrumented test since it relies on [JSONObject].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class MessageClassInstrumentedUnitTest {

    private var messageJSON: JSONObject? = null
    private var message: Message? = null
    private val timestamp = "TestTimestamp"
    private val senderEmail = "testemail@email.email"
    private val messageText = "Test message text"
    private val attachedCoins = ArrayList<Coin>()
    private val attachedCoinJSONs = ArrayList<JSONObject>()

    /**
     * Set up an example [Message] and the fields to compare it to.
     */
    @Before
    fun setUpMessage() {
        // Add 0 to 10 coins to attach to the message
        for (i in 0 until ThreadLocalRandom.current().nextInt(0, 11)) {
            val coinCurrency = "testCurrency$i"
            val coinId = "$coinCurrency|$timestamp$i"  // This is how Message generates the IDs
            val coinValue = ThreadLocalRandom.current().nextDouble()
            val coin = Coin(coinId, coinCurrency, coinValue)
            attachedCoinJSONs.add(coin.toJSON())
            attachedCoins.add(coin)
        }

        val messageJSONCp = JSONObject()
        messageJSONCp.put(SENDER, senderEmail)
        messageJSONCp.put(TIMESTAMP, timestamp)
        messageJSONCp.put(MESSAGE_TEXT, messageText)
        messageJSONCp.put(MESSAGE_ATTACHMENTS, attachedCoinJSONs)

        message = Message(messageJSONCp)
        messageJSON = messageJSONCp

    }

    /**
     * Check that the message's timestamp is as expected.
     */
    @Test
    fun timestampIsCorrect() {
        assertEquals(timestamp, message?.timestamp)
    }

    /**
     * Test that the sender email saved in the message is as expected.
     */
    @Test
    fun senderIsCorrect() {
        assertEquals(senderEmail, message?.senderEmail)
    }

    /**
     * Check that the message's main text body is as expected.
     */
    @Test
    fun textIsCorrect() {
        assertEquals(messageText, message?.messageText)
    }

    /**
     * Check that the coins attached to the message equal to the ones we passed to it.
     */
    @Test
    fun coinsAreCorrect() {
        assertEquals(attachedCoins, message?.attachedCoins)
    }

    /**
     * Check that the JSONObject -> Message -> JSONObject process is invertible (i.e. the
     * first and last JSONObjects are equivalent). This tests [Message.toJSONString] in
     * unison with the constructor.
     */
    @Test
    fun toJSONStringIsCorrect() {
        assertEquals(messageJSON.toString(), JSONObject(message?.toJSONString()).toString())
    }

}