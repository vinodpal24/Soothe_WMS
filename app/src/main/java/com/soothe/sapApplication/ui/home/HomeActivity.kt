package com.soothe.sapApplication.ui.home

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.R
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivityHomeBinding
import com.soothe.sapApplication.ui.deliveryOneOrder.ui.DeliveryOneActivity
import com.soothe.sapApplication.ui.deliveryOrder.activity.DeliveryListActivity
import com.soothe.sapApplication.ui.inventoryTransferRequest.activity.InventoryRequestListActivity
import com.soothe.sapApplication.ui.invoiceOrder.UI.InvoiceOrderActivity
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.productionOrderLines.ProductionListActivity
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.login.Model.LoginResponseModel
import com.soothe.sapApplication.ui.saleOrderDelivery.activity.SaleOrderListActivity
import com.soothe.sapApplication.ui.setting.SettingActivity
import com.webapp.internetconnection.CheckNetwoorkConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeActivity : AppCompatActivity() {
    private lateinit var homeBinding: ActivityHomeBinding
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var sessionManagement: SessionManagement
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    var doubleBackToExitPressedOnce = false
    var flag: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(homeBinding.root)
        //todo set title on header...
        title = "Menu Screen"
        supportActionBar?.setDisplayShowHomeEnabled(true)

        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@HomeActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this)


        homeBinding.apply {
            cvInventoryRequest.setOnClickListener {
                flag = "Issue_Order"
                startActivity(Intent(this@HomeActivity, InventoryRequestListActivity::class.java))
                //startActivity(Intent(this@HomeActivity, ProductionListActivity::class.java).apply { putExtra("flag", flag) })
            }

            cvSaleToDelivery.setOnClickListener {
                startActivity(Intent(this@HomeActivity, SaleOrderListActivity::class.java))
            }

            cvDeliveryOrder.setOnClickListener {
                startActivity(Intent(this@HomeActivity, DeliveryListActivity::class.java))
            }
        }

        homeBinding.issueCard.setOnClickListener {
            //callIssueCardApi()
            flag = "Issue_Order"
            var intent: Intent = Intent(this@HomeActivity, ProductionListActivity::class.java)
            intent.putExtra("flag", flag)
            startActivity(intent)
        }

        homeBinding.returnCard.setOnClickListener {
            GlobalMethods.showMessage(this, "Work In Process.")
            /*  var intent: Intent = Intent(this, ProductionListActivity::class.java)
              startActivity(intent)*/
        }

        homeBinding.receiptCard.setOnClickListener {
            GlobalMethods.showMessage(this, "Work In Process.")
            /* var intent: Intent = Intent(this, ProductionListActivity::class.java)
             startActivity(intent)*/
        }
        homeBinding.deliveryCard.setOnClickListener {
            flag = "Delivery_Order"
            var intent: Intent = Intent(this, ProductionListActivity::class.java)
            intent.putExtra("flag", flag)
            startActivity(intent)
        }

        homeBinding.invoiceCard.setOnClickListener {
            var intent: Intent = Intent(this, InvoiceOrderActivity::class.java)
            startActivity(intent)
        }

        homeBinding.deliveryOneCard.setOnClickListener {
            var intent: Intent = Intent(this, DeliveryOneActivity::class.java)
            startActivity(intent)
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
//        dialog.dismiss()

        var cancel = dialog.findViewById<Button>(R.id.cancelbtn)
        var logoutbtn = dialog.findViewById<Button>(R.id.logoutbtn)


        cancel.setOnClickListener {
            dialog.dismiss()
        }

        logoutbtn.setOnClickListener {
            logoutApiHit()
        }
        dialog.show()
    }


    //todo issue for production api calling...
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
                                var i: Intent = Intent(this@HomeActivity, LoginActivity::class.java)
                                startActivity(i)
                                Prefs.putString(AppConstants.BPLID, "")
                                sessionManagement.setSessionId(this@HomeActivity, "")
                                sessionManagement.setSessionTimeout(this@HomeActivity, "")
                                sessionManagement.setLoggedIn(this@HomeActivity, false)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@HomeActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@HomeActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                        }

                    })
                }

            } else {
                materialProgressDialog.dismiss()
                Toast.makeText(this@HomeActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //todo choose scanner type..
    @SuppressLint("MissingInflatedId")
    private fun chooseScannerPopupDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this).inflate(R.layout.scanner_custom_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui text ...
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
        val radioLaser = view.findViewById<RadioButton>(R.id.radioLaser)
        val radioQrScanner = view.findViewById<RadioButton>(R.id.radioQrScanner)
        val goBtn = view.findViewById<AppCompatButton>(R.id.goBtn)

        //todo get radio buttons selected id..
        var checkGender = ""

        radioGroup?.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            var radioButton = group.findViewById<RadioButton>(checkedId)
            checkGender = radioButton.text.toString()
            when (checkedId) {
                R.id.radioLaser -> {
                    radioLaser.isChecked = true
                }

                R.id.radioQrScanner -> {
                    radioQrScanner.isChecked = true
                }
            }
            /*  if (radioButton != null && checkedId != -1) {
                  Toast.makeText(this, radioButton.text, Toast.LENGTH_SHORT).show()
              } else {
                  return@OnCheckedChangeListener
              }*/
        })

        //todo validation for toggle..
        if (sessionManagement.getScannerType(this) == "LEASER") {
            radioLaser.isChecked = true
        } else if (sessionManagement.getScannerType(this) == "QR_SCANNER") {
            radioQrScanner.isChecked = true
        }

        //todo go btn..
        goBtn?.setOnClickListener {
            if (checkGender.equals("L")) {
//                sessionManagement.setLaser(1)
//                sessionManagement.setQRScanner(0)
                sessionManagement.setScannerType(this, "LEASER")
            } else if (checkGender.equals("S")) {
//                sessionManagement.setLaser(0)
//                sessionManagement.setQRScanner(1)
                sessionManagement.setScannerType(this, "QR_SCANNER")
            }
            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()

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