package com.soothe.sapApplication.ui.deliveryOneOrder.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityDeliveryOneBinding
import com.soothe.sapApplication.ui.deliveryOneOrder.adapter.DeliveryOneAdapter
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

class DeliveryOneActivity : AppCompatActivity() {
    lateinit var binding : ActivityDeliveryOneBinding
    private var invoiceOderList_gl: ArrayList<InvoiceListModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var invoiceListAdapter: DeliveryOneAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDeliveryOneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@DeliveryOneActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@DeliveryOneActivity)

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)

        loadInvoiceRequestItems()


        binding.ibSearch.setOnClickListener {
            if (binding.edtSearch.text.toString().trim().isNotEmpty()){
                handleSearch(binding.edtSearch.text.toString().trim())
            }
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {


                handleSearch(p0.toString())
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })


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
                networkClient.doGetDeliveryOneList().apply {
                    enqueue(object : Callback<InvoiceListModel> {
                        override fun onResponse(call: Call<InvoiceListModel>, response: Response<InvoiceListModel>) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var valueList = productionListModel1.value
                                    Toast.makeText(this@DeliveryOneActivity, "Successfully!", Toast.LENGTH_SHORT)

                                    if (!valueList.isNullOrEmpty() && valueList.size > 0) {
                                        Log.e("page---->", page.toString())

                                        invoiceOderList_gl.addAll(valueList)

                                        setInvoiceOrderAdapter()

                                        invoiceListAdapter?.notifyDataSetChanged()

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
                                            GlobalMethods.showError(this@DeliveryOneActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@DeliveryOneActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@DeliveryOneActivity, LoginActivity::class.java)
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
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@DeliveryOneActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@DeliveryOneActivity, t.message ?: "")
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


    //todo calling api adapter here---
    private fun setInvoiceOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        binding.rvDeliveryList.layoutManager = layoutManager
        invoiceListAdapter = DeliveryOneAdapter(invoiceOderList_gl)
        binding.rvDeliveryList.adapter = invoiceListAdapter

        invoiceListAdapter!!.OnItemClickListener { values, pos ->
            var invoiceOrderDocList = values[pos].DocumentLines
            var invoiceOrderValueList = values[pos]
            binding.edtSearch.clearFocus()

            CoroutineScope(Dispatchers.Main).launch {
               // var intent: Intent = Intent(this@DeliveryOneActivity, DemoActivity::class.java)
                var intent: Intent = Intent(this@DeliveryOneActivity, DeliveryOneLineActivity::class.java)
                intent.putExtra("invoiceData", invoiceOrderDocList as Serializable)
                intent.putExtra("invoiceDataValue", invoiceOrderValueList as Serializable)
                intent.putExtra("pos", pos)
                startActivity(intent)
                binding.edtSearch.setText("")


            }

        }


    }


    override fun onResume() {
        super.onResume()
        if(!invoiceOderList_gl.isEmpty()){
            invoiceOderList_gl.clear()
        }

        //  handleSearch(binding.edtSearch.text.toString().trim())

    }






    //todo search filter..
    private fun handleSearch(query: String) {
        val filteredList = issueSearchList(query)
        invoiceListAdapter?.setFilteredItems(filteredList)
    }


    //todo this function filter issue for production list based on text...
    fun issueSearchList(query: String): ArrayList<InvoiceListModel.Value> {
        val filteredList = ArrayList<InvoiceListModel.Value>()
        for (item in invoiceOderList_gl) {
            if (item.DocNum.contains(query, ignoreCase = true) || item.CardName.contains(query, ignoreCase = true)) {
                filteredList.add(item)
            }
        }

        return filteredList
    }


}