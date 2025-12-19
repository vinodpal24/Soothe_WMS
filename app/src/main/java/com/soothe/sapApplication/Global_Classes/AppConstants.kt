package com.soothe.sapApplication.Global_Classes

object AppConstants {
    //todo api response fields keys store..o
    const val SESSION_TIMEOUT = "SessionTimeout"
    const val LOGIN_SESSION_TIMEOUT = "LOGIN_SessionTimeout"
    const val SESSION_ID = "SessionId"
    const val LOGIN_ID = "_LOGIN_ID"
    const val LOGIN_PASSWORD = "_LOGIN_PASSWORD"
    var FromWhere = "Login"
    var WHAREHOUSE = "Warehouse"
    var SCANNER_CHECK = "scanner_check"
    var LEASER_CHECK = "leaser_check"
    const val isFirstTime = "false"
    var SCANNER_TYPE = "scanner_type"
    var USER_PASSWORD = "Password"
    var USER_NAME = "UserName"
    var IS_LOGGED_IN = "IsLoggedIn"
    var IS_VPN_REQUIRED = "IS_VPN_REQUIRED"
    var IS_SCAN = false
    var BPLID = "_BPLID"

    var isVpnRequired = false // true if vpn require else false
    var isDevelopment = false //BuildConfig.IS_DEVELOPMENT_CLIENT // true if test env ui is visible else false

    //var isOldDevelopment = true
    var isOldDevelopment = false


    var ISSUE_FOR_PRODUCTION = "ISSUE_FOR_PRODUCTION"
    var SCAN_AND_VIEW = "SCAN_AND_VIEW"
    var INVENTORY_REQ = "INVENTORY_REQ"
    var GOODS_ISSUE = "GOODS_ISSUE"
    var INVENTORY_TRANSFER_GRPO = "INVENTORY_TRANSFER_GRPO"
    var GOODS_RECEIPT_PO = "GOODS_RECEIPT_PO"
    var SALE_TO_INVOICE = "SALE_TO_INVOICE"
    var RECEIPT_FROM_PRODUCTION = "RECEIPT_FROM_PRODUCTION"
    var PICK_LIST = "PICK_LIST"
    var RETURN_COMPONENTS = "RETURN_COMPONENTS"
    var GOODS_RECEIPT = "GOODS_RECEIPT"
    var INVENTORY_TRANSFER_STANDALONE = "INVENTORY_TRANSFER_STANDALONE"
    var SALE_TO_DELIVERY = "SALE_TO_DELIVERY"
    var DELIVERY_ORDER = "DELIVERY_ORDER"
    var CREDIT_MEMO = "CREDIT_MEMO"


    //todo SQL server credentials..
    //const val IP = "220.158.165.54"
    const val IP = "103.194.8.40"
    const val PORT = "1433"

    //    const val PORT = "2499"
    // const val COMPANY_DB = "Innotex_Test"  //17122022 -> 14042023 -->  TEST_25042023  //Test
    //const val COMPANY_DB   = "TEST_Soothe_130923"  //17122022 -> 14042023 -->  TEST_25042023  //Test
    //const val COMPANY_DB = "Soothe_Healthcare_DB"  //todo Live
    const val COMPANY_DB = "Test_04_Dec_2025"  // test db created on 04-12-2025
    const val USERNAME = "sa"

    //const val USERNAME   = "sa"
    // const val PASSWORD: String = "$" + "V$9Y$&5E$&Z"
    const val PASSWORD: String = "SqLSrvR@190923"
    const val Classes = "net.sourceforge.jtds.jdbc.Driver"


    /** login User name = manager , password = 9191 **/

}