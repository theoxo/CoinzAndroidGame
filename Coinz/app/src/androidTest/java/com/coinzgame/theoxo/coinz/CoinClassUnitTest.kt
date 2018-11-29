package com.coinzgame.theoxo.coinz

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom

@SmallTest
@RunWith(AndroidJUnit4::class)
class CoinClassUnitTest {

    private var coinJson: JSONObject? = null
    private val coinId = "testId"
    private val coinCurrency = "testCurrency"
    private val coinValue = ThreadLocalRandom.current().nextDouble()

    @Before
    fun setUpCoinJson() {
        val coin = Coin(coinId, coinCurrency, coinValue)
        coinJson = coin.toJSON()
    }

    @Test
    fun idIsCorrect() {
        assertEquals(coinId, coinJson?.get(ID))
    }

    @Test
    fun currencyIsCorrect() {
        assertEquals(coinCurrency, coinJson?.get(CURRENCY))
    }

    @Test
    fun valueIsCorrect() {
        assertEquals(coinValue, coinJson?.get(VALUE))
    }

}