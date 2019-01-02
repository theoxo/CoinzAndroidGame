[com.coinzgame.theoxo.coinz](../index.md) / [DownloadFileTask](.)

# DownloadFileTask

`class DownloadFileTask : Any`

[AsyncTask](#) which downloads a file from a remote server using a [HttpURLConnection](http://docs.oracle.com/javase/6/docs/api/java/net/HttpURLConnection.html).
In this case, used to download the coin locations.
Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete](../-download-complete-listener/download-complete.md) method,
passing the downloaded data as a string.

### Parameters

`caller` - the object implementing [DownloadCompleteListener](../-download-complete-listener/index.md) to send the results back to.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `DownloadFileTask(caller: `[`DownloadCompleteListener`](../-download-complete-listener/index.md)`)`<br>[AsyncTask](#) which downloads a file from a remote server using a [HttpURLConnection](http://docs.oracle.com/javase/6/docs/api/java/net/HttpURLConnection.html).
In this case, used to download the coin locations.
Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete](../-download-complete-listener/download-complete.md) method,
passing the downloaded data as a string. |

### Functions

| Name | Summary |
|---|---|
| [doInBackground](do-in-background.md) | `fun doInBackground(vararg urls: String): String`<br>The background task to perform; downloads the data in the file specified. |
| [onPostExecute](on-post-execute.md) | `fun onPostExecute(result: String): Unit`<br>Listener for the task having finished. Sends the resulting string back to the caller. |
