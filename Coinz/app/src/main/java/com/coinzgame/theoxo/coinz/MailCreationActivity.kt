package com.coinzgame.theoxo.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import kotlinx.android.synthetic.main.activity_mail_creation.*

private var currentUserEmail : String? = null

class MailCreationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mail_creation)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        targetEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Not interested
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not interested
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = !targetEmail.text.isEmpty()
            }
        })
    }
}
