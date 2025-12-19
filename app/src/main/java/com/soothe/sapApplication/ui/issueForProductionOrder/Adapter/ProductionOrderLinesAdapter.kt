package com.soothe.sapApplication.ui.issueForProductionOrder.Adapter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.soothe.sapApplication.ui.home.HomeActivity
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ProductionListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.productionOrderLines.ProductionOrderLinesActivity
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.sql.*


class ProductionOrderLinesAdapter(private val context: Context, var list: ArrayList<ProductionListModel.ProductionOrderLine>, private val networkConnection: NetworkConnection,
    private val materialProgressDialog: MaterialProgressDialog, private val callback: AdapterCallback, private val save: Chip, var width: Double?, var length: Double?, var gsm: Double?) : RecyclerView.Adapter<ProductionOrderLinesAdapter.ViewHolder>(), BatchItemsAdapter.OnDeleteItemClickListener {

    //todo declaration..
    private var connection: Connection? = null
    val REQUEST_CODE = 100
    private lateinit var sessionManagement: SessionManagement
    var hashMap: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var quantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var batchItemsAdapter: BatchItemsAdapter? = null
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView
    lateinit var tvOpenQty : TextView

    init {
        setSqlServer()
        sessionManagement = SessionManagement(context)
    }

    //todo interfaces...
    interface AdapterCallback {
        fun onApiResponse(
            response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
            quantityResponse: HashMap<String, ArrayList<String>>
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {
                binding.tvItemName.text = ":   "+this.ItemNo
                var qty = this.PlannedQuantity - this.IssuedQuantity
                binding.tvOpenQty.text = ":   "+qty.toString()
                binding.tvWidth.text = ":   "+ width.toString()
                binding.tvLength.text = ":   "+ length.toString()
                binding.tvGsm.text = ":   "+ gsm.toString()



                //todo add adapter size in hashmap at once.
                var count = 0
                for (i in 0 until list.size){
                    //TODO set count adapter position size store in list for batch scan...
                    var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                    if (hashMap.size != list.size) {
                        hashMap.put("Item" + count, itemList_gl)
                    }

                    //TODO set count adapter position size store in list for batch quantity...
                    var stringList: ArrayList<String> = ArrayList()
                    if (quantityHashMap.size != list.size) {
                        quantityHashMap.put("Item" + count, stringList)
                    }
                    count ++
                }
                Log.e("count ===> ", count.toString())


                Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())

                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER") { //sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0
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


                    binding.edBatchCodeScan.setOnKeyListener { view1, keyCode, keyEvent ->
                        if (binding.edBatchCodeScan.isFocused() && binding.edBatchCodeScan.isCursorVisible() && binding.edBatchCodeScan.hasFocus()) {
                            var inputMethodManager = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(binding.edBatchCodeScan.getWindowToken(), 0)
                        }
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {

                            var text = binding.edBatchCodeScan.text.toString().trim()
                            var size = 0
                            //TODO validation for stop multiple scanning at one order line...
                            if (hashMap.get("Item" + position) is List<*>) {
                                val list = hashMap.get("Item" + position) as List<*>
                                if (list.size == size) {
                                    Log.e("size===>", list.size.toString())
                                        var str = text
                                        if(str.contains(",")) {
                                            str = str.split(",")[0]
                                            if (checkDuplicate(hashMap.get("Item" + position)!!, str)) {
                                                //todo scan call api here...
                                                scanOrderLinesItem(str, binding.rvBatchItems, adapterPosition, this.ItemNo, binding.tvOpenQty,  binding.tvTotalScannQty, binding.tvTotalScanGw)
                                            }
                                        }else{
                                            if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                                //todo scan call api here...
                                                scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemNo, binding.tvOpenQty,  binding.tvTotalScannQty, binding.tvTotalScanGw)
                                            }
                                        }
                                } else {
                                    GlobalMethods.showError(context, "Can not Scan multiple Batch")
                                    Log.e("SizeGreaterThanOne===>", "Error")
                                }
                            }
                            binding.edBatchCodeScan.setText("")
                            binding.edBatchCodeScan.requestFocus()
                        }
                        return@setOnKeyListener true
                    }

                }

                //todo is qr scanner type choose..
                else if (sessionManagement.getScannerType(context) == "QR_SCANNER" || sessionManagement.getScannerType(context) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.VISIBLE

                    //TODO click on barcode scanner for popup..
                    binding.ivScanBatchCode.setOnClickListener {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        recyclerView = binding.rvBatchItems
                        itemCode = this.ItemNo.toString()
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanQty = binding.tvTotalScannQty
                        tvTotalScanGW = binding.tvTotalScanGw
                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }
                        else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            pos = adapterPosition
                            (context as ProductionOrderLinesActivity).startActivityForResult(intent, REQUEST_CODE)
                        }

                    }

                    //TODO for manual batch entry..
                    binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                        //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
                        if (actionId == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                            var text = binding.edBatchCodeScan.text.toString().trim()
                            var size = 0
                            //TODO validation for stop multiple scanning at one order line...
                            if (hashMap.get("Item" + position) is List<*>) {
                                val list = hashMap.get("Item" + position) as List<*>
                                if (list.size == size) {
                                    Log.e("size===>", list.size.toString())
                                    if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                        //todo scan call api here...
                                        scanOrderLinesItem(text, binding.rvBatchItems, position, this.ItemNo, binding.tvOpenQty, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                    }
                                }
                                else{
                                    GlobalMethods.showError(context, "Can not Scan multiple Batch")
                                    Log.e("SizeGreaterThanOne===>", "Error")
                                }
                            }
                            binding.edBatchCodeScan.setText("")

                            true

                        } else {
                            false
                        }
                    }
                }


                //TODO save order lines listener by interface...
                save.setOnClickListener {
                    callback.onApiResponse(hashMap, quantityHashMap)
                }

            }
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    //TODO viewholder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + list[pos].Batch + " Batch Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->
                    Log.e("before_valueList===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    list.removeAt(pos)
                    quantityHashMap1.removeAt(pos)
                    batchItemsAdapter?.notifyDataSetChanged()

                    Log.e("after_valueList===>", list.size.toString())
                    Log.e("after_batch===>", quantityHashMap1.size.toString())
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })
            .show()
    }



    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.Batch.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }

    //TODO scan item lines api here....
    private fun scanOrderLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
            val networkClient = NetworkClients.create(context)
            networkClient.doGetBatchNumScanDetails("Batch eq '" + text + "'" + " and ItemCode eq '" + itemCode + "'")
                .apply {
                    enqueue(object : Callback<ScanedOrderBatchedItems> {
                        override fun onResponse(call: Call<ScanedOrderBatchedItems>, response: Response<ScanedOrderBatchedItems>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        var modelResponse = responseModel.value
                                        scanedBatchedItemsList_gl.addAll(modelResponse)

                                        var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                        itemList_gl.addAll(hashMap.get("Item" + position)!!)
                                        //itemList_gl= hashMap.get("Item"+position)!!
                                        var stringList: ArrayList<String> = ArrayList()
                                        stringList.addAll(quantityHashMap.get("Item" + position)!!)

                                        itemList_gl.add(responseModel.value[0])
                                        hashMap.put("Item" + position, itemList_gl)

                                        if (!itemList_gl.isNullOrEmpty()) {

                                            Log.e("list_size-----", itemList_gl.size.toString())

                                            //todo quantity..
                                            getQuanity(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty)

                                            var totalGrossWeight = GlobalMethods.changeDecimal(GlobalMethods.sumBatchGrossWeight(position, hashMap.get("Item" + position)!!).toString())
                                            tvTotalScanGw.text = ":   "+ totalGrossWeight

                                            if (quantityHashMap.get("Item" + position)!!.size > 0) {
                                                if (!quantityHashMap.get("Item" + position)!!.contains("0") ) {
                                                    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                    rvBatchItems.layoutManager = layoutManager
                                                    batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "IssueOrder")
                                                    //todo call setOnItemListener Interface Function...
                                                    batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
                                                    rvBatchItems.adapter = batchItemsAdapter
                                                }else{
                                                    batchItemsAdapter?.notifyDataSetChanged()
                                                    GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                                }
                                            }else{
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "No Quantity Found of this Production Order.")
                                            }

                                        }
                                    } else {
                                        GlobalMethods.showError(context, "Invalid Batch Code")
                                        Log.e("not_response---------", response.message())
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

                        override fun onFailure(call: Call<ScanedOrderBatchedItems>, t: Throwable) {
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

    //TODO get quantity for batch code...
    private fun getQuanity(batchCode: String, itemCode: String, position: Int, stringList: ArrayList<String>, tvOpenQty: TextView, tvTotalScannQty: TextView) {
        if (connection != null) {
            var statement: Statement? = null
            try {
                statement = connection!!.createStatement()
                val resultSet = statement.executeQuery("SELECT T0.[Quantity] FROM OBTQ T0  INNER JOIN OBTN T1 ON T0.[SysNumber] = T1.[SysNumber] and T0.ItemCode=T1.ItemCode WHERE T1.[DistNumber]   =  '$batchCode' AND  T1.[ItemCode] = '$itemCode' AND T0.[WhsCode] ='${sessionManagement.getWarehouseCode(context)}'")
                while (resultSet.next()) {
                    Log.e("ConStatus", "Success=>" + resultSet.getString(1))
                    //todo remove zero digits from quantity...
                    stringList.add(GlobalMethods.changeDecimal(resultSet.getString(1))!!)
                    Log.e("stringList", "Success=>" + stringList)
                    quantityHashMap.put("Item" + position, stringList)
                    //TODO sum of quantity of batches..
//                    tvOpenQty.text = "  : "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()
                    tvTotalScannQty.text = "  : "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()
                }

            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            Log.e("Result=>", "Connection is null")
        }
    }


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

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("batch_code")
            var size = 0
            //todo validation for stop multiple scanning at one order line...
            if (hashMap.get("Item" + pos) is List<*>) {
                val list = hashMap.get("Item" + pos) as List<*>
                if (list.size == size) {

                    //todo spilt string and get string at 0 index...
                    scanOrderLinesItem(result.toString().split(",")[0], recyclerView, pos,
                        itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW)
                } else {
                    GlobalMethods.showError(context, "Can not Scan multiple Batch")
                    Log.e("SizeGreaterThanOne===>", "Error")
                }
            }
        }
    }


    //TODO set sql server for query...
    private fun setSqlServer() {
        Log.e("ConStatus", "Call setSqlServer() in ProductionOrderLinesAdapter")
        try {
            val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(context)
            ActivityCompat.requestPermissions(context as Activity, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
            val policy = ThreadPolicy.Builder().permitAll().build()
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
        }catch (e: IOException){
            e.printStackTrace()
            Log.e("ConStatus", "IOException -> ${e.message}")
        }

    }

    //TODO connection ..
    private fun connection(connection: Connection) {

        if (connection != null) {
            var statement: Statement? = null
            try {
                statement = connection!!.createStatement()
                // ResultSet resultSet = statement.executeQuery("Select * from CompanyDBs");
                val resultSet: ResultSet =
                    statement.executeQuery("Select Name,CompanyName,Version from CompanyDBs")
                while (resultSet.next()) {
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            Log.e("Result=>", "Connection is null")
        }
    }

    //TODO POPUP DIALOG...
    //TODO override function of
    private fun scannerPopupDialog(text: String, rvBatchItems: RecyclerView, context: Context, position: Int, itemCode: String?, tvOpenQty: TextView) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(context).inflate(R.layout.scanner_custom_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui text ...
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
        val goBtn = view.findViewById<AppCompatButton>(R.id.goBtn)

        //todo get radio buttons selected id..
        var checkGender = ""

        radioGroup?.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radioButton = group.findViewById<RadioButton>(checkedId)
            checkGender = radioButton.text.toString()
            if (radioButton != null && checkedId != -1) {
                Toast.makeText(context, radioButton.text, Toast.LENGTH_SHORT).show()
            } else {
                return@OnCheckedChangeListener
            }
        })

        //todo go btn..
        goBtn?.setOnClickListener {
            if (checkGender.equals("L")) {
                sessionManagement.setLaser(1)
                //todo laser popup
                laserScannerPopupDialog(rvBatchItems, position, itemCode, tvOpenQty)
            } else if (checkGender.equals("S")) {
                sessionManagement.setQRScanner(1)
                val intent = Intent(context, QRScannerActivity::class.java)
                pos = position
                (context as ProductionOrderLinesActivity).startActivityForResult(intent, REQUEST_CODE)
            }
            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()

    }

    //TODO popup for laser scanner...
    @SuppressLint("ClickableViewAccessibility")
    fun laserScannerPopupDialog(rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty: TextView)
    {
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
        var okBtn = view.findViewById<AppCompatButton>(R.id.okBtn)

        cancelBtn?.setOnClickListener {
            builder.dismiss()
        }

        //todo for laser scanner..

        ed_batch_code_scan.setOnTouchListener(OnTouchListener { view1: View, motionEvent: MotionEvent? ->
            ed_batch_code_scan.setText("")
            ed_batch_code_scan.setCursorVisible(true)
            ed_batch_code_scan.setFocusableInTouchMode(true)
            ed_batch_code_scan.requestFocus()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view1.windowToken, 0)
            true
        })

        ed_batch_code_scan.setOnKeyListener { view1, keyCode, keyEvent ->
            if (ed_batch_code_scan.isFocused() && ed_batch_code_scan.isCursorVisible() && ed_batch_code_scan.hasFocus()) {
                var inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(ed_batch_code_scan.getWindowToken(), 0)
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                var text = ed_batch_code_scan.text.toString().trim()
                var size = 0
                //todo validation for stop multiple scanning at one order line...
                if (hashMap.get("Item" + position) is List<*>) {
                    val list = hashMap.get("Item" + position) as List<*>
                    if (list.size == size) {
                        Log.e("size===>", list.size.toString())
                        //todo scan call api here...
                        scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW)
                    } else {
                        GlobalMethods.showError(context, "Can not Scan multiple Batch")
                        Log.e("SizeGreaterThanOne===>", "Error")
                    }
                }
                ed_batch_code_scan.setText("")
                ed_batch_code_scan.requestFocus()
            }
            return@setOnKeyListener false
        }


        //todo for manual batch entry..
        ed_batch_code_scan?.setOnEditorActionListener { v, actionId, event ->
            //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
            if (actionId == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                var text = ed_batch_code_scan.text.toString().trim()
                var size = 0
                //todo validation for stop multiple scanning at one order line...
                if (hashMap.get("Item" + position) is List<*>) {
                    val list = hashMap.get("Item" + position) as List<*>
                    if (list.size == size) {
                        Log.e("size===>", list.size.toString())
                        //todo scan call api here...
                        scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW)
                    } else {
                        GlobalMethods.showError(context, "Can not Scan multiple Batch")
                        Log.e("SizeGreaterThanOne===>", "Error")
                    }
                }

                builder.dismiss()
                /*   okBtn?.setOnClickListener {
                       var text = ed_batch_code_scan.text.toString().trim()
                       //todo scan call api here...
                       scanOrderLinesItem(text, rvBatchItems, position)
                       builder.dismiss()
                   }*/
                true

            } else {
                false
            }
        }

        //todo scanned batch lines...
        okBtn?.setOnClickListener {
            var text = ed_batch_code_scan?.text.toString().trim()
            var size = 0
            //todo validation for stop multiple scanning at one order line...
            if (hashMap.get("Item" + position) is List<*>) {
                val list = hashMap.get("Item" + position) as List<*>
                if (list.size == size) {
                    Log.e("size===>", list.size.toString())
                    //todo scan call api here...
                    scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW)
                } else {
                    GlobalMethods.showError(context, "Can not Scan multiple Batch")
                    Log.e("SizeGreaterThanOne===>", "Error")
                }
            }

            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()
    }


}
