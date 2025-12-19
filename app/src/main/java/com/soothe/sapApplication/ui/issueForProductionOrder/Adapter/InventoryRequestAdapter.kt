package com.soothe.sapApplication.ui.issueForProductionOrder.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.databinding.IssueOrderListAdapterLayoutBinding
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import kotlin.collections.ArrayList

class InventoryRequestAdapter(var list: ArrayList<InventoryRequestModel.Value>): RecyclerView.Adapter<InventoryRequestAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<InventoryRequestModel.Value>, pos : Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = IssueOrderListAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
       {
        with(holder){
            with(list[position]){
                binding.tvProd.text = this.DocDate.split("T")[0]
               // binding.tvProd.text = this.DocDate.split("T")[0]
                binding.tvProductionNo.text =  this.DocNum
                binding.tvCustomer.text =  this.CardName

                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click->
                        click(list, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int
       {
        return list.size
    }


    class ViewHolder(val binding: IssueOrderListAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<InventoryRequestModel.Value>, pos: Int) -> Unit )
       {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<InventoryRequestModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }

}