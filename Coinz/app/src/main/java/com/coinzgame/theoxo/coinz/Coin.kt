package com.coinzgame.theoxo.coinz

import org.json.JSONObject

/**
 * Represents a Coin in the game by specifying its parameters.
 *
 * @param id the unique identifer for the coin.
 * @param currency the currency of the coin.
 * @param value the coin's specified value.
 */
class Coin (val id : String, val currency : String, val value : Double) {

    fun toJSON() : JSONObject {
        val coinJSON = JSONObject()
        coinJSON.put(CURRENCY, currency)
        coinJSON.put(ID, id)
        coinJSON.put(VALUE, value)
        return coinJSON
    }
}