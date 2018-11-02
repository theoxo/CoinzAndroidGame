package com.coinzgame.theoxo.coinz

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class Message {

    private val tag = "MessageClass"

    var timestamp : String? = null
    var senderEmail : String? = null
    var messageText : String? = null
    var attachedCoins : ArrayList<Coin>? = null

    constructor(messageJSON :JSONObject) {
        this.timestamp = messageJSON.get(TIMESTAMP).toString()
        this.senderEmail = messageJSON.get(SENDER).toString()

        this.attachedCoins = ArrayList()
        messageText = messageJSON.getString(MESSAGE_TEXT)
        val messageAttachments = JSONArray(messageJSON.getString(MESSAGE_ATTACHMENTS))
        for (i in 0..messageAttachments.length()-1) {
            val attachment : JSONObject? = messageAttachments[i] as? JSONObject
            val currency : String? = attachment?.getString(CURRENCY)
            val value : String? = attachment?.getString(VALUE)
            // Need an ID for the coin. Let's generate a unique one from the timestamp
            // and index in the message
            when {
                currency == null -> Log.e(tag, "[constructor] currency is null at $i")
                value == null -> Log.e(tag, "[constructor] value is null at $i")
                else -> {
                    val id = "$currency|$timestamp$i"
                    Log.d(tag, "[constructor] Adding coin $i to attachedCoins")
                    val coin = Coin(id, currency, value)
                    attachedCoins?.add(coin)
                }
            }
        }
    }


    fun getMessageTag() : String {
        return "$timestamp|$senderEmail".replace('.', ':')
    }

    fun toJSONString() : String {
        val json = JSONObject()
        addSenderEmailToJSON(json)
        addTimestampToJSON(json)
        addMessageTextToJSON(json)
        addAttachedCoinsToJSON(json)

        return json.toString()
    }

    fun removeCoin(coin : Coin) : Boolean? {
        return attachedCoins?.remove(coin)
    }

    private fun addSenderEmailToJSON(json: JSONObject) {
        val senderEmailCp = senderEmail
        if (senderEmailCp == null) {
            Log.e(tag, "[addSenderEmailToJSON] senderEmailCp is null")
        } else {
            json.put(SENDER, senderEmailCp)
        }
    }

    private fun addTimestampToJSON(json: JSONObject) {
        val timestampCp = timestamp
        if (timestampCp == null) {
            Log.e(tag, "[addTimestampToJSON] timestampCp is null")
        } else {
            json.put(TIMESTAMP, timestampCp)
        }
    }

    private fun addMessageTextToJSON(json: JSONObject) {
        val messageTextCp = messageText
        if (messageTextCp == null) {
            Log.e(tag, "[addMessageTextToJSON] messageTextCp is null")
        } else {
            json.put(MESSAGE_TEXT, messageTextCp)
        }
    }

    private fun addAttachedCoinsToJSON(json: JSONObject) {
        val attachedCoinsCp = attachedCoins
        if (attachedCoinsCp == null) {
            Log.e(tag, "[addAttachedCoinsToJSON] attachedCoinsCp is null")
        } else {
            val coinJSONs = ArrayList<JSONObject>()

            for (coin: Coin in attachedCoinsCp) {
                val currency: String? = coin.currency
                val id: String? = coin.id
                val value: String? = coin.value

                when {
                    currency == null -> {
                        Log.e(tag, "[addAttachedCoinsToJSON] null currency of coin")
                    }
                    id == null -> {
                        Log.e(tag, "[addAttachedCoinsToJSON] null id of coin")
                    }
                    value == null -> {
                        Log.e(tag, "[addAttachedCoinsToJSON] null value of coin")
                    }
                    else -> {
                        Log.d(tag, "[addAttachedCoinsToJSON] Adding coin $id")
                        val coinJSON = JSONObject()
                        coinJSON.put(CURRENCY, currency)
                        coinJSON.put(ID, id)
                        coinJSON.put(VALUE, value)

                        coinJSONs.add(coinJSON)
                    }
                }
            }

            // Add the generated list of JSONObjects to the overall JSON
            json.put(MESSAGE_ATTACHMENTS, coinJSONs)
        }
    }

}