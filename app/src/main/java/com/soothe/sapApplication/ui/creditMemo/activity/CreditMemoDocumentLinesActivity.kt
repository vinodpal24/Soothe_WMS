package com.soothe.sapApplication.ui.creditMemo.activity

import android.annotation.SuppressLint
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
import com.soothe.sapApplication.databinding.ActivitySalesOrderDocumentLinesBinding
import com.soothe.sapApplication.ui.creditMemo.adapter.CreditMemoDocumentLinesItemAdapter
import com.soothe.sapApplication.ui.creditMemo.model.ArInvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoicePostModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import com.soothe.sapApplication.ui.saleOrderDelivery.adapter.SalesOrderDocumentLinesItemAdapter
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleOrdersModel
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleToDeliveryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CreditMemoDocumentLinesActivity : AppCompatActivity() {
    private lateinit var binding:ActivitySalesOrderDocumentLinesBinding

    private lateinit var networkConnection: NetworkConnection
    private lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement

    private lateinit var soDocumentLinesList: ArrayList<ArInvoiceListModel.Value.DocumentLine>
    private var creditMemoValue: ArInvoiceListModel.Value? = null

    private lateinit var creditMemoDocumentLinesAdapter:CreditMemoDocumentLinesItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesOrderDocumentLinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()
    }

    private fun initViews() {
        supportActionBar?.hide()
        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@CreditMemoDocumentLinesActivity)
        sessionManagement = SessionManagement(this@CreditMemoDocumentLinesActivity)

        binding.apply {
            try {
                val intentData = intent

                creditMemoValue =
                    intentData.getSerializableExtra("inventReqModel") as ArInvoiceListModel.Value

                soDocumentLinesList =
                    intentData.getSerializableExtra("productionLinesList")
                            as ArrayList<ArInvoiceListModel.Value.DocumentLine>

                tvTitle.text =
                    "SO Document No. : ${creditMemoValue?.DocNum ?: ""}"

                Log.e("TAG", "onCreate:===> ${soDocumentLinesList.size}")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                setLinesItemAdapter()
                setupScanner()

            } catch (e: Exception) {
                Log.e("TAG", "Intent data error", e)
                Toast.makeText(this@CreditMemoDocumentLinesActivity, e.localizedMessage ?: "Something went wrong", Toast.LENGTH_SHORT).show()
            }

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
                        GlobalMethods.showError(this@CreditMemoDocumentLinesActivity, "Invalid Code")
                        return
                    }

                    val res = response.body()!!.value[0]

                    Log.i("NAV_CODE_SCANNING", "Scanning Data: $res")

                    creditMemoDocumentLinesAdapter.updateScannedItem(
                        itemCode = res.ItemCode,
                        packQty = res.U_PACK_QTY.toInt(),
                        navCode = navCode
                    )


                }

                override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                    materialProgressDialog.dismiss()
                    if (t.message == "VPN_Exception") {
                        GlobalMethods.showError(this@CreditMemoDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                        )
                    }else{
                        GlobalMethods.showError(this@CreditMemoDocumentLinesActivity, t.message ?: "")
                    }                }
            })
    }

    private fun setLinesItemAdapter() {
        binding.rvSaleOrderDocLines.apply {
            layoutManager = LinearLayoutManager(this@CreditMemoDocumentLinesActivity, LinearLayoutManager.VERTICAL, false)
            creditMemoDocumentLinesAdapter  = CreditMemoDocumentLinesItemAdapter { qtyList ->
                createCreditNotes(creditMemoValue!!, qtyList)
            }
            adapter = creditMemoDocumentLinesAdapter
            creditMemoDocumentLinesAdapter.submitList(soDocumentLinesList)

        }
    }

    @SuppressLint("NewApi")
    private fun createCreditNotes(creditMemoValue: ArInvoiceListModel.Value, list: ArrayList<ArInvoiceListModel.Value.DocumentLine>) {
if(networkConnection.getConnectivityStatusBoolean(applicationContext)){

    val currentDate = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val formattedDate = currentDate.format(formatter)
    var postedJson = JsonObject()
    postedJson.addProperty("Series", creditMemoValue.Series?.toIntOrNull() ?: 0)//594 for delivery
    postedJson.addProperty("DocDate", formattedDate)
    postedJson.addProperty("DocDueDate", creditMemoValue.DocDueDate)
    postedJson.addProperty("TaxDate", creditMemoValue.TaxDate)
    postedJson.addProperty("CardCode", creditMemoValue.CardCode)
    postedJson.addProperty("BPLID", creditMemoValue.BPLID)
    postedJson.addProperty("Comments", "Credit memo against AR Invoice")  // added by Vinod @28Apr,2025

    var StockTransferLines = JsonArray()

    for (i in list.indices) {

        val jsonObject = JsonObject()
        jsonObject.addProperty("BaseEntry", list[i].DocEntry)
        jsonObject.addProperty("LineNum", list[i].LineNum)
        jsonObject.addProperty("Quantity", list[i].totalPktQty)
        jsonObject.addProperty("ItemCode", list[i].ItemCode)
        jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
        jsonObject.addProperty("BaseLine", list[i].LineNum)
        jsonObject.addProperty("BaseType", "13")
        jsonObject.addProperty("TaxCode", list[i].TaxCode)
        jsonObject.addProperty("WithoutInventoryMovement", "tYES")
        if (list[i].isScanned > 0) {
            StockTransferLines.add(jsonObject)
        }

    }

    postedJson.add("DocumentLines", StockTransferLines)

    Log.e("success--PayLoad==>", "==>" + postedJson.toString())
    val apiConfig = ApiConstantForURL()
    NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
    val networkClient = NetworkClients.create(this@CreditMemoDocumentLinesActivity)
    networkClient.postCreditNotes(postedJson).apply {
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
                    Log.e("success---BP---", "==>" + response.code())
                    if (response.code() == 201 || response.code() == 200) {
                        Log.e("success------", "Successful!")
                        Log.d(
                            "Doc_Num",
                            "onResponse: " + response.body()!!.DocNum.toString()
                        )
                        GlobalMethods.showSuccess(
                            this@CreditMemoDocumentLinesActivity,
                            "Post Successfully. " + response.body()!!.DocNum.toString()
                        )
                        onBackPressed()
                    } else {
                        materialProgressDialog.dismiss()
                        val gson1 = GsonBuilder().create()
                        var mError: OtpErrorModel
                        try {
                            val s = response.errorBody()!!.string()
                            mError = gson1.fromJson(s, OtpErrorModel::class.java)
                            if (mError.error.code.equals(400)) {
                                GlobalMethods.showError(
                                    this@CreditMemoDocumentLinesActivity,
                                    mError.error.message.value
                                )
                            }
                            if (mError.error.message.value != null) {
                                GlobalMethods.showError(
                                    this@CreditMemoDocumentLinesActivity,
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
                    GlobalMethods.showError(this@CreditMemoDocumentLinesActivity,"VPN is not connected. Please connect VPN and try again."
                    )
                }else{
                    GlobalMethods.showError(this@CreditMemoDocumentLinesActivity, t.message ?: "")
                }
                Log.e("orderLines_failure-----", t.toString())
                materialProgressDialog.dismiss()
            }

        })
    }
}else {
    materialProgressDialog.dismiss()
        Toast.makeText(
            this@CreditMemoDocumentLinesActivity,
            "No Network Connection",
            Toast.LENGTH_SHORT
        ).show()
}


    }

    private fun clickListeners() {
        binding.apply {
            ivOnback.setOnClickListener {
                onBackPressed()
            }


            //todo cancel lines...
            chipCancel.setOnClickListener {
                onBackPressed()
            }

            chipSave.setOnClickListener {
                creditMemoDocumentLinesAdapter.onSaveClicked()
            }
        }
    }
}