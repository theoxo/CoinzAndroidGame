package com.coinzgame.theoxo.coinz

/**
 * Simple interface which defines the downloadComplete function, allowing for the downloaded
 * coin locations to be passed back to the caller.
 */
interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}
