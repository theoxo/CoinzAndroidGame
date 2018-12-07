package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_message_view.*
import org.json.JSONObject

/**
 * A pop-up screen allowing the user to view a message in its inbox.
 */
class MessageViewActivity : AppCompatActivity() {

    private val tag = "MessageViewActivity"

    /**
     * Sets up the screen and fills it with the message info.
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_view)

        // Get the retrieved message
        val messageJSONStr = intent?.getStringExtra(MESSAGE_JSON_STRING)
        val message = Message(JSONObject(messageJSONStr))

        // Update the title and  textview accordingly
        title = "${message.senderEmail}"
        messageTimestampTextView.text = "${message.timestamp}"
        messageTextView.text = "${message.messageText}"

        val attachedCoins = message.attachedCoins
        if (attachedCoins == null || attachedCoins.size == 0) {
            Log.d(tag, "[onCreate] No attached coins found")
            // Since there are no coins attached, we don't need to set an adapter to
            // list view which would be showing them.
        } else {
            // There is at least one coin attached, so get an adapter for the listview and set it
            val attachedCoinsAdapter = CoinAdapter(
                    this, attachedCoins, false)
            messageAttachedCoinsListView.adapter = attachedCoinsAdapter
        }
    }
}
