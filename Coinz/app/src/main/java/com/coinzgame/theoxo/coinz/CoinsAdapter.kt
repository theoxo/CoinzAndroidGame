package com.coinzgame.theoxo.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.coinzgame.theoxo.coinz.R.id.coinListItemText

class CoinsAdapter(context : Context, coins : ArrayList<Coin>) : ArrayAdapter<Coin>(context, 0, coins) {

    private lateinit var view : View

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val coin : Coin? =  getItem(position)

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        } else {
            view = convertView
        }

        val tvItem = view.findViewById<CheckedTextView>(R.id.coinListItemText)

        tvItem?.text = "${coin?.currency}: ${coin?.value?.take(5)}..."

        return view
    }
}