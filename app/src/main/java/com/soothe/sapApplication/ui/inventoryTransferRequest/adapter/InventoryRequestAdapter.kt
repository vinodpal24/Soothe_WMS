package com.soothe.sapApplication.ui.inventoryTransferRequest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.databinding.RvItemPrimaryBinding
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel

class InventoryRequestAdapter(
    private val onItemClick: (item: InventoryRequestModel.Value, position: Int) -> Unit
) : ListAdapter<InventoryRequestModel.Value, InventoryRequestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemPrimaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position),position)
    }

    class ViewHolder(
        private val binding: RvItemPrimaryBinding,
        private val onItemClick: (item: InventoryRequestModel.Value, position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryRequestModel.Value, position: Int) = with(binding) {

            tvDocNum.text = item.DocNum
            tvCustomerName.text = item.CardName
            tvDocDate.text = GlobalMethods.formatDate(item.DocDate)

            cvListItem.setOnClickListener { onItemClick(item,adapterPosition) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InventoryRequestModel.Value>() {
        override fun areItemsTheSame(
            oldItem: InventoryRequestModel.Value,
            newItem: InventoryRequestModel.Value
        ) = oldItem.DocEntry == newItem.DocEntry   // unique ID

        override fun areContentsTheSame(
            oldItem: InventoryRequestModel.Value,
            newItem: InventoryRequestModel.Value
        ) = oldItem == newItem
    }

    // Optional: For search filter updates
    fun submitFilteredList(filteredList: List<InventoryRequestModel.Value>) {
        submitList(filteredList)
    }

}
