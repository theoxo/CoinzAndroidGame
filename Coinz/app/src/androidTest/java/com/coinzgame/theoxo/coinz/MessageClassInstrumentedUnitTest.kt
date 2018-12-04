package com.coinzgame.theoxo.coinz

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom

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

    @Before
    fun setUpMessage() {
        val attachedCoinJsons = ArrayList<JSONObject>()
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

    @Test
    fun timestampIsCorrect() {
        assertEquals(timestamp, message?.timestamp)
    }

    @Test
    fun senderIsCorrect() {
        assertEquals(senderEmail, message?.senderEmail)
    }

    @Test
    fun textIsCorrect() {
        assertEquals(messageText, message?.messageText)
    }

    @Test
    fun coinsAreCorrect() {
        assertEquals(attachedCoins, message?.attachedCoins)
    }

    @Test
    fun toJSONStringIsCorrect() {
        assertEquals(messageJSON.toString(), JSONObject(message?.toJSONString()).toString())
    }

}