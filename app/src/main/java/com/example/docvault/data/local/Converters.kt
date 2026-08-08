package com.example.docvault.data.local

import androidx.room.TypeConverter
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.domain.model.HistoryActionType

/**
 * Type converters for Room to handle non-primitive types.
 *
 * Converts custom Enums and Lists to String for storage and back.
 */
class Converters {
    /** Converts [DocumentCategory] to its String name. */
    @TypeConverter
    fun fromCategory(category: DocumentCategory): String = category.name

    /** Converts a String back to [DocumentCategory]. */
    @TypeConverter
    fun toCategory(category: String): DocumentCategory = DocumentCategory.valueOf(category)

    /** Converts [HistoryActionType] to its String name. */
    @TypeConverter
    fun fromHistoryActionType(actionType: HistoryActionType): String = actionType.name

    /** Converts a String back to [HistoryActionType]. */
    @TypeConverter
    fun toHistoryActionType(actionType: String): HistoryActionType = HistoryActionType.valueOf(actionType)

    /** Converts a list of strings to a single comma-separated string. */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    /** Converts a comma-separated string back to a list of strings. */
    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
}
