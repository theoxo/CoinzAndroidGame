package com.coinzgame.theoxo.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast

/**
 * A login screen that offers login via email/password, authenticating via Firebase.
 */
class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"

    private var mAuth : FirebaseAuth? = null

    private var emailEmpty : Boolean = true
    private var pwEmpty : Boolean = true

    /**
     * Adds text and click listeners to the screen and sets up the authentication service.
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize authentication service
        mAuth = FirebaseAuth.getInstance()

        email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not interested
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not interested
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                emailEmpty = email.text.isEmpty()
                updateButtons()
            }
        })

        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not interested
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not interested
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pwEmpty = password.text.isEmpty()
                updateButtons()
            }
        })

        email_sign_in_button.setOnClickListener {
            signInUser(email.text.toString(), password.text.toString())
        }

        email_register_button.setOnClickListener {
            createUser(email.text.toString(), password.text.toString())
        }

        // Check if this is the first time the app is being run on this device; if so,
        // set up the alarms for ancient coin spawn timings.
        val storedPrefs = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (!storedPrefs.getBoolean(FIRST_TIME_RUNNING, false)) {
            Log.d(tag, "[onCreate] First time running the app")
            val alarmSetupIntent = Intent()
            alarmSetupIntent.action = FIRST_RUN_ACTION
            sendBroadcast(alarmSetupIntent)

            // Mark that it is no longer the first time this app is running.
            val editor = storedPrefs.edit()
            editor.putBoolean(FIRST_TIME_RUNNING, true)
            editor.apply()
        } else {
            Log.d(tag, "[onCreate] Not first run of app")
        }

        // Check if the user is already logged in (that is, has logged in on this device before
        // without explicitly logging out)
        val currentUser = mAuth?.currentUser
        if (currentUser != null) {
            Log.d(tag, "User already logged in, moving on to main")
            val email : String? = currentUser.email
            if (email == null) {
                Log.e(tag, "[onCreate] User is already logged in but email is null")
            } else {
                startMain(email)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // If we return to this activity by logging out, make sure the buttons are appropriately
        // visible
        updateButtons()

    }

    /**
     * Enables and disables the "Sign in" and "Register" buttons depending on the text values.
     * This is so that the [FirebaseAuth] methods do not throw errors due to the text values
     * being null.
     */
    private fun updateButtons() {
        if (!pwEmpty && !emailEmpty) {
            // If both textfields are non-empty, enable the buttons
            email_sign_in_button.isEnabled = true
            email_register_button.isEnabled = true
        } else {
            email_sign_in_button.isEnabled = false
            email_register_button.isEnabled = false
        }
    }

    /**
     * Attempts to create a new user with the given login information.
     *
     * @param email The email account to use to set up the account.
     * @param password The password to use for the account.
     */
    private fun createUser(email : String, password : String) {
        progressBar.visibility = View.VISIBLE
        email_sign_in_button.isEnabled = false
        email_register_button.isEnabled = false
        try {
            mAuth?.createUserWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener {
                        // Whatever the result is, hide the progress bar upon completion
                        this@LoginActivity.progressBar.visibility = View.GONE

                        if (it.isSuccessful) {
                            Log.d(tag, "[createUser]: Succesful")
                            toast("Account creation succesful")
                            startMain(email)
                        } else {
                            this@LoginActivity.email_sign_in_button.isEnabled = true
                            this@LoginActivity.email_register_button.isEnabled = true
                            Log.d(tag, "[createUser]: Failed")
                            toast("Account creation failed. Are you already a registered user?")
                        }
                    }
        } catch (e: FirebaseException) {
            Log.e(tag, "[createUser] Firebase exception $e")
        }
    }

    /**
     * Attempts to sign in the user with the given account information.
     *
     * @param email the email corresponding to the account.
     * @param password the password to log in to the account with.
     */
    private fun signInUser(email : String, password : String) {
        progressBar.visibility = View.VISIBLE
        email_sign_in_button.isEnabled = false
        email_register_button.isEnabled = false
        try {
            mAuth?.signInWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener {
                        // Whatever the result is, hide the progress bar upon completion
                        this@LoginActivity.progressBar.visibility = View.GONE

                        if (it.isSuccessful) {
                            Log.d(tag, "[signInUser]: Successful")
                            toast("Sign in successful!")
                            startMain(email)
                        } else {
                            this@LoginActivity.email_sign_in_button.isEnabled = true
                            this@LoginActivity.email_register_button.isEnabled = true
                            Log.d(tag, "[signInUser]: Failed")
                            toast("Sign in failed. Have you entered your details correctly?")
                        }
                    }
        } catch (e: FirebaseException) {
            Log.e(tag, "[signInUser]: Log in failed with exception $e")
        }
    }

    /**
     * Starts a new [MainActivity].
     */
    private fun startMain(email: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(USER_EMAIL, email)
        startActivity(intent)
    }

    /**
     * If the user has requested to be logged out, sign them out of the [FirebaseAuth] instance.
     * Otherwise, pass the intent on to the super function.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Might have received intent to log out here
        val extras = intent?.extras
        if (extras == null) {
            Log.d(tag, "[onResume] No extras in intent")
        } else {
            val shouldLogoutUser : Boolean = extras.getBoolean(LOGOUT_FLAG, false)
            if (shouldLogoutUser) {
                Log.d(tag, "[onResume] Logging out the user")
                mAuth?.signOut()
            } else {
                Log.d(tag, "[onResume] Extras non-null but log out not requested")
            }
        }
    }
}
