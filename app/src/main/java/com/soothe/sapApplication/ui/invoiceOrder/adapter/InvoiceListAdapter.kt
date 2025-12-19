package com.soothe.sapApplication.ui.invoiceOrder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.databinding.IssueOrderListAdapterLayoutBinding
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import java.lang.ref.WeakReference

class InvoiceListAdapter(var list: ArrayList<InvoiceListModel.Value>): RecyclerView.Adapter<InvoiceListAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<InvoiceListModel.Value>, pos : Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = IssueOrderListAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val weakReference = WeakReference(list[position])
        val myObject = weakReference.get()
        with(holder) {
            with(myObject) {
                binding.docNum.text = "Doc Num   : "
                binding.tvProd.text = this?.DocNum
                binding.docEntry.text = "Doc Entry   : "
                binding.tvProductionNo.text = this?.DocEntry
                binding.tvCustomer.text = this?.CardName


                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click->
                        click(list, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(val binding: IssueOrderListAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<InvoiceListModel.Value>, pos: Int) -> Unit ) {
        onItemClickListener = listener
    }


}