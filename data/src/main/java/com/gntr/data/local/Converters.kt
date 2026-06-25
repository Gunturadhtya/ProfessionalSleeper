package com.gntr.data.local

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.gntr.domain.model.SessionStatus
import com.gntr.domain.model.SessionType
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Converters {
    @RequiresApi(Build.VERSION_CODES.O)
    private val writeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC)

    @RequiresApi(Build.VERSION_CODES.O)
    private val readFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

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
    fun fromZonedDateTime(value: ZonedDateTime?): String? {
        return value?.format(writeFormatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.parse(it, readFormatter).withZoneSameInstant(ZoneId.systemDefault())
        }
    }
}