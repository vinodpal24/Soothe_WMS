package com.soothe.sapApplication.SessionManagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.ui.login.LoginActivity
import com.soothe.sapApplication.ui.splashScreen.SplashActivity

class SessionManagement(_context: Context) {

    var _context: Context
    init {
        this._context = _context
    }

    private val NAME = "ProductionApp"
    private val MODE = Context.MODE_PRIVATE

    // todo Shared Preferences
    var pref: SharedPreferences = _context.getSharedPreferences(NAME, MODE)

    //todo  Editor for Shared preferences
    lateinit var editor: SharedPreferences.Editor

    //todo code for first login...
    private val IS_FIRST_RUN = "IsFirstRun+"

    // todo All Shared Preferences Keys
    private val IS_LOGIN = "IsLoggedIn"

    var userDetail: Boolean? = null

    // todo Get Login State
    val isLoggedIn: Boolean
        get() = pref.getBoolean(IS_LOGIN, false)

    val isFirstRun: Boolean
        get() = pref.getBoolean(IS_FIRST_RUN, true)

    fun getData(): Boolean? {
        val user: Boolean = pref.getBoolean(IS_FIRST_RUN, false)
        userDetail = user
        return userDetail
    }

    fun createFirstSession() {
        editor.putBoolean(IS_FIRST_RUN, true)
        editor.commit()
    }


    fun firstRun(_context: Context, sessionID: String?) {
        // todo Check login status
        if (!isFirstRun) {
            checkLogin(_context, sessionID)
        } else {
            val i = Intent(_context, SplashActivity::class.java)
            // todo Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // todo Add new Flag to start new Activity
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            i.putExtra("token", sessionID)

            // todo Staring Login Activity
            _context.startActivity(i)

        }
    }

    fun checkLogin(_context: Context, sessionID: String?) {
        // todo Check login status
        if (!isLoggedIn) {
            // todo user is not logged in redirect him to Login Activity
            val i = Intent(_context, LoginActivity::class.java)
            // todo Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            i.putExtra("sessionID", sessionID)

            // todo Add new Flag to start new Activity
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            // todo Staring Login Activity
            _context.startActivity(i)
        } else {
            val i = Intent(_context, LoginActivity::class.java)
            // todo Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // todo Add new Flag to start new Activity
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            //todo Staring Login Activity
            _context.startActivity(i)
        }
    }


    //todo Shared preference String value store..
    fun setSharedPrefernce(context: Context, key: String, value: String) {
        editor = pref.edit()
        editor.putString(key, value )
        editor.commit()
    }

    private fun getDataFromSharedPreferences(context: Context, Key: String): String? {
        return try {
            val returnString: String? = pref.getString(Key, null)
            returnString
        } catch (e: java.lang.Exception) {
            ""
        }
    }

    //todo Shared preference int value store..
    fun setIntSharedPrefernce(key: String, value: Int) {
        editor = pref.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    fun setSharedPrefrenceBoolean(context: Context, key: String, value: Boolean) {
        editor = pref.edit()
        editor.putBoolean(key, value )
        editor.commit()
    }

    private fun getBooleanDataFromSharedPreferences(context: Context, key: String): Boolean {
        return try {
            pref.getBoolean(key, false)
        } catch (e: Exception) {
            false
        }
    }

    private fun getIntDataFromSharedPreferences(Key: String): Int {
        val returnInt: Int = pref.getInt(Key, 0)
        return returnInt
    }

    fun ClearSession(mContext: Context) {
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
        editor.clear()
        editor.commit()
    }

    fun setSessionId(context: Context, SessionId: String?) {
        if (SessionId != null) {
            setSharedPrefernce(context, AppConstants.SESSION_ID, SessionId)
        }
    }

    fun getSessionId(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SESSION_ID)
    }

    fun setLoginId(context: Context, SessionId: String?) {
        if (SessionId != null) {
            setSharedPrefernce(context, AppConstants.LOGIN_ID, SessionId)
        }
    }

    fun getLoginId(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.LOGIN_ID)
    }


    fun setLoginPassword(context: Context, SessionId: String?) {
        if (SessionId != null) {
            setSharedPrefernce(context, AppConstants.LOGIN_PASSWORD, SessionId)
        }
    }

    fun getLoginPassword(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.LOGIN_PASSWORD)
    }

    fun setSessionTimeout(context: Context, SessionTimeout: String?) {
        if (SessionTimeout != null) {
            setSharedPrefernce(context, AppConstants.SESSION_TIMEOUT, SessionTimeout)
        }
    }

    fun getSessionTimeout(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SESSION_TIMEOUT)
    }


    fun setLoginSessionTimeout(context: Context, SessionTimeout: String?) {
        if (SessionTimeout != null) {
            setSharedPrefernce(context, AppConstants.LOGIN_SESSION_TIMEOUT, SessionTimeout)
        }
    }

    fun getLoginSessionTimeout(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.LOGIN_SESSION_TIMEOUT)
    }

    fun setFromWhere(context: Context, fromWhere: String?) {
        if (fromWhere != null) {
            setSharedPrefernce(context, AppConstants.FromWhere, fromWhere)
        }
    }

    fun getFromWhere(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.FromWhere)
    }

    fun setWarehouseCode(context: Context, Warehouse: String?) {
        if (Warehouse != null) {
            setSharedPrefernce(context, AppConstants.WHAREHOUSE, Warehouse)
        }
    }

    fun getWarehouseCode(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.WHAREHOUSE)
    }

    fun setQRScanner(scanner_check: Int?) {
        if (scanner_check != null) {
            setIntSharedPrefernce( AppConstants.SCANNER_CHECK, scanner_check)
        }
    }

    fun getQRScannerCheck(): Int? {
        return getIntDataFromSharedPreferences(AppConstants.SCANNER_CHECK)
    }

    fun setLaser(leaser_check: Int?) {
        if (leaser_check != null) {
            setIntSharedPrefernce(AppConstants.LEASER_CHECK, leaser_check)
        }
    }

    fun getLeaserCheck(): Int? {
        return getIntDataFromSharedPreferences(AppConstants.LEASER_CHECK)
    }

    fun setCompanyDB(context: Context, CompanyDB: String?) {
        if (CompanyDB != null) {
            setSharedPrefernce(context, AppConstants.COMPANY_DB, CompanyDB)
        }
    }

    fun getCompanyDB(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.COMPANY_DB)
    }

    fun setScannerType(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.SCANNER_TYPE, type)
        }
    }

    fun getScannerType(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SCANNER_TYPE)
    }

    fun setPassword(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.USER_PASSWORD, type)
        }
    }

    fun getPassword(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.USER_PASSWORD)
    }

    fun setUsername(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.USER_NAME, type)
        }
    }

    fun getUsername(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.USER_NAME)
    }

    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        setSharedPrefrenceBoolean(context, AppConstants.IS_LOGGED_IN, isLoggedIn)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getBooleanDataFromSharedPreferences(context, AppConstants.IS_LOGGED_IN) ?: false
    }

    companion object {
        /*fun saveStringPreferences(mContext: Context, key: String, value: String) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
            val editor = sharedPreferences.edit()
            editor.putString(key, value)
            editor.apply()
        }

        fun saveIntPreferences(mContext: Context, key: String, value: Int) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
            val editor = sharedPreferences.edit()
            editor.putInt(key, value)
            editor.apply()
        }

        fun getPreferencesString(mContext: Context, key: String): String? {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
            return sharedPreferences.getString(key, "")
        }

        fun getIntPreferences(mContext: Context, key: String): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
            return sharedPreferences.getInt(key, 0)
        }

        fun ClearSession(mContext: Context) {
            val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
            editor.clear()
            editor.commit()
        }*/
    }


}