package com.soothe.sapApplication.ui.home

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Adapter.HomeAdapter
import com.soothe.sapApplication.BuildConfig
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.Model.HomeItem
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityHomeDashboardBinding
import com.soothe.sapApplication.ui.creditMemo.activity.CreditMemoListActivity
import com.soothe.sapApplication.ui.deliveryOrder.activity.DeliveryListActivity
import com.soothe.sapApplication.ui.inventoryTransferRequest.activity.InventoryRequestListActivity
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.login.Model.LoginResponseModel
import com.soothe.sapApplication.ui.saleOrderDelivery.activity.SaleOrderListActivity
import com.soothe.sapApplication.ui.setting.SettingActivity
import com.webapp.internetconnection.CheckNetwoorkConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeDashboardBinding
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var sessionManagement: SessionManagement
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection

    var doubleBackToExitPressedOnce = false

    private var flag: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()
    }

    private fun initViews() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@HomeDashboardActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this)

        val items = listOf(
            HomeItem(
                R.drawable.issue_prod_icon,
                "Issue for Production",
                AppConstants.ISSUE_FOR_PRODUCTION,
                "N"
            ),
            HomeItem(R.drawable.ic_scan_view, "Scan & View", AppConstants.SCAN_AND_VIEW, "N"),
            HomeItem(
                R.drawable.ic_inventory_req,
                "Inventory Transfer Req.",
                AppConstants.INVENTORY_REQ,
                "Y"
            ),
            HomeItem(R.drawable.delivery_icon, "Goods Issue", AppConstants.GOODS_ISSUE, "N"),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Inventory Transfer (GRPO)",
                AppConstants.INVENTORY_TRANSFER_GRPO,
                "N"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Goods Receipt PO",
                AppConstants.GOODS_RECEIPT_PO,
                "N"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Sale To Invoice",
                AppConstants.SALE_TO_INVOICE,
                "N"
            ), // for demo only
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Receipt from Production",
                AppConstants.RECEIPT_FROM_PRODUCTION,
                "N"
            ),
            HomeItem(R.drawable.receipt_prod_icon, "Pick List", AppConstants.PICK_LIST, "N"),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Return Components",
                AppConstants.RETURN_COMPONENTS,
                "N"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Goods Receipt",
                AppConstants.GOODS_RECEIPT,
                "N"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Inventory Transfer",
                AppConstants.INVENTORY_TRANSFER_STANDALONE,
                "N"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "SO To Delivery",
                AppConstants.SALE_TO_DELIVERY,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Delivery Order",
                AppConstants.DELIVERY_ORDER,
                "Y"
            ),
            HomeItem(
                R.drawable.ic_credit_memo,
                "Credit Memo",
                AppConstants.CREDIT_MEMO,
                "Y"
            )
        )

        val filterItems = items.filter { it.status == "Y" }
        setHomeItemAdapter(filterItems)

        binding.apply {
            tvAppVersion.text = BuildConfig.FORCED_VERSION_NAME //getString(R.string.version)
            tvDBName.text = "DB : ${sessionManagement.getCompanyDB(this@HomeDashboardActivity)}"
        }
    }

    private fun setHomeItemAdapter(items: List<HomeItem>) {
        val spanCount = 2

        binding.rvHomeItems.apply {
            layoutManager =
                GridLayoutManager(this@HomeDashboardActivity, spanCount, GridLayoutManager.VERTICAL, false)
            val homeAdapter = HomeAdapter(items) { clickedId ->
                handleItemClick(clickedId)
            }
            adapter = homeAdapter
        }
    }

    private fun handleItemClick(clickedId: String) {
        when (clickedId) {
            AppConstants.ISSUE_FOR_PRODUCTION -> {
                flag = "Issue_Order"
                /*var intent: Intent = Intent(this, ProductionListActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)*/
            }

            AppConstants.SCAN_AND_VIEW -> {
                /*var intent: Intent = Intent(this, ScanQRViewActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.INVENTORY_REQ -> {
                startActivity(Intent(this@HomeDashboardActivity, InventoryRequestListActivity::class.java))
            }

            AppConstants.GOODS_ISSUE -> {
                /*var intent: Intent = Intent(this, GoodsOrderActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.INVENTORY_TRANSFER_GRPO -> {
                /*var intent: Intent = Intent(this, InventoryOrderActivity_ITR_GRPO::class.java)
                startActivity(intent)*/
            }

            AppConstants.GOODS_RECEIPT_PO -> {
                /* var intent: Intent = Intent(this, PurchaseOrderActivity::class.java)
                 startActivity(intent)*/
            }

            AppConstants.SALE_TO_INVOICE -> {
                flag = AppConstants.SALE_TO_INVOICE
                /*var intent: Intent = Intent(this, SaleToInvoiceActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)*/
            }

            AppConstants.RECEIPT_FROM_PRODUCTION -> {
                /*var intent: Intent = Intent(this, RFPActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.PICK_LIST -> {
                /*var intent = Intent(this, PickListActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.RETURN_COMPONENTS -> {
                flag = "Issue_Order"
                /*var intent = Intent(this@HomeActivity, ReturnComponentListActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)*/
            }

            AppConstants.GOODS_RECEIPT -> {
                /*var intent: Intent = Intent(this, GoodsReceiptActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.INVENTORY_TRANSFER_STANDALONE -> {
                /*var intent: Intent = Intent(this, InventoryTransferActivity::class.java)
                startActivity(intent)*/
            }

            AppConstants.SALE_TO_DELIVERY -> {
                startActivity(Intent(this@HomeDashboardActivity, SaleOrderListActivity::class.java))
            }

            AppConstants.DELIVERY_ORDER -> {
                startActivity(Intent(this@HomeDashboardActivity, DeliveryListActivity::class.java))
            }

            AppConstants.CREDIT_MEMO -> {
                startActivity(Intent(this@HomeDashboardActivity, CreditMemoListActivity::class.java))
            }

        }
    }

    //todo set search icon on activity...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                //todo Handle icon click
                dialog()
                return true
            }

            R.id.settings -> {
                //chooseScannerPopupDialog()
                startActivity(Intent(this, SettingActivity::class.java))
                finish()

                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logout_menu, menu)
        val item = menu.findItem(R.id.logout)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }

    fun dialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.logout_layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set full width
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val cancel = dialog.findViewById<Button>(R.id.cancelbtn)
        val logoutbtn = dialog.findViewById<Button>(R.id.logoutbtn)

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        logoutbtn.setOnClickListener {
            logoutApiHit()
        }

        dialog.show()
    }

    fun logoutApiHit() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
                val networkClient = NetworkClients.create(this)
                networkClient.doGetLogoutCall("B1SESSION=" + sessionManagement.getSessionId(this)).apply {
                    enqueue(object : Callback<LoginResponseModel> {
                        override fun onResponse(call: Call<LoginResponseModel>, response: Response<LoginResponseModel>) {
                            try {
                                var i: Intent = Intent(this@HomeDashboardActivity, LoginActivity::class.java)
                                startActivity(i)
                                Prefs.putString(AppConstants.BPLID, "")
                                sessionManagement.setSessionId(this@HomeDashboardActivity, "")
                                sessionManagement.setSessionTimeout(this@HomeDashboardActivity, "")
                                sessionManagement.setLoggedIn(this@HomeDashboardActivity, false)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@HomeDashboardActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@HomeDashboardActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }

                    })
                }

            } else {
                materialProgressDialog.dismiss()
                Toast.makeText(this@HomeDashboardActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clickListeners() {

    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }
}