package com.soothe.sapApplication.test

import android.content.ContentValues
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.AppConstants
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.soothe.sapApplication.ui.deliveryOneOrder.adapter.DeliveryOneLineAdapter
import com.soothe.sapApplication.ui.deliveryOneOrder.ui.DeliveryOneLineActivity
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import java.lang.ref.WeakReference

class DeliverOneLineShubhAdapter(var lineArrayList: ArrayList<InvoiceListModel.DocumentLine>) :
    RecyclerView.Adapter<DeliveryOneLineAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeliveryOneLineAdapter.ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeliveryOneLineAdapter.ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: DeliveryOneLineAdapter.ViewHolder, position: Int) {
        val weakReference = WeakReference(lineArrayList[position])
        val myObject = weakReference.get()
        with(holder) {
            with(myObject) {
                binding.trBaseQuantity.visibility = android.view.View.VISIBLE

                binding.tvTotalBaseQuantity.text = ":   " + this?.Quantity.toString()

                binding.tvItemName.text = ":   " + this?.ItemDescription
                binding.tvOpenQty.text = ":   " + this?.ItemCode

                binding.issueAndDocumentlayout.visibility = android.view.View.VISIBLE
                binding.fromWarehouseRow.visibility = android.view.View.GONE

                var qtyBox = 0;
                if (com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this?.Quantity.toString())!!
                        .toInt() > 0
                ) {
                    if (this!!.U_IQTY.equals("0")) {
                        qtyBox =
                            com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this?.RemainingQuantity.toString())!!
                                .toInt() / 1

                    } else {
                        qtyBox =
                            com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this?.RemainingQuantity.toString())!!
                                .toInt() / com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(
                                this!!.U_IQTY
                            )!!.toInt()

                    }

                }

                var doBoxQuantity = 0;
                if (com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this!!.U_IQTY)!!
                        .toInt() > 0
                ) {
                    if (this.U_IQTY.equals("0")) {
                        doBoxQuantity =
                            com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this!!.Quantity.toString())!!
                                .toInt() / 1

                    } else {
                        doBoxQuantity =
                            com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(this!!.Quantity.toString())!!
                                .toInt() / com.soothe.sapApplication.Global_Classes.GlobalMethods.numberToK(
                                this.U_IQTY
                            )!!.toInt()
                    }
                }



                binding.tvDoBoxQuantity.text = ":   $doBoxQuantity"


                binding.tvWidth.text = ":   $qtyBox"
                binding.tvTotalScannQty.text = ":   " + this?.RemainingQuantity
                this!!.initialBoxes = qtyBox

                binding.tvTotalScanGw.text = ":   " + this.totalPktQty

                binding.tvLength.text = ":   " + this.isScanned

//                binding.tvNavicode.text = this.NavisionCode?.let { date -> " : " + date } ?: " :" //todo NEW CHANGE comment bcz now already gettiing nav code in document line --
                binding.tvNavicode.text = " :   " + this.NavCode


            }
        }
    }


    //TODO view holder...
    inner class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int {
        return lineArrayList.size
    }
}