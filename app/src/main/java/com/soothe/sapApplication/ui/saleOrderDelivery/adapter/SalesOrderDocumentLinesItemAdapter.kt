package com.soothe.sapApplication.ui.saleOrderDelivery.adapter

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
import com.soothe.sapApplication.databinding.RvItemSecondaryLinesSoBinding
import com.soothe.sapApplication.ui.saleOrderDelivery.model.SaleOrdersModel

class SalesOrderDocumentLinesItemAdapter(private val onSave: (
    ArrayList<SaleOrdersModel.Value.DocumentLine>
) -> Unit) :
    ListAdapter<SaleOrdersModel.Value.DocumentLine,
            SalesOrderDocumentLinesItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemSecondaryLinesSoBinding.inflate(
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
        private val binding: RvItemSecondaryLinesSoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaleOrdersModel.Value.DocumentLine) = with(binding) {

            val iqTy = toWholeInt(item.U_IQTY.toString())
            val reqBoxQty =
                if (iqTy > 0) toWholeInt(item.RemainingQuantity.toString()) / iqTy else 0
            val baseBoxQty =
                if (iqTy > 0) toWholeInt(item.Quantity.toString()) / iqTy else 0

            tvItemName.text = item.ItemDescription
            tvItemCode.text = item.ItemCode
            tvNavCode.text = item.NavCode
            //tvFromWhs.text = item.FromWarehouseCode
            tvToWhs.text = item.WarehouseCode

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

        if (item.totalPktQty >= toWholeInt(item.RemainingQuantity.toString())) {
            GlobalMethods.showError(MyApp.currentApp, "Scanning completed for this Item.")
            return
        }

        val newScanCount = item.isScanned + 1

        newList[index] = item.copy(
            isScanned = newScanCount,
            totalPktQty = packQty * newScanCount,
            NavCode = navCode
        )

        submitList(newList)
        GlobalMethods.showSuccess(MyApp.currentApp, "Box added")
    }

    class DiffCallback : DiffUtil.ItemCallback<SaleOrdersModel.Value.DocumentLine>() {
        override fun areItemsTheSame(
            old: SaleOrdersModel.Value.DocumentLine,
            new: SaleOrdersModel.Value.DocumentLine
        ) =
            old.DocEntry == new.DocEntry && old.LineNum == new.LineNum

        override fun areContentsTheSame(
            old: SaleOrdersModel.Value.DocumentLine,
            new: SaleOrdersModel.Value.DocumentLine
        ) =
            old == new
    }
}
