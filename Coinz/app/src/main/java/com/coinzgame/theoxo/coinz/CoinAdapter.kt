package com.coinzgame.theoxo.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.TextView

/**
 * Provides a specialized [ArrayAdapter] to dynamically list [Coin]s in a list view.
 *
 * @param context the context which is invoking the adapter.
 * @param coins the coins to put in the list.
 * @param listIsMultipleChoice whether the target list view is in multiple choice mode.
 */
class CoinAdapter(context : Context, coins : ArrayList<Coin>, private var listIsMultipleChoice : Boolean)
    : ArrayAdapter<Coin>(context, 0, coins) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val coin : Coin? =  getItem(position)

        val view : View = when {
            convertView != null -> {
               convertView
            }
            listIsMultipleChoice -> {
               LayoutInflater.from(context).inflate(R.layout.checked_list_item, parent, false)
            }
            else -> {
                LayoutInflater.from(context).inflate(R.layout.standard_list_item, parent, false)
            }
        }

        if (listIsMultipleChoice) {
            val tvItem = view.findViewById<CheckedTextView>(R.id.checkedListItemText)
            tvItem?.text = "${coin?.currency}: ${coin?.value?.take(5)}..."
        } else {
            val tvItem = view.findViewById<TextView>(R.id.standardListItemText)
            tvItem?.text = "${coin?.currency}: ${coin?.value?.take(5)}..."
        }

        return view
    }
}