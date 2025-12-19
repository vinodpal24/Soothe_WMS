package com.soothe.sapApplication.ui.invoiceOrder.adapter

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
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
import com.soothe.sapApplication.ui.home.HomeActivity
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.invoiceOrder.UI.InvoiceOrderLineActivity
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.NavScanResModel
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.ArrayList

class InvoiceLineListAdapter(private val context: Context, var lineArrayList: ArrayList<InvoiceListModel.DocumentLine>, private val scanButton: AppCompatImageView,
                             val laserCode: EditText, private val callback: InvoiceOrderLineActivity, private val save: Chip): RecyclerView.Adapter<InvoiceLineListAdapter.ViewHolder>() {

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


    init {
        sessionManagement = SessionManagement(context)
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(context)
    }

    //todo interfaces...
    interface AdapterCallback {

        fun onApiResponseStock(quantityResponse: ArrayList<InvoiceListModel.DocumentLine>)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvoiceLineListAdapter.ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(lineArrayList[position]) {
                binding.tvItemName.text = ":   " + this.ItemDescription
                binding.tvOpenQty.text = ":   " + this.ItemCode

                binding.issueAndDocumentlayout.visibility = View.VISIBLE
                binding.fromWarehouseRow.visibility = View.GONE

                var qtyBox = 0;
                if (GlobalMethods.numberToK(this.Quantity.toString())!!.toInt() > 0)
                    qtyBox = GlobalMethods.numberToK(this.RemainingQuantity.toString())!!.toInt() / GlobalMethods.numberToK(this.U_IQTY)!!.toInt()

                binding.tvWidth.text = ":   $qtyBox"
                binding.tvTotalScannQty.text = ":   " + this.RemainingQuantity
                initialBoxes = qtyBox
//                binding.fromWarehouse.text = ":   " + this.FromWarehouseCode
//                binding.toWarehouse.text = ":   " + this.WarehouseCode
                binding.tvTotalScanGw.text = ":   " +this.totalPktQty

                binding.tvLength.text = ":   " + this.isScanned

                binding.tvNavicode.text = this.NavisionCode?.let { date -> " : " + date } ?: " :"

                sessionManagement.setWarehouseCode(context, this.WarehouseCode)

                /*laserCode.requestFocus()

                //todo Show the soft keyboard when the activity starts
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(laserCode, InputMethodManager.HIDE_IMPLICIT_ONLY)

                laserCode.setOnKeyListener { view1, keyCode, keyEvent ->
                    if (laserCode.isFocused() && laserCode.isCursorVisible() && laserCode.hasFocus()) {
                        try {
                            var inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(laserCode.getWindowToken(), 0)
                        } catch (e: Exception) {
                            Log.e("Adapter", "onBindViewHolder: WINDOW LEAAAKED")
                        }
                    }
                    var text = laserCode.text.toString().trim()
                    Log.e("text=====", text)
                    // if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER)
                    if (!text.isEmpty()) {

                        try {
                            var text = laserCode.text.toString().trim()
                            if (text.contains("/"))
                                text = text.split("/")[0]
                            //var x = text
                            Log.e("text", text)


//                            scanNavCodeItem(text, position, "")//todo comment by me
                            laserCode.setText("")
                            laserCode.requestFocus()

                        } catch (e: Exception) {
                            Log.e("Adapter", "onBindViewHolder: WINDOW ")
                            e.message
                        }
                    }
                    return@setOnKeyListener true
                }


                scanButton.setOnClickListener {

                     Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())

                    if (sessionManagement.getScannerType(context) == "LEASER") {
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
                                    var x = text
                                    Log.e("text", text)
                                    if (x.contains("/")) {
                                        //todo validation for scan QR code from laser device and get batch code.
                                        x = x.split("/")[0]
                                        //todo scan call api here...
//                                        scanNavCodeItem(x.split("/")[0], position, "")//todo comment by me

                                    } else {
                                        //todo scan call api here...
//                                        scanNavCodeItem(text.split("/")[0], position, "")//todo comment by me

                                    }
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

                    } else {
                        val intent = Intent(context, QRScannerActivity::class.java)
                        pos = adapterPosition
                        scanCount = list[pos].isScanned
                        //totalPktQty = list[pos].totalPktQty
                        (context as InvoiceOrderLineActivity).startActivityForResult(intent, REQUEST_CODE)
                    }
                }*/


                Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())

                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER" || sessionManagement.getScannerType(context) == null) {
                    binding.ivScanBatchCode.visibility = View.GONE

                    binding.edBatchCodeScan.setOnTouchListener(android.view.View.OnTouchListener { view1: View, motionEvent: MotionEvent? ->
                        binding.edBatchCodeScan.setText("")
                        binding.edBatchCodeScan.setCursorVisible(true)
                        binding.edBatchCodeScan.setFocusableInTouchMode(true)
                        binding.edBatchCodeScan.requestFocus()
                        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view1.windowToken, 0)
                        true
                    })



                    laserCode.requestFocus()//todo commment by me

                    // Show the soft keyboard when the activity starts
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(laserCode, InputMethodManager.HIDE_IMPLICIT_ONLY)

                    laserCode.setOnKeyListener { view1, keyCode, keyEvent ->

                        // if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER)
                        if (!laserCode.text.toString().trim().isEmpty()) {

                            try {
                                var text = laserCode.text.toString().trim()
                                // var x = text

                                if (text.contains("/"))
                                    text = text.split("/")[0]

                                Log.e("text", text)
                                // itemPo = setScanDataOnItem(documentLineList_gl,text)

                                scanNavCodeItem(text)//todo comment by me
                                laserCode.setText("")
                                laserCode.requestFocus()

                            } catch (e: Exception) {
                                Log.e("Adapter", "onBindViewHolder: WINDOW ")
                                e.message
                            }
                        }
                        return@setOnKeyListener true
                    }


                    scanButton.setOnClickListener {

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }

                        else if (sessionManagement.getScannerType(context) == "LEASER") {

                            val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
                            val view = LayoutInflater.from(context).inflate(R.layout.laser_scanner_dialog_layout, null)
                            builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            builder.window?.setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            builder.window?.setGravity(Gravity.CENTER)
                            builder.setView(view)

                            var ed_batch_code_scan =
                                view.findViewById<EditText>(R.id.ed_batch_code_scan)
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

                        } else {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            pos = adapterPosition
                            Log.e("Leaser Scan"," -> "+ adapterPosition);
                            scanCount = lineArrayList[pos].isScanned
                            //totalPktQty = list[pos].totalPktQty
                            (context as InvoiceOrderLineActivity).startActivityForResult(intent, REQUEST_CODE)
                        }
                    }


                    binding.edBatchCodeScan.setOnKeyListener { view1, keyCode, keyEvent ->
                        if (binding.edBatchCodeScan.isFocused() && binding.edBatchCodeScan.isCursorVisible() && binding.edBatchCodeScan.hasFocus()) {
                            try {
                                var inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                inputMethodManager.hideSoftInputFromWindow(binding.edBatchCodeScan.getWindowToken(), 0)
                            } catch (e: Exception) {
                                Log.e("TAG===>", "onBindViewHolder: WINDOW LEAAAKED")
                            }
                        }
                        if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {

                            try {
                                var text = binding.edBatchCodeScan.text.toString().trim()
                                var x = text
                                if (x.contains("/")) { //todo validation for scan QR code from laser device and get batch code.
                                    x = x.split("/")[0]
                                    //todo scan call api here...
                                    scanNavCodeItem(x.split("/")[0]) //todo comment by me--
                                    /*if (checkDuplicate(hashMap.get("Item" + position)!!, x)) {
                                        scanOrderLinesItem(x, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, this.RemainingOpenQuantity, this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                    }*/
                                } else {
                                    //todo scan call api here...
                                    scanNavCodeItem(text.split("/")[0]) //todo comment by me--
                                    /* if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                         scanOrderLinesItem(text, binding.rvBatchItems,adapterPosition, this.ItemCode, binding.tvOpenQty, this.RemainingOpenQuantity, this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                     }*/
                                }
                                binding.edBatchCodeScan.setText("")
                                binding.edBatchCodeScan.requestFocus()
                            } catch (e: Exception) {
                                Log.e("TAG==>", "onBindViewHolder: WINDOW ")
                            }
                        }
                        return@setOnKeyListener true
                    }

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
                            (context as InvoiceOrderLineActivity).startActivityForResult(intent, REQUEST_CODE)

                        }

                    }

                    //todo for manual batch entry..
                    binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                        //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
                        if (actionId == KeyEvent.ACTION_UP && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                            var text = binding.edBatchCodeScan.text.toString().trim()
                          /*  if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                //todo scan call api here...
                                scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, RemainingOpenQuantity,
                                    this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)

                            }*/
                            binding.edBatchCodeScan.setText("")
                            true

                        } else {
                            false
                        }
                    }
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
                    Log.e(TAG, "isItemScan ==> : "+ AppConstants.IS_SCAN )
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


    private fun setScanDataOnItem(arrayList: ArrayList<InvoiceListModel.DocumentLine>, itemCode: String): Int {

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

    private fun scanNavCodeItem(text: String) {
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


    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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