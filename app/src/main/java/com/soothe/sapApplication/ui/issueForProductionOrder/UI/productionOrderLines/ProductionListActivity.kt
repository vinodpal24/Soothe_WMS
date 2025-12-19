package com.soothe.sapApplication.ui.issueForProductionOrder.UI.productionOrderLines


import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityListProductionBinding
import com.soothe.sapApplication.ui.deliveryOrderModule.Adapter.DeliveryListAdapter
import com.soothe.sapApplication.ui.deliveryOrderModule.Model.DeliveryModel
import com.soothe.sapApplication.ui.deliveryOrderModule.UI.DeliveryDocumentLineActivity
import com.soothe.sapApplication.ui.issueForProductionOrder.Adapter.IssueOderAdapter
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ProductionListModel
import com.soothe.sapApplication.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.AppConstants.isOldDevelopment
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.ui.issueForProductionOrder.Adapter.InventoryRequestAdapter
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import com.webapp.internetconnection.CheckNetwoorkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.Serializable


class ProductionListActivity : AppCompatActivity(), DeliveryListAdapter.ItemClickListener {
    private lateinit var activityListBinding: ActivityListProductionBinding
    private var issueOderAdapter: IssueOderAdapter? = null
    private var requestAdapter: InventoryRequestAdapter? = null
    private var productionListModel_gl: ArrayList<ProductionListModel.Value> = ArrayList()
    private var requestListModel_gl: ArrayList<InventoryRequestModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var deliveryModelList_gl: ArrayList<DeliveryModel.Value> = ArrayList()
    private var deliveryAdapter: DeliveryListAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityListBinding = ActivityListProductionBinding.inflate(layoutInflater)
        setContentView(activityListBinding.root)

//        clearCache(this)
        deleteCache(this)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@ProductionListActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@ProductionListActivity)


        //todo get arguments from previous activity...
        val myIntent = intent
        flag = myIntent.getStringExtra("flag")!!

        if (flag.equals("Issue_Order")) {

            title = "Inventory Transfer Requests"

            //todo loading initial list items and calling adapter-----
            Log.e("loadMoreListItems==>", "Items_loading...")
            // loadIssueOrderListItems(0)

            setIssueOrderAdapter()
            loadInventoryRequestItems()

            //todo adapter on item click listener....
            requestAdapter?.OnItemClickListener { list, pos ->
                if (list.get(pos).StockTransferLines != null) {
                    var productionValueList = list.get(pos).StockTransferLines
                    var productionLinesList = list.get(pos).StockTransferLines
                    var itemObject = list.get(pos);

                    CoroutineScope(Dispatchers.IO).launch {
                        val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }
                        var intent: Intent = Intent(this@ProductionListActivity, InventoryTransferLinesActivity::class.java)
                        intent.putExtra("productionLinesList", productionLinesList as Serializable)
                        intent.putExtra("productionValueList", productionValueList as Serializable)
                        intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                        intent.putExtras(bundle)
                        startActivity(intent)

                        withContext(Dispatchers.Main) {


                        }
                    }
                } else {
                    Toast.makeText(this, "NO StockLines Found", Toast.LENGTH_SHORT).show()
                }


            }


            //todo recycler view scrollListener for add more items in list...
            activityListBinding.rvProductionList.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    var lastCompletelyVisibleItemPosition =
                        (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    Log.e(
                        "current1--->",
                        totalskipCount(lastCompletelyVisibleItemPosition).toString()
                    )
                    if (isScrollingpage && lastCompletelyVisibleItemPosition == productionListModel_gl.size - 2 && apicall) {
                        page++
                        Log.e("page--->", page.toString())
                        loadIssueOrderListItems(totalskipCount(lastCompletelyVisibleItemPosition))
                        isScrollingpage = false
                    } else {
                        recyclerView.setPadding(0, 0, 0, 0);
                    }

                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) { //it means we are scrolling
                        isScrollingpage = true

                    }
                }
            })
        } else if (flag.equals("Delivery_Order")) {
            title = "Delivery Order"
            loadDeliveryOrderListItems()

        }


    }


    fun clearCache(context: Activity) {
        try {
            // Get the cache directory for your application
            val cacheDir = context.cacheDir

            // Check if the cache directory exists
            if (cacheDir != null && cacheDir.isDirectory) {
                // Delete all files and subdirectories in the cache directory
                val children = cacheDir.list()
                for (child in children) {
                    val cacheFile = File(cacheDir, child)
                    cacheFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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


    override fun onRestart() {
        super.onRestart()
        Log.d("Restart====>", "Restart")
        productionListModel_gl.clear()
        deliveryModelList_gl.clear()

    }

    fun totalskipCount(curret: Int): Int {
        var total = limit * page
        return limit * page;
    }

    lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onResume() {
        super.onResume()
        if (!requestListModel_gl.isEmpty())
            requestListModel_gl.clear()
    }

    //todo set adapter...
    fun setIssueOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        activityListBinding.rvProductionList.layoutManager = layoutManager
        /*issueOderAdapter = IssueOderAdapter(productionListModel_gl)
        activityListBinding.rvProductionList.adapter = issueOderAdapter*/

        requestAdapter = InventoryRequestAdapter(requestListModel_gl)
        activityListBinding.rvProductionList.adapter = requestAdapter
    }

    // Open InventoryRequests

    fun loadInventoryRequestItems() {
        checkNetwoorkConnection.observe(this)
        { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()

                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                val currentApi = if (isOldDevelopment) networkClient.doGetRequestListCount(
                    "DocEntry,DocNum,Series,DocDate,TaxDate,DueDate,CardCode,CardName,Address,Reference1,Comments,JournalMemo,PriceList,FromWarehouse,ToWarehouse,FinancialPeriod,DocObjectCode,BPLID,ShipToCode,DutyStatus,U_DOCTYP,U_TRNTYP,StockTransfer_ApprovalRequests,ElectronicProtocols,StockTransferLines,StockTransferTaxExtension,DocumentReferences",
                    "DocumentStatus eq 'O'"

                ) else networkClient.inventoryTransferRequestList(
                    savedBPLID, "", sessionManagement.getCompanyDB(this@ProductionListActivity).toString()

                )
                currentApi.apply { // ,"" + skip
                    enqueue(object : Callback<InventoryRequestModel> {
                        override fun onResponse(
                            call: Call<InventoryRequestModel>,
                            response: Response<InventoryRequestModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@ProductionListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT
                                    )
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        Log.e("page---->", page.toString())

                                        requestListModel_gl.addAll(productionList_gl)
                                        if (requestListModel_gl.size == 0) {
                                            Toast.makeText(
                                                this@ProductionListActivity,
                                                "No New Transfer Requests Found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }


                                        requestAdapter?.notifyDataSetChanged()

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

                                    }

                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        Log.e("MSZ==>", mError.error.message.value)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                            val mainIntent = Intent(
                                                this@ProductionListActivity,
                                                LoginActivity::class.java
                                            )
                                            startActivity(mainIntent)
                                            finish()
                                        }
                                        /*if (mError.error.message.value != null) {
                                            AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }*/
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<InventoryRequestModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@ProductionListActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@ProductionListActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }
                    })
                }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }


    //todo ISSUE ORDER API LIST load next production item list....
    fun loadIssueOrderListItems(skip: Int) {
        checkNetwoorkConnection.observe(this)
        { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
                val networkClient = NetworkClients.create(this)
                networkClient.doGetProductionListCount(
                    "odata.maxpagesize=" + 100,
                    "U_Cal ne '" + "C" + "'",
                    "" + skip,
                    "AbsoluteEntry desc",
                    "ProductionOrderStatus eq '" + "boposReleased" + "'"
                ).apply { // ,"" + skip
                    enqueue(object : Callback<ProductionListModel> {
                        override fun onResponse(
                            call: Call<ProductionListModel>,
                            response: Response<ProductionListModel>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@ProductionListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT
                                    )
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        Log.e("page---->", page.toString())

                                        productionListModel_gl.addAll(productionList_gl)

                                        issueOderAdapter?.notifyDataSetChanged()
                                        if (productionListModel_gl.size == 0) {
                                            GlobalMethods.showSuccess(
                                                this@ProductionListActivity,
                                                "No Pending Request found."
                                            )
                                        }

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

                                    }

                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                            val mainIntent = Intent(
                                                this@ProductionListActivity,
                                                LoginActivity::class.java
                                            )
                                            startActivity(mainIntent)
                                            finish()
                                        }
                                        /*if (mError.error.message.value != null) {
                                            AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }*/
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ProductionListModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@ProductionListActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@ProductionListActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }
                    })
                }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }

    //todo DELIVERY ORDER API LIST ITEMS BIND....
    fun loadDeliveryOrderListItems() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
                val networkClient = NetworkClients.create(this)
                networkClient.deliveryOrder("DocumentStatus eq 'bost_Open'", "DocEntry desc")
                    .apply {
                        enqueue(object : Callback<DeliveryModel> {
                            override fun onResponse(
                                call: Call<DeliveryModel>,
                                response: Response<DeliveryModel>
                            ) {
                                try {
                                    if (response.isSuccessful) {
                                        Log.e("delivery_response===>", response.toString())
                                        materialProgressDialog.dismiss()
                                        var listResponse = response.body()!!
                                        deliveryModelList_gl = listResponse.value
                                        if (deliveryModelList_gl.size == 0) {
                                            GlobalMethods.showSuccess(
                                                this@ProductionListActivity,
                                                "No Pending Request found."
                                            )
                                        }


                                        setDeliveryOrderAdapter()
                                    } else {
                                        materialProgressDialog.dismiss()
                                        val gson1 = GsonBuilder().create()
                                        var mError: OtpErrorModel
                                        try {
                                            val s = response.errorBody()!!.string()
                                            mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                            if (mError.error.code == 400) {
                                                GlobalMethods.showError(
                                                    this@ProductionListActivity,
                                                    mError.error.message.value
                                                )
                                            }
                                            if (mError.error.code == 306 && mError.error.message.value != null) {
                                                GlobalMethods.showError(
                                                    this@ProductionListActivity,
                                                    mError.error.message.value
                                                )
                                                val mainIntent = Intent(
                                                    this@ProductionListActivity,
                                                    LoginActivity::class.java
                                                )
                                                startActivity(mainIntent)
                                                finish()
                                            }
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onFailure(call: Call<DeliveryModel>, t: Throwable) {
                                Log.e("delivery_failure-----", t.toString())
                                if (t.message == "VPN_Exception") {
                                    GlobalMethods.showError(this@ProductionListActivity,"VPN is not connected. Please connect VPN and try again."
                                    )
                                }else{
                                    GlobalMethods.showError(this@ProductionListActivity, t.message ?: "")
                                }
                                materialProgressDialog.dismiss()
                            }

                        })
                    }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }

    //todo bind delivery order adapter...
    fun setDeliveryOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        activityListBinding.rvProductionList.layoutManager = layoutManager
        deliveryAdapter =
            DeliveryListAdapter(
                deliveryModelList_gl,
                this@ProductionListActivity
            )
        activityListBinding.rvProductionList.adapter = deliveryAdapter
        deliveryAdapter?.notifyDataSetChanged()
    }


    //todo delivery document order line adapter on paricular item click listener...
    override fun onItemClick(valueList: List<DeliveryModel.Value>, pos: Int) {

        val bundle = Bundle().apply {
            putSerializable("deliveryItemModel", valueList[pos])
        }
        var deliveryValueList = valueList[pos]
        var documentLineList = valueList[pos].DocumentLines
        var intent: Intent = Intent(this, DeliveryDocumentLineActivity::class.java)
        intent.putExtra("documentLineList", documentLineList as Serializable)
        intent.putExtra("deliveryValueList", deliveryValueList as Serializable)
        intent.putExtra("pos", pos)
        intent.putExtras(bundle)
        startActivity(intent)


    }


    //todo set search icon on activity...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_icon -> {
                //todo Handle icon click
                return true
            }

            R.id.list_icon -> {
                //todo Handle icon click
                var intent = Intent(this@ProductionListActivity, com.soothe.sapApplication.ui.DemoActivity::class.java)
                startActivity(intent)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@ProductionListActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                handleSearch(newText)
                return true
            }
        })
        return true
    }

    //todo search filter..
    private fun handleSearch(query: String) {
        if (flag.equals("Issue_Order")) {
            val filteredList = issueSearchList(query)
            issueOderAdapter?.setFilteredItems(filteredList)
        } else if (flag.equals("Delivery_Order")) {
            val deliveryFilterList = deliverySearchList(query)
            deliveryAdapter?.setFilteredItems(deliveryFilterList)
        }
    }

    //todo this function filter issue for production list based on text...
    fun issueSearchList(query: String): ArrayList<ProductionListModel.Value> {
        val filteredList = ArrayList<ProductionListModel.Value>()
        for (item in productionListModel_gl) {
            if (item.ItemNo.contains(
                    query,
                    ignoreCase = true
                ) || item.DocumentNumber.contains(query, ignoreCase = true)
            ) {
                filteredList.add(item)
            }
        }

        return filteredList
    }

    //todo this function filter delivery order list based on text...
    fun deliverySearchList(query: String): ArrayList<DeliveryModel.Value> {
        val filteredList = ArrayList<DeliveryModel.Value>()
        for (item in deliveryModelList_gl) {
            if (item.DocNum.contains(query, ignoreCase = true) || item.CardCode.contains(
                    query,
                    ignoreCase = true
                )
            ) {
                filteredList.add(item)
            }
        }
        return filteredList
    }


    override fun onBackPressed() {
        super.onBackPressed()
        /* var intent = Intent(this@ProductionListActivity, HomeActivity::class.java)
         intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
         startActivity(intent)*/
        finish()
    }


}