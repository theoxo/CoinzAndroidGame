package com.coinzgame.theoxo.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_account.*

/**
 * A simple screen allowing the user access to account-specific features such as signing out.
 */
class AccountActivity : AppCompatActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener {

    private var currentUserEmail: String? = null

    /**
     * Sets up the activity and adds click events to the button(s).
     *
     * @param savedInstanceState the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        currentUserEmail = intent?.getStringExtra(USER_EMAIL)

        log_out_button.setOnClickListener {
                logOutUser()
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener(this)

    }

    override fun onStart() {
        super.onStart()
        // Set default item pressed to account as that is what was pressed to get here
        bottom_nav_bar.selectedItemId = R.id.account_nav
    }

    /**
     * Handles click events on the [BottomNavigationView], causing transitions to other activities.
     *
     * @param item the menu item which was clicked on the [BottomNavigationView].
     */
    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_nav -> startMainActivity()
            R.id.messaging_nav -> startInboxActivity()
            else -> return true //do nothing
        }

        return true
    }

    private fun startInboxActivity() {
        val intent = Intent(this, InboxActivity::class.java)
        //intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Reorders the previous [MainActivity] back to the front.
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        //intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra(USER_EMAIL, currentUserEmail)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Reorders the previous [LoginActivity] to the front and requests that it signs out the user.
     */
    private fun logOutUser() {
        val logoutIntent = Intent(this, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        logoutIntent.putExtra(LOGOUT_FLAG, true)
        startActivity(logoutIntent)
    }
}
