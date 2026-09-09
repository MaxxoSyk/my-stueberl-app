package de.landstueberl.mystueberlapp.data.converter

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.util.Currency

class MoneyConverters {

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? =
        value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? =
        value?.let { BigDecimal(it) }

    @TypeConverter
    fun currencyToString(currency: Currency?): String? =
        currency?.currencyCode

    @TypeConverter
    fun stringToCurrency(code: String?): Currency? =
        code?.let { Currency.getInstance(it) }

}