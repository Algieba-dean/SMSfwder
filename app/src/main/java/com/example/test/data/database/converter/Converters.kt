package com.example.test.data.database.converter

import androidx.room.TypeConverter
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.MatchType
import com.example.test.domain.model.RuleType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromForwardStatus(status: ForwardStatus): String {
        return status.name
    }

    @TypeConverter
    fun toForwardStatus(status: String): ForwardStatus {
        return ForwardStatus.valueOf(status)
    }

    @TypeConverter
    fun fromRuleType(type: RuleType): String {
        return type.name
    }

    @TypeConverter
    fun toRuleType(type: String): RuleType {
        return RuleType.valueOf(type)
    }

    @TypeConverter
    fun fromMatchType(type: MatchType): String {
        return type.name
    }

    @TypeConverter
    fun toMatchType(type: String): MatchType {
        return MatchType.valueOf(type)
    }
} 