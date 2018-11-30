package com.coinzgame.theoxo.coinz

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * A class representing messages sent between users.
 *
 * @param messageJSON the JSON describing this message.
 * @property timestamp the timestamp of this message.
 * @property senderEmail the email of the user who sent the message.
 * @property messageText the main text body of the message.
 * @property attachedCoins the coins which are attached to the message.
 * @constructor Sets up a message and its properties from the JSON given.
 */
class Message(messageJSON: JSONObject) {

    private val tag = "MessageClass"

    var timestamp : String? = null
    var senderEmail : String? = null
    var messageText : String? = null
    var attachedCoins : ArrayList<Coin>? = null


    init {
        // First get the elementary data from the JSON we were given
        this.timestamp = messageJSON.get(TIMESTAMP).toString()
        this.senderEmail = messageJSON.get(SENDER).toString()

        // Now loop over any and all coins which were attached to the message, adding
        // them into our list of attached coins
        this.attachedCoins = ArrayList()
        messageText = messageJSON.getString(MESSAGE_TEXT)
        val messageAttachments = JSONArray(messageJSON.getString(MESSAGE_ATTACHMENTS))
        for (i in 0 until messageAttachments.length()) {
            val attachment : JSONObject? = messageAttachments[i] as? JSONObject
            val currency : String? = attachment?.getString(CURRENCY)
            val value : Double? = attachment?.getDouble(VALUE)
            when {
                currency == null -> Log.e(tag, "[constructor] currency is null at $i")
                value == null -> Log.e(tag, "[constructor] value is null at $i")
                else -> {
                    // Need an ID for the coin. Let's generate a unique one from the timestamp
                    // and index in the message
                    val id = "$currency|$timestamp$i"

                    // Set up the coin with the values we've retrieved and generated
                    val coin = Coin(id, currency, value)

                    // Add it to our list
                    Log.d(tag, "[constructor] Adding coin $i to attachedCoins")
                    attachedCoins?.add(coin)
                }
            }
        }
    }


    /**
     * Gets a tag for the message which can be used as its key in the database.
     *
     * @return the tag generated.
     */
    fun getMessageTag() : String {
        // Generate the tag from the timestamp of the message and the user we retrieved it from
        // in a way that adheres to the firebase assumptions here:
        // https://firebase.google.com/docs/firestore/quotas
        // and also which does not contain periods as this bears a special meaning in
        // Firestore.
        return "`$timestamp|$senderEmail`".replace('.', ':')
    }

    /**
     * Generates a JSON String representation of the message.
     *
     * @return a String representation of the JSON containing all the message's data.
     */
    fun toJSONString() : String {
        val json = JSONObject()
        addSenderEmailToJSON(json)
        addTimestampToJSON(json)
        addMessageTextToJSON(json)
        addAttachedCoinsToJSON(json)

        return json.toString()
    }

    /**
     * Removes the requested coin from the message's list of attached coins.
     *
     * @param coin the coin to remove.
     * @return whether the removal was successful.
     */
    fun removeCoin(coin : Coin) : Boolean? {
        return attachedCoins?.remove(coin)
    }

    /**
     * Adds the message's [senderEmail] to the JSON object given.
     *
     * @param json the JSON object to add the email address to.
     */
    private fun addSenderEmailToJSON(json: JSONObject) {
        val senderEmailCp = senderEmail
        if (senderEmailCp == null) {
            Log.e(tag, "[addSenderEmailToJSON] senderEmailCp is null")
        } else {
            json.put(SENDER, senderEmailCp)
        }
    }

    /**
     * Adds the message's [timestamp] to the JSON object given.
     *
     * @param json the JSON object to add the timestamp to.
     */
    private fun addTimestampToJSON(json: JSONObject) {
        val timestampCp = timestamp
        if (timestampCp == null) {
            Log.e(tag, "[addTimestampToJSON] timestampCp is null")
        } else {
            json.put(TIMESTAMP, timestampCp)
        }
    }

    /**
     * Adds the message's [messageText] to the JSON object given.
     *
     * @param json the JSON object to add the message text to.
     */
    private fun addMessageTextToJSON(json: JSONObject) {
        val messageTextCp = messageText
        if (messageTextCp == null) {
            Log.e(tag, "[addMessageTextToJSON] messageTextCp is null")
        } else {
            json.put(MESSAGE_TEXT, messageTextCp)
        }
    }

    /**
     * Adds the message's [attachedCoins] to the JSON object given.
     *
     * @param json the JSON object to add the attached coins to.
     */
    private fun addAttachedCoinsToJSON(json: JSONObject) {
        val attachedCoinsCp = attachedCoins
        if (attachedCoinsCp == null) {
            Log.e(tag, "[addAttachedCoinsToJSON] attachedCoinsCp is null")
        } else {
            val coinJSONs = ArrayList<JSONObject>()

            for (coin: Coin in attachedCoinsCp) {
                coinJSONs.add(coin.toJSON())
            }

            // Add the generated list of JSONObjects to the overall JSON
            json.put(MESSAGE_ATTACHMENTS, coinJSONs)
        }
    }

}