package com.soothe.sapApplication.ui.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import com.soothe.sapApplication.databinding.ActivitySettingBinding
import com.soothe.sapApplication.ui.home.HomeDashboardActivity
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.setting.model.ModelGetBranch
import com.webapp.internetconnection.CheckNetwoorkConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private var branchList_gl: ArrayList<ModelGetBranch.Value> = ArrayList()
    private var selectedBranchName = ""
    private var selectedBranchCode = ""

    lateinit var sessionManagement: SessionManagement
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        initViews()
        clickListeners()
        setContentView(binding.root)
    }

    private fun initViews() {
        materialProgressDialog = MaterialProgressDialog(this@SettingActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this)
        sessionManagement.setScannerType(this, "LEASER")
        callGetBranchList()
        if (Prefs.getBoolean(AppConstants.IS_VPN_REQUIRED)) {
            binding.switchForVpn.isChecked = true
            binding.switchForVpn.text = "VPN Required"
            Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_VPN_REQUIRED)}")
        } else {
            binding.switchForVpn.isChecked = false
            binding.switchForVpn.text = "VPN Not Required"
            Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_VPN_REQUIRED)}")
        }
    }

    private fun clickListeners() {
        binding.apply {
            tvChooseScannerType.setOnClickListener {
                chooseScannerPopupDialog()
            }

            ivBackArrow.setOnClickListener {
                onBackPressed()
            }

            switchForVpn.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Prefs.putBoolean(AppConstants.IS_VPN_REQUIRED, true)
                    switchForVpn.text = "VPN Required"
                    Log.e("Environment", "SettingActivity: switchForVpn.setOnCheckedChangeListener => ${Prefs.getBoolean(AppConstants.IS_VPN_REQUIRED)}")
                } else {
                    Prefs.putBoolean(AppConstants.IS_VPN_REQUIRED, false)
                    switchForVpn.text = "VPN Not Required"
                    Log.e("Environment", "SettingActivity: switchForVpn.setOnCheckedChangeListener => ${Prefs.getBoolean(AppConstants.IS_VPN_REQUIRED)}")
                }
            }
        }
    }

    override fun onBackPressed() {
        val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
        if (savedBPLID.isNotEmpty()) {
            var intent: Intent = Intent(this@SettingActivity, HomeDashboardActivity::class.java)
            startActivity(intent)
            finish()
            super.onBackPressed()
        } else {
            GlobalMethods.showError(this@SettingActivity, "First you have to select branch then go back")
        }

    }

    private fun callGetBranchList() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.CUSTOM)
                val networkClient = NetworkClients.create(this)
                networkClient.getBranchList().apply {
                    enqueue(object : Callback<ModelGetBranch> {
                        override fun onResponse(
                            call: Call<ModelGetBranch>, response: Response<ModelGetBranch>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var branches = response.body()?.value
                                    Toast.makeText(this@SettingActivity, "Successfully!", Toast.LENGTH_SHORT)
                                    if (!branches.isNullOrEmpty() && branches.size > 0) {

                                        branchList_gl.addAll(branches)
                                        if (branchList_gl.size == 0) {
                                            Toast.makeText(this@SettingActivity, "No Branch Found.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val branchList = branchList_gl.map { it.bPLName }
                                            val adapter = ArrayAdapter(this@SettingActivity, R.layout.drop_down_item_textview, branchList)
                                            binding.acBranches.setAdapter(adapter)

                                            if (branchList_gl.isNotEmpty()) {
                                                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                                                val matchedBranch = branchList_gl.find { it.bPLID == savedBPLID }

                                                if (matchedBranch != null) {
                                                    selectedBranchName = matchedBranch.bPLName ?: ""
                                                    selectedBranchCode = matchedBranch.bPLID ?: ""
                                                    Log.e("FILTER_DIALOG", "Already Saved Branch -> $selectedBranchName ($selectedBranchCode)")
                                                } /*else {
                                                    val defaultBranch = branchList_gl[0]
                                                    selectedBranchName = defaultBranch.bPLName ?: ""
                                                    selectedBranchCode = defaultBranch.bPLID ?: ""
                                                    Prefs.putString(AppConstants.BPLID, selectedBranchCode) // update prefs
                                                    Log.e("FILTER_DIALOG", "Default Branch -> $selectedBranchName ($selectedBranchCode)")
                                                }*/

                                                binding.acBranches.setText(selectedBranchName, false)
                                                Log.e("FILTER_DIALOG", "Set n Save to Prefs: Branch -> $selectedBranchName ($selectedBranchCode)")
                                            }

                                            binding.acBranches.setOnItemClickListener { parent, _, position, _ ->
                                                val branch = parent.getItemAtPosition(position) as String
                                                selectedBranchName = branch
                                                selectedBranchCode = branches[position].bPLID.toString()
                                                Prefs.putString(AppConstants.BPLID, selectedBranchCode)
                                                Log.e("FILTER_DIALOG", "Selected Branch -> $selectedBranchName ($selectedBranchCode)")
                                                if (!binding.acBranches.text.toString().isNullOrEmpty()) {
                                                    var intent: Intent = Intent(this@SettingActivity, HomeDashboardActivity::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }

                                                if (branch.isNotEmpty()) {
                                                    binding.acBranches.setText(branch, false)
                                                } else {
                                                    selectedBranchName = ""
                                                    binding.acBranches.setText("")
                                                }
                                            }
                                        }


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
                                            GlobalMethods.showError(this@SettingActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@SettingActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@SettingActivity, LoginActivity::class.java)
                                            startActivity(mainIntent)
                                            finish()
                                        }/*if (mError.error.message.value != null) {
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

                        override fun onFailure(call: Call<ModelGetBranch>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(
                                    this@SettingActivity, "VPN is not connected. Please connect VPN and try again."
                                )
                            } else {
                                GlobalMethods.showError(this@SettingActivity, t.message ?: "")
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

    @SuppressLint("MissingInflatedId")
    private fun chooseScannerPopupDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this).inflate(R.layout.scanner_custom_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
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
            }/*  if (radioButton != null && checkedId != -1) {
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
}