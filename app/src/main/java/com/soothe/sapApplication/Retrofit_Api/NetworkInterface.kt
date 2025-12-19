package com.soothe.sapApplication.Retrofit_Api

import com.soothe.sapApplication.ui.deliveryOrderModule.Model.DeliveryModel
import com.soothe.sapApplication.ui.login.Model.LoginResponseModel
import com.google.gson.JsonObject
import com.soothe.sapApplication.ui.creditMemo.model.ArInvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoicePostModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.BinResModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryPostResponse
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ProductionListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleOrdersModel
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleToDeliveryResponse
import com.soothe.sapApplication.ui.setting.model.ModelGetBranch
import retrofit2.Call
import retrofit2.http.*

interface NetworkInterface {

    //todo login API---
    @POST(ApiConstant.LOGIN)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetLoginCall(@Body jsonObject: JsonObject): Call<LoginResponseModel>

    @POST(ApiConstant.LOGOUT)
    fun doGetLogoutCall(
        @Header("Cookie") cookieToken: String
    ): Call<LoginResponseModel>

    //todo production list api...
    @GET(ApiConstant.PRODUCTION_ORDERS)
    fun doGetProductionList(
//        @Query("filter") longitude: String,
    ): Call<ProductionListModel>

    //todo production list api test...
    @GET(ApiConstant.PRODUCTION_ORDERS)
    fun doGetProductionListCount(
        @Header("Prefer") Prefer: String,
        @Query("$" + "filter") filter: String,
        @Query("$" + "skip") skip: String,
        @Query("$" + "orderby") orderby: String,
        @Query("$" + "filter") status: String
    ): Call<ProductionListModel>

    @GET(ApiConstant.INVENTORY_REQUEST)
    fun doGetRequestListCount(
        @Query("$" + "select") select: String,
        @Query("$" + "filter") filter: String
    ): Call<InventoryRequestModel>

    @GET(ApiConstant.SCAN_NAV_CODE)
    fun doGetScan(
        @Query("$" + "select") select: String,
        @Query("$" + "filter") filter: String
    ): Call<NavScanResModel>

    @GET(ApiConstant.BIN_LOCATION)
    fun doGetBin(
        @Query("$" + "select") select: String,
        @Query("$" + "filter") filter: String
    ): Call<BinResModel>

    @GET
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetRequestListCount(@Url url: String): Call<InventoryRequestModel>

    // ?$select=DocEntry,DocNum,Series,DocDate,TaxDate,DueDate,CardCode,Address,Reference1,Comments,JournalMemo,PriceList,FromWarehouse,ToWarehouse,FinancialPeriod,DocObjectCode,BPLID,ShipToCode,DutyStatus,U_DOCTYP,U_TRNTYP,StockTransfer_ApprovalRequests,ElectronicProtocols,StockTransferLines,StockTransferTaxExtension,DocumentReferences&$filter=DocumentStatus eq 'O'
    //todo login API---
    @POST(ApiConstant.INVENTORY_GEN_EXITS)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetInventoryGenExits(@Body jsonObject: JsonObject): Call<InventoryGenExitsModel>

    @POST(ApiConstant.STOCK_TRANSFER)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun dostockTransfer(@Body jsonObject: JsonObject): Call<InventoryPostResponse>

    //todo scan item lines api...
    @GET(ApiConstant.BATCH_NUMBER_DETAILS)
    fun doGetBatchNumScanDetails(
        @Query("$" + "filter") filter: String,
    ): Call<ScanedOrderBatchedItems>

    //todo get BPL id api...
    @GET(ApiConstant.BPLID_WAREHOUSE)
    fun doGetBplID(
        @Query("$" + "select") select: String,
        @Query("$" + "filter") filter: String,
    ): Call<WarehouseBPL_IDModel>

    //todo get delivery order list..
    @GET(ApiConstant.DELIVERY_ORDER)
    fun deliveryOrder(
        @Query("$" + "filter") filter: String,
        @Query("$" + "orderby") orderby: String,
    ): Call<DeliveryModel>

    //todo post delivery order items...
    @POST(ApiConstant.DELIVERY_NOTES)
    fun doGetDeliveryNotes(@Body jsonObject: JsonObject): Call<InventoryGenExitsModel>

    //todo open invoice order items...
    @GET(ApiConstant.OPEN_INVOICE_LIST)
    fun doGetInvoiceOrderList(): Call<InvoiceListModel>

    //todo post invoice order items...
    @POST(ApiConstant.DBS)
    fun postInvoiceItems(@Body jsonObject: JsonObject): Call<InvoicePostModel>

    //todo post invoice order items...
    @PATCH(ApiConstant.INVOICE_UPDATE)
    fun callInvoiceUpdate(
        @Path("id") invoiceId: String,
        @Body jsonObject: JsonObject
    ): Call<Any>


    //todo get delivery order list..
    @GET(ApiConstant.UPDATE_STATUS)
    fun updateStatus(
        @Query("$" + "docentry") docentry: String,
    ): Call<Any>


    //todo api's for delivery one module--
    @GET(ApiConstant.OPEN_DELIVERY_LIST)
    fun doGetDeliveryOneList(): Call<InvoiceListModel>


    //todo post delivery one order items...
    @PATCH(ApiConstant.DELIVERY_UPDATE)
    fun callDeliveryUpdate(
        @Path("id") invoiceId: String,
        @Body jsonObject: JsonObject
    ): Call<Any>


    //todo get delivery order list..
    @GET(ApiConstant.UPDATE_DELIVERY_STATUS)
    fun updateDeliveryStatus(
        @Query("$" + "docentry") docentry: String,
    ): Call<Any>

    @GET(ApiConstant.INVENTORY_TRANSFER_REQUEST)
    fun inventoryTransferRequestList(
        @Query("BPLId") bplId: String,
        @Query("DocNum") docNum: String,
        @Query("DBName") dBName: String
    ): Call<InventoryRequestModel>

    @GET(ApiConstant.GET_SALES_ORDER_LIST)
    fun salesOrderList(
        @Query("BPLId") bplId: String
    ): Call<SaleOrdersModel>

    @GET(ApiConstant.GET_ALL_BRANCHES)
    fun getBranchList(): Call<ModelGetBranch>

    @POST(ApiConstant.DELIVERY_NOTES)
    fun postDeliveryDocument(@Body jsonObject: JsonObject): Call<SaleToDeliveryResponse>


    @GET(ApiConstant.GET_OPEN_DELIVERY_LIST)
    fun getDeliveryList(
        @Query("BPLId") bplId: String
    ): Call<InvoiceListModel>

    @GET(ApiConstant.GET_INVOICE_LIST)
    fun getInvoiceList(
        @Query("BPLId") bplId: String
    ): Call<ArInvoiceListModel>

    @GET(ApiConstant.GET_RETURN_REQUEST_LIST)
    fun getReturnRequestList(
        @Query("BPLId") bplId: String
    ): Call<ArInvoiceListModel>

    @POST(ApiConstant.POST_CREDIT_NOTES)
    fun postCreditNotes(@Body jsonObject: JsonObject): Call<InvoicePostModel>

}