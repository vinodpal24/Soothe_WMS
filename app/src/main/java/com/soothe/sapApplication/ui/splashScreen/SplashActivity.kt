package com.soothe.sapApplication.ui.splashScreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pixplicity.easyprefs.library.Prefs
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.MaterialProgressDialog
import com.soothe.sapApplication.Global_Notification.NetworkConnection
import com.soothe.sapApplication.R
import com.soothe.sapApplication.SessionManagement.SessionManagement
import com.soothe.sapApplication.databinding.ActivitySplashBinding
import com.soothe.sapApplication.ui.home.HomeDashboardActivity
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.setting.SettingActivity

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {
    var networkConnection = NetworkConnection()
    private lateinit var sessionManagement: SessionManagement
    private lateinit var binding: ActivitySplashBinding
    lateinit var materialProgressDialog: MaterialProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        sessionManagement = SessionManagement(applicationContext)
        materialProgressDialog = MaterialProgressDialog(this@SplashActivity)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_slide)
        binding.headerIcon.startAnimation(slideAnimation)


        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            // todo end rotate
            Handler().postDelayed({
                goToLogin()
            }, 3000)

        } else {
            var alertDialog = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Internet Connection Alert")
                .setMessage("Please Check Your Internet Connection")
                .setPositiveButton("") { dialogInterface, i ->
                    var intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }.show()
            alertDialog.setCanceledOnTouchOutside(false)
        }

    }

    private fun goToLogin() {
        Log.e("Splash==>", "SessionId: ${sessionManagement.getSessionId(applicationContext)}\nSession Timeout: ${sessionManagement.getSessionTimeout(applicationContext)} ")
        val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
        val isAlreadyLoggedIn = sessionManagement.isLoggedIn(this@SplashActivity)
        if (!sessionManagement.getSessionId(applicationContext).isNullOrEmpty() && !sessionManagement.getSessionTimeout(applicationContext).equals(null)) {
            if (isAlreadyLoggedIn) {
                if (savedBPLID.isEmpty()) {
                    Log.e("Splash==>", "gotoLogin() => if condition to navigate to SettingActivity")
                    val intent = Intent(this@SplashActivity, SettingActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Log.e("Splash==>", "gotoLogin() => if condition to navigate to HomeActivity")
                    val intent = Intent(this@SplashActivity, HomeDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                Log.e("Splash==>", "gotoLogin() => if condition to navigate to LoginActivity")
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

        } else {
            Log.e("Splash==>", "gotoLogin() => if condition to navigate to LoginActivity")
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}