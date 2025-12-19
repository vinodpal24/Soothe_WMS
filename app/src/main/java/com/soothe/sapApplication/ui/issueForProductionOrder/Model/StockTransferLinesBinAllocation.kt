package com.soothe.sapApplication.ui.issueForProductionOrder.Model

data class StockTransferLinesBinAllocation(
    val AllowNegativeQuantity: String,
    val BaseLineNumber: Int,
    val BinAbsEntry: Int,
    val BinActionType: String,
    val Quantity: Double,
    val SerialAndBatchNumbersBaseLine: Int
)