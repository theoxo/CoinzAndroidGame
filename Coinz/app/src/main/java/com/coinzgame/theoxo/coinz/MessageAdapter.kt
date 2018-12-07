package com.coinzgame.theoxo.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Provides a specialized [ArrayAdapter] to dynamically list [Message]s in a list view.
 *
 * @param context the context which is invoking the adapter.
 * @param messages the messages to put in the list view.
 */

class MessageAdapter(context: Context, messages: ArrayList<Message>)
    : ArrayAdapter<Message>(context, 0, messages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val message: Message? =  getItem(position)
        val view: View = convertView ?:
                          LayoutInflater.from(context).inflate(R.layout.standard_list_item,
                                  parent, false)

        val tvItem = view.findViewById<TextView>(R.id.standardListItemText)

        var messageText = "${message?.senderEmail}\n${message?.timestamp}"
        val numAttachedCoins: Int? = message?.attachedCoins?.size
        when (numAttachedCoins) {
            null -> {
                // Skip
            }
            0 -> {
                // Skip
            }
            1 -> {
                messageText += "\n$numAttachedCoins coin"
            }
            else -> {
                messageText += "\n$numAttachedCoins coins"
            }
        }
        val messageBodyLength: Int? = message?.messageText?.length
        when {
            messageBodyLength == null || messageBodyLength == 0 -> {
                // Skip
            }
            messageBodyLength <= 15 -> {
                messageText += "\n${message.messageText}"
            }
            else -> {
                messageText += "\n${message.messageText?.take(15)}..."
            }

        }

        tvItem?.text = messageText

        return view
    }
}