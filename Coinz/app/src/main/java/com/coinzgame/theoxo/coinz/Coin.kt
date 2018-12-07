package com.coinzgame.theoxo.coinz

import android.support.annotation.VisibleForTesting
import org.json.JSONObject

/**
 * Represents a Coin in the game by specifying its parameters.
 *
 * @param id the unique identifier for the coin.
 * @param currency the currency of the coin.
 * @param value the coin's specified value.
 */
class Coin (
        @VisibleForTesting (otherwise = VisibleForTesting.PACKAGE_PRIVATE) val id: String,
        @VisibleForTesting (otherwise = VisibleForTesting.PACKAGE_PRIVATE) val currency: String,
        @VisibleForTesting (otherwise = VisibleForTesting.PACKAGE_PRIVATE) val value: Double)
{

    /**
     * Turns the coin into a [JSONObject].
     *
     * @return the JSON holding the coin's fields.
     */
    fun toJSON(): JSONObject {
        val coinJSON = JSONObject()
        coinJSON.put(CURRENCY, currency)
        coinJSON.put(ID, id)
        coinJSON.put(VALUE, value)
        return coinJSON
    }
}