package com.coinzgame.theoxo.coinz

import android.util.Log
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory

/**
 * Provides means to generate appropriate [Icon]s for coins.
 *
 * @param iconFactory the [IconFactory] to generate the icons with.
 */
class CoinIconFactory(private val iconFactory: IconFactory) {

    private val tag = "CoinIconFactory"

    /**
     * Get an [Icon] appropriate for the specified coin.
     *
     * @param id the coin's id.
     * @param currency the coin's currency.
     * @param value the coin's value.
     * @return the icon if appropriate, or null.
     */
    fun getIconForCoin(id: String, currency: String, value: Double) : Icon? {
        return when {
            // Begin by checking if the coin is Ancient. If so,
            // the icon will be the same regardless of the coins value.
            (id.startsWith("ANCIENT")
                    && currency == "SHIL") -> {
                Log.d(tag, "[getIconForCoin] Found an ancient SHIL $id")
                iconFactory.fromResource(R.mipmap.ancient_shil)
            }

            (id.startsWith("ANCIENT")
                    && currency == "DOLR") -> {
                Log.d(tag, "[getIconForCoin] Found an ancient DOLR $id")
                iconFactory.fromResource(R.mipmap.ancient_dolr)
            }

            (id.startsWith("ANCIENT")
                    && currency == "QUID") -> {
                Log.d(tag, "[getIconForCoin] Found an ancient QUID $id")
                iconFactory.fromResource(R.mipmap.ancient_quid)
            }

            (id.startsWith("ANCIENT")
                    && currency == "PENY") -> {
                Log.d(tag, "[getIconForCoin] Found an ancient PENY $id")
                iconFactory.fromResource(R.mipmap.ancient_peny)
            }

            // For non-ancient coins, invoke the appropriate method accordingly to
            // the coin's currency to get an icon which depends on the value of the coin.
            currency == "SHIL" -> {
                getSHILIcon(value)
            }

            currency == "DOLR" -> {
                getDOLRIcon(value)
            }

            currency == "PENY" -> {
                getPENYIcon(value)
            }

            currency == "QUID" -> {
                getQUIDIcon(value)
            }

            // If all else has failed, we cannot recognize the coin which we were given.
            else -> {
                Log.e(tag, "[getIconForCoin] Unrecognized currency $currency for id $id")
                null
            }
        }
    }

    /**
     * Gets an appropriate icon for a SHIL coin depending on its value.
     *
     * @param value the SHIL coin's value.
     * @return the icon generated if appropriate, or null.
     */
    private fun getSHILIcon(value: Double) : Icon? {
        return when {
            value < 0 || value >= 10 -> {
                Log.e(tag, "[getSHILIcon] Unexpected value, icon will be null")
                null
            }
            value < 2 -> {
                iconFactory.fromResource(R.mipmap.shil0_2)
            }
            value < 4 -> {
                iconFactory.fromResource(R.mipmap.shil2_4)
            }
            value < 6 -> {
                iconFactory.fromResource(R.mipmap.shil4_6)
            }
            value < 8 -> {
                iconFactory.fromResource(R.mipmap.shil6_8)
            }
            else -> {
                iconFactory.fromResource(R.mipmap.shil8_10)
            }
        }
    }

    /**
     * Gets an appropriate icon for a DOLRL coin depending on its value.
     *
     * @param value the DOLR coin's value.
     * @return the icon generated if appropriate, or null.
     */
    private fun getDOLRIcon(value: Double) : Icon? {
        return when {
            value < 0 || value >= 10 -> {
                Log.e(tag, "[getDOLRIcon] Unexpected value, icon will be null")
                null
            }
            value < 2 -> {
                iconFactory.fromResource(R.mipmap.dolr0_2)
            }
            value < 4 -> {
                iconFactory.fromResource(R.mipmap.dolr2_4)
            }
            value < 6 -> {
                iconFactory.fromResource(R.mipmap.dolr4_6)
            }
            value < 8 -> {
                iconFactory.fromResource(R.mipmap.dolr6_8)
            }
            else -> {
                iconFactory.fromResource(R.mipmap.dolr8_10)
            }
        }
    }

    /**
     * Gets an appropriate icon for a QUID coin depending on its value.
     *
     * @param value the QUID coin's value.
     * @return the icon generated if appropriate, or null.
     */
    private fun getQUIDIcon(value: Double) : Icon? {
        return when {
            value < 0 || value >= 10 -> {
                Log.e(tag, "[getQUIDIcon] Unexpected value, icon will be null")
                null
            }
            value < 2 -> {
                iconFactory.fromResource(R.mipmap.quid0_2)
            }
            value < 4 -> {
                iconFactory.fromResource(R.mipmap.quid2_4)
            }
            value < 6 -> {
                iconFactory.fromResource(R.mipmap.quid4_6)
            }
            value < 8 -> {
                iconFactory.fromResource(R.mipmap.quid6_8)
            }
            else -> {
                iconFactory.fromResource(R.mipmap.quid8_10)
            }
        }
    }

    /**
     * Gets an appropriate icon for a PENY coin depending on its value.
     *
     * @param value the PENY coin's value.
     * @return the icon generated if appropriate, or null.
     */
    private fun getPENYIcon(value: Double) : Icon? {
        return when {
            value < 0 || value >= 10 -> {
                Log.e(tag, "[getPENYIcon] Unexpected value, icon will be null")
                null
            }
            value < 2 -> {
                iconFactory.fromResource(R.mipmap.peny0_2)
            }
            value < 4 -> {
                iconFactory.fromResource(R.mipmap.peny2_4)
            }
            value < 6 -> {
                iconFactory.fromResource(R.mipmap.peny4_6)
            }
            value < 6 -> {
                iconFactory.fromResource(R.mipmap.peny6_8)
            }
            else -> {
                iconFactory.fromResource(R.mipmap.peny8_10)
            }
        }
    }
}