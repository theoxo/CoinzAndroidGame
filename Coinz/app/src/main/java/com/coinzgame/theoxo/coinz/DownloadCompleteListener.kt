package com.coinzgame.theoxo.coinz

/**
 * Simple interface which defines the [downloadComplete] function.
 * This allows the downloaded coin locations to be passed back to the caller.
 */
interface DownloadCompleteListener {

    /**
     * Handles the download having finished, dealing with the result appropriately.
     *
     * @param result the downloaded data.
     */
    fun downloadComplete(result: String)
}
