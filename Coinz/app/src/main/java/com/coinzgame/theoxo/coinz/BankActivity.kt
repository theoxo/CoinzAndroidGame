package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.View
import android.widget.AbsListView.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_bank.*
import org.json.JSONObject

/**
 * The screen which allows the user to deposit their coins into the bank.
 * Pops up when the user engages with the bank.
 */
class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"

    // Firebase Firestore database
    private var firestore : FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var firestoreBank : DocumentReference? = null
    private var firestoreInbox : DocumentReference? = null
    private var currentUserEmail : String? = null

    private var rates : JSONObject? = null

    private var sourceUpdateDone : Boolean = true
    private var creditUpdateDone : Boolean = true

    private var choiceIsWallet : Boolean = true

    private var coinToMessage : MutableMap<Coin, Message>? = null

    /**
     * Sets up the local fields and invokes [updateListView].
     * This includes getting the [currentUserEmail] from the intent
     * and setting up the [firestore] related instances.
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        coinToMessage = HashMap()

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)
        val ratesString = intent?.getStringExtra(EXCHANGE_RATES)
        if (ratesString == null || ratesString == "") {
            Log.e(tag, "[onCreate] Exchange rates are null or empty")
        } else {
            rates = JSONObject(ratesString)
        }

        // Set up Firestore
        firestore = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp instead of java.util.Date
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        depositButton.setOnClickListener { _ -> depositSelectedCoins()}

        chooseWalletButton.setOnClickListener {
            choiceIsWallet = true
            switchToDepositMode()
        }

        chooseInboxButton.setOnClickListener {
            choiceIsWallet = false
            switchToDepositMode()
        }

        bankTextView.text = ("Hello and welcome to the Bank!\n"
                            + "Today's rates are:\n"
                            + "\t* DOLR to Gold: ${rates?.get("DOLR")}\n"
                            + "\t* PENY to Gold: ${rates?.get("PENY")}\n"
                            + "\t* QUID to Gold: ${rates?.get("QUID")}\n"
                            + "\t* SHIL to Gold: ${rates?.get("SHIL")}\n")

        val emailTag : String? = currentUserEmail
        if (emailTag == null) {
            Log.e(tag, "[onCreate] null user email")
        } else {
            firestoreBank = firestore?.collection(emailTag)?.document(BANK_DOCUMENT)
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)
            firestoreInbox = firestore?.collection(emailTag)?.document(INBOX_DOCUMENT)

            updateListView()
        }
    }

    /**
     * Switches to deposit mode, hiding irrelevant elements and showing new ones.
     */
    private fun switchToDepositMode() {
        chooseWalletButton.visibility = View.GONE
        textView2.visibility = View.GONE
        chooseInboxButton.visibility = View.GONE
        coinsListView.visibility = View.VISIBLE
        depositButton.visibility = View.VISIBLE
        updateListView()
    }

    /**
     * Updates [coinsListView] with the latest user wallet info.
     */
    private fun updateListView() {
        val sourceChoiceIsWallet = choiceIsWallet
        val source : DocumentReference? = when (sourceChoiceIsWallet) {
            true -> firestoreWallet
            else -> firestoreInbox
        }

        source?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                val sourceSnapshot = docSnapshot.data?.toMap()
                if (sourceSnapshot == null) {
                    Log.w(tag, "[updateListView] sourceSnapshot is null")
                } else {
                    val items = ArrayList<Coin>()
                    if (sourceChoiceIsWallet) {
                        for ((key, value) in sourceSnapshot) {

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
                    } else {
                        // Otherwise the user has indicated that they want to use their inbox as
                        // the source of coins, i.e. they want to deposit coins which have been
                        // sent to them
                        for ((_, messageAny) in sourceSnapshot) {
                            val messageStr = messageAny.toString()
                            val message = Message(JSONObject(messageStr))

                            val attachedCoins = message.attachedCoins
                            if (attachedCoins == null) {
                                Log.d(tag, "[updateListView] No coins in message from "
                                                 + "${message.senderEmail} at ${message.timestamp}")
                            } else {
                                attachedCoins.forEach { coin -> coinToMessage?.put(coin, message) }
                                items.addAll(attachedCoins)
                            }
                        }
                    }

                    // Sort coins descendently by value
                    items.sortByDescending { coin -> coin.value }

                    val coinsAdapter = CoinAdapter(this@BankActivity, items, true)
                    coinsListView.choiceMode = CHOICE_MODE_MULTIPLE
                    coinsListView.adapter = coinsAdapter
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateListView] Source get failed: $e")
            }
        }
    }

    /**
     * Deposits the currently selected coins in [coinsListView] to the user's bank.
     */
    private fun depositSelectedCoins() {

        val sourceModeIsWallet = choiceIsWallet

        val sourceChoiceIsWallet = choiceIsWallet
        val source : DocumentReference? = when (sourceChoiceIsWallet) {
            true -> firestoreWallet
            else -> firestoreInbox
        }

        // First of all disable the deposit button until we're done depositing
        depositButton.isEnabled = false

        var depositAmount = 0.0
        val sourceUpdate = HashMap<String, String>()
        val chosenCoins = ArrayList<Coin>()
        val ticks : SparseBooleanArray = coinsListView.checkedItemPositions
        val listLength = coinsListView.count
        for (i in 0 until listLength) {
            if (ticks[i]) {
                // The item at this position is ticked. Deposit it
                val coin : Coin? = coinsListView.getItemAtPosition(i) as? Coin
                val currency : String? = coin?.currency
                val value : Double? = coin?.value?.toDouble()
                val id : String? = coin?.id
                val exchangeRate : Double? = rates?.get(currency) as? Double

                when {
                    coin == null -> {
                        Log.e(tag, "[depositSelectedCoins] Could not cast item at pos $i to coin")
                    }
                    currency == null -> {
                        Log.e(tag, "[depositSelectedCoins] Coin at $i has null currency")
                    }
                    value == null -> {
                        Log.e(tag, "[depositSelectedCoins] Null value for coin at $i")
                    }
                    id == null -> {
                        Log.e(tag, "[depositSelectedCoins] ID of coin at $i is null")
                    }
                    exchangeRate == null -> {
                        Log.e(tag, "[depositSelectedCoins] Exchange rate for currency "
                                         + "$currency is null")
                    }
                    else -> {
                        chosenCoins.add(coin)
                        depositAmount += exchangeRate * value  // TODO is this the correct interpretation of exchangerate
                        if (sourceModeIsWallet) {
                            sourceUpdate["$currency|$id"] = COIN_DEPOSITED
                        } else {
                            // Otherwise it is the inbox whose contents we want to update
                            val message = coinToMessage?.get(coin)
                            if (message == null) {
                                Log.e(tag, "[depositSelectedCoins] Couldn't find message for "
                                                 + "coin at $i")
                            } else {
                                val tag = message.getMessageTag()
                                // Remove the coin from message
                                message.removeCoin(coin)
                                sourceUpdate[tag] = message.toJSONString()
                            }
                        }

                    }
                }
            }
        }

        // Add the calculated amount of gold to the user's bank account
        if (depositAmount > 0) {
            creditUpdateDone = false
            addToUsersBank(depositAmount)
        }

        // Update the deposited coin's values in the database to a special value
        // to indicate that they have been deposited (do not delete them to ensure they
        // are not added back to the map again)
        if (!sourceUpdate.isEmpty()) {
            sourceUpdateDone = false
            if (source == null) {
                Log.e(tag, "[depositSelectedCoins] Want to update source but ref to it is null")
            } else {
                updateSourceWithDepositedCoins(source, sourceUpdate)
            }
        }

    }

    /**
     * Updates the source of the coin (whether inbox or wallet) so as to remove the deposited coins.
     *
     * @param source the document reference for the source the coin was retrieved from.
     * @param sourceUpdate a map with the data to set in the source.
     */
    private fun updateSourceWithDepositedCoins(source : DocumentReference, sourceUpdate : Map<String, String>) {
        source.update(sourceUpdate).run {
            addOnSuccessListener { _ ->
                Log.d(tag, "[updateSourceWithDepositedCoins] Success with "
                                 + "${sourceUpdate.size} items")

                sourceUpdateDone = true
                enableFurtherDeposits()
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateSourceWithDepositedCoins] Failed: $e")
            }
        }
    }

    /**
     * Gets the user's current bank credit and adds the deposited amount to it.
     *
     * @param depositAmount the amount of GOLD that is being deposited.
     */
    private fun addToUsersBank(depositAmount : Double) {
        firestoreBank?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                val bankContents = docSnapshot.data
                val currentAmount = bankContents?.get(GOLD_FIELD_TAG) as? Double
                if (currentAmount == null) {
                    Log.w(tag, "[addToUsersBank] Current amount is null, setting user's "
                                     + "bank credit to $depositAmount")
                    setUsersBankCredit(depositAmount)
                } else {
                    Log.d(tag, "[addToUsersBank] Found $currentAmount in user's bank. "
                                     +"Setting user's credit to ${currentAmount + depositAmount}")
                    setUsersBankCredit(depositAmount + currentAmount)
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[addToUsersBank] Bank get failed: $e")
            }
        }
    }

    /**
     * Sets the user's bank credit to the specified amount.
     *
     * @param credit the amount of GOLD to set the user's credit to.
     */
    private fun setUsersBankCredit(credit : Double) {
        // Overwrites whatever credit is currently stored in the bank. Make sure this is
        // only called through addToUsersBank
        firestoreBank?.set(mapOf(GOLD_FIELD_TAG to credit))?.run {
            addOnSuccessListener { _ ->
                Log.d(tag, "[setUsersBankCredit] Succeeded at setting credit = $credit")
                creditUpdateDone = true
                enableFurtherDeposits()
            }

            addOnFailureListener { e ->
                Log.e(tag, "[setUsersBankCredit] Failed: $e")
            }
        }
    }

    /**
     * Invokes [updateListView] and re-enables the [depositButton] once the source is updated.
     */
    private fun enableFurtherDeposits() {
        if (creditUpdateDone && sourceUpdateDone) {
            // If both credit update and wallet update succeeded, enable further
            // depositing redo the list view
            updateListView()
            depositButton.isEnabled = true
        }
    }
}
