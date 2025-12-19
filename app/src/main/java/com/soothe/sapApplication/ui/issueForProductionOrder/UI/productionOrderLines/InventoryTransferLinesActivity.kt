package com.soothe.sapApplication.ui.issueForProductionOrder.UI.productionOrderLines

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.soothe.sapApplication.databinding.ActivityProductionOrderLinesBinding
import com.soothe.sapApplication.interfaces.PassList
import com.soothe.sapApplication.ui.issueForProductionOrder.Adapter.InventoryTransferItemAdapter
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement


private const val TAG = "ProductionOrderLinesAct"

class InventoryTransferLinesActivity : AppCompatActivity(), PassList,
    InventoryTransferItemAdapter.AdapterCallback {

    var inventoryItem: InventoryRequestModel.Value? = null

    private lateinit var activityFormBinding: ActivityProductionOrderLinesBinding
    private lateinit var InventoryTransferItemAdapter: InventoryTransferItemAdapter
    private lateinit var productionOrderLineList_gl: ArrayList<InventoryRequestModel.StockTransferLines>

    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<InventoryRequestModel.StockTransferLines> = ArrayList()
    private var connection: Connection? = null
    var openQty = 0.0

    //todo batch scan and quantity list interface override...
    var hashMapBatchList: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> =
        HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: HashMap<String, ArrayList<String>> =
        HashMap<String, ArrayList<String>>()

    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFormBinding = ActivityProductionOrderLinesBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        title = "Form Screen"
        try {
            inventoryItem = intent.getSerializableExtra("inventReqModel") as InventoryRequestModel.Value
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

        deleteCache(this)

        supportActionBar?.hide()


        activityFormBinding.ivLaserCode.setFocusable(true)
        activityFormBinding.ivLaserCode.requestFocus()

        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)


        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@InventoryTransferLinesActivity)
        sessionManagement = SessionManagement(this@InventoryTransferLinesActivity)


        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<InventoryRequestModel.StockTransferLines>
            position = intent.extras?.getInt("pos")
            activityFormBinding.tvTitle.text = "Request No : " + inventoryItem!!.DocNum
            Log.e(TAG, "onCreate:===> " + productionOrderLineList_gl.size)

            setAdapter();

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)

            e.printStackTrace()
        }



        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }


        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            onBackPressed()
        }


    }

    var tempList: ArrayList<ProductionListModel.ProductionOrderLine> = ArrayList()

    //todo set adapter....
    fun setAdapter() {

        for (i in 0 until productionOrderLineList_gl.size) {
            var j: Int = i
            try {

            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }


        var width = 50
        var length = 50
        var gsm = 50


        activityFormBinding.ivNoDataFound.visibility = View.GONE
        activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
        activityFormBinding.btnLinearLayout.visibility = View.VISIBLE
        productionOrderLineList_gl =
            setFilteredList(productionOrderLineList_gl) as ArrayList<InventoryRequestModel.StockTransferLines> /* = java.util.ArrayList<com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel.StockTransferLines> */
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...
        InventoryTransferItemAdapter = InventoryTransferItemAdapter(
            this@InventoryTransferLinesActivity,
            productionOrderLineList_gl,
            networkConnection,
            materialProgressDialog,
            this@InventoryTransferLinesActivity,
            activityFormBinding.chipSave,
            width.toDouble(),
            length.toDouble(),
            gsm.toDouble(), activityFormBinding.ivScanBatchCode, activityFormBinding.ivLaserCode
        )//getWarehouseCode
        activityFormBinding.rvProductionOrderList.adapter = InventoryTransferItemAdapter


    }

    var productionOrderLineList_temp: MutableList<InventoryRequestModel.StockTransferLines> = mutableListOf()

    private fun setFilteredList(arrayList: java.util.ArrayList<InventoryRequestModel.StockTransferLines>): MutableList<InventoryRequestModel.StockTransferLines> {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item.RemainingOpenQuantity != null) {
                if (item is InventoryRequestModel.StockTransferLines && item.RemainingOpenQuantity.toDouble() > 0 && item.RemainingOpenQuantity != "0.0") {
                    productionOrderLineList_temp.add(item)
                }
            } else {
                Toast.makeText(this, "Remaining Quantity is Missing", Toast.LENGTH_SHORT).show()
            }


        }
        return productionOrderLineList_temp

    }

    override fun onApiResponseStock(
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
        quantityResponse: ArrayList<InventoryRequestModel.StockTransferLines>
    ) {
        Log.e("hashmap--->", quantityResponse.toString())

        activityFormBinding.chipSave.isEnabled = false
        activityFormBinding.chipSave.isCheckable = false
        postInventorystock(inventoryItem!!, quantityResponse);
    }

    //todo getting batch order lines and quantity data from adapter to activity...
    override fun onApiResponse(
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
        quantityResponse: HashMap<String, ArrayList<String>>
    ) {

        hashMapBatchList = response
        hashmapBatchQuantityList = quantityResponse
        Log.e("hashmap--->", hashMapBatchList.toString())
        Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())
        saveProductionOrderLinesItems()

    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: ArrayList<String>


    //todo here save issue for production lines items of order...
    private fun postInventorystock(
        inventoryItem: InventoryRequestModel.Value,
        list: ArrayList<InventoryRequestModel.StockTransferLines>
    ) {
        var ii = 0;
        var postedJson = JsonObject()
        postedJson.addProperty("Series", "100")
        postedJson.addProperty("DocDate", inventoryItem.DocDate)
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


        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var StockTransferLines = JsonArray()

            for (i in list.indices) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                jsonObject.addProperty("BaseLine", list[i].LineNum)
                jsonObject.addProperty("BaseType", list[i].BaseType)
                jsonObject.addProperty("FromWarehouseCode", list[i].FromWarehouseCode)
                /*jsonObject.addProperty("ItemCode",list[i].ItemCode)
                jsonObject.addProperty("ItemDescription",list[i].ItemDescription)*/
                jsonObject.addProperty("Price", list[i].Price)
                jsonObject.addProperty("Quantity", list[i].totalPktQty)
                jsonObject.addProperty("UnitPrice", list[i].UnitPrice)
                jsonObject.addProperty("U_ACT_QTY", list[i].U_ACT_QTY)
                jsonObject.addProperty("U_BOX_QTY", list[i].isScanned)
                jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
                jsonObject.addProperty("BaseType", "InventoryTransferRequest")


                Log.e("isScanned==>", "" + list[i].isScanned)
                if (list[i].isScanned > 0) {
                    val stockBin = JsonArray()
                    val jsonObject1 = JsonObject()
                    jsonObject1.addProperty("BinAbsEntry", "5")
                    jsonObject1.addProperty("Quantity", list[i].totalPktQty)
                    jsonObject1.addProperty("BaseLineNumber", ii)
                    jsonObject1.addProperty("BinActionType", "batToWarehouse")
                    stockBin.add(jsonObject1)
                    jsonObject.add("StockTransferLinesBinAllocations", stockBin)
                    StockTransferLines.add(jsonObject)
                    ii++;
                }
            }
            postedJson.add("StockTransferLines", StockTransferLines)
            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            if (false)
                return
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@InventoryTransferLinesActivity)
            networkClient.dostockTransfer(postedJson).apply {
                enqueue(object : Callback<InventoryPostResponse> {
                    override fun onResponse(
                        call: Call<InventoryPostResponse>,
                        response: Response<InventoryPostResponse>
                    ) {
                        try {
                            activityFormBinding.chipSave.isEnabled = true
                            activityFormBinding.chipSave.isCheckable = true

                            AppConstants.IS_SCAN = false
                            materialProgressDialog.dismiss()
                            Log.e("success---BP---", "==>" + response.code())
                            if (response.code() == 201 || response.code() == 200) {
                                Log.e("success------", "Successful!")
                                GlobalMethods.showSuccess(this@InventoryTransferLinesActivity, "Post Successfully.")
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
                                            this@InventoryTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@InventoryTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }
                        } catch (e: Exception) {
                            activityFormBinding.chipSave.isEnabled = true
                            activityFormBinding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                            Log.e("catch---------", e.toString())
                        }

                    }

                    override fun onFailure(call: Call<InventoryPostResponse>, t: Throwable) {
                        activityFormBinding.chipSave.isEnabled = true
                        activityFormBinding.chipSave.isCheckable = true
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@InventoryTransferLinesActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, t.message ?: "")
                        }
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@InventoryTransferLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //todo here save issue for production lines items of order...
    private fun saveProductionOrderLinesItems() {
        var comments = valueList[0].ItemDescription.toString()
        var docDate = valueList[0].WarehouseCode
        var absoluteEntry = valueList[0].ItemCode

        var series = getSeriesValue(docDate)

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("BPL_IDAssignedToInvoice", BPLIDNum)
            postedJson.addProperty("Comments", comments)
            postedJson.addProperty("DocDate", docDate) //todo current date will send here---
            postedJson.addProperty("Series", series)

            val DocumentLinesArray = JsonArray()

            for (i in tempList.indices) {
                //TODO sum of order line batch quantities and compare with line quantity..
                var quantity = 0.000
                quantity =
                    GlobalMethods.sumBatchQuantity(i, hashmapBatchQuantityList.get("Item" + i)!!)


                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", absoluteEntry)
                jsonObject.addProperty("BaseLine", tempList[i].LineNumber)
                jsonObject.addProperty("BaseType", "202")
                jsonObject.addProperty("Quantity", quantity)
                jsonObject.addProperty("WarehouseCode", sessionManagement.getWarehouseCode(this))


                var u_width = 0.0
                var u_length = 0.0
                var u_gsm = 0.0
                var BatchNumbersArray = JsonArray()

                batchList = hashMapBatchList.get("Item" + i)!!
                batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!
                for (i in batchList.indices) {
                    for (j in i until batchQuantityList.size) {
                        var jsonLinesObject = JsonObject()
                        u_width = batchList[i].U_Width
                        u_length = batchList[i].U_Length
                        u_gsm = batchList[i].U_GSM

                        jsonLinesObject.addProperty("BatchNumber", batchList[i].Batch)
                        jsonLinesObject.addProperty("SystemSerialNumber", batchList[i].SystemNumber)
                        jsonLinesObject.addProperty("Quantity", batchQuantityList[j])

                        BatchNumbersArray.add(jsonLinesObject)
                        break
                    }

                }

                jsonObject.addProperty("Factor1", u_width)
                jsonObject.addProperty("Factor2", u_length)
                jsonObject.addProperty(
                    "Factor3",
                    1
                ) //todo batchList[0].U_RG.toDouble()--> change by SAP factor3 value always send 1 according to batch count..
                jsonObject.addProperty("Factor4", u_gsm)

                jsonObject.add("BatchNumbers", BatchNumbersArray)
                if (batchList.size > 0)
                    DocumentLinesArray.add(jsonObject)
            }

            postedJson.add("DocumentLines", DocumentLinesArray)
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@InventoryTransferLinesActivity)
            networkClient.doGetInventoryGenExits(postedJson).apply {
                enqueue(object : Callback<InventoryGenExitsModel> {
                    override fun onResponse(
                        call: Call<InventoryGenExitsModel>,
                        response: Response<InventoryGenExitsModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 201) {
                                    Log.e("success------", "Successful!")
                                    GlobalMethods.showSuccess(
                                        this@InventoryTransferLinesActivity,
                                        "Issue Production Order Post Successfully."
                                    )
                                }
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
                                            this@InventoryTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@InventoryTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    }
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

                    override fun onFailure(call: Call<InventoryGenExitsModel>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@InventoryTransferLinesActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@InventoryTransferLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val handler = Handler(Looper.getMainLooper())


    //todo getting BPL_ID Number ....
    private fun getBPL_IDNumber() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            try {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
                val networkClient = NetworkClients.create(this@InventoryTransferLinesActivity)
                var batch = "Quality"//WIP
                networkClient.doGetBplID(
                    "BusinessPlaceID,WarehouseCode",
                    "WarehouseCode eq '" + sessionManagement.getWarehouseCode(this) + "'"
                ).apply {
                    enqueue(object : Callback<WarehouseBPL_IDModel> {
                        override fun onResponse(
                            call: Call<WarehouseBPL_IDModel>,
                            response: Response<WarehouseBPL_IDModel>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    var responseModel = response.body()!!
                                    if (!responseModel.value.isNullOrEmpty()) {
                                        BPLIDNum = responseModel.value[0].BusinessPlaceID
//                                        getWarehouseCode = responseModel.value[0].WarehouseCode
                                        sessionManagement.setWarehouseCode(
                                            this@InventoryTransferLinesActivity,
                                            responseModel.value[0].WarehouseCode
                                        )
                                        setAdapter()
//                                        Toast.makeText(this@InventoryTransferLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@InventoryTransferLinesActivity,
                                            "Not Found!",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                                                this@InventoryTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: InvocationTargetException) {
                                materialProgressDialog.dismiss()
                                e.printStackTrace()
                                Log.e("error---------", e.toString())
                            }
                        }

                        override fun onFailure(call: Call<WarehouseBPL_IDModel>, t: Throwable) {
                            Log.e("scannedItemFailure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@InventoryTransferLinesActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@InventoryTransferLinesActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }

                    })
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("e-----------", e.toString())
            }

        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(applicationContext, "No Network Connection", Toast.LENGTH_SHORT).show()
        }

    }


    //todo getting api list response from adapter to activity trough interface...
    override fun passList(dataList: List<ScanedOrderBatchedItems.Value>) {
        batchList = dataList
    }

    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            Log.e("Result==>B", data?.getStringExtra("batch_code").toString())
            InventoryTransferItemAdapter.onActivityResult(requestCode, resultCode, data)
        }
    }

    //todo query for series..
    fun getSeriesValue(docDate: String): String {
        var series = ""
        if (connection != null) {
            var statement: Statement? = null
            try {
                statement = connection!!.createStatement()
                var resultSet =
                    statement.executeQuery("Select  T0. Series as SeriesCode, T0.SeriesName  From NNM1 T0 WHERE T0.ObjectCode ='60'  and T0.Indicator=(select distinct Indicator from OFPR where '$docDate' between F_RefDate and T_RefDate ) and T0.Locked='N'")
                while (resultSet.next()) {
                    Log.e("ConStatus", "Success=>" + resultSet.getString(1))
                    //todo remove zero digits from quantity...
                    series = resultSet.getString(1)
                    Log.e("series", "Success=>" + series)
                }

            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            Log.e("Result=>", "Connection is null")
        }
        return series
    }

    //TODO set sql server for query...
    private fun setSqlServer() {
        Log.e("ConStatus", "Call setSqlServer() in InventoryTransferLinesActivity")
        val url =
            "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(this@InventoryTransferLinesActivity)
        ActivityCompat.requestPermissions(
            this as Activity,
            arrayOf<String>(Manifest.permission.INTERNET),
            PackageManager.PERMISSION_GRANTED
        )
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            Class.forName(AppConstants.Classes)
            connection =
                DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
            Log.e("ConStatus", "Success$connection")

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.e("ConStatus", "ClassNotFoundException -> ${e.message}")
        } catch (e: SQLException) {
            e.printStackTrace()
            Log.e("ConStatus", "SQLException -> ${e.message}")
        }
    }


    fun deleteCache(context: Activity) {
        try {
            val dir: File = context.getCacheDir()
            deleteDir(dir)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }


    private val barcode = StringBuffer()

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {

        if (event?.action == KeyEvent.ACTION_DOWN) {
            val pressedKey = event.unicodeChar.toChar()
            barcode.append(pressedKey)
        }
        if (event?.action == KeyEvent.ACTION_DOWN && event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            barcode.delete(0, barcode.length)
        }

        return super.dispatchKeyEvent(event)
    }

}