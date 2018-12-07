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
 * A simple fragment allowing the user access to account-specific features such as signing out.
 *
 * @property mainActivity the [MainActivity] which the fragment has been attached to
 */
class AccountFragment : Fragment() {

    private var fragTag = "AccountFragment"
    private var mainActivity: MainActivity? = null

    /**
     * Saves the [MainActivity] which the fragment is attached to for later reference.
     *
     * @param context the context which the fragment has been attached to
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // We only expect to be attached to a MainActivity so this cast wil not result in null
        // unless the context is null
        mainActivity = context as? MainActivity
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "[onCreateView] Inflating view")
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(fragTag, "[onViewCreated] Setting up button listener")
        super.onViewCreated(view, savedInstanceState)

        // Set up the click event for the signing out button
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
