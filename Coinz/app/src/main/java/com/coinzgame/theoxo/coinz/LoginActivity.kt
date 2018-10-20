package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {

    public var mAuth : FirebaseAuth? = null

    private val TAG = "LoginActivity"

    private var emailEmpty : Boolean = true
    private var pwEmpty : Boolean = true

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
                if (email.text.toString().trim().length == 0) {
                    emailEmpty = true
                } else {
                    emailEmpty = false
                }

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
                if (password.text.toString().trim().length == 0) {
                    pwEmpty = true
                } else {
                    pwEmpty = false
                }

                updateButtons()
            }
        })

        email_sign_in_button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                signInUser(email.text.toString(), password.text.toString())
            }
        })

        email_register_button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                createUser(email.text.toString(), password.text.toString())
            }
        })

        val currentUser = mAuth?.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, moving on to main")
            startMain()
        }
    }

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

    private fun createUser(email : String, password : String) {
        mAuth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this, object : OnCompleteListener<AuthResult> {
                    override fun onComplete(task : Task<AuthResult>) {
                        if (task.isSuccessful) {
                            Log.d(TAG, "[createUser]: Succesful")
                            toast("Account creation succesful")
                            startMain()
                        } else {
                            Log.d(TAG, "[createUser]: Failed")
                            toast("Account creation failed")
                        }
                    }
                })
    }
    private fun signInUser(email : String, password : String) {
        mAuth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this, object : OnCompleteListener<AuthResult> {
                    override fun onComplete(task : Task<AuthResult>) {
                       if (task.isSuccessful) {
                           Log.d(TAG, "[signInUser]: Successful")
                           toast("Sign in successful!")
                           startMain()
                       } else {
                           Log.d(TAG, "[signInUser]: Failed")
                           toast("Sign in failed")
                       }
                    }
                })
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Might have received intent to log out here
        val extras = intent?.extras
        if (extras == null) {
            Log.d(TAG, "[onResume] No extras in intent")
        } else {
            val shouldLogoutUser : Boolean = extras.getBoolean(LOGOUT_FLAG, false)
            if (shouldLogoutUser) {
                Log.d(TAG, "[onResume] Logging out the user")
                mAuth?.signOut()
            } else {
                Log.d(TAG, "[onResume] Extras non-null but log out not requested")
            }
        }
    }
}
