package com.soothe.sapApplication.ui.saleOrderDelivery.model

import java.io.Serializable

data class SaleOrdersModel(
    var value: List<Value> = listOf()
) : Serializable {
    data class Value(
        var BPLID: String = "",
        var BPLName: String = "",
        var Cancelled: String = "",
        var CardCode: String = "",
        var CardName: String = "",
        var DocDate: String = "",
        var DocDueDate: String = "",
        var DocEntry: String = "",
        var DocNum: String = "",
        var DocRate: String = "",
        var DocStatus: String = "",
        var DocTotal: String = "",
        var DocType: String = "",
        var DocumentLines: List<DocumentLine> = listOf(),
        var NumAtCard: String = "",
        var PayToCode: String = "",
        var PaymentMethod: String = "",
        var Series: String = "",
        var SeriesDel: String = "",
        var ShipToCode: String = "",
        var TaxDate: String = ""
    ) : Serializable {
        data class DocumentLine(
            var BinABSEntry: String = "",
            var BinCode: String = "",
            var BinManaged: String = "",
            var DefaultABSEntry: String = "",
            var DefaultBinCD: String = "",
            var DiscountPercent: Double = 0.0,
            var DocEntry: String = "",
            var HSNEntry: String = "",
            var ItemCode: String = "",
            var ItemDescription: String = "",
            var ItemType: String = "",
            var LineNum: Int = 0,
            var NavCode: String = "",
            var Price: Double = 0.0,
            var Quantity: Int = 0,
            var Rate: String = "",
            var RemainingQuantity: Int = 0,
            var TaxCode: String = "",
            var U_IQTY: Int = 0,
            var WarehouseCode: String = "",
            var isScanned: Int = 0,
            var UnitPrice: String = "",
            var U_ACT_QTY: String = "",
            var U_BOX_QTY: String = "",
            var totalPktQty: Int = 0,
        ) : Serializable
    }
}