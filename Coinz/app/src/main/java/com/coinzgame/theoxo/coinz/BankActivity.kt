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
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * The screen which allows the user to deposit their coins into the bank.
 * Pops up when the user engages with the bank.
 */
class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"

    // Firebase Firestore database
    private var firestore: FirebaseFirestore? = null
    private var firestoreWallet: DocumentReference? = null
    private var firestoreBank: DocumentReference? = null
    private var firestoreInbox: DocumentReference? = null
    private var currentUserEmail: String? = null

    private var rates: JSONObject? = null
    private var todaysDate: String? = null
    private var goldInBank: Double? = null
    private var coinsDepositedToday: Long? = null

    private var sourceUpdateDone: Boolean = true
    private var creditUpdateDone: Boolean = true

    private var choiceIsWallet: Boolean = true

    private var coinToMessage: MutableMap<Coin, Message>? = null

    /**
     * Sets up the local fields and invokes [pullFromDatabase].
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

        val emailTag: String? = currentUserEmail
        if (emailTag == null) {
            Log.e(tag, "[onCreate] null user email")
        } else {
            firestoreBank = firestore?.collection(emailTag)?.document(BANK_DOCUMENT)
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)
            firestoreInbox = firestore?.collection(emailTag)?.document(INBOX_DOCUMENT)
        }
    }

    override fun onStart() {
        super.onStart()

        val calendar = Calendar.getInstance()
        todaysDate = "${calendar.get(Calendar.YEAR)}-" +
                "${calendar.get(Calendar.MONTH) + 1}-" +  // Add 1 as month is 0-indexed
                "${calendar.get(Calendar.DAY_OF_MONTH)}"

    }
    /**
     * Switches to deposit mode, hiding irrelevant elements and showing new ones.
     */
    private fun switchToDepositMode() {
        chooseWalletButton?.visibility = View.GONE
        chooseInboxButton?.visibility = View.GONE
        coinsListView?.visibility = View.VISIBLE
        depositButton?.visibility = View.VISIBLE
        lowerTextView?.text = ("Today's rates are:\n"
                + "• DOLR to Gold: ${String.format("%.2f", rates?.get("DOLR"))}\n"
                + "• PENY to Gold: ${String.format("%.2f", rates?.get("PENY"))}\n"
                + "• QUID to Gold: ${String.format("%.2f", rates?.get("QUID"))}\n"
                + "• SHIL to Gold: ${String.format("%.2f", rates?.get("SHIL"))}")
        upperTextView?.text = "Loading your bank data…"
        pullFromDatabase()
    }

    /**
     * Updates [coinsListView] with the latest user wallet info.
     */
    private fun pullFromDatabase() {
        val sourceChoiceIsWallet = choiceIsWallet
        val coinSource: DocumentReference? = when (sourceChoiceIsWallet) {
            true -> firestoreWallet
            else -> firestoreInbox
        }

        // Get the list of coins from the user's chosen source and update the listview.
        coinSource?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                val sourceSnapshot = docSnapshot.data?.toMap()
                if (sourceSnapshot == null || sourceSnapshot.isEmpty()) {
                    Log.w(tag, "[pullFromDatabase] sourceSnapshot is null or empty")
                } else {
                    val items = ArrayList<Coin>()
                    if (sourceChoiceIsWallet) {
                        for ((_, coinJsonString) in sourceSnapshot) {
                            if (coinJsonString == COIN_DEPOSITED) {
                                // The coin has already been sent away or deposited. Don't list
                                // it as an option
                                continue
                            } else {
                                val coinJson = try {
                                    JSONObject(coinJsonString.toString())
                                } catch (e: JSONException) {
                                    Log.e(tag, "[pullFromDatabase] JSON String is not "
                                            +"COIN_DEPOSITED but JSON cast still failed.")
                                    JSONObject()
                                }

                                val id: String? = try {
                                    coinJson.getString(ID)
                                } catch (e: JSONException) {
                                    Log.e(tag, "[pullFromDatabase] Encountered exception $e "
                                            + "when getting ID from coin.")
                                    null
                                }
                                val currency: String? = try {
                                    coinJson.getString(CURRENCY)
                                } catch (e: JSONException) {
                                    Log.e(tag, "[pullFromDatabase] Encountered exception $e "
                                            + "when getting currency from coin.")
                                    null
                                }
                                val coinValue: Double? = try {
                                    coinJson.getDouble(VALUE)
                                } catch (e: JSONException) {
                                    Log.e(tag, "[pullFromDatabase] Encountered exception $e "
                                            + "when getting coinValue from coin.")
                                    null
                                }

                                if (id != null && currency != null && coinValue != null) {
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
                                Log.d(tag, "[pullFromDatabase] No coins in message from "
                                                 + "${message.senderEmail} at ${message.timestamp}")
                            } else {
                                attachedCoins.forEach { coin -> coinToMessage?.put(coin, message) }
                                items.addAll(attachedCoins)
                            }
                        }
                    }

                    // Sort coins descendently by value
                    items.sortByDescending { coin -> coin.value }

                    val coinsAdapter = CoinAdapter(
                            this@BankActivity, items, true)
                    coinsListView?.choiceMode = CHOICE_MODE_MULTIPLE
                    coinsListView?.adapter = coinsAdapter
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[pullFromDatabase] Coin source get failed: $e")
            }
        }

        // Also get the amount of coins already deposited today and the user's current bank credit
        upperTextView.text = "Updating your bank info..."
        firestoreBank?.get()?.run {
            addOnSuccessListener { docSnapshot ->
                var goldAmount = docSnapshot.get(GOLD_FIELD_TAG) as? Double
                if (goldAmount == null) {
                    // The user has not deposited any coins before and thus has 0 credit in their
                    // bank.
                    goldAmount = 0.0
                }

                goldInBank = goldAmount

                val todaysDateCp = todaysDate // copy field for thread safety
                if (todaysDateCp == null) {
                    Log.e(tag, "[pullFromDatabase] todaysDateCp is null, cannot get number "
                            + "of coins deposited already.")
                } else {
                    var depositedToday = docSnapshot.get(todaysDateCp) as? Long
                    if (depositedToday == null) {
                        Log.d(tag, "[pullFromDatabase] depositedToday is null")
                        // The user has not deposited any coins today.
                        depositedToday = 0
                    }
                    coinsDepositedToday = depositedToday
                }


                // Update the text displayed to the user appropriately
                upperTextView?.text = ("Coins deposited from wallet today: $coinsDepositedToday."
                        + "\nCurrent bank credit: ${String.format("%.2f", goldInBank)} GOLD.")
            }

            addOnFailureListener { e ->
                Log.e(tag, "[pullFromDataBase] Bank get failed: $e")
                upperTextView?.text = "Could not find your bank account."
            }
        }
    }

    /**
     * Deposits the currently selected coins in [coinsListView] to the user's bank.
     */
    private fun depositSelectedCoins() {

        // Display the progress bar to let the user know that we are waiting for a database
        // access
        bankProgressBar?.visibility = View.VISIBLE

        // Get copies of the fields for thread safety
        val sourceModeIsWallet = choiceIsWallet
        val previouslyDepositedAmount = coinsDepositedToday
        val previousCredit = goldInBank

        // Set up the source of the coins depending on the user's choice so we know
        // which document to update
        val source: DocumentReference? = when (sourceModeIsWallet) {
            true -> firestoreWallet
            else -> firestoreInbox
        }

        // First of all disable the deposit button until we're done depositing
        depositButton?.isEnabled = false

        // Variables to set
        var depositAmount = 0.0
        val sourceUpdate = HashMap<String, String>()

        // Get the coins selected by the user
        val ticks: SparseBooleanArray = coinsListView.checkedItemPositions

        // Loop over the coins and extract their details
        val listLength = coinsListView.count
        for (i in 0 until listLength) {
            if (ticks[i]) {
                // The item at this position is ticked. Deposit it
                val coin: Coin? = coinsListView.getItemAtPosition(i) as? Coin
                val currency: String? = coin?.currency
                val value: Double? = coin?.value
                val id: String? = coin?.id
                val exchangeRate: Double? = rates?.get(currency) as? Double

                when {
                    coin == null -> {
                        Log.e(tag, "[depositSelectedCoins] Could not cast item at pos $i to "
                                + "coin")
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

                        // All properties extracted are non-null.
                        // Update the amount of gold the user is depositing
                        depositAmount += value * exchangeRate

                        // Add the coin to the update map accordingly to whether it is from
                        // the wallet or the inbox
                        if (sourceModeIsWallet) {
                            // The coin came from the wallet, just mark it as deposited
                            sourceUpdate["`$currency|$id`"] = COIN_DEPOSITED
                        } else {
                            // Otherwise it came from the inbox; update the message it was
                            // attached to by removing this coin.
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

        if (previouslyDepositedAmount != null
                && sourceUpdate.size > (25 - previouslyDepositedAmount)
                && sourceModeIsWallet) {
            // The user is trying to deposit more coins than they are allowed to today.
            // Show them an error dialog
            alert {
                title = "Your banking is out of control!"
                message = ("It looks like you're trying to deposit ${sourceUpdate.size} coins "
                        + "from your wallet, however you can only deposit "
                        + "${25 - previouslyDepositedAmount} due "
                        + "to the maximum of 25 deposited coins from the wallet per day.")

                positiveButton("Got it!"){
                    this@BankActivity.enableFurtherDeposits()
                }
            }.show()

            // Return early as we do not want to go through with the transaction
            return
        }

        // Update the source if needed
        if (!sourceUpdate.isEmpty() && depositAmount > 0) {
            when {
                source == null -> {
                    Log.e(tag, "[depositSelectedCoins] Want to update source but ref to it "
                            + "is null")
                    enableFurtherDeposits()
                }
                previouslyDepositedAmount == null -> {
                    Log.e(tag, "[depositSelectedCoins] previouslyDepositedAmount null")
                }
                previousCredit == null -> {
                    Log.e(tag, "[depositSelectedCoins] previousCredit null")

                }
                else -> {
                    sourceUpdateDone = false
                    creditUpdateDone = false
                    updateSourceWithDepositedCoins(source, sourceUpdate)
                    val newBankCredit = depositAmount + previousCredit
                    val newDepositedCounter = if (sourceModeIsWallet) {
                        // If the user is depositing from the wallet we want to update the counter.
                        previouslyDepositedAmount + sourceUpdate.size
                        // If not, pass the update to the value it was already at
                    } else {
                        previouslyDepositedAmount
                    }

                    setUsersBankStatus(newBankCredit, newDepositedCounter)
                }
            }
        } else {
            Log.e(tag, "[depositSelectedCoins] SourceUpdate is empty or depositAmount is 0")
            enableFurtherDeposits()
        }

    }

    /**
     * Updates the target source (whether inbox or wallet) so as to remove the deposited coins.
     *
     * @param source the document reference for the source the coin was retrieved from.
     * @param sourceUpdate a map with the data to set in the source.
     */
    private fun updateSourceWithDepositedCoins(
            source: DocumentReference, sourceUpdate: Map<String, Any>) {

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
     * Sets the user's bank credit and updates the counter for today's deposited wallet coins.
     *
     * @param credit the amount of GOLD to set the user's credit to.
     * @param numberOfDepositedFromWallet the number of coins deposited from the wallet today
     */
    private fun setUsersBankStatus(credit: Double, numberOfDepositedFromWallet: Long) {

        val currentDate = todaysDate  // copy field for thread safety

        if (currentDate == null) {
            Log.e(tag, "[setUsersBankStatus] currentDate is null, aborting")
        } else {
            val updateMap = mapOf(
                    GOLD_FIELD_TAG to credit, currentDate to numberOfDepositedFromWallet)

            // Set the bank data as desired
            firestoreBank?.set(updateMap)?.run {
                addOnSuccessListener { _ ->
                    Log.d(tag, "[setUsersBankStatus] Succeeded.")
                    creditUpdateDone = true
                    enableFurtherDeposits()
                    toast("Successfully deposited your coins!")
                }

                addOnFailureListener { e ->
                    Log.e(tag, "[setUsersBankStatus] Failed: $e")
                }
            }
        }
    }

    /**
     * If both updates are done invokes [pullFromDatabase] and re-enables the [depositButton].
     */
    private fun enableFurtherDeposits() {
        if (creditUpdateDone && sourceUpdateDone) {
            // If both credit update and wallet update succeeded, enable further
            // depositing and refill the listview
            pullFromDatabase()
            depositButton?.isEnabled = true
            bankProgressBar?.visibility = View.GONE
        }
    }
}
