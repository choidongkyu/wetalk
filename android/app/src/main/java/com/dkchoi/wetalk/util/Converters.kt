package com.dkchoi.wetalk.util

import androidx.room.TypeConverter
import com.dkchoi.wetalk.data.User
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun listToJson(value: MutableList<User>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToList(value: String): MutableList<User> {
        return Gson().fromJson(value, Array<User>::class.java).toMutableList()
    }
}