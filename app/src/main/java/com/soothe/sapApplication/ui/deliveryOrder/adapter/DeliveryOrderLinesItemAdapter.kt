package com.soothe.sapApplication.ui.deliveryOrder.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.Global_Classes.GlobalMethods.toWholeInt
import com.soothe.sapApplication.Global_Classes.MyApp
import com.soothe.sapApplication.R
import com.soothe.sapApplication.databinding.RvItemSecondaryLinesDeliveryBinding
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel

class DeliveryOrderLinesItemAdapter(private val onSave: (
    ArrayList<InvoiceListModel.DocumentLine>
) -> Unit) :
    ListAdapter<InvoiceListModel.DocumentLine,
            DeliveryOrderLinesItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemSecondaryLinesDeliveryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: RvItemSecondaryLinesDeliveryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InvoiceListModel.DocumentLine) = with(binding) {

            val iqTy = toWholeInt(item.U_IQTY)
            val reqBoxQty =
                if (iqTy > 0) toWholeInt(item.RemainingQuantity.toString()) / iqTy else 0
            val baseBoxQty =
                if (iqTy > 0) toWholeInt(item.Quantity.toString()) / iqTy else 0

            tvItemName.text = item.ItemDescription
            tvItemCode.text = item.ItemCode
            tvNavCode.text = item.NavCode

            tvBaseBoxQty.text = baseBoxQty.toString()
            tvBasePktQty.text = item.Quantity.toString()
            tvReqBoxQty.text = reqBoxQty.toString()
            tvReqPKtQty.text = item.RemainingQuantity.toString()

            tvScanBoxQty.text = item.isScanned.toString()
            tvScanPktQty.text = item.totalPktQty.toString()

            val isScannedValid = item.isScanned > 0 && item.totalPktQty > 0

            layoutPoItem.setBackgroundResource(
                if (isScannedValid) {
                    R.drawable.rounded_border_dark_blue
                } else {
                    R.drawable.rounded_border_light_blue
                }
            )
        }
    }

    fun onSaveClicked() {
        onSave(ArrayList(currentList))
    }

    // -------- UPDATE SCANNED ITEM SAFELY --------
    fun updateScannedItem(
        itemCode: String,
        packQty: Int,
        navCode: String
    ) {
        Log.i("NAV_CODE_SCANNING", "Scanning Data in updateScannedItem() -> ItemCode: $itemCode, Pack Qty: $packQty, NavCode: $navCode")
        val newList = currentList.toMutableList()
        val index = newList.indexOfFirst { it.ItemCode == itemCode }
        if (index == -1) {
            GlobalMethods.showError(MyApp.currentApp, "Please scan correct item.")
            return
        }

        val item = newList[index]

        if (item.totalPktQty >= toWholeInt(item.RemainingQuantity.toString())) return

        val newScanCount = item.isScanned + 1

        newList[index] = item.copy(
            isScanned = newScanCount,
            totalPktQty = packQty * newScanCount,
            NavisionCode = navCode
        )

        submitList(newList)
        GlobalMethods.showSuccess(MyApp.currentApp, "Box added")
    }

    class DiffCallback : DiffUtil.ItemCallback<InvoiceListModel.DocumentLine>() {
        override fun areItemsTheSame(
            old: InvoiceListModel.DocumentLine,
            new: InvoiceListModel.DocumentLine
        ) =
            old.DocEntry == new.DocEntry && old.LineNum == new.LineNum

        override fun areContentsTheSame(
            old: InvoiceListModel.DocumentLine,
            new: InvoiceListModel.DocumentLine
        ) =
            old == new
    }
}
