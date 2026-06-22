package com.gntr.professionalsleeper.data.local

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.gntr.professionalsleeper.data.local.entity.SessionStatus
import com.gntr.professionalsleeper.data.local.entity.SessionType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Converters {
    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    @TypeConverter
    fun fromSessionType(value: SessionType): String = value.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = enumValueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = enumValueOf(value)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime?): String? = value?.format(formatter)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? = value?.let { ZonedDateTime.parse(it, formatter) }
}