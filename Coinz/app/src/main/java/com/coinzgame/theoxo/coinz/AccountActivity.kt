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

    /**
     * Sets up the screen and adds click events to the button(s).
     *
     * @param[savedInstanceState] the previously saved instance state, if it exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        log_out_button.setOnClickListener {
                logOutUser()
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener(this)

        // Set default item pressed to account as that is what was pressed to get here
        bottom_nav_bar.selectedItemId = R.id.account_nav
    }

    /**
     * Handles click events on the [BottomNavigationView], causing transitions to other activities.
     *
     * @param item the [MenuItem] which was clicked on the [BottomNavigationView]
     */
    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_nav -> startMain()
            else -> return true //do nothing
        }

        return true
    }

    /**
     * Reorders the previous [MainActivity] back to the front.
     */
    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
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
