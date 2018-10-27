package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AbsListView.CHOICE_MODE_MULTIPLE
import android.widget.ArrayAdapter
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_bank.*
import kotlinx.android.synthetic.main.list_item.*
import org.json.JSONObject

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"

    // Firebase Firestore database
    private var firestore :  FirebaseFirestore? = null
    private var firestoreWallet : DocumentReference? = null
    private var currentUserEmail : String? = null

    private var walletContents : MutableMap<String, Any>? = null
    private var rates : JSONObject? = null


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
            firestoreWallet = firestore?.collection(emailTag)?.document(WALLET_DOCUMENT)
            firestoreWallet?.get()?.run {
                addOnSuccessListener { docSnapshot ->
                    walletContents = docSnapshot.data
                    updateListView()
                }

                addOnFailureListener { e ->
                    Log.e(tag, "[onCreate] Wallet get failed: $e")
                }
            }

        }

    }

    private fun updateListView() {
        val walletSnapshot = walletContents?.toSortedMap()
        when {
            walletSnapshot == null -> Log.e(tag, "[updateListView] walletSnapshot is null")
            else -> {
                val items = ArrayList<Coin>()
                for ((key, value) in walletSnapshot) {
                    val currency = key.substringBefore("|")
                    val id = key.substringAfter("|")
                    val coinValue = value as? String
                    if (coinValue == null) {
                        Log.e(tag, "[updateListView] coinValue of $currency $id is null")
                    } else {
                        val coin = Coin(id, currency, coinValue)
                        items.add(coin)
                    }
                }
                val coinsAdapter = CoinsAdapter(this, items)

                coinsListView.choiceMode = CHOICE_MODE_MULTIPLE
                coinsListView.adapter = coinsAdapter
            }
        }
    }
}
