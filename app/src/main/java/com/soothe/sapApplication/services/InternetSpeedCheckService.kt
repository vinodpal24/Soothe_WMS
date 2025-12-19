package com.soothe.sapApplication.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.widget.Toast

class InternetSpeedCheckService : Service() {
    private val MIN_SPEED_THRESHOLD_MBPS = 100 // Minimum speed threshold in Mbps

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkInternetSpeed()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkInternetSpeed() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))) {
                    val currentSpeedMbps = getCurrentSpeedMbps()
                    if (currentSpeedMbps < MIN_SPEED_THRESHOLD_MBPS) {
                        // Internet speed is slower than threshold, show toast
                        showToast("Internet speed is slow!")
                    }
                }
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    val currentSpeedMbps = getCurrentSpeedMbps()
                    if (currentSpeedMbps < MIN_SPEED_THRESHOLD_MBPS) {
                        // Internet speed is slower than threshold, show toast
                        showToast("Internet speed is slow!")
                    }
                }
            }
        }
    }

    private fun getCurrentSpeedMbps(): Float {
        // Placeholder method, actual implementation of measuring internet speed is complex and requires additional libraries or services.
        // For simplicity, we're returning a constant value here.
        return 100f // Assuming a constant speed of 100 Mbps
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
