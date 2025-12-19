package com.soothe.sapApplication.interfaces

import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems

interface PassList {
    fun passList(dataList : List<ScanedOrderBatchedItems.Value>)
}