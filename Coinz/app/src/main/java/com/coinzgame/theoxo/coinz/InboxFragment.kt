package com.coinzgame.theoxo.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_inbox.*
import org.jetbrains.anko.toast
import org.json.JSONObject

/**
 * A screen which shows the user their current inbox.
 * Also allows them to move on to [MessageCreationActivity] so as to draft a new [Message].
 */
class InboxFragment : Fragment() {

    private val fragTag = "InboxFragment"
    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainActivity = context as? MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "Fragment created")
        return inflater.inflate(R.layout.fragment_inbox, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newMessageButton.setOnClickListener {_ -> startmessageCreationActivity() }
        inboxListView.setOnItemClickListener { _, _, position, _ ->
            val message : Message? = inboxListView.getItemAtPosition(position) as? Message
            if (message == null) {
                Log.e(fragTag, "[inboxListView.onItemClick] Could not cast item at $position to "
                        + "Message")
            } else {
                val intent = Intent(mainActivity, MessageViewActivity::class.java)
                intent.putExtra(MESSAGE_JSON_STRING, message.toJSONString())
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        updateListView()
    }

    private fun startmessageCreationActivity() {
        val intent = Intent(mainActivity, MessageCreationActivity::class.java)
        intent.putExtra(USER_EMAIL, mainActivity?.currentUserEmail)
        startActivity(intent)
    }

    private fun updateListView() {
        Log.d(fragTag, "[updateListView] Invoked")
        inboxProgressBar.visibility = View.VISIBLE
        mainActivity?.firestoreInbox?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                inboxProgressBar.visibility = View.GONE

                val inboxSnapshot = docSnapshot.data?.toSortedMap()
                if (inboxSnapshot == null) {
                    Log.w(fragTag, "[updateListView] inboxSnapshot is null")
                } else {
                    val items = ArrayList<Message>()
                    for ((_, messageAny) in inboxSnapshot) {
                        val messageJSON = JSONObject(messageAny.toString())
                        val message = Message(messageJSON)
                        items.add(message)

                    }

                    val context = mainActivity
                    if (context == null) {
                        Log.e(fragTag, "[updateListView] Current context is null")
                    } else {

                        items.sortByDescending { message -> message.timestamp }
                        val messagesAdapter = MessageAdapter(context, items)
                        inboxListView.adapter = messagesAdapter
                    }
                }
            }

            addOnFailureListener { e ->
                inboxProgressBar.visibility = View.GONE
                Log.e(fragTag, "[updateListView] Inbox get failed: $e")
                mainActivity?.toast("Could not fetch your inbox")
            }
        }
    }
}
