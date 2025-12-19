package com.soothe.sapApplication.ui.invoiceOrder.Model

data class InvoiceListModel(
    val value: List<Value>
) : java.io.Serializable {
    data class Value(

        val DocEntry: String,

        val DocNum: String,

        val DocType: String,

        var DocDate: String,

        val DocDueDate: String,

        val CardCode: String,

        val CardName: String,

        val DocStatus: String,

        val DocTotal: Double,

        val TaxDate: String,

        val DocumentLines: ArrayList<DocumentLine>,
    ) : java.io.Serializable

    data class DocumentLine(
        var isScanned: Int = 0,
        var initialBoxes: Int = 0,
        val LineNum: Int,
        var totalPktQty: Int = 0,

        val DocEntry: String,

        val ItemCode: String,

        val ItemDescription: String,

        var Quantity: Double,
        val U_IQTY: String,
        val NavCode: String,

        val RemainingQuantity: Double,

        val Price: Double,

        val WarehouseCode: String,
        var NavisionCode: String = "",
    ) : java.io.Serializable
}
