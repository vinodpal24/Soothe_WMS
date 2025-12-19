package com.soothe.sapApplication.ui.creditMemo.activity

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityCreditMemoListBinding
import com.soothe.sapApplication.ui.creditMemo.adapter.ArInvoiceAdapter
import com.soothe.sapApplication.ui.creditMemo.model.ArInvoiceListModel
import com.soothe.sapApplication.ui.inventoryTransferRequest.activity.InventoryRequestLinesActivity
import com.soothe.sapApplication.ui.inventoryTransferRequest.adapter.InventoryRequestAdapter
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

class CreditMemoListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreditMemoListBinding

    private lateinit var checkNetworkConnection: CheckNetwoorkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement

    private var invoiceListAdapter: ArInvoiceAdapter? = null
    private var invoiceListModel: ArrayList<ArInvoiceListModel.Value> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditMemoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()

    }

    private fun initViews() {
        title = "Credit Memo"
        checkNetworkConnection = CheckNetwoorkConnection(application)
        materialProgressDialog = MaterialProgressDialog(this@CreditMemoListActivity)
        sessionManagement = SessionManagement(this@CreditMemoListActivity)
        setCreditMemoType()
        callInvoiceReturnListApi("AR Invoice")
    }

    private fun callInvoiceReturnListApi(isArInvoice: String) {
        when (isArInvoice) {
            "AR Invoice" -> {
                callInvoiceListApi()
            }
            "Sales Return Request" -> {
                callReturnRequestListApi()
            }
            else -> {
                callInvoiceListApi()
                callReturnRequestListApi()
            }
        }
    }

    private fun clickListeners() {

    }

    private fun setCreditMemoType() {

        val cmNames = arrayListOf("AR Invoice", "Sales Return Request")
        binding.acCreditMemoType.setText(cmNames[0], false)
        val adapter = ArrayAdapter(
            this@CreditMemoListActivity, R.layout.simple_spinner_dropdown_item, cmNames
        )

        binding.acCreditMemoType.setAdapter(adapter)
        binding.acCreditMemoType.hint = "Select Credit Memo Type"

        binding.acCreditMemoType.threshold = 0 // Show suggestions even without typing

        // Handle item selection
        binding.acCreditMemoType.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            callInvoiceReturnListApi(selectedItem.toString())
            binding.acCreditMemoType.setText(selectedItem, false) // false to prevent filtering again
        }
    }

    fun callInvoiceListApi() {
        checkNetworkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                val currentApi = networkClient.getInvoiceList(savedBPLID)
                currentApi.enqueue(
                    object : Callback<ArInvoiceListModel> {
                        override fun onResponse(
                            call: Call<ArInvoiceListModel>,
                            response: Response<ArInvoiceListModel>,
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@CreditMemoListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    invoiceListModel.clear()
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {

                                        invoiceListModel.addAll(productionList_gl)

                                        setInvoiceListAdapter(invoiceListModel)

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
                                            GlobalMethods.showError(
                                                this@CreditMemoListActivity,
                                                mError.error.message.value,
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@CreditMemoListActivity,
                                                mError.error.message.value,
                                            )
                                            val mainIntent = Intent(
                                                this@CreditMemoListActivity,
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
                            } catch (e: IOException) {
                                if (e.message == "VPN_Exception") {
                                    // notify UI
                                    GlobalMethods.showError(
                                        this@CreditMemoListActivity, "VPN is not connected. Please connect VPN and try again."
                                    )
                                }
                            }
                        }

                        override fun onFailure(call: Call<ArInvoiceListModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(
                                    this@CreditMemoListActivity, "VPN is not connected. Please connect VPN and try again."
                                )
                            } else {
                                GlobalMethods.showError(this@CreditMemoListActivity, t.message ?: "")
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

    fun callReturnRequestListApi() {
        checkNetworkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                val currentApi = networkClient.getReturnRequestList(savedBPLID)
                currentApi.enqueue(
                    object : Callback<ArInvoiceListModel> {
                        override fun onResponse(
                            call: Call<ArInvoiceListModel>,
                            response: Response<ArInvoiceListModel>,
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(
                                        this@CreditMemoListActivity,
                                        "Successfully!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    invoiceListModel.clear()
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {

                                        invoiceListModel.addAll(productionList_gl)

                                        setInvoiceListAdapter(invoiceListModel)

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
                                            GlobalMethods.showError(
                                                this@CreditMemoListActivity,
                                                mError.error.message.value,
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@CreditMemoListActivity,
                                                mError.error.message.value,
                                            )
                                            val mainIntent = Intent(
                                                this@CreditMemoListActivity,
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
                            } catch (e: IOException) {
                                if (e.message == "VPN_Exception") {
                                    // notify UI
                                    GlobalMethods.showError(
                                        this@CreditMemoListActivity, "VPN is not connected. Please connect VPN and try again."
                                    )
                                }
                            }
                        }

                        override fun onFailure(call: Call<ArInvoiceListModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(
                                    this@CreditMemoListActivity, "VPN is not connected. Please connect VPN and try again."
                                )
                            } else {
                                GlobalMethods.showError(this@CreditMemoListActivity, t.message ?: "")
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

    private fun setInvoiceListAdapter(invoiceListModel: java.util.ArrayList<ArInvoiceListModel.Value>) {
        binding.rvCreditMemo.apply {
            layoutManager = LinearLayoutManager(this@CreditMemoListActivity, LinearLayoutManager.VERTICAL, false)
            invoiceListAdapter = ArInvoiceAdapter { selectedItem, pos ->
                if (selectedItem.DocumentLines != null) {
                    var productionLinesList = selectedItem.DocumentLines
                    var itemObject = selectedItem

                    CoroutineScope(Dispatchers.IO).launch {

                        startActivity(Intent(this@CreditMemoListActivity, CreditMemoDocumentLinesActivity::class.java).apply {
                            putExtra("inventReqModel", itemObject as Serializable)
                            putExtra("productionLinesList", productionLinesList as Serializable)
                            putExtra("pos", pos)
                        })

                    }
                } else {
                    Toast.makeText(this@CreditMemoListActivity, "NO StockLines Found", Toast.LENGTH_SHORT).show()
                }

            }

            adapter = invoiceListAdapter

            invoiceListAdapter?.submitList(invoiceListModel)
        }
    }
}