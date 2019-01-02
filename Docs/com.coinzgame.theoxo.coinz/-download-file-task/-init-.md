[com.coinzgame.theoxo.coinz](../index.md) / [DownloadFileTask](index.md) / [&lt;init&gt;](.)

# &lt;init&gt;

`DownloadFileTask(caller: `[`DownloadCompleteListener`](../-download-complete-listener/index.md)`)`

[AsyncTask](#) which downloads a file from a remote server using a [HttpURLConnection](http://docs.oracle.com/javase/6/docs/api/java/net/HttpURLConnection.html).
In this case, used to download the coin locations.
Upon finishing the task, invokes the caller's [DownloadCompleteListener.downloadComplete](../-download-complete-listener/download-complete.md) method,
passing the downloaded data as a string.

### Parameters

`caller` - the object implementing [DownloadCompleteListener](../-download-complete-listener/index.md) to send the results back to.