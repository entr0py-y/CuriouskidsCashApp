package com.curiouskids.cashapp.core.common

import java.text.NumberFormat
import java.util.Locale

fun formatPaiseToRupees(paise: Long): String {
    val rupees = paise / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(rupees)
}
