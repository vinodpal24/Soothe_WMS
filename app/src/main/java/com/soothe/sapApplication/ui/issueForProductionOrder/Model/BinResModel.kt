package com.soothe.sapApplication.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class BinResModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
   // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<Value>,
    ):Serializable  {
    data class Value(
        var ItemCode: String,
        var U_BIN: String

    ) : Serializable



}