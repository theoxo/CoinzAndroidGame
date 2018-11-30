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
class CoinAdapter(context : Context, coins : ArrayList<Coin>,
                  private var listIsMultipleChoice : Boolean)
    : ArrayAdapter<Coin>(context, 0, coins) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val coin : Coin? =  getItem(position)

        val view : View = when {
            convertView != null -> {
                // If the view is already given, simply use it
               convertView
            }
            listIsMultipleChoice -> {
                // If the list which this adapter will be attached to has multiple-choice
                // selection enabled, inflate a layout with checkmarks.
               LayoutInflater.from(context).inflate(R.layout.checked_list_item, parent,
                       false)
            }
            else -> {
                // Otherwise inflate a layout which doesn't have checkmarks as these will
                // not be necessary.
                LayoutInflater.from(context).inflate(R.layout.standard_list_item, parent,
                        false)
            }
        }

        // Get the appropriate text view depending on the type of list we are attaching to
        val tvItem = if (listIsMultipleChoice) {
            view.findViewById<CheckedTextView>(R.id.checkedListItemText)
        } else {
            view.findViewById<TextView>(R.id.standardListItemText)
        }

        // Set its text by filling in the placeholders with coins currency and value
        tvItem?.text = context.getString(R.string.coinListItemText, coin?.currency, coin?.value)

        return view
    }
}