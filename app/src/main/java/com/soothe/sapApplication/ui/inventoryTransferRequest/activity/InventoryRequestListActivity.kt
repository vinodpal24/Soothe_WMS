package com.soothe.sapApplication.ui.inventoryTransferRequest.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.AppConstants.isOldDevelopment
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityInventoryRequestListBinding
import com.soothe.sapApplication.ui.inventoryTransferRequest.adapter.InventoryRequestAdapter
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import com.soothe.sapApplication.ui.login.LoginActivity
import com.webapp.internetconnection.CheckNetwoorkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class InventoryRequestListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInventoryRequestListBinding
    private var requestAdapter: InventoryRequestAdapter? = null
    private var requestListModel_gl: ArrayList<InventoryRequestModel.Value> = ArrayList()

    private lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var checkNetworkConnection: CheckNetwoorkConnection
    private lateinit var sessionManagement: SessionManagement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryRequestListBinding.inflate(layoutInflater)
        initViews()
        clickListeners()
        setContentView(binding.root)

    }

    private fun initViews() {
        supportActionBar?.setDisplayShowHomeEnabled(false)
        //title = "Inventory Transfer Requests"
        supportActionBar?.title = "Inventory Transfer Requests"
        materialProgressDialog = MaterialProgressDialog(this@InventoryRequestListActivity)
        checkNetworkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@InventoryRequestListActivity)
        binding.apply {
            callInventoryRequestListApi()
        }
    }

    private fun clickListeners() {
        binding.apply {

        }
    }

    fun callInventoryRequestListApi() {
        checkNetworkConnection.observe(this)
        { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient =NetworkClients.create(this)
                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                val currentApi =
                    if (isOldDevelopment) networkClient.doGetRequestListCount(
                        "DocEntry,DocNum,Series,DocDate,TaxDate,DueDate,CardCode,CardName,Address,Reference1,Comments,JournalMemo,PriceList,FromWarehouse,ToWarehouse,FinancialPeriod,DocObjectCode,BPLID,ShipToCode,DutyStatus,U_DOCTYP,U_TRNTYP,StockTransfer_ApprovalRequests,ElectronicProtocols,StockTransferLines,StockTransferTaxExtension,DocumentReferences",
                        "DocumentStatus eq 'O'"

                    ) else networkClient.inventoryTransferRequestList(
                        savedBPLID, "", sessionManagement.getCompanyDB(this@InventoryRequestListActivity).toString()
                    )
                currentApi.enqueue(
                    object : Callback<InventoryRequestModel> {
                        override fun onResponse(
                            call: Call<InventoryRequestModel>,
                            response: Response<InventoryRequestModel>,
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@InventoryRequestListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {

                                        requestListModel_gl.addAll(productionList_gl)

                                        setRequestListAdapter(requestListModel_gl)

                                        requestAdapter?.notifyDataSetChanged()


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
                                                this@InventoryRequestListActivity,
                                                mError.error.message.value,
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryRequestListActivity,
                                                mError.error.message.value,
                                            )
                                            val mainIntent = Intent(
                                                this@InventoryRequestListActivity,
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
                            }catch (e: IOException) {
                                if (e.message == "VPN_Exception") {
                                    // notify UI
                                    GlobalMethods.showError(this@InventoryRequestListActivity,"VPN is not connected. Please connect VPN and try again."
                                    )
                                }
                            }
                        }

                        override fun onFailure(call: Call<InventoryRequestModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@InventoryRequestListActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@InventoryRequestListActivity, t.message ?: "")
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@InventoryRequestListActivity).supportActionBar!!.themedContext)

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
        requestAdapter?.submitFilteredList(
            requestListModel_gl.filterByQuery(query)
        )
    }

    // Extension function for filtering
    private fun List<InventoryRequestModel.Value>.filterByQuery(
        query: String
    ): ArrayList<InventoryRequestModel.Value> {

        if (query.isBlank()) return ArrayList(this)

        val lowerQuery = query.trim().lowercase()

        return ArrayList(
            filter { item ->
                item.DocNum.contains(lowerQuery, ignoreCase = true) ||
                        item.CardName.contains(lowerQuery, ignoreCase = true)
            }
        )
    }

    private fun setRequestListAdapter(requestlistmodelGl: java.util.ArrayList<InventoryRequestModel.Value>) {
        binding.rvItrRequest.apply {
            layoutManager = LinearLayoutManager(this@InventoryRequestListActivity, LinearLayoutManager.VERTICAL, false)
            requestAdapter = InventoryRequestAdapter { selectedItem, pos ->
                if (selectedItem.StockTransferLines != null) {
                    var productionLinesList = selectedItem.StockTransferLines
                    var itemObject = selectedItem

                    CoroutineScope(Dispatchers.IO).launch {

                        startActivity(Intent(this@InventoryRequestListActivity, InventoryRequestLinesActivity::class.java).apply {
                            putExtra("inventReqModel", itemObject as Serializable)
                            putExtra("productionLinesList", productionLinesList as Serializable)
                            putExtra("pos", pos)
                        })

                    }
                } else {
                    Toast.makeText(this@InventoryRequestListActivity, "NO StockLines Found", Toast.LENGTH_SHORT).show()
                }

            }

            adapter = requestAdapter

            requestAdapter?.submitList(requestlistmodelGl)
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestListModel_gl.isNotEmpty())
            requestListModel_gl.clear()
    }
}