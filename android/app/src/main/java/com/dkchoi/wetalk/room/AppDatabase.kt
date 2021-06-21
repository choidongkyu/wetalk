package com.dkchoi.wetalk.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.User

@Database(entities = [User::class, ChatRoom::class], version = 1)
abstract class AppDatabase : RoomDatabase(){
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
}