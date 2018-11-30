package com.coinzgame.theoxo.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_account.*

/**
 * A simple screen allowing the user access to account-specific features such as signing out.
 */
class AccountFragment : Fragment() {

    private var fragTag = "AccountFragment"
    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainActivity = context as? MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "Fragment created")
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(fragTag, "Fragment created")
        super.onViewCreated(view, savedInstanceState)

        log_out_button.setOnClickListener {
            logOutUser()
        }
    }

    /**
     * Reorders the previous [LoginActivity] to the front and requests that it signs out the user.
     */
    private fun logOutUser() {
        val logoutIntent = Intent(mainActivity, LoginActivity::class.java)
        logoutIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        logoutIntent.putExtra(LOGOUT_FLAG, true)
        startActivity(logoutIntent)
    }
}
