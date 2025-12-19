package com.soothe.sapApplication.ui.invoiceOrder.UI

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityInvoiceOrderBinding
import com.soothe.sapApplication.ui.invoiceOrder.adapter.InvoiceListAdapter
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
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

class InvoiceOrderActivity : AppCompatActivity(){

    lateinit var binding: ActivityInvoiceOrderBinding
    private var invoiceOderList_gl: ArrayList<InvoiceListModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var invoiceListAdapter: InvoiceListAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@InvoiceOrderActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@InvoiceOrderActivity)

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)

        loadInvoiceRequestItems()

       /* //todo recycler view scrollListener for add more items in list...
        binding.rvProductionList.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                var lastCompletelyVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                if (isScrollingpage && lastCompletelyVisibleItemPosition == productionListModel_gl.size - 2 && apicall) {
                    page++
                    Log.e("page--->", page.toString())
                    loadInvoiceOrderListItems(totalskipCount(lastCompletelyVisibleItemPosition))
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
        })*/


        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

    }


    // todo Open invoice list--

    fun loadInvoiceRequestItems() {
        checkNetwoorkConnection.observe(this)
        { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                networkClient.doGetInvoiceOrderList().apply {
                    enqueue(object : Callback<InvoiceListModel> {
                        override fun onResponse(call: Call<InvoiceListModel>, response: Response<InvoiceListModel>) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var valueList = productionListModel1.value
                                    Toast.makeText(this@InvoiceOrderActivity, "Successfully!", Toast.LENGTH_SHORT)

                                    if (!valueList.isNullOrEmpty() && valueList.size > 0) {
                                        Log.e("page---->", page.toString())

                                        invoiceOderList_gl.addAll(valueList)

                                        setInvoiceOrderAdapter()

                                        invoiceListAdapter?.notifyDataSetChanged()
/*

                                        if (productionListModel1.value.size < 100)
                                            apicall = false
*/

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
                                            GlobalMethods.showError(this@InvoiceOrderActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@InvoiceOrderActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@InvoiceOrderActivity, LoginActivity::class.java)
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

                        override fun onFailure(call: Call<InvoiceListModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            materialProgressDialog.dismiss()
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@InvoiceOrderActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@InvoiceOrderActivity, t.message ?: "")
                            }
                        }
                    })
                }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }


    //todo calling api adapter here---
    private fun setInvoiceOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        binding.rvProductionList.layoutManager = layoutManager
        invoiceListAdapter = InvoiceListAdapter(invoiceOderList_gl)
        binding.rvProductionList.adapter = invoiceListAdapter

        invoiceListAdapter!!.OnItemClickListener { values, pos ->
            var invoiceOrderDocList = values[pos].DocumentLines
            var invoiceOrderValueList = values[pos]
            CoroutineScope(Dispatchers.IO).launch {
//                val bundle = Bundle().apply { putSerializable("invoiceData", invoiceOrderValueList) }
                var intent: Intent = Intent(this@InvoiceOrderActivity, InvoiceOrderLineActivity::class.java)
                intent.putExtra("invoiceData", invoiceOrderDocList as Serializable)
                intent.putExtra("invoiceDataValue", invoiceOrderValueList as Serializable)
                intent.putExtra("pos", pos)
                startActivity(intent)

            }

        }


    }


    override fun onResume() {
        super.onResume()
        if(!invoiceOderList_gl.isEmpty())
            invoiceOderList_gl.clear()
    }


}