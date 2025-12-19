package com.soothe.sapApplication.ui.deliveryOneOrder.adapter

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.gson.GsonBuilder
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.soothe.sapApplication.ui.deliveryOneOrder.ui.DeliveryOneLineActivity
import com.soothe.sapApplication.ui.home.HomeActivity
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.ref.WeakReference


class DeliveryOneLineAdapter (private val context: Context, var lineArrayList: ArrayList<InvoiceListModel.DocumentLine>, private val scanButton: AppCompatImageView,
                              val laserCode: EditText, private val callback: DeliveryOneLineActivity, private val save: Chip
): RecyclerView.Adapter<DeliveryOneLineAdapter.ViewHolder>() {

    private lateinit var sessionManagement: SessionManagement
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    val REQUEST_CODE = 100
    private var pos: Int = 0
    private var scanCount: Int = 0
    private var itemCode = ""
    var remainingOpenQuantity: Double = 0.0
    var width = 0.0
    var U_gsmso = 0.0
    lateinit var tvOpenQty: TextView
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView

    var IS_NAV_FIRST_TIME = false


    init {
        sessionManagement = SessionManagement(context)
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(context)
    }

    interface AdapterCallback {

        fun onApiResponseStock(quantityResponse: ArrayList<InvoiceListModel.DocumentLine>)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryOneLineAdapter.ViewHolder
             {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeliveryOneLineAdapter.ViewHolder(binding)
    }



 override fun onBindViewHolder(holder: ViewHolder, position: Int)
             {
        val weakReference = WeakReference(lineArrayList[position])
        val myObject = weakReference.get()
        with(holder) {
            with(myObject) {
                binding.trBaseQuantity.visibility=View.VISIBLE

                binding.tvTotalBaseQuantity.text=":   "+this?.Quantity.toString()

                binding.tvItemName.text = ":   " + this?.ItemDescription
                binding.tvOpenQty.text = ":   " + this?.ItemCode

                binding.issueAndDocumentlayout.visibility = View.VISIBLE
                binding.fromWarehouseRow.visibility = View.GONE

                var qtyBox = 0;
                if (GlobalMethods.numberToK(this?.Quantity.toString())!!.toInt() > 0)
                    qtyBox = GlobalMethods.numberToK(this?.RemainingQuantity.toString())!!.toInt() / GlobalMethods.numberToK(this!!.U_IQTY)!!.toInt()

                var doBoxQuantity = 0;
                if (GlobalMethods.numberToK(this!!.U_IQTY)!!.toInt() > 0)
                    doBoxQuantity = GlobalMethods.numberToK(this!!.Quantity.toString())!!.toInt() / GlobalMethods.numberToK(
                        this.U_IQTY
                    )!!.toInt()

                binding.tvDoBoxQuantity.text = ":   $doBoxQuantity"


                binding.tvWidth.text         = ":   $qtyBox"
                binding.tvTotalScannQty.text = ":   " + this?.RemainingQuantity
                this!!.initialBoxes          = qtyBox
//                binding.fromWarehouse.text = ":   " + this.FromWarehouseCode
//                binding.toWarehouse.text = ":   " + this.WarehouseCode
                binding.tvTotalScanGw.text = ":   " +this.totalPktQty

                binding.tvLength.text = ":   " + this.isScanned

//                binding.tvNavicode.text = this.NavisionCode?.let { date -> " : " + date } ?: " :" //todo NEW CHANGE comment bcz now already gettiing nav code in document line --
                binding.tvNavicode.text =  " :   " + this.NavCode

                sessionManagement.setWarehouseCode(context, this.WarehouseCode)

                //todo calling api here to get nav code at particular scan items code--


              /*  laserCode.requestFocus()

                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(laserCode, InputMethodManager.HIDE_IMPLICIT_ONLY)
*/



                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER" || sessionManagement.getScannerType(context) == null) {
                    binding.ivScanBatchCode.visibility = View.GONE

                    /*** Comment ***/

                    laserCode.requestFocus()//todo comment by me

                    laserCode.isCursorVisible = true


                    laserCode.setOnKeyListener { view, keyCode, event ->
                        //||event.action == KeyEvent.ACTION_UP
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            // Handle the Enter key press event
                            // Retrieve the scanned data from the EditText
                            val scannedData = laserCode.text.toString()
                            // Process the scanned data
                            Log.e("Scanned Data", scannedData)
                            if (scannedData.trim()=="D"){
                                /*laserCode.setFocusableInTouchMode(true);
                                laserCode.requestFocus()
                                laserCode.setFocusable(true)*/
                                laserCode.setText("")
                                laserCode.requestFocus()
                                GlobalMethods.showMessage(context, "Focus Adjusting. Start Scanning!")
                            }
                            else if(!scannedData.toString().trim().isEmpty()) {

                                try {
                                    var text = scannedData.trim()
                                    // var x = text

                                    if (text.contains("/"))
                                        text = text.split("/")[0]

                                    Log.e("text ===>", text)

                                    scanNavCodeItem(text)//todo comment by me
                                     laserCode.setText("")
                                   /* laserCode.setFocusableInTouchMode(true);
                                    laserCode.requestFocus()
                                    laserCode.setFocusable(true)*/



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


                  /*  laserCode.setOnKeyListener { view1, keyCode, keyEvent ->
                     if(keyEvent.keyCode==0)
                        Log.e("text1234 ===>", laserCode.text.toString())
                        Log.e("textAction ===>", keyEvent.action.toString())


                       val textt = laserCode.text.toString()
                        if (textt.trim().isEmpty()){
                            laserCode.setFocusableInTouchMode(true);
                            laserCode.requestFocus()
                            laserCode.setFocusable(true)

                            GlobalMethods.showMessage(context, "Something went Wrong. Try Again!")
                        }

                      else  if (!textt.toString().trim().isEmpty()) {

                            try {
                                var text = textt.trim()
                                // var x = text

                                if (text.contains("/"))
                                    text = text.split("/")[0]

                                Log.e("text ===>", text)

                                scanNavCodeItem(text)//todo comment by me
                               // laserCode.setText("")
                                laserCode.setFocusableInTouchMode(true);
                                laserCode.requestFocus()
                                laserCode.setFocusable(true)



                            } catch (e: Exception) {
                                Log.e("Adapter", "onBindViewHolder: WINDOW ")
                                e.message
                            }
                        }

                        return@setOnKeyListener true
                    }*/


                    scanButton.setOnClickListener {

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }

                        /*  else if (sessionManagement.getScannerType(context) == "LEASER") {

                              val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
                              val view = LayoutInflater.from(context).inflate(R.layout.laser_scanner_dialog_layout, null)
                              builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                              builder.window?.setLayout(
                                  ViewGroup.LayoutParams.MATCH_PARENT,
                                  ViewGroup.LayoutParams.WRAP_CONTENT
                              )
                              builder.window?.setGravity(Gravity.CENTER)
                              builder.setView(view)

                              var ed_batch_code_scan = view.findViewById<EditText>(R.id.ed_batch_code_scan)
                              var cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)
                              var okView = view.findViewById<LinearLayout>(R.id.okView)
                              var okBtn = view.findViewById<AppCompatButton>(R.id.okBtn)
                              cancelBtn.visibility = View.GONE
                              cancelBtn.visibility = View.GONE
                              okView.visibility = View.GONE
                              ed_batch_code_scan.requestFocus()

                              builder.setCancelable(true)
                              builder.show()

                              ed_batch_code_scan.setOnKeyListener { view1, keyCode, keyEvent ->
                                  if (ed_batch_code_scan.isFocused() && ed_batch_code_scan.isCursorVisible() && ed_batch_code_scan.hasFocus()) {
                                      try {
                                          var inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                          inputMethodManager.hideSoftInputFromWindow(ed_batch_code_scan.getWindowToken(), 0)
                                      } catch (e: Exception) {
                                          Log.e("Adapter", "onBindViewHolder: WINDOW LEAAAKED")
                                      }
                                  }
                                  if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {

                                      try {
                                          var text = ed_batch_code_scan.text.toString().trim()
                                          if (text.contains("/"))
                                              text = text.split("/")[0];

                                          Log.e("text==>Leaser", text)


                                          scanNavCodeItem(text)//todo comment by me---

                                          ed_batch_code_scan.setText("")
                                          ed_batch_code_scan.requestFocus()
                                          builder.dismiss()
                                      } catch (e: Exception) {
                                          Log.e("Adapter", "onBindViewHolder: WINDOW ")
                                          e.message
                                      }
                                  }
                                  return@setOnKeyListener true
                              }

                          } *///todo

                        else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            pos = adapterPosition
                            Log.e("Leaser Scan"," -> "+ adapterPosition);
                            scanCount = lineArrayList[pos].isScanned
                            //totalPktQty = list[pos].totalPktQty
                            (context as DeliveryOneLineActivity).startActivityForResult(intent, REQUEST_CODE)
                        }
                        else{
                            showPopupNotChooseScanner()
                        }

                    }


//todo

                    false
                }

                //todo is qr scanner type choose..
                else if (sessionManagement.getScannerType(context) == "QR_SCANNER" || sessionManagement.getScannerType(context) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.VISIBLE

                    //TODO click on barcode scanner for popup..
                    scanButton.setOnClickListener {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        itemCode = this.ItemCode
//                        recyclerView = binding.rvBatchItems
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanQty = binding.tvTotalScannQty
                        tvTotalScanGW = binding.tvTotalScanGw
                        Log.e("Manual Scan","");
                        /*  width = this.Factor1
                          U_gsmso = this.U_GSMSO  */
                        remainingOpenQuantity = this.RemainingQuantity

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }

                        else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            Log.e("Manual Scan","+"+lineArrayList.get(adapterPosition).ItemCode);
                            pos = setScanDataOnItem(lineArrayList, lineArrayList.get(adapterPosition).ItemCode)
                            // pos = adapterPosition
                            (context as DeliveryOneLineActivity).startActivityForResult(intent, REQUEST_CODE)

                        }

                    }

                    //todo for manual batch entry..
                    /*  binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                          //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
                          if (actionId == KeyEvent.ACTION_UP && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                              var text = binding.edBatchCodeScan.text.toString().trim()
                              *//*  if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                  //todo scan call api here...
                                  scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, RemainingOpenQuantity,
                                      this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)

                              }*//*
                            binding.edBatchCodeScan.setText("")
                            true

                        } else {
                            false
                        }
                    }*///todo commnet


                }

                else{
                    showPopupNotChooseScanner()
                }


                //TODO save order lines listener by interface...
                save.setOnClickListener {

                    for (i in 0 until lineArrayList.size) {
                        if (lineArrayList[i].isScanned != 0){
                            AppConstants.IS_SCAN = true
                        }
                    }
                    Log.e(ContentValues.TAG, "isItemScan ==> : "+ AppConstants.IS_SCAN )
                    if (AppConstants.IS_SCAN == false ){
                        save.isEnabled = false
                        save.isCheckable = false
                        GlobalMethods.showError(context, "Items Not Scan.")
                    }else{

                        save.isEnabled = false
                        save.isCheckable = false
                        callback.onApiResponseStock(lineArrayList)
                    }

                }



            }
        }
    }


    override fun getItemCount(): Int {
        return lineArrayList.size
    }

    //TODO view holder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)




    //todo CALLING ITEMS CODE API HERE TO GET NAV CODE AND SHOW ON SCREEN--
    private fun scanItemCodeGetNavCode(text: String, tvNavicode: TextView)
            {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(context)
            networkClient.doGetScan("U_NAVI_CODE,ItemCode", "ItemCode eq '" + text + "'").apply {
                enqueue(object : Callback<NavScanResModel> {
                    override fun onResponse(
                        call: Call<NavScanResModel>,
                        response: Response<NavScanResModel>
                    ) {
                        try {

                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                Log.e("response---------", response.body().toString())

                                var responseModel = response.body()!!
                                if (responseModel.value.size > 0) {

                                    Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                    Log.e("U_NAVI_CODE==>", "" + responseModel.value[0].U_NAVI_CODE)

                                    tvNavicode.text = " : "+ responseModel.value[0].U_NAVI_CODE

                                    try {
                                        itemPo = setScanDataOnItem(lineArrayList, responseModel.value[0].ItemCode)

                                        lineArrayList[itemPo].NavisionCode = responseModel.value[0].U_NAVI_CODE
                                    }catch (e:Exception){
                                        e.printStackTrace()
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
                                        GlobalMethods.showError(context, mError.error.message.value)
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(context, mError.error.message.value)
                                        Log.e("json_error------", mError.error.message.value)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(context,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(context, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setScanDataOnItem(arrayList: ArrayList<InvoiceListModel.DocumentLine>, itemCode: String): Int
            {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is InvoiceListModel.DocumentLine && item.ItemCode == itemCode) {
                position = index
                break
            }
        }
        return position

    }

    private var itemPo: Int = -1

    private fun scanNavCodeItem(text: String)
             {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(context)
            networkClient.doGetScan("ItemCode,U_PCS_QTY,U_PACK_QTY", "U_NAVI_CODE eq '" + text + "'").apply {
                enqueue(object : Callback<NavScanResModel> {
                    override fun onResponse(
                        call: Call<NavScanResModel>,
                        response: Response<NavScanResModel>
                    ) {
                        try {
                            save.isEnabled = true
                            save.isCheckable = true
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                Log.e("response---------", response.body().toString())

                                var responseModel = response.body()!!

                                if (responseModel.value.size > 0) {
                                    Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                    itemPo = setScanDataOnItem(lineArrayList, responseModel.value[0].ItemCode)
                                    Log.e("ItemPo==>", "" + itemPo)
                                }



                                if (itemPo == -1) {
                                    GlobalMethods.showError(context, "Item Code not matched")
                                }
                                else if (lineArrayList[itemPo].totalPktQty.toDouble() >= lineArrayList[itemPo].RemainingQuantity.toDouble()) {
                                    GlobalMethods.showError(context, "Scanning completed for this Item")
                                }
                                else {
                                    if (lineArrayList.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        scanCount = lineArrayList[itemPo].isScanned
                                        ++scanCount

                                        var modelResponse = responseModel.value[0]

                                        if (lineArrayList[itemPo].totalPktQty.toDouble() > lineArrayList[itemPo].RemainingQuantity.toDouble()) {
                                            GlobalMethods.showError(context, "Scanning completed for this Item")
                                        } else {

                                            //todo comment
//                                            IS_NAV_FIRST_TIME = true //todo make it true after sacn any item --

                                            lineArrayList[itemPo].isScanned = scanCount
                                            lineArrayList[itemPo].totalPktQty = responseModel.value[0].U_PACK_QTY.toInt() * scanCount
                                            lineArrayList[itemPo].NavisionCode = text //todo comment by me---
                                            GlobalMethods.showSuccess(context, "Box added")
                                            notifyDataSetChanged()
                                        }

                                        Log.e("linearList===>", "onResponse: " + lineArrayList.toString() )

                                    } else {
                                        GlobalMethods.showError(context, "Invalid Batch Code")
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
                                            context,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            context,
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
                                    context,
                                    "Something Went wrong.Please try again."
                                )
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<NavScanResModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        if (t.message == "VPN_Exception") {
                            GlobalMethods.showError(context,"VPN is not connected. Please connect VPN and try again."
                            )
                        }else{
                            GlobalMethods.showError(context, t.message ?: "")
                        }
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }


    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
         {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("batch_code")
            Log.e("ItemCode===>", itemCode)
            Log.e("Test==> BP-2", "" + result.toString())
            //todo spilt string and get string at 0 index...
            Log.e("Test==> BP", "" + result.toString().split("/")[0])

            var text = result.toString()

            if (text.contains("/"))
                text = text.split("/")[0]

            Log.e("text API NOT Calling", text)

            scanNavCodeItem(text!!) //todo comment by me---

        }
    }


    //TODO POPUP DIALOG...

    //todo show popup when not selected scanner type button click popup.
    private fun showPopupNotChooseScanner() {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(context).inflate(R.layout.custom_popup_alert, null)
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
            var intent = Intent(context, HomeActivity::class.java)
            context.startActivity(intent)
            builder.dismiss()
            notifyDataSetChanged()
        }

        builder.setCancelable(true)
        builder.show()
    }




}
