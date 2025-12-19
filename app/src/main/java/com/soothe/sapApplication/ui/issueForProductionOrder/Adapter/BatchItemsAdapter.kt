package com.soothe.sapApplication.ui.issueForProductionOrder.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.soothe.sapApplication.databinding.BatchItemsScannedLayoutBinding

class BatchItemsAdapter(private val context: Context, private val scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>,
                        private  val quantityHashMap: ArrayList<String> , private val flag : String) : RecyclerView.Adapter<BatchItemsAdapter.ViewHolder>() {


    private var onDeleteItemClick: OnDeleteItemClickListener? = null
    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, pos: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(scanedBatchedItemsList_gl[position]) {
                binding.tvBatch.text = ":   "+ this.Batch
                binding.tvDocEntry.text = ":   "+ this.DocEntry
                binding.tvItemCode.text = ":   "+ this.ItemCode
                binding.tvItemDesc.text = ":   "+ this.ItemDescription
                binding.tvWidth.text = ":   "+ this.U_Width.toString()
                binding.tvBatchLength.text = ":   "+this.U_Length.toString()
                binding.tvBatchGsm.text = ":   "+this.U_GSM.toString()
                binding.tvBatchGrossWeigth.text = ":   "+this.U_GW.toString()

                binding.ivDelete.setOnClickListener {
                    onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, quantityHashMap, position)
                }

                with(quantityHashMap[position]){
                    binding.tvBatchQuantity.text = ":   "+this
                }


            }
        }
    }

    override fun getItemCount(): Int {
        return scanedBatchedItemsList_gl.size
    }

    class ViewHolder(val binding: BatchItemsScannedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }

}