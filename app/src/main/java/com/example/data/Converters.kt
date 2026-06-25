package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromActivityLogList(value: List<ActivityLogEntry>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ActivityLogEntry::class.java)
        val adapter = moshi.adapter<List<ActivityLogEntry>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toActivityLogList(value: String?): List<ActivityLogEntry> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ActivityLogEntry::class.java)
        val adapter = moshi.adapter<List<ActivityLogEntry>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromExtensionRequestList(value: List<ExtensionRequest>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ExtensionRequest::class.java)
        val adapter = moshi.adapter<List<ExtensionRequest>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toExtensionRequestList(value: String?): List<ExtensionRequest> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ExtensionRequest::class.java)
        val adapter = moshi.adapter<List<ExtensionRequest>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }
}
