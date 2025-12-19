package com.soothe.sapApplication.ui.login

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.ui.login.Model.LoginResponseModel
import com.soothe.sapApplication.Model.OtpErrorModel
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.Validation.Validation
import com.soothe.sapApplication.databinding.ActivityLoginBinding
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Retrofit_Api.ApiConstant
import com.soothe.sapApplication.Retrofit_Api.ApiConstant.getBaseUrl
import com.soothe.sapApplication.Retrofit_Api.ApiConstantForURL
import com.soothe.sapApplication.Retrofit_Api.NetworkClients
import com.soothe.sapApplication.ui.home.HomeDashboardActivity
import com.soothe.sapApplication.ui.setting.SettingActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class LoginActivity : AppCompatActivity() {
    private lateinit var activityLoginBinding: ActivityLoginBinding

    //    private var loginViewModel: LoginViewModel? = null
    lateinit var validation: Validation
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(activityLoginBinding.root)

        supportActionBar?.hide()

        //todo initialization...
        validation = Validation()
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@LoginActivity)
        sessionManagement = SessionManagement(this@LoginActivity)
        val baseUrlCustom = getBaseUrl(ApiConstant.ApiType.CUSTOM)
        val baseUrlStandard = getBaseUrl(ApiConstant.ApiType.STANDARD)
        Log.w("Base_URL", "Custom URL: $baseUrlCustom\nStandard URL: $baseUrlStandard")
        //todo Place cursor at the end of text in EditText
        activityLoginBinding.loginUsername.setSelection(activityLoginBinding.loginUsername.length())

        //todo
        sessionManagement.setFromWhere(applicationContext, "Login")
        setStaticDbName()
        //todo login click listener..
        activityLoginBinding.loginButton.setOnClickListener {
//            initiateLoginCall()
            apiCall()
        }
    }

    private fun setStaticDbName() {
        val dbNames = arrayListOf("Soothe_Healthcare_DB", "Test_04_Dec_2025")

        val adapter = ArrayAdapter(
            this@LoginActivity,
            R.layout.simple_spinner_dropdown_item,
            dbNames
        )

        activityLoginBinding.AcDbNameList.setAdapter(adapter)
        activityLoginBinding.AcDbNameList.hint = "Select DB"

        activityLoginBinding.AcDbNameList.threshold = 0 // Show suggestions even without typing

        // Handle item selection
        activityLoginBinding.AcDbNameList.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            activityLoginBinding.AcDbNameList.setText(selectedItem, false) // false to prevent filtering again
        }
    }

    private fun apiCall() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            if (activityLoginBinding.AcDbNameList.text.toString().trim().isNullOrEmpty()) {
                activityLoginBinding.AcDbNameList.error = "Please enter your Company DB"
            } else if (activityLoginBinding.loginUsername.text.toString().trim().isNullOrEmpty()) {
                activityLoginBinding.loginUsername.error = "Please enter user name"
            } else if (activityLoginBinding.loginPassword.text.toString().trim().isNullOrEmpty()) {
                activityLoginBinding.loginPassword.error = "Please enter password"
            } else {
                sessionManagement.setCompanyDB(applicationContext, activityLoginBinding.AcDbNameList.text.toString())
                var jsonObject: JsonObject = JsonObject()
                jsonObject.addProperty("CompanyDB", activityLoginBinding.AcDbNameList.text.toString().trim())
                jsonObject.addProperty("Password", activityLoginBinding.loginPassword.text.toString().trim())
                jsonObject.addProperty("UserName", activityLoginBinding.loginUsername.text.toString().trim())
                val apiConfig = ApiConstantForURL()
                NetworkClients.updateBaseUrlFromConfig(apiConfig, ApiConstantForURL.ApiType.STANDARD)
                NetworkClients.create(this).doGetLoginCall(jsonObject).apply {
                    enqueue(object : Callback<LoginResponseModel> {
                        override fun onResponse(call: Call<LoginResponseModel>, response: Response<LoginResponseModel>) {
                            try {
                                if (response.isSuccessful) {
                                    materialProgressDialog.dismiss()
                                    var loginResponseModel = response.body()!!
                                    //todo shares preference store....
                                    sessionManagement.setSessionId(this@LoginActivity, loginResponseModel.SessionId)
                                    sessionManagement.setSessionTimeout(this@LoginActivity, loginResponseModel.SessionTimeout)
                                    sessionManagement.setLoginId(this@LoginActivity, activityLoginBinding.loginUsername.text.toString())
                                    sessionManagement.setLoginPassword(this@LoginActivity, activityLoginBinding.loginPassword.text.toString())
                                    sessionManagement.setLoginSessionTimeout(this@LoginActivity, System.currentTimeMillis().toString())
                                    sessionManagement.setLoggedIn(this@LoginActivity, true)
                                    sessionManagement.setFromWhere(this@LoginActivity, "ElseCase")
                                    Log.e("api_success-----", response.toString())
                                    if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) {
                                        var intent: Intent = Intent(this@LoginActivity, HomeDashboardActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        var intent: Intent = Intent(this@LoginActivity, SettingActivity::class.java)
                                        startActivity(intent)
                                    }
                                    finish()
                                    GlobalMethods.showSuccess(this@LoginActivity, "Successfully Login.")
                                } else {
                                    materialProgressDialog.dismiss()

                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                            Log.e("login_api_failure-----", t.toString())
                            if (t.message == "VPN_Exception") {
                                GlobalMethods.showError(this@LoginActivity,"VPN is not connected. Please connect VPN and try again."
                                )
                            }else{
                                GlobalMethods.showError(this@LoginActivity, t.message ?: "")
                            }
                            materialProgressDialog.dismiss()
                            Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT)
                        }

                    })
                }
            }

        } else {
            materialProgressDialog.dismiss()
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Internet Connection Alert")
                .setMessage("Please Check Your Internet Connection")
                .setPositiveButton("Close") { dialogInterface, i ->
                    finish()
                }.show()
        }
    }


    /* //todo login through mvvm architecture...
     private fun initiateLoginCall() {
         val loginRequest = LoginRequest(
             "17122022",
             activityLoginBinding.loginPassword.text.toString(),
             activityLoginBinding.loginUsername.text.toString()
         )
         loginViewModel?.doGetLoginCall(
             activityLoginBinding.loginPassword.text.toString(),
             activityLoginBinding.loginUsername.text.toString()
         )?.observe(this, Observer { response ->
             if (response == null) {
                 Log.e("failure-----", response.toString())
                 Toast.makeText(this, "Failed to Login!", Toast.LENGTH_SHORT)
             } else {
                 var intent: Intent = Intent(this, HomeActivity::class.java)
                 startActivity(intent)
                 Log.e("reponse-----", response.toString())
 //                MDToast.makeText(this, "Successfully Login.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR)
                 Toast.makeText(this, "Successfully Login.", Toast.LENGTH_SHORT)
             }
         })
     }*/

}