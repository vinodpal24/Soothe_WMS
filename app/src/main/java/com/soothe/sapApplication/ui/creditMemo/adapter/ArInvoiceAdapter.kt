package com.soothe.sapApplication.ui.creditMemo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.databinding.RvItemPrimaryBinding
import com.soothe.sapApplication.ui.creditMemo.model.ArInvoiceListModel

class ArInvoiceAdapter(
    private val onItemClick: (item: ArInvoiceListModel.Value, position: Int) -> Unit
) : ListAdapter<ArInvoiceListModel.Value, ArInvoiceAdapter.ViewHolder>(DiffCallback()) {

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
        private val onItemClick: (item: ArInvoiceListModel.Value, position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArInvoiceListModel.Value, position: Int) = with(binding) {

            tvDocNum.text = item.DocNum
            tvCustomerName.text = item.CardName
            tvDocDate.text = GlobalMethods.formatDate(item.DocDate)

            cvListItem.setOnClickListener { onItemClick(item,adapterPosition) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ArInvoiceListModel.Value>() {
        override fun areItemsTheSame(
            oldItem: ArInvoiceListModel.Value,
            newItem: ArInvoiceListModel.Value
        ) = oldItem.DocEntry == newItem.DocEntry   // unique ID

        override fun areContentsTheSame(
            oldItem: ArInvoiceListModel.Value,
            newItem: ArInvoiceListModel.Value
        ) = oldItem == newItem
    }

    // Optional: For search filter updates
    fun submitFilteredList(filteredList: List<ArInvoiceListModel.Value>) {
        submitList(filteredList)
    }

}
