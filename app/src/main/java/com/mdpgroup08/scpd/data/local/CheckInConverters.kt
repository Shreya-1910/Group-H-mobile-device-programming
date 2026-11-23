package com.mdpgroup08.scpd.data.local

import androidx.room.TypeConverter

class CheckInConverters {

    @TypeConverter
    fun fromCheckInStatus(status: CheckInStatus): String {
        return status.name
    }

    @TypeConverter
    fun toCheckInStatus(value: String): CheckInStatus {
        return CheckInStatus.valueOf(value)
    }

    @TypeConverter
    fun fromCheckInMethod(method: CheckInMethod): String {
        return method.name
    }

    @TypeConverter
    fun toCheckInMethod(value: String): CheckInMethod {
        return CheckInMethod.valueOf(value)
    }
}