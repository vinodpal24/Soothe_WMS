package com.soothe.sapApplication.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityDemo2Binding
import com.soothe.sapApplication.ui.home.HomeActivity
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import com.soothe.sapApplication.ui.login.Model.LoginResponseModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

class DemoActivity : AppCompatActivity() {
    lateinit var binding: ActivityDemo2Binding
    private lateinit var mediaPlayer: MediaPlayer

    lateinit var documentLineArrayList: ArrayList<InvoiceListModel.DocumentLine>
    lateinit var orderValueLineArrayList: InvoiceListModel.Value
    lateinit var deliverOneLineShubhAdapter: DeliverOneLineShubhAdapter
    var position = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement

    val REQUEST_CODE = 100
    private var pos: Int = 0
    private var scanCount: Int = 0
    private var itemCode = ""
    private var itemPo: Int = -1

    val THRESHOLD_25_MINUTES = 26* 60 * 1000

    companion object {
        private const val TAG = "DemoActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemo2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.hide()
        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@DemoActivity)
        sessionManagement = SessionManagement(this@DemoActivity)
        binding.ivScanBatchCode.visibility = View.GONE

        /*    binding.edtSearch.requestFocus()
            val imm =getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT)*/
//binding.edtSearch.requestFocus()
        /*    binding.edtSearch.apply {
               setFocusableInTouchMode(true);
                setFocusable(true)
             requestFocus();
                isCursorVisible=true
            }*/
        binding.ivOnback.setOnClickListener {
            finish()
        }



        binding.edtSearch.setOnKeyListener { view, keyCode, event ->
            //||event.action == KeyEvent.ACTION_UP

            // Handle the Enter key press event
            // Retrieve the scanned data from the EditText
            val scannedData = binding.edtSearch.text.toString()
            // Process the scanned data
            Log.e("Scanned Data", scannedData)
            if (!scannedData.toString().trim().isEmpty()) {

                try {
                    var text = scannedData.trim()
                    // var x = text

                    if (text.contains("/"))
                        text = text.split("/")[0]

                    Log.e("text ===>", text)

                   val startTimeMillis= sessionManagement.getLoginSessionTimeout(this@DemoActivity)!!
                        .toLong()
                   val endTimeMillis=  System.currentTimeMillis()
                    val timeDifference = endTimeMillis - startTimeMillis
                    Log.e(TAG, "checkTimeDifferenceAndAct: $timeDifference" )

                    if (timeDifference >= THRESHOLD_25_MINUTES) {
                        GlobalMethods.showMessage(this@DemoActivity,"Session Reconnecting")
                        playSoundSession()
                        apiCall()

                    }
                    else{
                        scanNavCodeItem(text)
                    }

                   //todo comment by me
                    binding.edtSearch.setText("")
                    binding.edtSearch.setFocusableInTouchMode(true);
                    binding.edtSearch.requestFocus()
                    binding.edtSearch.setFocusable(true)


                } catch (e: Exception) {
                    Log.e("Adapter", "onBindViewHolder: WINDOW ")
                    e.message
                }
            }
            // Return true to indicate that the event has been consumed

            return@setOnKeyListener true

        }


        //todo get arguments data...
        try {

            val intent = intent
            documentLineArrayList =
                intent.getSerializableExtra("invoiceData") as ArrayList<InvoiceListModel.DocumentLine>
            orderValueLineArrayList =
                intent.getSerializableExtra("invoiceDataValue") as InvoiceListModel.Value
            position = intent.extras?.getInt("pos")!!

            binding.tvTitle.text = "Delivery No. : " + orderValueLineArrayList.DocNum
            binding.tvCustomer.text = "Customer : " + orderValueLineArrayList.CardName
            setAdapter()


        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            e.printStackTrace()
        }


        /*   deleteCache(this)

           //todo if leaser type choose..
           if (sessionManagement.getScannerType(this) == "LEASER" || sessionManagement.getScannerType(this) == null) {
               binding.ivScanBatchCode.visibility = View.GONE

               */
        /*** Comment ***//*

            binding.edtSearch.requestFocus()//todo comment by me

            binding.edtSearch.isCursorVisible = true


            binding.edtSearch.setOnKeyListener { view, keyCode, event ->
                //||event.action == KeyEvent.ACTION_UP
                if (event.action == KeyEvent.ACTION_DOWN) {
                    // Handle the Enter key press event
                    // Retrieve the scanned data from the EditText
                    val scannedData = binding.edtSearch.text.toString()
                    // Process the scanned data
                    Log.e("Scanned Data", scannedData)
                    if (scannedData.trim() == "D") {
                        *//*laserCode.setFocusableInTouchMode(true);
                        laserCode.requestFocus()
                        laserCode.setFocusable(true)*//*
                        binding.edtSearch.setText("")
                        binding.edtSearch.requestFocus()
                        GlobalMethods.showMessage(this, "Focus Adjusting. Start Scanning!")
                    } else if (!scannedData.toString().trim().isEmpty()) {

                        try {
                            var text = scannedData.trim()
                            // var x = text

                            if (text.contains("/"))
                                text = text.split("/")[0]

                            Log.e("text ===>", text)

                              scanNavCodeItem(text)//todo comment by me
                            binding.edtSearch.setText("")
                            *//* laserCode.setFocusableInTouchMode(true);
                             laserCode.requestFocus()
                             laserCode.setFocusable(true)*//*


                        } catch (e: Exception) {
                            Log.e("Adapter", "onBindViewHolder: WINDOW ")
                            e.message
                        }
                    }
                    // Return true to indicate that the event has been consumed

                    return@setOnKeyListener true
                }
                // Return false to indicate that the event has not been consumed
                return@setOnKeyListener false
            }





            binding.ivScanBatchCode.setOnClickListener {

                if (sessionManagement.getScannerType(this) == null) {
                    showPopupNotChooseScanner()
                }



                else if (sessionManagement.getScannerType(this) == "QR_SCANNER") {
                 *//*   val intent = Intent(this, QRScannerActivity::class.java)
                    pos = adapterPosition
                    Log.e("Leaser Scan"," -> "+ adapterPosition);
                    scanCount = lineArrayList[pos].isScanned
                    //totalPktQty = list[pos].totalPktQty
                    startActivityForResult(intent, REQUEST_CODE)*//*
                }
                else{
                    showPopupNotChooseScanner()
                }

            }


//todo

            false
        }

        //todo is qr scanner type choose..
        else if (sessionManagement.getScannerType(this) == "QR_SCANNER" || sessionManagement.getScannerType(this) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
            binding.ivScanBatchCode.visibility = View.VISIBLE

            //TODO click on barcode scanner for popup..
         *//*   binding.ivScanBatchCode.setOnClickListener {
                var text = binding.edBatchCodeScan.text.toString().trim()
                itemCode = this.ItemCode
//                        recyclerView = binding.rvBatchItems
                tvOpenQty = binding.tvOpenQty
                tvTotalScanQty = binding.tvTotalScannQty
                tvTotalScanGW = binding.tvTotalScanGw
                Log.e("Manual Scan","");
                *//**//*  width = this.Factor1
                  U_gsmso = this.U_GSMSO  *//**//*
                remainingOpenQuantity = this.RemainingQuantity

                if (sessionManagement.getScannerType(this) == null) {
                    showPopupNotChooseScanner()
                }

                else if (sessionManagement.getScannerType(this) == "QR_SCANNER") {
                    val intent = Intent(this, QRScannerActivity::class.java)
                    Log.e("Manual Scan","+"+lineArrayList.get(adapterPosition).ItemCode);
                    pos = setScanDataOnItem(lineArrayList, lineArrayList.get(adapterPosition).ItemCode)
                    // pos = adapterPosition
                   startActivityForResult(intent, REQUEST_CODE)

                }

            }*//*

            //todo for manual batch entry..
            *//*  binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                  //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
                  if (actionId == KeyEvent.ACTION_UP && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                      var text = binding.edBatchCodeScan.text.toString().trim()
                      *//**//*  if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                  //todo scan call api here...
                                  scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, RemainingOpenQuantity,
                                      this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)

                              }*//**//*
                            binding.edBatchCodeScan.setText("")
                            true

                        } else {
                            false
                        }
                    }*//*//todo commnet


        }

        else{
            showPopupNotChooseScanner()
        }*/


    }

    //todo set adapter....
    fun setAdapter() {

        if (documentLineArrayList.size > 0) {
            sessionManagement.setWarehouseCode(this, documentLineArrayList[0].WarehouseCode)
        }

        binding.ivNoDataFound.visibility = View.GONE
        binding.ivScanBatchCode.visibility = View.GONE
        binding.rvProductionOrderList.visibility = View.VISIBLE
        binding.btnLinearLayout.visibility = View.VISIBLE
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...
        // deliveryOneLineAdapter = DeliveryOneLineAdapter(this@DeliveryOneLineActivity, documentLineArrayList,binding.ivScanBatchCode, binding.ivLaserCode,this@DeliveryOneLineActivity, binding.chipSave)
        deliverOneLineShubhAdapter = DeliverOneLineShubhAdapter(documentLineArrayList)
        binding.rvProductionOrderList.adapter = deliverOneLineShubhAdapter
        deliverOneLineShubhAdapter.notifyDataSetChanged()


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

    //todo show popup when not selected scanner type button click popup.
    private fun showPopupNotChooseScanner() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this).inflate(R.layout.custom_popup_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui..
        val cancelBtn = view.findViewById<MaterialButton>(R.id.cancel_btn)
        val yesBtn = view.findViewById<MaterialButton>(R.id.ok_btn)

        cancelBtn.setOnClickListener {
            builder.dismiss()
        }

        yesBtn.setOnClickListener {
            var intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            builder.dismiss()
            deliverOneLineShubhAdapter.notifyDataSetChanged()
        }

        builder.setCancelable(true)
        builder.show()
    }


    private fun scanNavCodeItem(text: String) {


        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(this)
            networkClient.doGetScan(
                "ItemCode,U_PCS_QTY,U_PACK_QTY",
                "U_NAVI_CODE eq '" + text + "'"
            ).apply {
                enqueue(object : Callback<NavScanResModel> {
                    override fun onResponse(
                        call: Call<NavScanResModel>,
                        response: Response<NavScanResModel>
                    ) {
                        try {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {



                                binding.edtSearch.setText("")
                                binding.edtSearch.setFocusableInTouchMode(true);
                                binding.edtSearch.requestFocus()
                                binding.edtSearch.setFocusable(true)
                                Log.e("response---------", response.body().toString())

                                var responseModel = response.body()!!

                                if (responseModel.value.size > 0) {
                                    Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                    itemPo = GlobalMethods.setScanDataOnItem(
                                        documentLineArrayList,
                                        responseModel.value[0].ItemCode
                                    )
                                    Log.e("ItemPo==>", "" + itemPo)
                                }



                                if (itemPo == -1) {
                                    GlobalMethods.showError(
                                        this@DemoActivity,
                                        "Item Code not matched"
                                    )
                                } else if (documentLineArrayList[itemPo].totalPktQty.toDouble() >= documentLineArrayList[itemPo].RemainingQuantity.toDouble()) {
                                    GlobalMethods.showError(
                                        this@DemoActivity,
                                        "Scanning completed for this Item"
                                    )
                                } else {
                                    if (documentLineArrayList.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        scanCount = documentLineArrayList[itemPo].isScanned
                                        ++scanCount

                                        var modelResponse = responseModel.value[0]

                                        if (documentLineArrayList[itemPo].totalPktQty.toDouble() > documentLineArrayList[itemPo].RemainingQuantity.toDouble()) {
                                            GlobalMethods.showError(
                                                this@DemoActivity,
                                                "Scanning completed for this Item"
                                            )
                                        } else {

                                            //todo comment
//                                            IS_NAV_FIRST_TIME = true //todo make it true after sacn any item --

                                            documentLineArrayList[itemPo].isScanned = scanCount
                                            documentLineArrayList[itemPo].totalPktQty =
                                                responseModel.value[0].U_PACK_QTY.toInt() * scanCount
                                            documentLineArrayList[itemPo].NavisionCode =
                                                text //todo comment by me---
                                            GlobalMethods.showSuccess(
                                                this@DemoActivity,
                                                "Box added"
                                            )
                                            playSound()
                                            deliverOneLineShubhAdapter.notifyDataSetChanged()
                                        }

                                        Log.e(
                                            "linearList===>",
                                            "onResponse: " + documentLineArrayList.toString()
                                        )

                                    } else {
                                        GlobalMethods.showError(
                                            this@DemoActivity,
                                            "Invalid Batch Code"
                                        )
                                        Log.e("not_response---------", response.message())
                                    }
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
                                            this@DemoActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@DemoActivity,
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

                            GlobalMethods.showError(
                                this@DemoActivity,
                                "Something Went wrong.Please try again."
                            )
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(this@DemoActivity,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(this@DemoActivity, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }


    fun checkTimeDifferenceAndAct(context: Context, startTimeMillis: Long, endTimeMillis: Long):Boolean {
        val timeDifference = endTimeMillis - startTimeMillis
        Log.e(TAG, "checkTimeDifferenceAndAct: $timeDifference" )

        if (timeDifference >= THRESHOLD_25_MINUTES) {
            apiCall()
            return true
        }
        return false
    }

    private fun playSound() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.boxx_added)
        mediaPlayer.start()
    }


    private fun playSoundSession() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.session_reconnecting)
        mediaPlayer.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

    private fun apiCall() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

                var jsonObject: JsonObject = JsonObject()
                jsonObject.addProperty("CompanyDB", sessionManagement.getCompanyDB(this@DemoActivity))
                jsonObject.addProperty(
                    "Password",
                    sessionManagement.getLoginPassword(this@DemoActivity)
                )
                jsonObject.addProperty(
                    "UserName",
                    sessionManagement.getLoginId(this@DemoActivity)
                )
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
              NetworkClients.create(this).doGetLoginCall(jsonObject).apply {
                    enqueue(object : Callback<LoginResponseModel> {
                        override fun onResponse(
                            call: Call<LoginResponseModel>,
                            response: Response<LoginResponseModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    materialProgressDialog.dismiss()
                                    var loginResponseModel = response.body()!!
                                    //todo shares preference store....
                                    sessionManagement.setSessionId(
                                        this@DemoActivity,
                                        loginResponseModel.SessionId
                                    )
                                    sessionManagement.setSessionTimeout(
                                        this@DemoActivity,
                                        loginResponseModel.SessionTimeout
                                    )
                                    sessionManagement.setLoginSessionTimeout(
                                        this@DemoActivity,
                                        System.currentTimeMillis().toString()
                                    )
                                    sessionManagement.setFromWhere(this@DemoActivity, "ElseCase")
                                    Log.e("api_success-----", response.toString())

                                } else {
                                    materialProgressDialog.dismiss()

                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(
                                                this@DemoActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@DemoActivity,
                                                mError.error.message.value
                                            )
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                            Log.e("login_api_failure-----", t.toString())
                            materialProgressDialog.dismiss()
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@DemoActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@DemoActivity, t.message ?: "")
                            }
                        }

                    })
                }


        } else {
            materialProgressDialog.dismiss()
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Internet Connection Alert")
                .setMessage("Please Check Your Internet Connection")
                .setPositiveButton("Close") { dialogInterface, i ->
                    finish()
                }.show()
        }
    }
}