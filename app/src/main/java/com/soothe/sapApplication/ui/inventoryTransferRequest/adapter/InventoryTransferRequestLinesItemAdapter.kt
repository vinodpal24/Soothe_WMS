package com.soothe.sapApplication.ui.inventoryTransferRequest.adapter

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
import com.soothe.sapApplication.databinding.RvItemSecondaryLinesBinding
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel

class InventoryTransferRequestLinesItemAdapter(private val onSave: (
    ArrayList<InventoryRequestModel.StockTransferLines>
) -> Unit) :
    ListAdapter<InventoryRequestModel.StockTransferLines,
            InventoryTransferRequestLinesItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemSecondaryLinesBinding.inflate(
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
        private val binding: RvItemSecondaryLinesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryRequestModel.StockTransferLines) = with(binding) {

            val iqTy = toWholeInt(item.U_IQTY)
            val reqBoxQty =
                if (iqTy > 0) toWholeInt(item.RemainingOpenQuantity) / iqTy else 0
            val baseBoxQty =
                if (iqTy > 0) toWholeInt(item.Quantity) / iqTy else 0

            tvItemName.text = item.ItemDescription
            tvItemCode.text = item.ItemCode
            tvNavCode.text = item.NavisionCode
            tvFromWhs.text = item.FromWarehouseCode
            tvToWhs.text = item.WarehouseCode

            tvBaseBoxQty.text = baseBoxQty.toString()
            tvBasePktQty.text = item.Quantity
            tvReqBoxQty.text = reqBoxQty.toString()
            tvReqPKtQty.text = item.RemainingOpenQuantity

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

        if (item.totalPktQty >= toWholeInt(item.RemainingOpenQuantity)) return

        val newScanCount = item.isScanned + 1

        newList[index] = item.copy(
            isScanned = newScanCount,
            totalPktQty = packQty * newScanCount,
            NavisionCode = navCode
        )

        submitList(newList)
        GlobalMethods.showSuccess(MyApp.currentApp, "Box added")
    }

    class DiffCallback : DiffUtil.ItemCallback<InventoryRequestModel.StockTransferLines>() {
        override fun areItemsTheSame(
            old: InventoryRequestModel.StockTransferLines,
            new: InventoryRequestModel.StockTransferLines
        ) =
            old.DocEntry == new.DocEntry && old.LineNum == new.LineNum

        override fun areContentsTheSame(
            old: InventoryRequestModel.StockTransferLines,
            new: InventoryRequestModel.StockTransferLines
        ) =
            old == new
    }
}
