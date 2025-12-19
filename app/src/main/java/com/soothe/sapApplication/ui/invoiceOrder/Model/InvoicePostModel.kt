package com.soothe.sapApplication.ui.invoiceOrder.Model

import com.google.gson.annotations.SerializedName

data class InvoicePostModel(
    val Canceled: String,
    val CreateDate: String,
    val CreateTime: String,
    val Creator: String,
    val DBS_ROWCollection: List<DBSROWCollection>,
    val DataSource: String,
    val DocEntry: Int,
    val DocNum: Int,
    val Handwrtten: String,
    val Instance: Int,
    val LogInst: Any,
    val Object: String,
    val Period: Int,
    val Remark: Any,
    val RequestStatus: String,
    val Series: Int,
    val Status: String,
    val Transfered: String,
    val U_ARInvoice: Any,
    val U_BPCode: String,
    val U_BPName: String,
    val U_DocumentNo: Any,
    val U_PostingDate: String,
    val U_SDNo: Any,
    val U_SDT: String,
    val UpdateDate: String,
    val UpdateTime: String,
    val UserSign: Int,
    @SerializedName("odata.metadata") val metadata: String,
    @SerializedName("odata.eta") val etadata: String
){
    data class DBSROWCollection(
        val DocEntry: Int,
        val U_ITEMLINENO: Int,
        val LineId: Int,
        val LogInst: Any,
        val Object: String,
        val U_BoxQty: Int,
        val U_ItemCode: String,
        val U_ItemName: String,
        val U_NavisionCode: String,
        val U_PktQty: Int,
        val U_SBQ: Int,
        val U_SPQ: Int,
        val VisOrder: Int
    )
}