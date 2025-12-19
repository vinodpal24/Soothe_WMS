package com.soothe.sapApplication.Retrofit_Api

import com.soothe.sapApplication.Global_Classes.AppConstants.isDevelopment
import com.soothe.sapApplication.Global_Classes.AppConstants.isOldDevelopment


class ApiConstantForURL {

    enum class ApiType {
        STANDARD, CUSTOM
    }

    private val port: Int
        get() = if (isDevelopment) 9090 else 9090

    private val baseUrlStandard: String
        get() = "http://192.168.0.173:50001/b1s/v1/"

    private val baseUrlCustom: String
        get() {
            return if(isOldDevelopment) "http://192.168.0.173" else "http://192.168.0.173:$port/api/"
        }

    private val baseUrls: Map<ApiType, String> = mapOf(
        ApiType.STANDARD to baseUrlStandard,
        ApiType.CUSTOM to baseUrlCustom
    )

    fun getBaseUrl(apiType: ApiType): String {
        return baseUrls[apiType]
            ?: throw IllegalStateException("Base URL for $apiType not defined")
    }
}