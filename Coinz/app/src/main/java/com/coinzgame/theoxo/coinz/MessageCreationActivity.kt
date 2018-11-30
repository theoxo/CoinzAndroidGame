package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AbsListView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_message_creation.*
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
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
    private var firestoreBank : DocumentReference? = null
    private var todaysDate: String? = null

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

        // Set up a regex that matches ".", "..", and any "__.*__"
        // where only in the last on is period a special character
        // Do this by escaping all the literals involed for safety.
        // This will be needed to ensure the target email is a valid collection reference key
        val escapedPeriod = Regex.escape(".")
        val escapedUnderscore = Regex.escape("_")
        val invalidCollectionRegex = Regex("$escapedPeriod|$escapedPeriod$escapedPeriod"
            + "|$escapedUnderscore$escapedUnderscore.*$escapedUnderscore$escapedUnderscore")

        targetEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not interested
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not interested
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable the send message button only if it is safe to do so.
                // This means we do not want the target email to be empty, we do not want
                // it to be ourselves, and as this will be used to reference a colleciton
                // we must adhere to firestore's assumptions as defined here:
                // https://firebase.google.com/docs/firestore/quotas
                // Meaning it must not contain "/", be only "." or "..",
                // not match the regex "__.*__",
                // and not be more than 1500 bytes long.
                val targetEmailString = targetEmail.text.toString()
                sendButton.isEnabled = ((!targetEmailString.isEmpty())
                        && targetEmailString.all { c -> c != '/' }
                        && (!invalidCollectionRegex.matches(targetEmailString))
                        && targetEmailString.toByteArray().size <= 1500)
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
            firestoreBank = firestore?.collection(emailTag)?.document(BANK_DOCUMENT)
        }
    }

    override fun onStart() {
        super.onStart()

        val calendar = Calendar.getInstance()
        todaysDate = "${calendar.get(Calendar.YEAR)}-" +
                "${calendar.get(Calendar.MONTH) + 1}-" +  // Add 1 as month is 0-indexed
                "${calendar.get(Calendar.DAY_OF_MONTH)}"
        checkIfElligebleToSendCoins()
    }

    /**
     * Generates a new message from the information input by the user and then calls [sendMessage].
     */
    private fun generateMessage() {
        messageSentProgressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false

        val targetEmail : String = targetEmail.text.toString()
        val currentTime = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }



        if (currentUserEmail == null) {
            Log.e(tag, "[generateMessage] currentUserEmail is null")
        } else {

            val messageJSON = JSONObject()
            val addedCoins = ArrayList<JSONObject>()
            val sentCoinsDeletionMap = HashMap<String, String>()

            val ticks = coinsListView.checkedItemPositions
            val listViewLength = if (ticks == null) {
                0
                // If the ticks is null, likely because the user is not allowed to
                // send any coins, set this to 0 to not loop over them.
            } else {
                ticks.size()
            }
            for (i in 0 until listViewLength) {
                if (ticks[i]) {
                    // If the coin is ticked,
                    // add it to the email body
                    val coin : Coin? = coinsListView.getItemAtPosition(i) as? Coin
                    val currency : String? = coin?.currency
                    val value : Double? = coin?.value
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
            messageJSON.put(TIMESTAMP, "${currentTime.get(Calendar.DAY_OF_MONTH)}-" +
                    "${currentTime.get(Calendar.MONTH) + 1}-${currentTime.get(Calendar.YEAR)}" +
                    " ${currentTime.get(Calendar.HOUR_OF_DAY)}:${currentTime.get(Calendar.MINUTE)}" +
                    ":${currentTime.get(Calendar.SECOND)}")

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
                    this@MessageCreationActivity.toast("Could not find user $targetEmail")
                    this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                    this@MessageCreationActivity.sendButton.isEnabled = true
                } else {
                    var mailBoxExists = false
                    for (docSnapshot in querySnapshot.documents) {
                        if (docSnapshot.reference == targetInbox) {
                            Log.d(tag, "[sendMessage] Found mailbox in user's collection")

                            // The target user has an existant inbox. Add this mail to it
                            targetInbox.update(messageMap).run {
                                addOnSuccessListener {
                                    this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                                    this@MessageCreationActivity.sendButton.isEnabled = true
                                    Log.d(tag, "[sendMessage] Added mail to existent mailbox")
                                    toast("Mail sent")
                                    deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                                }

                                addOnFailureListener { e ->
                                    this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                                    this@MessageCreationActivity.sendButton.isEnabled = true
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
                                this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                                this@MessageCreationActivity.sendButton.isEnabled = true
                                Log.d(tag, "[sendMessage] Added mail to new mailbox")
                                toast("Mail sent")
                                deleteSentCoinsFromUsersWallet(sentCoinsDeletionMap)
                            }

                            addOnFailureListener { e ->
                                this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                                this@MessageCreationActivity.sendButton.isEnabled = true
                                Log.e(tag, "[sendMessage] Failed at adding mail to new "
                                        + "mailbox: $e")
                            }
                        }
                    }
                }
            }

            addOnFailureListener { e ->
                this@MessageCreationActivity.messageSentProgressBar.visibility = View.GONE
                this@MessageCreationActivity.sendButton.isEnabled = true
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

    private fun checkIfElligebleToSendCoins() {
        val todaysDateCp = todaysDate // copy field for thread safety
        if (todaysDateCp == null) {
            Log.e(tag, "[checkIfElligebleToSendCoins] Today's date is null, returning early")
            return
        }

        coinListProgressBar.visibility = View.VISIBLE
        // First check that the user has already deposited 25 coins today,
        // meaning the rest is spare change. Only if this is true, update the list view
        // with the coins wallet.
        firestoreBank?.get()?.run {
            addOnSuccessListener { documentSnapshot ->
                var depositedToday = documentSnapshot.get(todaysDateCp) as? Long
                if (depositedToday == null) {
                    // The field is not in the user's bank or the bank doesn't exist;
                    // either way they have not deposited any coins today.
                    depositedToday = 0
                }

                if (depositedToday >= 25) {
                    // Allow the user to send away coins
                    updateListView()
                } else {
                    coinListProgressBar.visibility = View.GONE
                    notElligbleText.visibility = View.VISIBLE
                }

            }

            addOnFailureListener { e ->
                Log.e(tag, "[checkIfElligebleToSendCoins] Bank get failed: $e")
            }
        }
    }


    /**
     * Updates the coin list view with the user's latest wallet.
     */
    private fun updateListView() {

        firestoreWallet?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                this@MessageCreationActivity.coinListProgressBar.visibility = View.GONE
                val walletSnapshot = docSnapshot.data?.toSortedMap()
                if (walletSnapshot == null) {
                    Log.w(tag, "[updateListView] walletSnapshot is null")
                } else {
                    val items = ArrayList<Coin>()
                    for ((_, coinJsonString) in walletSnapshot) {
                        if (coinJsonString == COIN_DEPOSITED) {
                            // The coin has already been sent away or deposited. Don't list
                            // it as an option
                            continue
                        } else {
                            val coinJson = try {
                                JSONObject(coinJsonString.toString())
                            } catch (e: JSONException) {
                                Log.e(tag, "[updateListView] JSON String is not "
                                        + "COIN_DEPOSITED but JSON cast still failed.")
                                JSONObject()
                            }

                            val id: String? = try {
                                coinJson.getString(ID)
                            } catch (e: JSONException) {
                                Log.e(tag, "[updateListView] Encountered exception $e when "
                                        + "getting ID from coin.")
                                null
                            }
                            val currency: String? = try {
                                coinJson.getString(CURRENCY)
                            } catch (e: JSONException) {
                                Log.e(tag, "[updateListView] Encountered exception $e when "
                                        + "getting currency from coin.")
                                null
                            }
                            val coinValue: Double? = try {
                                coinJson.getDouble(VALUE)
                            } catch (e: JSONException) {
                                Log.e(tag, "[updateListView] Encountered exception $e when "
                                        + "getting coinValue from coin.")
                                null
                            }

                            if (id != null && currency != null && coinValue != null) {
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
                this@MessageCreationActivity.coinListProgressBar.visibility = View.GONE
            }
        }
    }
}
