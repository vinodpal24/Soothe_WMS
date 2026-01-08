package com.soothe.sapApplication.ui.saleOrderDelivery.activity

import android.annotation.SuppressLint
import android.media.MediaPlayer
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
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivitySalesOrderDocumentLinesBinding
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

class SalesOrderDocumentLinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySalesOrderDocumentLinesBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var networkConnection: NetworkConnection
    private lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement

    private lateinit var soDocumentLinesList: ArrayList<SaleOrdersModel.Value.DocumentLine>
    private var saleOrderValue: SaleOrdersModel.Value? = null

    private lateinit var salesOrderDocLinesAdapter: SalesOrderDocumentLinesItemAdapter

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
        materialProgressDialog = MaterialProgressDialog(this@SalesOrderDocumentLinesActivity)
        sessionManagement = SessionManagement(this@SalesOrderDocumentLinesActivity)

        binding.apply {
            try {
                val intentData = intent

                saleOrderValue =
                    intentData.getSerializableExtra("saleReqModel") as SaleOrdersModel.Value

                soDocumentLinesList =
                    intentData.getSerializableExtra("documentLinesList")
                            as ArrayList<SaleOrdersModel.Value.DocumentLine>

                tvTitle.text =
                    "SO Document No. : ${saleOrderValue?.DocNum ?: ""}"

                Log.e("TAG", "onCreate:===> ${soDocumentLinesList.size}")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                setLinesItemAdapter()
                setupScanner()

            } catch (e: Exception) {
                Log.e("TAG", "Intent data error", e)
                Toast.makeText(this@SalesOrderDocumentLinesActivity, e.localizedMessage ?: "Something went wrong", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun playSound() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.boxx_added)
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
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
                        GlobalMethods.showError(this@SalesOrderDocumentLinesActivity, "Invalid Code")
                        return
                    }

                    val res = response.body()!!.value[0]

                    Log.i("NAV_CODE_SCANNING", "Scanning Data: $res")
                    //GlobalMethods.showSuccess(this@SalesOrderDocumentLinesActivity,"Box added.")
                    playSound()
                    salesOrderDocLinesAdapter.updateScannedItem(
                        itemCode = res.ItemCode,
                        packQty = res.U_PACK_QTY.toInt(),
                        navCode = navCode
                    )


                }

                override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                    materialProgressDialog.dismiss()
                    if (t.message == "VPN_Exception") {
                        GlobalMethods.showError(
                            this@SalesOrderDocumentLinesActivity, "VPN is not connected. Please connect VPN and try again."
                        )
                    } else {
                        GlobalMethods.showError(this@SalesOrderDocumentLinesActivity, t.message ?: "")
                    }
                }
            })
    }

    private fun setLinesItemAdapter() {
        binding.rvSaleOrderDocLines.apply {
            layoutManager = LinearLayoutManager(this@SalesOrderDocumentLinesActivity, LinearLayoutManager.VERTICAL, false)
            salesOrderDocLinesAdapter = SalesOrderDocumentLinesItemAdapter { qtyList ->
                createSaleToDelivery(saleOrderValue!!, qtyList)
            }
            adapter = salesOrderDocLinesAdapter
            salesOrderDocLinesAdapter.submitList(soDocumentLinesList)

        }
    }

    @SuppressLint("NewApi")
    private fun createSaleToDelivery(saleOrderValue: SaleOrdersModel.Value, list: ArrayList<SaleOrdersModel.Value.DocumentLine>) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            val currentDate = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val formattedDate = currentDate.format(formatter)
            var postedJson = JsonObject()
            postedJson.addProperty("Series", saleOrderValue.SeriesDel?.toIntOrNull() ?: 0)//594 for delivery
            postedJson.addProperty("DocDate", formattedDate)
            postedJson.addProperty("DocDueDate", saleOrderValue.DocDueDate)
            postedJson.addProperty("TaxDate", saleOrderValue.TaxDate)
            postedJson.addProperty("CardCode", saleOrderValue.CardCode)
            postedJson.addProperty("BPL_IDAssignedToInvoice", saleOrderValue.BPLID)
            postedJson.addProperty("NumAtCard", saleOrderValue.NumAtCard ?: "")  //saleOrderValue.NumAtCard
            postedJson.addProperty("Comments", "")  // added by Vinod @28Apr,2025
            postedJson.addProperty("DocType", "dDocument_Items")  //saleOrderValue.NumAtCard
            postedJson.addProperty("U_Type", "SO")
            postedJson.addProperty("PayToCode", saleOrderValue.PayToCode)
            postedJson.addProperty("ShipToCode", saleOrderValue.ShipToCode)
            postedJson.addProperty("U_WMSUSER", sessionManagement.getLoginId(this@SalesOrderDocumentLinesActivity))
            postedJson.addProperty("U_Scanned", "Y")

            var StockTransferLines = JsonArray()


            for (i in list.indices) {

                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                jsonObject.addProperty("LineNum", list[i].LineNum)
                //jsonObject.addProperty("Price", list[i].Price)
                jsonObject.addProperty("Quantity", list[i].totalPktQty)
                //jsonObject.addProperty("UnitPrice", list[i].UnitPrice)
                jsonObject.addProperty("U_ACT_QTY", list[i].U_ACT_QTY)
                jsonObject.addProperty("U_BOX_QTY", list[i].isScanned)
                jsonObject.addProperty("ItemCode", list[i].ItemCode)
                jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
                jsonObject.addProperty("BaseLine", list[i].LineNum)
                jsonObject.addProperty("BaseType", "17")
                jsonObject.addProperty("TaxCode", list[i].TaxCode)
                jsonObject.addProperty("U_Size", "")
                if (list[i].isScanned > 0) {
                    StockTransferLines.add(jsonObject)
                }

            }

            postedJson.add("DocumentLines", StockTransferLines)

            Log.e("success--PayLoad==>", "==>" + postedJson.toString())
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@SalesOrderDocumentLinesActivity)
            networkClient.postDeliveryDocument(postedJson).apply {
                enqueue(object : Callback<SaleToDeliveryResponse> {
                    override fun onResponse(
                        call: Call<SaleToDeliveryResponse>,
                        response: Response<SaleToDeliveryResponse>
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
                                    this@SalesOrderDocumentLinesActivity,
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
                                            this@SalesOrderDocumentLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@SalesOrderDocumentLinesActivity,
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

                    override fun onFailure(call: Call<SaleToDeliveryResponse>, t: Throwable) {
                        binding.chipSave.isEnabled = true
                        binding.chipSave.isCheckable = true
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(
                                this@SalesOrderDocumentLinesActivity, "VPN is not connected. Please connect VPN and try again."
                            )
                        } else {
                            GlobalMethods.showError(this@SalesOrderDocumentLinesActivity, t.message ?: "")
                        }
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(
                this@SalesOrderDocumentLinesActivity,
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
                salesOrderDocLinesAdapter.onSaveClicked()
            }
        }
    }
}