package com.coinzgame.theoxo.coinz

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom

/**
 * Tests whether the [Coin] class functions as expected.
 * This unit test needs to be run as an instrumented test since it relies on [JSONObject].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class CoinClassInstrumentedUnitTest {

    private var coinJson: JSONObject? = null
    private val coinId = "testId"
    private val coinCurrency = "testCurrency"
    private val coinValue = ThreadLocalRandom.current().nextDouble()

    /**
     * Set up the [Coin] and get its corresponding [JSONObject].
     */
    @Before
    fun setUpCoinJson() {
        val coin = Coin(coinId, coinCurrency, coinValue)
        coinJson = coin.toJSON()
    }

    /**
     * Check that the coin's id matches in the JSON.
     */
    @Test
    fun idIsCorrect() {
        assertEquals(coinId, coinJson?.get(ID))
    }

    /**
     * Check that the coin's currency matches in the JSON.
     */
    @Test
    fun currencyIsCorrect() {
        assertEquals(coinCurrency, coinJson?.get(CURRENCY))
    }

    /**
     * Check that the coin's value matches in the JSON.
     */
    @Test
    fun valueIsCorrect() {
        assertEquals(coinValue, coinJson?.get(VALUE))
    }

}