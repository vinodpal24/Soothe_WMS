package com.soothe.sapApplication.Global_Classes

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.soothe.sapApplication.ui.invoiceOrder.Model.InvoiceListModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel
import com.soothe.sapApplication.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import es.dmoral.toasty.Toasty
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object GlobalMethods {
    const val THRESHOLD_25_MINUTES = 25 * 60 * 1000


    fun setScanDataOnItem(arrayList: ArrayList<InvoiceListModel.DocumentLine>, itemCode: String): Int
    {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is InvoiceListModel.DocumentLine && item.ItemCode == itemCode) {
                position = index
                break
            }
        }
        return position

    }

     fun findItemPosition(
        list: List<InventoryRequestModel.StockTransferLines>,
        itemCode: String
    ): Int {
        return list.indexOfFirst { it.ItemCode == itemCode }
    }



    open fun showMessage(context: Context?, message: String?) {
        Toasty.warning(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    open fun showError(context: Context?, message: String?) {
        Toasty.error(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    open fun showSuccess(context: Context?, message: String?){
        Toasty.success(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    //todo remove digits after decimal..
    open fun changeDecimal(input: String): String? {
        val df = DecimalFormat("#.###")
        return df.format(input.toDouble())
    }

    open fun sumBatchQuantity(position: Int, quantityHashMap: ArrayList<String>):Double {
        var quantity = 0.000
        //TODO sum of order line batch quantities and store it in open quantity..
        var batchQuantityList: ArrayList<String>
//        batchQuantityList = quantityHashMap.get("Item" + position)!!
        batchQuantityList = quantityHashMap
        for (i in 0 until batchQuantityList.size) {
            var temp = batchQuantityList[i].toDouble()
            quantity += temp
        }
        return quantity
    }

    open fun sumBatchGrossWeight(position: Int, valueArrayList: ArrayList<ScanedOrderBatchedItems.Value>):Double {
        var quantity = 0.000
        //TODO sum of order line batch quantities and store it in open quantity..
        var batchQuantityList : ArrayList<ScanedOrderBatchedItems.Value>
        batchQuantityList = valueArrayList
        for (i in 0 until batchQuantityList.size) {
            var temp = batchQuantityList[i].U_GW
            quantity += temp
        }
        return quantity
    }

    fun toWholeInt(value: String?): Int =
        value?.toDoubleOrNull()?.roundToInt() ?: 0


    /*open fun numberToK(number: String): String? {
        var number = number
        if (number.isEmpty()) number = "0"

        // DecimalFormat df = new DecimalFormat("0.00");
        val df = DecimalFormat("0")
        val amount = df.format(number.toDouble()).toDouble()

        return amount.toString().split(".")[0]
    }*/

    fun numberToK(number: String?): String {
        return try {
            number?.toDouble()?.roundToInt()?.toString() ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }



    open fun getCurrentTodayDate() : String{
        // Get the current date
        val currentDate: LocalDate = LocalDate.now()

        // Define the date format
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // Format the current date
        val formattedDate: String = currentDate.format(formatter)

        // Print the formatted date
//        println("Current Date (yyyy-MM-dd): $formattedDate")
        return formattedDate
    }


    open fun convert_yyyy_mm_dd_T_into_dd_mm_yyyy(input : String): String {
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val inputDate = inputDateFormat.parse(input)

        val outputDateStr = outputDateFormat.format(inputDate)
        return outputDateStr
    }

    @SuppressLint("NewApi")
    fun formatDate(dateTime: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return LocalDateTime.parse(dateTime, inputFormatter)
            .format(outputFormatter)
    }

}