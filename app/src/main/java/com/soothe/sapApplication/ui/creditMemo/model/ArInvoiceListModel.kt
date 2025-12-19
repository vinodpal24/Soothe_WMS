package com.soothe.sapApplication.ui.creditMemo.model

import java.io.Serializable

data class ArInvoiceListModel(
    val value: List<Value> = listOf()
) : Serializable {
    data class Value(
        val BPLID: String = "",
        val BPLName: String = "",
        val Cancelled: String = "",
        val CardCode: String = "",
        val CardName: String = "",
        val DocDate: String = "",
        val DocDueDate: String = "",
        val DocEntry: String = "",
        val DocNum: String = "",
        val DocRate: String = "",
        val DocStatus: String = "",
        val DocTotal: Double = 0.0,
        val DocType: String = "",
        val DocumentLines: List<DocumentLine> = listOf(),
        val NumAtCard: String = "",
        val PayToCode: String = "",
        val PaymentMethod: String = "",
        val Series: String = "",
        val SeriesDel: String = "",
        val ShipToCode: String = "",
        val TaxDate: String = ""
    ) : Serializable {
        data class DocumentLine(
            val BinABSEntry: String = "",
            val BinCode: String = "",
            val BinManaged: String = "",
            val DefaultABSEntry: String = "",
            val DefaultBinCD: String = "",
            val DiscountPercent: Double = 0.0,
            val DocEntry: String = "",
            val HSNEntry: String = "",
            val ItemCode: String = "",
            val ItemDescription: String = "",
            val ItemType: String = "",
            val LineNum: Int = 0,
            val NavCode: String = "",
            val Price: Double = 0.0,
            val Quantity: Int = 0,
            val Rate: String = "",
            val RemainingQuantity: Int = 0,
            val TaxCode: String = "",
            val U_ACT_QTY: Int = 0,
            val U_BOX_QTY: Int = 0,
            val U_IQTY: Int = 0,
            val WarehouseCode: String = "",
            var isScanned: Int = 0,
            var UnitPrice: String = "",
            var totalPktQty: Int = 0,
        ) : Serializable
    }
}