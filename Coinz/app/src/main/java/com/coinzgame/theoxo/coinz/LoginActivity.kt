package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import android.content.Intent
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

    private var mAuth : FirebaseAuth? = null

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize authentication service
        mAuth = FirebaseAuth.getInstance()

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
            Log.d(TAG, "User already logged in ")
            startMain()
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
}
