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
 * A fragment which shows the user their current inbox and allows them to craft new messages.
 * Crafting new messages starts a [MessageCreationActivity].
 *
 * @property mainActivity the [MainActivity] which the fragment is attached to
 */
class InboxFragment : Fragment() {

    private val fragTag = "InboxFragment"
    private var mainActivity: MainActivity? = null

    /**
     * Saves the [MainActivity] which the fragment is being attached to for later reference.
     *
     * @param context the context which the fragment has been attached to
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // We only ever expect to be attached to MainActivities, so this cast will result in null
        // if and only if the context is null
        mainActivity = context as? MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "Fragment created")
        return inflater.inflate(R.layout.fragment_inbox, container, false)

    }

    /**
     * Sets up the click events for messages in the inbox and the button to craft a new message.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newMessageButton.setOnClickListener {_ -> startMessageCreationActivity() }
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

    /**
     * Updates the inbox list upon the fragment being shown to the user.
     */
    override fun onStart() {
        super.onStart()
        updateListView()
    }

    /**
     * Starts a new [MessageCreationActivity] so that the user can craft a new message.
     */
    private fun startMessageCreationActivity() {
        val intent = Intent(mainActivity, MessageCreationActivity::class.java)
        // Pass the new activity the user's email as stored in the main activity
        // as it will need it to access the appropriate firebase documents
        intent.putExtra(USER_EMAIL, mainActivity?.currentUserEmail)
        startActivity(intent)
    }

    /**
     * Updates the list view containing the user's inbox with the latest snapshot of the database.
     */
    private fun updateListView() {
        Log.d(fragTag, "[updateListView] Invoked")

        // Set a progress bar to be visible so the user knows we are waiting for an async task
        inboxProgressBar.visibility = View.VISIBLE

        // Get a snapshot of the user's current inbox state
        mainActivity?.firestoreInbox?.get()?.run {
            addOnSuccessListener { docSnapshot ->

                val inboxSnapshot = docSnapshot.data?.toSortedMap()
                if (inboxSnapshot == null) {
                    Log.w(fragTag, "[updateListView] inboxSnapshot is null")
                } else {

                    // Loop over the messages in the snapshot, constructing a Message object
                    // for each one.
                    val items = ArrayList<Message>()
                    for ((_, messageAny) in inboxSnapshot) {
                        val messageJSON = JSONObject(messageAny.toString())
                        val message = Message(messageJSON)
                        items.add(message)
                    }

                    // Attempt to update the listview's adapter
                    val context = mainActivity
                    if (context == null) {
                        Log.e(fragTag, "[updateListView] Current context is null")
                    } else {

                        // The context which we are operating in is non-null so we can safely
                        // construct a new MessageAdapter with the Messages we've created
                        // and set it to the listview, also showing the latest message
                        // first.
                        items.sortByDescending { message -> message.timestamp }
                        val messagesAdapter = MessageAdapter(context, items)
                        inboxListView.adapter = messagesAdapter
                    }
                }

                // Hide the progress bar as we've finished.
                inboxProgressBar.visibility = View.GONE
            }

            addOnFailureListener { e ->
                // Hide the progress bar and let the user know we failed, logging the exception
                inboxProgressBar.visibility = View.GONE
                Log.e(fragTag, "[updateListView] Inbox get failed: $e")
                mainActivity?.toast("Could not fetch your inbox")
            }
        }
    }
}
