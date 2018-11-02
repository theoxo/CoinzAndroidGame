package com.coinzgame.theoxo.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.TextView

class CoinsAdapter(context : Context, coins : ArrayList<Coin>, private var hasCheckmarks : Boolean)
    : ArrayAdapter<Coin>(context, 0, coins) {

    private lateinit var view : View

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val coin : Coin? =  getItem(position)

        when {
            convertView != null -> {
                view = convertView
            }
            hasCheckmarks -> {
                view = LayoutInflater.from(context).inflate(R.layout.checked_list_item, parent, false)
            }
            else -> {
                view = LayoutInflater.from(context).inflate(R.layout.standard_list_item, parent, false)
            }
        }

        if (hasCheckmarks) {
            val tvItem = view.findViewById<CheckedTextView>(R.id.checkedListItemText)
            tvItem?.text = "${coin?.currency}: ${coin?.value?.take(5)}..."
        } else {
            val tvItem = view.findViewById<TextView>(R.id.standardListItemText)
            tvItem?.text = "${coin?.currency}: ${coin?.value?.take(5)}..."
        }

        return view
    }
}