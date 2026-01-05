package com.soothe.sapApplication.ui.saleOrderDelivery.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivitySaleOrderListBinding
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.saleOrderDelivery.adapter.SaleOrdersAdapter
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleOrdersModel
import com.webapp.internetconnection.CheckNetwoorkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class SaleOrderListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySaleOrderListBinding
    private var salesOrderListModel: ArrayList<SaleOrdersModel.Value> = ArrayList()

    lateinit var materialProgressDialog: MaterialProgressDialog
    private var salesOrderAdapter: SaleOrdersAdapter? = null
    lateinit var checkNetworkConnection: CheckNetwoorkConnection
    private lateinit var sessionManagement: SessionManagement
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleOrderListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()

    }

    private fun initViews() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "Sales Order"
        materialProgressDialog = MaterialProgressDialog(this@SaleOrderListActivity)
        checkNetworkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@SaleOrderListActivity)
        binding.apply {
            callSaleOrdersListApi()
        }
    }

    private fun clickListeners() {

    }

    fun callSaleOrdersListApi() {
        checkNetworkConnection.observe(this)
        { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                val apiClient = networkClient.salesOrderList(savedBPLID)
                apiClient.enqueue(
                    object : Callback<SaleOrdersModel> {
                        override fun onResponse(
                            call: Call<SaleOrdersModel>,
                            response: Response<SaleOrdersModel>,
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@SaleOrderListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        binding.tvNoDataFound.visibility = View.GONE
                                        binding.rvSaleOrders.visibility = View.VISIBLE
                                        salesOrderListModel.addAll(productionList_gl)

                                        setSalesOrderListAdapter(salesOrderListModel)

                                        salesOrderAdapter?.notifyDataSetChanged()

                                    }else {
                                        binding.tvNoDataFound.visibility = View.VISIBLE
                                        binding.rvSaleOrders.visibility = View.GONE
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
                                                this@SaleOrderListActivity,
                                                mError.error.message.value,
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@SaleOrderListActivity,
                                                mError.error.message.value,
                                            )
                                            val mainIntent = Intent(
                                                this@SaleOrderListActivity,
                                                LoginActivity::class.java,
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

                        override fun onFailure(call: Call<SaleOrdersModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@SaleOrderListActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@SaleOrderListActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }
                    },
                )


            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }

    private fun setSalesOrderListAdapter(salesOrderListModel: ArrayList<SaleOrdersModel.Value>) {
        binding.rvSaleOrders.apply {
            layoutManager = LinearLayoutManager(this@SaleOrderListActivity, LinearLayoutManager.VERTICAL, false)
            salesOrderAdapter = SaleOrdersAdapter { selectedItem, pos ->
                if (selectedItem.DocumentLines != null) {
                    var productionLinesList = selectedItem.DocumentLines
                    var itemObject = selectedItem

                    CoroutineScope(Dispatchers.IO).launch {

                        startActivity(Intent(this@SaleOrderListActivity, SalesOrderDocumentLinesActivity::class.java).apply {
                            putExtra("saleReqModel", itemObject as Serializable)
                            putExtra("documentLinesList", productionLinesList as Serializable)
                            putExtra("pos", pos)
                        })

                    }
                } else {
                    Toast.makeText(this@SaleOrderListActivity, "NO StockLines Found", Toast.LENGTH_SHORT).show()
                }

            }

            adapter = salesOrderAdapter

            salesOrderAdapter?.submitList(salesOrderListModel)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@SaleOrderListActivity).supportActionBar!!.themedContext)

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

    // Search handler (single responsibility)
    private fun handleSearch(query: String) {
        salesOrderAdapter?.submitFilteredList(
            salesOrderListModel.filterByQuery(query)
        )
    }

    // Extension function for filtering
    private fun List<SaleOrdersModel.Value>.filterByQuery(
        query: String
    ): ArrayList<SaleOrdersModel.Value> {

        if (query.isBlank()) return ArrayList(this)

        val lowerQuery = query.trim().lowercase()

        return ArrayList(
            filter { item ->
                item.DocNum.contains(lowerQuery, ignoreCase = true) ||
                        item.CardName.contains(lowerQuery, ignoreCase = true)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (salesOrderListModel.isNotEmpty())
            salesOrderListModel.clear()
    }
}