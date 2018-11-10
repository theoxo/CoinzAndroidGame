package com.coinzgame.theoxo.coinz

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * [AsyncTask] which downloads a file from a remote server using a [HttpURLConnection].
 * In this case, used to download the coin locations.
 * Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete] method,
 * passing the downloaded data as a string.
 *
 * @param[caller] the object implementing [DownloadCompleteListener] to send the results back to.
 */
class DownloadFileTask(private val caller : DownloadCompleteListener):
        AsyncTask<String, Void, String>() {

    private val tag = "DownloadFileTask"

    /**
     * The background task to perform; downloads the data in the file specified.
     *
     * @param urls one or more urls, the first of which is assumed to be the target.
     * @return a [String] of the data downloaded.
     */
    override fun doInBackground(vararg urls : String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (ex: IOException) {
        "Unable to load content, check network connection"
    }

    /**
     * Invokes downloading the file and transforms the resulting data into a [String].
     *
     * @param urlString the URL to download the data from.
     * @return the [String] representation of the downloaded data.
     */
    private fun loadFileFromNetwork(urlString : String) : String {
        val stream : InputStream = downloadUrl(urlString)

        return stream.bufferedReader().use { it.readText() }
    }

    /**
     * Sets up the HTTP connection and gets the [InputStream] for the file at the given URL.
     *
     * @throws IOException
     * @param urlString the URL to download the data from.
     * @return the [InputStream] of the downloaded data.
     */
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String) : InputStream {
        Log.d(tag, "[downloadUrl] Starting the HTTP connection.")
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection

        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()

        return conn.inputStream

    }

    /**
     * Listener for the task having finished. Sends the resulting string back to the caller.
     *
     * @param result the resulting string built from the downloaded data.
     */
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }
}