package com.dkchoi.wetalk.util

import android.content.Context
import android.content.SharedPreferences

class Util {
    companion object {
        fun setSession(id: String, context: Context) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(
                    Config.SESSION,
                    Context.MODE_PRIVATE
                ) //session에 관련된 pref를 얻어옴

            val editor = sharedPreferences.edit()
            editor.putString(Config.SESSION_KEY, id) //id을 sharedPref에 저장
            editor.apply()
        }

        fun getSession(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(Config.SESSION, Context.MODE_PRIVATE)
            return sharedPreferences.getString(Config.SESSION_KEY, null)
        }
    }
}