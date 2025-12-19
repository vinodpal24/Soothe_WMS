package com.soothe.sapApplication.Retrofit_Api

import android.content.Context
import com.soothe.sapApplication.Global_Classes.VpnUtils
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class VpnCheckInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!VpnUtils.isVpnConnected(context)) {
            throw IOException("VPN_Exception")
        }
        return chain.proceed(chain.request())
    }
}
