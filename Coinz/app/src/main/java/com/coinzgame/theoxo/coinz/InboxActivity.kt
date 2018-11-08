package com.coinzgame.theoxo.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.util.Log
import android.view.MenuItem
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_inbox.*
import org.json.JSONObject

/**
 * A screen which shows the user their current inbox.
 * Also allows them to move on to [MessageCreationActivity] so as to draft a new [Message].
 */
class InboxActivity : AppCompatActivity(),
                        BottomNavigationView.OnNavigationItemSelectedListener {

    private val tag = "InboxActivity"

    // Firebase Firestore database related local fields
    private var firestore : FirebaseFirestore? = null
    private var firestoreInbox : DocumentReference? = null
    private var currentUserEmail : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        newMessageButton.setOnClickListener {_ -> startmessageCreationActivity() }
        inboxListView.setOnItemClickListener { _, _, position, _ ->
            val message : Message? = inboxListView.getItemAtPosition(position) as? Message
            if (message == null) {
                Log.e(tag, "[inboxListView.onItemClick] Could not cast item at $position to "
                                 + "Message")
            } else {
                val intent = Intent(this, MessageViewActivity::class.java)
                intent.putExtra(MESSAGE_JSON_STRING, message.toJSONString())
                startActivity(intent)
            }
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener(this)

        firestore = FirebaseFirestore.getInstance()

        val emailCopy : String? = currentUserEmail
        if (emailCopy == null) {
            Log.e(tag, "[onCreate] currentUserEmail is null")
        } else {
            firestoreInbox = firestore?.collection(emailCopy)?.document(INBOX_DOCUMENT)
            if (firestoreInbox == null) {
                Log.d(tag, "[onCreate] User's inbox is null")
            } else {
                updateListView()
            }
        }
    }

    private fun startmessageCreationActivity() {
        val intent = Intent(this, MessageCreationActivity::class.java)
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
    }

    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_nav -> startMainActivity()
            R.id.account_nav -> startAccountActivity()
            else -> return true //do nothing
        }

        return true
    }

    private fun updateListView() {
        firestoreInbox?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                val inboxSnapshot = docSnapshot.data?.toSortedMap()
                if (inboxSnapshot == null) {
                    Log.w(tag, "[updateListView] inboxSnapshot is null")
                } else {
                    val items = ArrayList<Message>()
                    for ((_, messageAny) in inboxSnapshot) {
                        val messageJSON = JSONObject(messageAny.toString())
                        val message = Message(messageJSON)
                        items.add(message)

                    }

                    items.sortByDescending { message -> message.timestamp }
                    val messagesAdapter = MessageAdapter(this@InboxActivity, items)
                    inboxListView.adapter = messagesAdapter
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateListView] Wallet get failed: $e")
            }
        }
    }

    /**
     * Reorders the previous [MainActivity] back to the front.
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    /**
     * Starts a new [AccountActivity].
     */
    private fun startAccountActivity() {
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
    }
}
