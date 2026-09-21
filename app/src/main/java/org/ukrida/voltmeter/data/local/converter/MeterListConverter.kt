package org.ukrida.voltmeter.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ukrida.voltmeter.data.local.entity.MeterDto

class MeterListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMeterList(meters: List<MeterDto>): String {
        return gson.toJson(meters)
    }

    @TypeConverter
    fun toMeterList(metersJson: String): List<MeterDto> {
        val type = object : TypeToken<List<MeterDto>>() {}.type
        return gson.fromJson(metersJson, type)
    }
}
