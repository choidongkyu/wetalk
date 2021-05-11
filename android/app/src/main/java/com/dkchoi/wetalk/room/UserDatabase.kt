package com.dkchoi.wetalk.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dkchoi.wetalk.data.User

@Database(entities = [User::class], version = 1)
abstract class UserDatabase : RoomDatabase(){
    abstract fun userDao(): UserDao
}