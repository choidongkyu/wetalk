package com.dkchoi.wetalk.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.User

@Database(entities = [ChatRoom::class], version = 1)
abstract class ChatRoomDatabase: RoomDatabase() {
    abstract fun chatRoomDao(): ChatRoomDao
}