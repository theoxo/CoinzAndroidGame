package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AbsListView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_message_creation.*
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.*


/**
 * A pop-up screen which allows the user to craft and send a new [Message].
 */
class MessageCreationActivity : AppCompatActivity() {

    private val tag = "MessageCreationActivity"

    private var currentUserEmail : String? = null

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null

    /**
     * Sets up the screen, the [FirebaseFirestore] instance, and event listeners.
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_creation)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        sendButton.setOnClickListener { _ -> generateMessage() }

        targetEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not interested
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not interested
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = !(targetEmail.text.isEmpty()
                                         || targetEmail.text.toString() == currentUserEmail
                                         || '~' in targetEmail.text
                                         || '/' in targetEmail.text)
            }
        })

        // Set up Firestore
        firestore = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp instead of java.util.Date
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        val emailTag : String? = currentUserEmail
        if (emailTag == null) {
            Log.e(tag, "[onCreate] null user email")
        } else {
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)

            updateListView()
        }
    }

    /**
     * Generates a new message from the information input by the user and then calls [sendMessage].
     */
    private fun generateMessage() {
        val targetEmail : String = targetEmail.text.toString()
        val currentTime = LocalDateTime.now()

        if (currentUserEmail == null) {
            Log.e(tag, "[generateMessage] currentUserEmail is null")
        } else {

            val messageJSON = JSONObject()
            val addedCoins = ArrayList<JSONObject>()
            val sentCoinsDeletionMap = HashMap<String, String>()

            val ticks = coinsListView.checkedItemPositions
            val listViewLength = ticks.size()
            for (i in 0 until listViewLength) {
                if (ticks[i]) {
                    // If the coin is ticked,
                    // add it to the email body
                    val coin : Coin? = coinsListView.getItemAtPosition(i) as? Coin
                    val currency : String? = coin?.currency
                    val value : String? = coin?.value
                    val id : String? = coin?.id
                    when {
                        coin == null -> {
                            Log.e(tag, "[generateMessage] Could not cast item at pos $i to coin")
                        }
                        currency == null -> {
                            Log.e(tag, "[generateMessage] Coin at $i has null currency")
                        }
                        value == null -> {
                            Log.e(tag, "[generateMessage] Null value for coin at $i")
                        }
                        id == null -> {
                            Log.e(tag, "[generateMessage] Null id for coin at $i")
                        }
                        else -> {
                            Log.d(tag, "[generateMessage] Adding $value $currency to the email")
                            val coinJSON = JSONObject()
                            coinJSON.put(CURRENCY, currency)
                            coinJSON.put(VALUE, value)
                            addedCoins.add(coinJSON)
                            sentCoinsDeletionMap["$currency|$id"] = COIN_DEPOSITED
                        }
                    }
                }
            }

            messageJSON.put(MESSAGE_ATTACHMENTS, addedCoins)
            messageJSON.put(MESSAGE_TEXT, message.text.toString())
            messageJSON.put(SENDER, currentUserEmail)
            messageJSON.put(TIMESTAMP, currentTime)

            val  generatedMessage = Message(messageJSON)

            // Have generated the mail to send. Send it away!
            sendMessage(targetEmail, generatedMessage, sentCoinsDeletionMap)
        }
    }

    /**
     * Sends the given message.
     * Upon success, invokes [deleteSentCoinsFromUsersWallet].
     *
     * @param targetEmail the recipient's email.
     * @param message the message to send to the target user.
     * @param sentCoinsDeletionMap a map of the coins to mark as invalid in the user's wallet.
     */
    private fun sendMessage(targetEmail : String, message : Message,
                            sentCoinsDeletionMap : Map<String, String>) {

        val messageTag = message.getMessageTag()
        val messageMap = mapOf(messageTag to message.toJSONString())
        val targetCollection = firestore?.collection(targetEmail)
        targetCollection?.get()?.run {
            addOnSuccessListener { querySnapshot ->
                val targetInbox: DocumentReference = targetCollection.document(INBOX_DOCUMENT)
                if (querySnapshot.isEmpty) {
                    // Collection does not exist, i.e. the user is not registered in the system
                    Log.w(tag, "[sendMessage] The target user  $targetEmail is not registered")
                    toast("Could not find user $targetEmail")
                } else {
                    var mailBoxExists = false
                    for (docSnapshot in querySnapshot.documents) {
                        if (docSnapshot.reference == targetInbox) {
                            Log.d(tag, "[sendMessage] Found mailbox in user's collection")

                            // The target user has an existant inbox. Add this mail to it
                            targetInbox.update(messageMap).run {
                                addOnSuccessListener {
                                    Log.d(tag, "[sendMessage] Added mail to existent mailbox")
                                    toast("Mail sent")
                                    deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                                }

                                addOnFailureListener { e ->
                                    Log.e(tag, "[sendMessage] Failed at adding mail to existent "
                                            + "mailbox: $e")
                                }
                            }

                            // We are not interested in any of the other documents at this point,
                            // so can break here
                            mailBoxExists = true
                            break
                        }
                    }

                    if (!mailBoxExists) {
                        // Have been unable to locate the mailbox in the target user's collection.
                        // Create it and set its contents to this message
                        // Target mailbox doesn't exist. Make it!
                        targetInbox.set(messageMap).run {
                            addOnSuccessListener {
                                Log.d(tag, "[sendMessage] Added mail to new mailbox")
                                toast("Mail sent")
                                deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                            }

                            addOnFailureListener { e ->
                                Log.e(tag, "[sendMessage] Failed at adding mail to new "
                                        + "mailbox: $e")
                            }
                        }
                    }
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[sendMessage] Target user's collection get failed: $e")
            }
        }
    }

    /**
     * Removes the coins sent away from the user's wallet and finishes the activity.
     *
     * @param sentCoinsDeletionMap a map of the coins to mark as invalid in the user's wallet.
     */
    private fun deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap: Map<String, String>) {
        firestoreWallet?.update(sentCoinsDeletionMap)?.run {
            addOnSuccessListener {
                Log.d(tag, "[deleteSentCoinsFromUsersWallet] Deleted "
                                 + "${sentCoinsDeletionMap.size} coins")
            }

            addOnFailureListener { e ->
                Log.e(tag, "[deleteSentCoinsFromUsersWallet] Failed: $e")
            }
        }

        finish()
    }

    /**
     * Updates the coin list view with the user's latest wallet info.
     */
    private fun updateListView() {
        firestoreWallet?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                val walletSnapshot = docSnapshot.data?.toSortedMap()
                if (walletSnapshot == null) {
                    Log.w(tag, "[updateListView] walletSnapshot is null")
                } else {
                    val items = ArrayList<Coin>()
                    for ((key, value) in walletSnapshot) {
                        val currency = key.substringBefore("|")
                        val id = key.substringAfter("|")
                        val coinValue = value as? String
                        when (coinValue) {
                            null -> {
                                Log.e(tag, "[updateListView] coinValue of $currency $id is null")
                            }
                            COIN_DEPOSITED -> {
                                // Skip this coin
                            }
                            else -> {
                                val coin = Coin(id, currency, coinValue)
                                items.add(coin)
                            }
                        }
                    }

                    val coinsAdapter = CoinAdapter(this@MessageCreationActivity, items,
                                            true)
                    coinsListView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
                    coinsListView.adapter = coinsAdapter
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateListView] Wallet get failed: $e")
            }
        }
    }
}
