package com.soothe.sapApplication.ui.deliveryOrder.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityDeliveryDocumentLinesBinding
import com.soothe.sapApplication.ui.deliveryOrder.adapter.DeliveryOrderLinesItemAdapter
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoicePostModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class DeliveryDocumentLinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeliveryDocumentLinesBinding

    private var orderDocLineData : ArrayList<InvoiceListModel.DocumentLine> = ArrayList()

    private var orderValueItem: InvoiceListModel.Value? = null

    private lateinit var deliveryItemAdapter: DeliveryOrderLinesItemAdapter

    private lateinit var networkConnection: NetworkConnection
    private lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryDocumentLinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()

    }

    private fun initViews() {
        supportActionBar?.hide()
        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@DeliveryDocumentLinesActivity)
        sessionManagement = SessionManagement(this@DeliveryDocumentLinesActivity)

        binding.apply {
            try {
                val intentData = intent

                orderValueItem =
                    intentData.getSerializableExtra("inventReqModel") as InvoiceListModel.Value

                orderDocLineData =
                    intentData.getSerializableExtra("productionLinesList")
                            as ArrayList<InvoiceListModel.DocumentLine>

                tvTitleLabel.text = "Delivery No. :"
                tvTitle.text = orderValueItem?.DocNum ?: ""
                tvCustomerName.text = orderValueItem?.CardName ?: ""

                Log.e("TAG", "onCreate:===> ${orderDocLineData.size}")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                setLinesItemAdapter()
                setupScanner()

            } catch (e: Exception) {
                Log.e("TAG", "Intent data error", e)
                Toast.makeText(
                    this@DeliveryDocumentLinesActivity,
                    e.localizedMessage ?: "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun setLinesItemAdapter() {
        binding.rvDeliveryOrderItem.apply {
            layoutManager = LinearLayoutManager(
                this@DeliveryDocumentLinesActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            deliveryItemAdapter = DeliveryOrderLinesItemAdapter { qtyList ->
                orderDocLineData = qtyList
                postDeliveryOrderLineItems(orderValueItem!!, qtyList)
            }
            adapter = deliveryItemAdapter
            deliveryItemAdapter.submitList(orderDocLineData)

        }
    }

    // ---------------- SCANNER HANDLING ----------------
    private fun setupScanner() {
        binding.etScanItem.requestFocus()
        binding.etScanItem.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP &&
                binding.etScanItem.text.isNotEmpty()
            ) {
                val scannedText =
                    binding.etScanItem.text.toString().trim().split("/")[0]
                binding.etScanItem.setText("")
                Log.i("NAV_CODE_SCANNING", "Scanning Nav Code: $scannedText")
                scanNavCode(scannedText)
            }
            true
        }
    }

    // ---------------- API CALL ----------------
    private fun scanNavCode(navCode: String) {
        if (!networkConnection.getConnectivityStatusBoolean(this)) {
            GlobalMethods.showError(this, "No Internet Connection")
            return
        }

        materialProgressDialog.show()
        val apiConfig = ApiConstantForURL()
        NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
        NetworkClients.create(this)
            .doGetScan(
                "ItemCode,U_PCS_QTY,U_PACK_QTY",
                "U_NAVI_CODE eq '$navCode'"
            )
            .enqueue(object : Callback<NavScanResModel> {

                override fun onResponse(
                    call: Call<NavScanResModel>,
                    response: Response<NavScanResModel>
                ) {
                    materialProgressDialog.dismiss()

                    if (!response.isSuccessful || response.body()?.value.isNullOrEmpty()) {
                        GlobalMethods.showError(this@DeliveryDocumentLinesActivity, "Invalid Code")
                        return
                    }

                    val res = response.body()!!.value[0]

                    Log.i("NAV_CODE_SCANNING", "Scanning Data: $res")

                    deliveryItemAdapter.updateScannedItem(
                        itemCode = res.ItemCode,
                        packQty = res.U_PACK_QTY.toInt(),
                        navCode = navCode
                    )


                }

                override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                    materialProgressDialog.dismiss()
                    if (t.message == "VPN_Exception") {
                        GlobalMethods.showError(this@DeliveryDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                        )
                    }else{
                        GlobalMethods.showError(this@DeliveryDocumentLinesActivity, t.message ?: "")
                    }
                }
            })
    }

    private fun clickListeners() {
        binding.apply {
            ivOnback.setOnClickListener {
                finish()
            }


            //todo cancel lines...
            chipCancel.setOnClickListener {
                finish()
            }

            chipSave.setOnClickListener {
                deliveryItemAdapter.onSaveClicked()
            }
        }
    }

    private fun postDeliveryOrderLineItems(
        orderValueLineArrayList: InvoiceListModel.Value,
        orderDocLineData: ArrayList<InvoiceListModel.DocumentLine>
    ) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {


            materialProgressDialog.show()
            var ii = 0;
            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("U_BPCode", orderValueLineArrayList.CardCode)
            postedJson.addProperty("U_BPName", orderValueLineArrayList.CardName)
            postedJson.addProperty("U_SDT", "INV")
            postedJson.addProperty("U_SDNo", orderValueLineArrayList.DocEntry)
            postedJson.addProperty("U_DocumentNo", orderValueLineArrayList.DocNum)
            postedJson.addProperty(
                "U_PostingDate",
                GlobalMethods.getCurrentTodayDate()
            ) //todo current date will send here---
            postedJson.addProperty("U_ARInvoice", orderValueLineArrayList.DocNum)


            var DocumentLinesArray = JsonArray()

            for (i in orderDocLineData.indices) {
                var jsonObject = JsonObject()
                jsonObject.addProperty("U_ITEMLINENO", orderDocLineData[i].LineNum)
                jsonObject.addProperty("U_ItemCode", orderDocLineData[i].ItemCode)
                jsonObject.addProperty("U_NavisionCode", orderDocLineData[i].NavisionCode)
                jsonObject.addProperty("U_ItemName", orderDocLineData[i].ItemDescription)

                if (orderDocLineData[i].isScanned > 0) {

                    val jsonObject1 = JsonObject()
                    jsonObject.addProperty("U_BoxQty", orderDocLineData[i].initialBoxes)
                    jsonObject.addProperty(
                        "U_PktQty",
                        orderDocLineData[i].RemainingQuantity.toInt()
                    )
                    jsonObject.addProperty("U_SBQ", orderDocLineData[i].isScanned)
                    jsonObject.addProperty("U_SPQ", orderDocLineData[i].totalPktQty)

                    DocumentLinesArray.add(jsonObject)
                    ii++

                }

            }


            postedJson.add("DBS_ROWCollection", DocumentLinesArray)

            var jsonArray = JsonArray()
            jsonArray.add(postedJson)

            var jsonObject = JsonObject()
            jsonObject.add("value", jsonArray)

            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@DeliveryDocumentLinesActivity)
            networkClient.postInvoiceItems(postedJson).apply {
                enqueue(object : Callback<InvoicePostModel> {
                    override fun onResponse(
                        call: Call<InvoicePostModel>,
                        response: Response<InvoicePostModel>
                    ) {
                        try {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true

                            AppConstants.IS_SCAN = false
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 201) {
                                    Log.e("success------", "Successful!")
                                    val responseData = response.body()?.DBS_ROWCollection
                                    callInvoiceUpdateApi(responseData)
                                    GlobalMethods.showSuccess(
                                        this@DeliveryDocumentLinesActivity,
                                        "Delivery Order Post Successfully."
                                    )
                                }
//                                onBackPressed()
                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }

                        } catch (e: Exception) {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                            Log.e("catch---------", e.toString())
                        }

                    }

                    override fun onFailure(call: Call<InvoicePostModel>, t: Throwable) {
                        binding.chipSave.isEnabled = true
                        binding.chipSave.isCheckable = true
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@DeliveryDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryDocumentLinesActivity, t.message ?: "")
                        }
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }


        } else {
            materialProgressDialog.dismiss()

            Toast.makeText(
                this@DeliveryDocumentLinesActivity,
                "No Network Connection",
                Toast.LENGTH_SHORT
            ).show()
            
        }
    }


    private fun callInvoiceUpdateApi(responseData: List<InvoicePostModel.DBSROWCollection>?) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            val postedJson = JsonObject()
            val DocumentLinesArray = JsonArray()


            for (i in responseData!!.indices) {
                for (j in i until orderDocLineData.size) {
                    if (responseData[i].U_ITEMLINENO.equals(orderDocLineData[j].LineNum)) {

                        var jsonObject = JsonObject()

                        var resultCal =
                            responseData[i].U_SPQ + (orderDocLineData[j].Quantity - orderDocLineData[j].RemainingQuantity.toInt())

                        Log.e("TAG", "callInvoiceUpdateApi: " + resultCal)

                        jsonObject.addProperty("LineNum", responseData[i].U_ITEMLINENO)
                        jsonObject.addProperty("U_SCNPKTQTY", resultCal)

                        DocumentLinesArray.add(jsonObject)

                        break
                    }

                }

            }

            postedJson.add("DocumentLines", DocumentLinesArray)
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@DeliveryDocumentLinesActivity)

            networkClient.callDeliveryUpdate("(${orderValueItem?.DocEntry})", postedJson)
                .apply {
                    enqueue(object : Callback<Any> {
                        override fun onResponse(call: Call<Any>, response: Response<Any>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    if (response.code() == 204) {
                                        Log.e("success_invoice_api----", "Successful!")
                                        finish()
                                        //updateDocStatusApi() // comment by Vinod @17 Dec,2025
//                                    GlobalMethods.showSuccess(this@InvoiceOrderLineActivity, "Issue Production Order Post Successfully.")
                                    }
//                                onBackPressed()
                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(
                                                this@DeliveryDocumentLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }

                                }

                            } catch (e: Exception) {
                                materialProgressDialog.dismiss()
                                e.printStackTrace()
                                Log.e("catch---------", e.toString())
                            }

                        }

                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            Log.e("orderLines_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@DeliveryDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@DeliveryDocumentLinesActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }

                    })
                }

        } else {
            materialProgressDialog.dismiss()
                Toast.makeText(
                    this@DeliveryDocumentLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()

        }
    }


    private fun updateDocStatusApi() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@DeliveryDocumentLinesActivity)
            networkClient.updateDeliveryStatus(orderValueItem?.DocEntry.toString()).apply {
                enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 200) {
                                    Log.e("success_status_api----", "Successful!")
                                    finish()
//                                    GlobalMethods.showSuccess(this@InvoiceOrderLineActivity, "Issue Production Order Post Successfully.")
                                }

                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    GlobalMethods.showError(
                                        this@DeliveryDocumentLinesActivity,
                                        mError.error.message.value
                                    )
                                    Log.e("json_error------", mError.error.message.value)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }

                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                            Log.e("catch---------", e.toString())
                        }

                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@DeliveryDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryDocumentLinesActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
                Toast.makeText(
                    this@DeliveryDocumentLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }
}