package com.soothe.sapApplication.ui.deliveryOrderModule.UI

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.databinding.ActivityDeliveryDocumentLineBinding
import com.soothe.sapApplication.ui.deliveryOrderModule.Adapter.DocumentOrderLineAdapter
import com.soothe.sapApplication.ui.deliveryOrderModule.Model.DeliveryModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

class DeliveryDocumentLineActivity : AppCompatActivity(), DocumentOrderLineAdapter.AdapterCallback {
    private lateinit var deliveryOrderBinding: ActivityDeliveryDocumentLineBinding
    private lateinit var documentLineList_gl: ArrayList<DeliveryModel.DocumentLine>
    private lateinit var deliveryValueList_gl: DeliveryModel.Value
    private var deliveryValueList: List<DeliveryModel.Value> = ArrayList()
    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var documentOrderLineAdapter: DocumentOrderLineAdapter? = null
    private var connection: Connection? = null

    //todo batch scan and quantity list interface override...
    var hashMapBatchList: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> =
        HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: HashMap<String, ArrayList<String>> =
        HashMap<String, ArrayList<String>>()
    var deliveryItem: DeliveryModel.Value? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliveryOrderBinding = ActivityDeliveryDocumentLineBinding.inflate(layoutInflater)
        setContentView(deliveryOrderBinding.root)

        title = "Delivery Screen"
        supportActionBar?.hide()

        //  setSqlServer()

        //todo get arguments data...
        val intent = intent
        documentLineList_gl = intent.getSerializableExtra("documentLineList") as ArrayList<DeliveryModel.DocumentLine>
        deliveryValueList_gl = intent.getSerializableExtra("deliveryValueList") as DeliveryModel.Value //todo getting list selected item values and lines only not all size data..
        position = intent.extras?.getInt("pos")
        try {

            deliveryItem =
                intent.getSerializableExtra("deliveryItemModel") as DeliveryModel.Value


        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

        deliveryValueList = listOf(deliveryValueList_gl)

        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this)
        sessionManagement = SessionManagement(this@DeliveryDocumentLineActivity)
        setAdapter()

        deliveryOrderBinding.tvItemNo.text = deliveryValueList_gl.DocNum

        deliveryOrderBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }
        //todo cancel lines...
        deliveryOrderBinding.chipCancel.setOnClickListener {

            onBackPressed()
        }


    }

    //todo set document line adapter....
    fun setAdapter() {
        val iterator = documentLineList_gl.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().RemainingOpenQuantity
            try {
                if (item <= 0) {
                    iterator.remove()
                }
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        if (documentLineList_gl.size > 0) {
            deliveryOrderBinding.ivNoDataFound.visibility = View.GONE
            deliveryOrderBinding.rvDocumentLineList.visibility = View.VISIBLE
            deliveryOrderBinding.btnLinearLayout.visibility = View.VISIBLE

            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
            deliveryOrderBinding.rvDocumentLineList.layoutManager = layoutManager
            //todo parse save button in adapter constructor for click listener on adapter....
            documentOrderLineAdapter = DocumentOrderLineAdapter(
                this@DeliveryDocumentLineActivity,
                documentLineList_gl,
                deliveryOrderBinding.chipSave,
                this,
                deliveryOrderBinding.ivScanBatchCode,
                deliveryOrderBinding.ivLaserCode
            )
            deliveryOrderBinding.rvDocumentLineList.adapter = documentOrderLineAdapter

        } else {
            deliveryOrderBinding.ivNoDataFound.visibility = View.VISIBLE
            deliveryOrderBinding.rvDocumentLineList.visibility = View.GONE
            deliveryOrderBinding.btnLinearLayout.visibility = View.GONE
        }
    }

    override fun onApiResponse(response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>, documentLineList_gl: ArrayList<DeliveryModel.DocumentLine>) {
        hashMapBatchList = response
        // hashmapBatchQuantityList = quantityResponse
        Log.e("hashmap--->", "" + documentLineList_gl.size)
        Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())
        // saveDeliveryOrderItem()

        saveDeliveryOrderItemBP(documentLineList_gl);
    }

    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: ArrayList<String>


    //Save By Bhupi

    private fun saveDeliveryOrderItemBP(documentLineList_gl: ArrayList<DeliveryModel.DocumentLine>) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            var docDate = deliveryValueList[0].DocDate
            var ii = 0;
            var series = getSeriesValue(docDate)

            var postedJson = JsonObject()
            postedJson.addProperty("CardCode", deliveryItem!!.CardCode)
            postedJson.addProperty("BPL_IDAssignedToInvoice", deliveryItem!!.BPL_IDAssignedToInvoice)
            postedJson.addProperty("DocDate", deliveryItem!!.DocDate) //todo current date will send here---
            postedJson.addProperty("DocDueDate", deliveryItem!!.DocDueDate)
            postedJson.addProperty("Series", 98)
            postedJson.addProperty("TaxDate", deliveryItem!!.DocDate)
            postedJson.addProperty("CardName", deliveryItem!!.CardName)
            postedJson.addProperty("DocObjectCode", "oDeliveryNotes")
            postedJson.addProperty("DocType", deliveryItem!!.DocType)


            val DocumentLinesArray = JsonArray()

            for (obj in documentLineList_gl) {
                var itemJson = JsonObject()
                itemJson.apply {
                    /*                addProperty("ItemCode", obj!!.ItemCode)
                                    addProperty("ItemDescription", obj!!.ItemDescription)*/
                    addProperty("Quantity", obj!!.totalPktQty)
                    addProperty("Price", obj!!.Price)
                    addProperty("WarehouseCode", obj!!.WarehouseCode)
                    addProperty("BaseType", 17)
                    addProperty("BaseEntry", deliveryItem!!.DocEntry)
                    addProperty("BaseLine", obj!!.LineNum)
                    addProperty("TaxCode", obj!!.TaxCode)


                }

                if (obj.isScanned > 0) {
                    val stockBin = JsonArray()
                    val jsonObject1 = JsonObject()
                    jsonObject1.addProperty("BinAbsEntry", "5")
                    jsonObject1.addProperty("Quantity", obj.totalPktQty)
                    jsonObject1.addProperty("BaseLineNumber", ii)

                    stockBin.add(jsonObject1)
                    itemJson.add("DocumentLinesBinAllocations", stockBin)
                    DocumentLinesArray.add(itemJson)
                    ii++
                }
                //  DocumentLinesArray.add(itemJson)

            }
            postedJson.add("DocumentLines", DocumentLinesArray)
            Log.e("DocumentLines===>", DocumentLinesArray.toString())

            Log.e("postedJson===>", postedJson.toString())
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@DeliveryDocumentLineActivity)
            networkClient.doGetDeliveryNotes(postedJson).apply {
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
                                    var responseModel = response.body()!!
                                    GlobalMethods.showSuccess(
                                        this@DeliveryDocumentLineActivity,
                                        "Delivery Order ${responseModel.DocNum} Post Successfully "
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
                                            this@DeliveryDocumentLineActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLineActivity,
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
                            GlobalMethods.showError(this@DeliveryDocumentLineActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryDocumentLineActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@DeliveryDocumentLineActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //todo here saving order lines items of order...
    private fun saveDeliveryOrderItem() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            var docDate = deliveryValueList[0].DocDate

            var series = getSeriesValue(docDate)

            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("CardCode", deliveryValueList[0].CardCode)
            postedJson.addProperty("BPL_IDAssignedToInvoice", deliveryValueList[0].BPL_IDAssignedToInvoice)
            postedJson.addProperty("DocDate", docDate) //todo current date will send here---
            postedJson.addProperty("DocDueDate", docDate)
            postedJson.addProperty("Series", series)

            val DocumentLinesArray = JsonArray()

            for (i in documentLineList_gl.indices) {

                var unitOfMeasurement = documentLineList_gl[i].UnitsOfMeasurment

                //TODO sum of order line batch quantities and compare with line quantity..
                var quantity = 0.000
                var temp = 0.00
                try {
                    if (hashmapBatchQuantityList.get("Item" + i)!!.size > 0) {
                        temp = GlobalMethods.sumBatchQuantity(i, hashmapBatchQuantityList.get("Item" + i)!!)
                    } else {
                        temp = 0.0
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
                quantity = temp / unitOfMeasurement  //todo calculate delivery document line quantity..

                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseType", 17)
                jsonObject.addProperty("BaseEntry", documentLineList_gl[i].DocEntry)
                jsonObject.addProperty("BaseLine", documentLineList_gl[i].LineNum)
                jsonObject.addProperty("Quantity", GlobalMethods.changeDecimal(quantity.toString())!!)
                jsonObject.addProperty("TaxCode", documentLineList_gl[i].TaxCode)
                jsonObject.addProperty("UnitPrice", documentLineList_gl[i].UnitPrice)
                jsonObject.addProperty("WarehouseCode", documentLineList_gl[i].WarehouseCode)

                var BatchNumbersArray = JsonArray()
                batchList = hashMapBatchList.get("Item" + i)!!
                batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!
                for (i in batchList.indices) {
                    for (j in i until batchQuantityList.size) {
                        var jsonLinesObject = JsonObject()

                        jsonLinesObject.addProperty("BatchNumber", batchList[i].Batch)
                        jsonLinesObject.addProperty("SystemSerialNumber", batchList[i].SystemNumber)
                        jsonLinesObject.addProperty("Quantity", batchQuantityList[j])

                        BatchNumbersArray.add(jsonLinesObject)
                        break
                    }

                }

                jsonObject.add("BatchNumbers", BatchNumbersArray)
                if (batchList.size > 0)
                    DocumentLinesArray.add(jsonObject)
            }

            Log.e("DocumentLines===>", DocumentLinesArray.toString())
            postedJson.add("DocumentLines", DocumentLinesArray)
            Log.e("postedJson===>", postedJson.toString())
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this@DeliveryDocumentLineActivity)
            networkClient.doGetDeliveryNotes(postedJson).apply {
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
                                    var responseModel = response.body()!!
                                    GlobalMethods.showSuccess(
                                        this@DeliveryDocumentLineActivity,
                                        "Delivery Order ${responseModel.DocNum} Post Successfully "
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
                                            this@DeliveryDocumentLineActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@DeliveryDocumentLineActivity,
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
                            GlobalMethods.showError(this@DeliveryDocumentLineActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryDocumentLineActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@DeliveryDocumentLineActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val handler = Handler(Looper.getMainLooper())


    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            documentOrderLineAdapter?.onActivityResult(requestCode, resultCode, data)
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
                    statement.executeQuery("Select  T0. Series as SeriesCode, T0.SeriesName  From NNM1 T0 WHERE T0.ObjectCode ='15'  and T0.Indicator=(select distinct Indicator from OFPR where '$docDate' between F_RefDate and T_RefDate ) and T0.Locked='N'")
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
        Log.e("ConStatus", "Call setSqlServer() in DeliveryDocumentLineActivity")
        try {
            val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(this@DeliveryDocumentLineActivity)
            ActivityCompat.requestPermissions(this as Activity, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            try {
                Class.forName(AppConstants.Classes)
                connection = DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
                Log.e("ConStatus", "Success$connection")

            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                Log.e("ConStatus", "ClassNotFoundException -> ${e.message}")
            } catch (e: SQLException) {
                e.printStackTrace()
                Log.e("ConStatus", "SQLException -> ${e.message}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ConStatus", "IOException -> ${e.message}")
        }

    }

}