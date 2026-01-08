package com.soothe.sapApplication.ui.inventoryTransferRequest.activity

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
import com.soothe.sapApplication.databinding.ActivityInventoryRequestLinesBinding
import com.soothe.sapApplication.ui.inventoryTransferRequest.adapter.InventoryTransferRequestLinesItemAdapter
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryPostResponse
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InventoryRequestLinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInventoryRequestLinesBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var productionOrderLinesList: ArrayList<InventoryRequestModel.StockTransferLines>
    private var inventoryItem: InventoryRequestModel.Value? = null

    private lateinit var inventoryTransferItemAdapter: InventoryTransferRequestLinesItemAdapter

    private lateinit var networkConnection: NetworkConnection
    private lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryRequestLinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()
    }

    private fun initViews() {
        supportActionBar?.hide()
        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@InventoryRequestLinesActivity)
        sessionManagement = SessionManagement(this@InventoryRequestLinesActivity)

        binding.apply {
            try {
                val intentData = intent

                inventoryItem =
                    intentData.getSerializableExtra("inventReqModel") as InventoryRequestModel.Value

                productionOrderLinesList =
                    intentData.getSerializableExtra("productionLinesList")
                            as ArrayList<InventoryRequestModel.StockTransferLines>

                tvTitle.text =
                    "Request No : ${inventoryItem?.DocNum ?: ""}"

                Log.e("TAG", "onCreate:===> ${productionOrderLinesList.size}")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                setLinesItemAdapter()
                setupScanner()

            } catch (e: Exception) {
                Log.e("TAG", "Intent data error", e)
                Toast.makeText(this@InventoryRequestLinesActivity, e.localizedMessage ?: "Something went wrong", Toast.LENGTH_SHORT).show()
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
                        GlobalMethods.showError(this@InventoryRequestLinesActivity, "Invalid Code")
                        return
                    }

                    val res = response.body()!!.value[0]

                    Log.i("NAV_CODE_SCANNING", "Scanning Data: $res")
                    playSound()
                    inventoryTransferItemAdapter.updateScannedItem(
                        itemCode = res.ItemCode,
                        packQty = res.U_PACK_QTY.toInt(),
                        navCode = navCode
                    )


                }

                override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                    materialProgressDialog.dismiss()
                    if (t.message == "VPN_Exception") {
                        GlobalMethods.showError(
                            this@InventoryRequestLinesActivity, "VPN is not connected. Please connect VPN and try again."
                        )
                    } else {
                        GlobalMethods.showError(this@InventoryRequestLinesActivity, t.message ?: "")
                    }
                }
            })
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

    private fun setLinesItemAdapter() {
        binding.rvProductionOrderList.apply {
            productionOrderLinesList = setFilteredList(productionOrderLinesList) as ArrayList<InventoryRequestModel.StockTransferLines>
            layoutManager = LinearLayoutManager(this@InventoryRequestLinesActivity, LinearLayoutManager.VERTICAL, false)
            inventoryTransferItemAdapter = InventoryTransferRequestLinesItemAdapter { qtyList ->
                postInventoryTransferRequest(inventoryItem!!, qtyList);
            }
            adapter = inventoryTransferItemAdapter
            inventoryTransferItemAdapter.submitList(productionOrderLinesList)

        }
    }

    @SuppressLint("NewApi")
    private fun postInventoryTransferRequest(inventoryItem: InventoryRequestModel.Value, list: java.util.ArrayList<InventoryRequestModel.StockTransferLines>) {
        var ii = 0
        val now = LocalDateTime.now()
        val formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        var postedJson = JsonObject()
        postedJson.addProperty("Series", inventoryItem.Series)
        postedJson.addProperty("DocDate", formattedDate)
        postedJson.addProperty("DueDate", inventoryItem.DueDate)
        postedJson.addProperty("CardCode", inventoryItem.CardCode)
        postedJson.addProperty("Comments", inventoryItem.Comments)
        postedJson.addProperty("FromWarehouse", inventoryItem.FromWarehouse)
        postedJson.addProperty("ToWarehouse", inventoryItem.ToWarehouse)
        postedJson.addProperty("TaxDate", inventoryItem.TaxDate)
        postedJson.addProperty("DocObjectCode", "67")
        postedJson.addProperty("BPLID", inventoryItem.BPLID)
        postedJson.addProperty("ShipToCode", inventoryItem.ShipToCode)
        postedJson.addProperty("U_DOCTYP", inventoryItem.U_DOCTYP)
        postedJson.addProperty("U_TRNTYP", inventoryItem.U_TRNTYP)
        postedJson.addProperty("U_Scanned", "Y")


        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {


            var StockTransferLines = JsonArray()

            for (i in list.indices) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                jsonObject.addProperty("BaseLine", list[i].LineNum)
                jsonObject.addProperty("FromWarehouseCode", list[i].FromWarehouseCode)
                /*jsonObject.addProperty("ItemCode",list[i].ItemCode)
                jsonObject.addProperty("ItemDescription",list[i].ItemDescription)*/
                //jsonObject.addProperty("Price", list[i].Price)
                jsonObject.addProperty("Quantity", list[i].totalPktQty)
                //jsonObject.addProperty("UnitPrice", list[i].UnitPrice)
                jsonObject.addProperty("U_ACT_QTY", list[i].U_ACT_QTY)
                jsonObject.addProperty("U_BOX_QTY", list[i].isScanned)
                jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
                jsonObject.addProperty("BaseType", "InventoryTransferRequest")


                Log.e("isScanned==>", "" + list[i].isScanned)
                if (list[i].isScanned > 0) {
                    val stockBin = JsonArray()
                    /*val jsonObject1 = JsonObject()
                    jsonObject1.addProperty("BinAbsEntry", "5")
                    jsonObject1.addProperty("Quantity", list[i].totalPktQty)
                    jsonObject1.addProperty("BaseLineNumber", ii)
                    jsonObject1.addProperty("BinActionType", "batToWarehouse")
                    stockBin.add(jsonObject1)*/
                    jsonObject.add("StockTransferLinesBinAllocations", stockBin)
                    StockTransferLines.add(jsonObject)
                    ii++;
                }
            }
            postedJson.add("StockTransferLines", StockTransferLines)
            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            if (false)
                return
            materialProgressDialog.show()
            val networkClient = NetworkClients.create(this@InventoryRequestLinesActivity)
            networkClient.dostockTransfer(postedJson).apply {
                enqueue(object : Callback<InventoryPostResponse> {
                    override fun onResponse(
                        call: Call<InventoryPostResponse>,
                        response: Response<InventoryPostResponse>
                    ) {
                        try {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true

                            AppConstants.IS_SCAN = false
                            materialProgressDialog.dismiss()
                            Log.e("success---BP---", "==>" + response.code())
                            if (response.code() == 201 || response.code() == 200) {
                                Log.e("success------", "Successful!")
                                GlobalMethods.showSuccess(this@InventoryRequestLinesActivity, "Post Successfully.")
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
                                            this@InventoryRequestLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@InventoryRequestLinesActivity,
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

                    override fun onFailure(call: Call<InventoryPostResponse>, t: Throwable) {
                        binding.chipSave.isEnabled = true
                        binding.chipSave.isCheckable = true
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(
                                this@InventoryRequestLinesActivity, "VPN is not connected. Please connect VPN and try again."
                            )
                        } else {
                            GlobalMethods.showError(this@InventoryRequestLinesActivity, t.message ?: "")
                        }
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(
                this@InventoryRequestLinesActivity,
                "No Network Connection",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun setFilteredList(
        list: List<InventoryRequestModel.StockTransferLines>
    ): MutableList<InventoryRequestModel.StockTransferLines> {

        val filtered = mutableListOf<InventoryRequestModel.StockTransferLines>()

        list.forEach { item ->
            val qty = item.RemainingOpenQuantity?.toDoubleOrNull()
            when {
                qty == null ->
                    Toast.makeText(this, "Remaining Quantity is Missing", Toast.LENGTH_SHORT).show()

                qty > 0 ->
                    filtered.add(item)
            }
        }
        return filtered
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
                inventoryTransferItemAdapter.onSaveClicked()
            }
        }
    }
}