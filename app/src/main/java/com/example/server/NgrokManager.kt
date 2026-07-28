package com.example.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object NgrokManager {
    private var process: Process? = null
    var currentUrl: String? = null

    suspend fun start(context: Context, port: Int, authtoken: String, onUrlReady: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (authtoken.isBlank()) return@withContext
        stop()

        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
        val ngrokFile = File(nativeLibraryDir, "libngrok.so")
        
        if (!ngrokFile.exists()) {
            Log.e("NgrokManager", "ngrok binary not found at ${ngrokFile.absolutePath}")
            return@withContext
        }

        try {
            val pb = ProcessBuilder(
                ngrokFile.absolutePath,
                "http", port.toString(),
                "--authtoken", authtoken,
                "--log", "stdout",
                "--log-format", "json"
            )
            process = pb.start()

            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                Log.d("NgrokManager", "ngrok: $line")
                if (line?.contains("\"obj\":\"tunnels\"") == true && line?.contains("\"url\":\"https://") == true) {
                    val regex = "\"url\":\"(https://[^\"]+)\"".toRegex()
                    val match = regex.find(line!!)
                    if (match != null) {
                        val url = match.groupValues[1]
                        currentUrl = url
                        Log.d("NgrokManager", "Ngrok URL established: $url")
                        withContext(Dispatchers.Main) {
                            onUrlReady(url)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        process?.destroy()
        process = null
        currentUrl = null
    }
}
