package com.soothe.sapApplication.ui.deliveryOneOrder.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.soothe.sapApplication.databinding.ActivityDeliveryOneLineBinding
import com.soothe.sapApplication.services.InternetSpeedCheckService
import com.soothe.sapApplication.ui.deliveryOneOrder.adapter.DeliveryOneLineAdapter
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoicePostModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException


class DeliveryOneLineActivity : AppCompatActivity() , DeliveryOneLineAdapter.AdapterCallback{

    lateinit var binding : ActivityDeliveryOneLineBinding

    lateinit var documentLineArrayList : ArrayList<InvoiceListModel.DocumentLine>
    lateinit var orderValueLineArrayList : InvoiceListModel.Value
    var position = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    lateinit var deliveryOneLineAdapter: DeliveryOneLineAdapter

    companion object{
        private const val TAG = "DeliveryOneLineActivity"
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryOneLineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //todo initialization...
        networkConnection      = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@DeliveryOneLineActivity)
        sessionManagement      = SessionManagement(this@DeliveryOneLineActivity)

        binding.ivLaserCode.setFocusableInTouchMode(true);
        binding.ivLaserCode.setFocusable(true)
        binding.ivLaserCode.requestFocus();
        binding.ivLaserCode.isCursorVisible=true



        //todo get arguments data...
        try {

            val intent = intent
            documentLineArrayList = intent.getSerializableExtra("invoiceData") as ArrayList<InvoiceListModel.DocumentLine>
            orderValueLineArrayList = intent.getSerializableExtra("invoiceDataValue") as InvoiceListModel.Value
            position = intent.extras?.getInt("pos")!!

            binding.tvTitle.text = "Delivery No. : " + orderValueLineArrayList.DocNum
            binding.tvCustomer.text = "Customer : " + orderValueLineArrayList.CardName
            setAdapter()


        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            e.printStackTrace()
        }


        deleteCache(this)




      /*  Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)*/

       // supportActionBar?.hide()

        binding.ivOnback.setOnClickListener {
            onBackPressed()
        }

        //todo cancel lines...
        binding.chipCancel.setOnClickListener {
            onBackPressed()

//            InternetSpeedChecker.checkAndShowToastIfSlow(this)
//            InternetSpeedChecker.measureSpeedAndShowToast(this)
        }



        // Start the InternetSpeedCheckService
        val serviceIntent = Intent(this, InternetSpeedCheckService::class.java)
        startService(serviceIntent)


    }


    //todo set adapter....
    fun setAdapter() {

        binding.ivNoDataFound.visibility = View.GONE
        binding.rvProductionOrderList.visibility = View.VISIBLE
        binding.btnLinearLayout.visibility = View.VISIBLE
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...
       // deliveryOneLineAdapter = DeliveryOneLineAdapter(this@DeliveryOneLineActivity, documentLineArrayList,binding.ivScanBatchCode, binding.ivLaserCode,this@DeliveryOneLineActivity, binding.chipSave)
        deliveryOneLineAdapter = DeliveryOneLineAdapter(this@DeliveryOneLineActivity, documentLineArrayList,binding.ivScanBatchCode, binding.edtSearch,this@DeliveryOneLineActivity, binding.chipSave)
        binding.rvProductionOrderList.adapter = deliveryOneLineAdapter


    }


    var orderDocLineData : ArrayList<InvoiceListModel.DocumentLine> = ArrayList()

    //todo calling save button functionality---
    override fun onApiResponseStock(quantityResponse: java.util.ArrayList<InvoiceListModel.DocumentLine>) {
        orderDocLineData = quantityResponse
        Log.e("hashmap--->", orderDocLineData.toString())

        binding.chipSave.isEnabled = false
        binding.chipSave.isCheckable = false
        postInvoiceOrderLineItems()
    }

    val handler = Handler(Looper.getMainLooper())

    lateinit var batchList: ArrayList<InvoiceListModel.DocumentLine>

    private fun postInvoiceOrderLineItems() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {


            materialProgressDialog.show()
            var ii=0;
            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("U_BPCode", orderValueLineArrayList.CardCode)
            postedJson.addProperty("U_BPName", orderValueLineArrayList.CardName)
            postedJson.addProperty("U_SDT", "INV")
            postedJson.addProperty("U_SDNo", orderValueLineArrayList.DocEntry)
            postedJson.addProperty("U_DocumentNo", orderValueLineArrayList.DocNum)
            postedJson.addProperty("U_PostingDate", GlobalMethods.getCurrentTodayDate()) //todo current date will send here---
            postedJson.addProperty("U_ARInvoice", orderValueLineArrayList.DocNum)


            var DocumentLinesArray = JsonArray()

            for (i in orderDocLineData.indices) {
                var jsonObject = JsonObject()
                jsonObject.addProperty("U_ITEMLINENO", orderDocLineData[i].LineNum)
                jsonObject.addProperty("U_ItemCode", orderDocLineData[i].ItemCode)
                jsonObject.addProperty("U_NavisionCode", orderDocLineData[i].NavisionCode)
                jsonObject.addProperty("U_ItemName", orderDocLineData[i].ItemDescription)

                if(orderDocLineData[i].isScanned>0){

                    val jsonObject1 = JsonObject()
                    jsonObject.addProperty("U_BoxQty", orderDocLineData[i].initialBoxes)
                    jsonObject.addProperty("U_PktQty", orderDocLineData[i].RemainingQuantity.toInt())
                    jsonObject.addProperty("U_SBQ",orderDocLineData[i].isScanned)
                    jsonObject.addProperty("U_SPQ",orderDocLineData[i].totalPktQty)

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
            val networkClient = NetworkClients.create(this@DeliveryOneLineActivity)
            networkClient.postInvoiceItems(postedJson).apply {
                enqueue(object : Callback<InvoicePostModel> {
                    override fun onResponse(call: Call<InvoicePostModel>, response: Response<InvoicePostModel>) {
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
                                    GlobalMethods.showSuccess(this@DeliveryOneLineActivity, "Delivery Order Post Successfully.")
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
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
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
                            GlobalMethods.showError(this@DeliveryOneLineActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryOneLineActivity, t.message ?: "")
                        }
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }


        }
        else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(this@DeliveryOneLineActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun callInvoiceUpdateApi(responseData: List<InvoicePostModel.DBSROWCollection>?) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var postedJson: JsonObject = JsonObject()

            var DocumentLinesArray = JsonArray()

            /*  for (i in responseData!!.indices) {
                  var jsonObject = JsonObject()

                  var resultCal = responseData[i].U_SPQ + (orderDocLineData[responseData[i].U_ITEMLINENO].Quantity - orderDocLineData[responseData[i].U_ITEMLINENO].RemainingQuantity.toInt())

                  Log.e(TAG, "callInvoiceUpdateApi: "+resultCal )

                  jsonObject.addProperty("LineNum", responseData[i].U_ITEMLINENO)
                  jsonObject.addProperty("U_SCNPKTQTY", resultCal)

                  DocumentLinesArray.add(jsonObject)
              }*/

            for (i in responseData!!.indices){
                for (j in i until orderDocLineData.size) {
                    if (responseData[i].U_ITEMLINENO.equals(orderDocLineData[j].LineNum)){

                        var jsonObject = JsonObject()

                        var resultCal = responseData[i].U_SPQ + (orderDocLineData[j].Quantity - orderDocLineData[j].RemainingQuantity.toInt())

                        Log.e(TAG, "callInvoiceUpdateApi: "+resultCal )

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
            val networkClient = NetworkClients.create(this@DeliveryOneLineActivity)

            networkClient.callDeliveryUpdate("(${orderValueLineArrayList.DocEntry})",postedJson).apply {
                enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 204) {
                                    Log.e("success_invoice_api----", "Successful!")
                                    updateDocStatusApi()
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
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
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

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@DeliveryOneLineActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryOneLineActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        }
        else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(this@DeliveryOneLineActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun updateDocStatusApi(){
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
            val networkClient = NetworkClients.create(this@DeliveryOneLineActivity)
            networkClient.updateDeliveryStatus(orderValueLineArrayList.DocEntry).apply {
                enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 200) {
                                    Log.e("success_status_api----", "Successful!")
                                    onBackPressed()
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
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(this@DeliveryOneLineActivity, mError.error.message.value)
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

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@DeliveryOneLineActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DeliveryOneLineActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }

        }
        else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(this@DeliveryOneLineActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            Log.e("Result==> BP=>1", data?.getStringExtra("batch_code").toString())
            deliveryOneLineAdapter?.onActivityResult(requestCode, resultCode, data)
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




}