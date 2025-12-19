package com.soothe.sapApplication.Retrofit_Api

import com.soothe.sapApplication.Global_Classes.AppConstants.isDevelopment
import com.soothe.sapApplication.Global_Classes.AppConstants.isOldDevelopment

object ApiConstant {

    enum class ApiType {
        STANDARD, CUSTOM
    }
    // internal const val BASE_URL = "http://103.194.8.40:50001/b1s/v1/"
//    internal const val BASE_URL = "http://125.63.73.130:50001/b1s/v1/"//todo comment by abinas sir on 27_03_2024

    internal const val BASE_URL = "http://192.168.0.173:50001/b1s/v1/"
    internal const val Invoice_BASE_URL = "http://192.168.0.173:50001/b1s/v1/"
    internal const val Invoice_List_BASE_URL = "http://192.168.0.173:9090/"



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

    internal const val LOGIN = "Login"
    internal const val LOGOUT = "Logout"

    //todo ISSUE ORDER API..
    internal const val PRODUCTION_ORDERS = "ProductionOrders"

   // internal const val INVENTORY_REQUEST = "MobileAppTest/api/SaleInvoice/InventroyTransferRequestList"
    internal const val INVENTORY_REQUEST = "MobileApp/api/SaleInvoice/InventroyTransferRequestList"
    internal const val INVENTORY_GEN_EXITS = "InventoryGenExits"
    internal const val STOCK_TRANSFER = "StockTransfers"
    internal const val BATCH_NUMBER_DETAILS = "BatchNumberDetails"
    internal const val SCAN_NAV_CODE = "Items"
    internal const val BIN_LOCATION = "Items('"//')
    internal const val BPLID_WAREHOUSE = "Warehouses"

    //todo DELIVERY ORDER API..
    internal const val DELIVERY_ORDER = "Orders"
    internal const val DELIVERY_NOTES = "DeliveryNotes"

    //todo Invoice ORDER API's....
    internal const val OPEN_INVOICE_LIST = "MobileApp/api/SaleInvoice/OpenInvoiceList"
    internal const val DBS  = "DBS Scaning"
    internal const val INVOICE_UPDATE  = "Invoices{id}"
    internal const val UPDATE_STATUS = "MobileApp/api/SaleInvoice/UpdateStatus"


    //todo delivery API--

    internal const val OPEN_DELIVERY_LIST = "MobileApp/api/SalesDelivery/OpenDeliveryList"
    internal const val DELIVERY_UPDATE  = "DeliveryNotes{id}"
    internal const val UPDATE_DELIVERY_STATUS = "MobileApp/api/SaleInvoice/UpdateStatusDelivery"


    const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"


    // new api url
    internal const val INVENTORY_TRANSFER_REQUEST = "InventroyTransferRequestList" //Inventory Transfer Request List
    internal const val GET_OPEN_DELIVERY_LIST = "GetSalesList12" // Sales Delivery List
    internal const val GET_SALES_ORDER_LIST = "GetSalesList" // Sales Order List
    internal const val GET_ALL_BRANCHES = "GetBranchList" // All Branches
    internal const val GET_INVOICE_LIST = "GetInvoiceList" // Invoice List
    internal const val GET_RETURN_REQUEST_LIST = "GetReturnRequestList" // Return Request List
    internal const val POST_CREDIT_NOTES = "CreditNotes" // Create credit notes


}