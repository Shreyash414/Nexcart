package com.example.nexcart.data.local

import androidx.room.TypeConverter

/**
 * Room TypeConverter to store a List<String> as a single pipe-separated string column.
 * Empty list ↔ empty string.
 */
class StringListConverter {

    @TypeConverter
    fun fromList(list: List<String>): String =
        list.joinToString(separator = "|")

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
