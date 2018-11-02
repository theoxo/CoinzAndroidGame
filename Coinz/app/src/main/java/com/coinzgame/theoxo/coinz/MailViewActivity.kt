package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_mail_view.*
import org.json.JSONObject

class MailViewActivity : AppCompatActivity() {

    private val tag = "MailViewActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mail_view)

        // Get the retrieved message
        val messageJSONStr = intent?.getStringExtra(MESSAGE_JSON_STRING)
        val message = Message(JSONObject(messageJSONStr))

        // Update the textviews
        title = "${message.senderEmail},\n${message.timestamp}"

        messageTextView.text = message.messageText

        val attachedCoins = message.attachedCoins
        if (attachedCoins == null || attachedCoins.size == 0) {
            Log.d(tag, "[onCreate] No attached coins found")
        } else {
            val attachedCoinsAdapter = CoinsAdapter(this, attachedCoins, false)
            messageAttachedCoinsListView.adapter = attachedCoinsAdapter
        }
    }
}
