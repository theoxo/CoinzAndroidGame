package com.coinzgame.theoxo.coinz

/**
 * Simple interface which defines the [downloadComplete] function, allowing for the downloaded
 * coin locations to be passed back to the caller.
 */
interface DownloadCompleteListener {

    /**
     * Should handle the download having finished, dealing with the result appropriately.
     *
     * @param result The [String] built as a result of the download.
     */
    fun downloadComplete(result: String)
}
