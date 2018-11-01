package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.widget.AbsListView.CHOICE_MODE_MULTIPLE
import android.widget.ArrayAdapter
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_bank.*
import kotlinx.android.synthetic.main.list_item.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.json.JSONObject

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var firestoreBank : DocumentReference? = null
    private var currentUserEmail : String? = null

    private var rates : JSONObject? = null

    private var walletUpdateDone : Boolean = true
    private var creditUpdateDone : Boolean = true

    //private val progressDialog = indeterminateProgressDialog("Updating")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

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

            updateListView()
        }
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

                    val coinsAdapter = CoinsAdapter(this@BankActivity, items)
                    coinsListView.choiceMode = CHOICE_MODE_MULTIPLE
                    coinsListView.adapter = coinsAdapter
                }
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateListView] Wallet get failed: $e")
            }
        }
    }

    private fun depositSelectedCoins() {

        // First of all disable the deposit button until we're done depositing
        depositButton.isEnabled = false

        var depositAmount = 0.0
        val walletUpdate = HashMap<String, String>()
        val ticks : SparseBooleanArray = coinsListView.checkedItemPositions
        val listLength = coinsListView.count
        for (i in 0..listLength-1) {
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
                        depositAmount += exchangeRate * value  // TODO is this the correct interpretation of exchangerate
                        walletUpdate["$currency|$id"] = COIN_DEPOSITED
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
        if (!walletUpdate.isEmpty()) {
            walletUpdateDone = false
            updateWalletWithDepositedCoins(walletUpdate)
        }

    }

    private fun updateWalletWithDepositedCoins(walletUpdate : Map<String, String>) {
        firestoreWallet?.update(walletUpdate)?.run {
            addOnSuccessListener { _ ->
                Log.d(tag, "[updateWalletWithDepositedCoins] Success with "
                                 + "${walletUpdate.size} coins")

                walletUpdateDone = true
                enableFurtherDeposits()
            }

            addOnFailureListener { e ->
                Log.e(tag, "[updateWalletWithDepositedCoins] Failed: $e")
            }
        }
    }
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

    private fun enableFurtherDeposits() {
        if (creditUpdateDone && walletUpdateDone) {
            // If both credit update and wallet update succeeded, enable further
            // depositing redo the list view
            updateListView()
            depositButton.isEnabled = true
        }
    }
}
