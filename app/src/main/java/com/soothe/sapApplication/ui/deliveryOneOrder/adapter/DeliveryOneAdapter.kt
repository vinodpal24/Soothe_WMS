package com.soothe.sapApplication.ui.deliveryOneOrder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.Global_Classes.GlobalMethods
import com.soothe.sapApplication.databinding.IssueOrderListAdapterLayoutBinding
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import java.lang.ref.WeakReference

class DeliveryOneAdapter (var list: ArrayList<InvoiceListModel.Value>): RecyclerView.Adapter<DeliveryOneAdapter.ViewHolder>() {

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
                binding.docNum.text = "Delivery No.  : "
                binding.tvProd.text = this?.DocNum

                //todo hide doc entry text--
                binding.docNumLayout.visibility = View.GONE
                binding.docEntry.text = "Doc Entry   : "
                binding.tvProductionNo.text = this?.DocEntry

                //todo view delivery date--
                binding.deliveryDateLayout.visibility = View.VISIBLE
                binding.tvDeliveryDate.text = GlobalMethods.convert_yyyy_mm_dd_T_into_dd_mm_yyyy(this!!.DocDate)

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


    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<InvoiceListModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }



}