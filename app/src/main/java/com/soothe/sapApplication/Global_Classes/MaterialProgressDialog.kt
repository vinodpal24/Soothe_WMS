package com.soothe.sapApplication.Global_Classes

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window

class MaterialProgressDialog(context: Context):Dialog(context, com.soothe.sapApplication.R.style.LoadingDialogTheme){

    private val mContext: Context = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inflateView: View = inflater.inflate(com.soothe.sapApplication.R.layout.loader, findViewById(com.soothe.sapApplication.R.id.loading_container))
        setCancelable(false)
        setContentView(inflateView)
    }

}
