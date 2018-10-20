package com.coinzgame.theoxo.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_account.*

const val LOGOUT_FLAG = "LogOutCall"

class AccountActivity : AppCompatActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener {

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

    override fun onNavigationItemSelected(item : MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_nav -> startMain()
            else -> return true //do nothing
        }

        return true
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    private fun logOutUser() {
        val logoutIntent = Intent(this, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        logoutIntent.putExtra(LOGOUT_FLAG, true)
        startActivity(logoutIntent)
    }
}
