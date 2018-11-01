package com.coinzgame.theoxo.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_inbox.*

private var currentUserEmail : String? = null

class InboxActivity : AppCompatActivity(),
                        BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        newEmailButton.setOnClickListener {_ -> startMailCreationActivity() }

        bottom_nav_bar.setOnNavigationItemSelectedListener(this)
    }

    private fun startMailCreationActivity() {
        val intent = Intent(this, MailCreationActivity::class.java)
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
    }

    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_nav -> startMainActivity()
            R.id.account_nav -> startAccountActivity()
            else -> return true //do nothing
        }

        return true
    }

    /**
     * Reorders the previous [MainActivity] back to the front.
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    /**
     * Starts a new [AccountActivity].
     */
    private fun startAccountActivity() {
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
    }
}
