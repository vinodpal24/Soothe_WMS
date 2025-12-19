package com.soothe.sapApplication.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName

data class ScanedOrderBatchedItems(
    @SerializedName("odata.metadata") var odataMetadata: String,
    var value: ArrayList<Value>
    ){
    data class Value (
        var DocEntry: String,
        var ItemCode: String,
        var ItemDescription: String,
        var Status: String,
        var Batch: String,
        var BatchAttribute1: Any? = null,
        var BatchAttribute2: Any? = null,
        var AdmissionDate: String,
        var ManufacturingDate: Any? = null,
        var ExpirationDate: Any? = null,
        var Details: Any? = null,
        var SystemNumber: Long,
        var U_Length: Double,
        var U_Width: Double,
        var U_GSM: Double,
        var U_GW: Double = 0.0,
        var U_NW: Any? = null,
        var U_Type: String,
        var U_AGSM: Double,
        var U_GType: Any? = null,
        var U_RQ: Double,
        var U_PC: Any? = null,
        var U_RG: String
    )
}
