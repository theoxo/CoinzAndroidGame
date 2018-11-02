package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AbsListView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_mail_creation.*
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.*

private var currentUserEmail : String? = null

class MailCreationActivity : AppCompatActivity() {

    private val tag = "MailCreationActivity"

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mail_creation)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        sendButton.setOnClickListener { _ -> generateMail() }

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

    private fun generateMail() {
        val targetEmail : String = targetEmail.text.toString()
        val currentTime = LocalDateTime.now()

        if (currentUserEmail == null) {
            Log.e(tag, "[generateMail] currentUserEmail is null")
        } else {

            val mailJSON = JSONObject()
            val addedCoins = ArrayList<JSONObject>()
            val sentCoinsDeletionMap = HashMap<String, String>()

            val ticks = coinsListView.checkedItemPositions
            val listViewLength = ticks.size()
            for (i in 0..listViewLength-1) {
                if (ticks[i]) {
                    // If the coin is ticked,
                    // add it to the email body
                    val coin : Coin? = coinsListView.getItemAtPosition(i) as? Coin
                    val currency : String? = coin?.currency
                    val value : String? = coin?.value
                    val id : String? = coin?.id
                    when {
                        coin == null -> {
                            Log.e(tag, "[generateMail] Could not cast item at pos $i to coin")
                        }
                        currency == null -> {
                            Log.e(tag, "[generateMail] Coin at $i has null currency")
                        }
                        value == null -> {
                            Log.e(tag, "[generateMail] Null value for coin at $i")
                        }
                        id == null -> {
                            Log.e(tag, "[generateMail] Null id for coin at $i")
                        }
                        else -> {
                            Log.d(tag, "[generateMail] Adding $value $currency to the email")
                            val coinJSON = JSONObject()
                            coinJSON.put(CURRENCY, currency)
                            coinJSON.put(VALUE, value)
                            addedCoins.add(coinJSON)
                            sentCoinsDeletionMap["$currency|$id"] = COIN_DEPOSITED
                        }
                    }
                }
            }

            mailJSON.put(MESSAGE_ATTACHMENTS, addedCoins)
            mailJSON.put(MESSAGE_TEXT, message.text.toString())
            mailJSON.put(SENDER, currentUserEmail)
            mailJSON.put(TIMESTAMP, currentTime)

            val  generatedMessage = Message(mailJSON)

            // Have generated the mail to send. Send it away!
            sendMail(targetEmail, generatedMessage, sentCoinsDeletionMap)
        }
    }

    private fun sendMail(targetEmail : String, message : Message,
                         sentCoinsDeletionMap : Map<String, String>) {

        val mailTag = message.getMessageTag()
        val mailMap = mapOf(mailTag to message.toJSONString())
        val targetCollection = firestore?.collection(targetEmail)
        targetCollection?.get()?.run {
            addOnSuccessListener { querySnapshot ->
                val targetInbox: DocumentReference = targetCollection.document(INBOX_DOCUMENT)
                if (querySnapshot.isEmpty) {
                    // Collection does not exist, i.e. the user is not registered in the system
                    Log.w(tag, "[sendMail] The target user  $targetEmail is not registered")
                    toast("Could not find user $targetEmail")
                } else {
                    var mailBoxExists = false
                    for (docSnapshot in querySnapshot.documents) {
                        if (docSnapshot.reference == targetInbox) {
                            Log.d(tag, "[sendMail] Found mailbox in user's collection")

                            // The target user has an existant inbox. Add this mail to it
                            targetInbox.update(mailMap).run {
                                addOnSuccessListener {
                                    Log.d(tag, "[sendMail] Added mail to existent mailbox")
                                    toast("Mail sent")
                                    deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                                }

                                addOnFailureListener { e ->
                                    Log.e(tag, "[sendMail] Failed at adding mail to existent "
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
                        targetInbox.set(mailMap).run {
                            addOnSuccessListener {
                                Log.d(tag, "[sendMail] Added mail to new mailbox")
                                toast("Mail sent")
                                deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                            }

                            addOnFailureListener { e ->
                                Log.e(tag, "[sendMail] Failed at adding mail to new "
                                        + "mailbox: $e")
                            }
                        }
                    }
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[sendMail] Target user's collection get failed: $e")
            }
        }
    }

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

                    val coinsAdapter = CoinsAdapter(this@MailCreationActivity, items, true)
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
