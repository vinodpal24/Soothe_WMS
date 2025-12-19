package com.soothe.sapApplication.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InventoryRequestModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
    // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<Value>,
) : Serializable {
    data class Value(
        var DocEntry: String,
        var BPLID: String,
        var Series: String,
        var CardCode: String,
        var Comments: String,
        var DocDate: String,
        var DocNum: String,
        var DocObjectCode: String,
        var DueDate: String,
        var FinancialPeriod: String,
        var FromWarehouse: String,
        var JournalMemo: String,
        var Reference1: String,
        var ToWarehouse: String,
        var TaxDate: String,
        var ShipToCode: String,
        var U_DOCTYP: String,
        var U_TRNTYP: String,
        var CardName: String,

        var StockTransferLines: ArrayList<StockTransferLines> = ArrayList()
        /* var StockTransfer_ApprovalRequests: ArrayList<Any>,
         var ElectronicProtocols: List<Any?>,
         var DocumentReferences: List<Any?>*/

    ) : Serializable

    data class StockTransferLines(
        var isScanned: Int = 0,
        var totalPktQty: Int,
        var ItemCode: String,
        var LineNum: String,
        var ItemDescription: String,
        var Quantity: String,
        var Price: String,
        var WarehouseCode: String,
        var FromWarehouseCode: String,
        var BaseType: String,
        var BaseLine: String,
        var BaseEntry: String,
        var UnitPrice: String,
        var U_ACT_QTY: String,
        var U_BOX_QTY: String,
        var DocEntry: String,
        var RemainingOpenQuantity: String,
        var U_IQTY: String,
        var NavisionCode: String? = null,
        val NavCode: String? = null,

        ) : Serializable
}