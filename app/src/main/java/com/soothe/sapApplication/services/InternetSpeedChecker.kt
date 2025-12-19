package com.soothe.sapApplication.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Build
import android.widget.Toast
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object InternetSpeedChecker {
    private const val MIN_SPEED_THRESHOLD_MBPS = 100 // Minimum speed threshold in Mbps

    fun checkAndShowToastIfSlow(context: Context) {
        if (isNetworkConnected(context)) {
            val currentSpeedMbps = getCurrentSpeedMbps(context)
            if (currentSpeedMbps < MIN_SPEED_THRESHOLD_MBPS) {
                // Internet speed is slower than threshold, show toast
                Toast.makeText(context, "Internet speed is slow!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // No internet connection, show toast
            Toast.makeText(context, "No internet connection!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        }
        return false
    }

    private fun getCurrentSpeedMbps(context: Context): Float {
        // This is a placeholder method, actual implementation of measuring internet speed is complex and requires additional libraries or services.
        // For simplicity, we're returning a constant value here.
        var temp =  SpeedTestTask(context).execute(FILE_URL)
        return 20f // Assuming a constant speed of 100 Mbps
    }


    //todo find out internet speed
    fun measureSpeedAndShowToast(context: Context) {
        SpeedTestTask(context).execute(FILE_URL)
    }


    private const val FILE_URL = "http://ipv4.download.thinkbroadband.com/1MB.zip" // URL of a small file (1MB)
    private const val TIMEOUT_MILLISECONDS = 5000 // Timeout for the connection in milliseconds

    private class SpeedTestTask(private val context: Context) : AsyncTask<String, Void, Float>() {

        override fun doInBackground(vararg urls: String): Float {
            val startTime = System.currentTimeMillis()
            var speed: Float = 0f

            try {
                val url = URL(urls[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MILLISECONDS
                connection.readTimeout = TIMEOUT_MILLISECONDS
                connection.connect()

                val contentLength = connection.contentLength // Size of the file
                val inputStream: InputStream = url.openStream()
                val buffer = ByteArray(1024)
                var bytesRead: Int

                var totalBytesRead: Long = 0
                val endTime: Long

                do {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead >= 0) {
                        totalBytesRead += bytesRead
                    }
                } while (bytesRead > 0)

                endTime = System.currentTimeMillis()

                // Calculate speed in Mbps
                val totalTimeSeconds = (endTime - startTime) / 1000.0
                speed = (totalBytesRead * 8 / 1024 / 1024 / totalTimeSeconds).toFloat()

                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return speed
        }


        override fun onPostExecute(result: Float) {
            // Show toast with measured internet speed
            Toast.makeText(context, "Internet speed: $result Mbps", Toast.LENGTH_SHORT).show()
        }

    }




}
