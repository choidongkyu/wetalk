package com.dkchoi.wetalk.room

import android.content.Context
import androidx.room.*
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.util.Converters

@Database(entities = [User::class, ChatRoom::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao

    companion object {
        private var chatRoomInstance: AppDatabase? = null
        private var userInstance: AppDatabase? = null

        fun getInstance(context: Context, db: String): AppDatabase? {
            if(db == "user-database") {
                if (userInstance == null) {
                    synchronized(AppDatabase::class) {
                        userInstance = Room.databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            db
                        ).allowMainThreadQueries().build()
                    }
                }
                return userInstance
            } else {
                if (chatRoomInstance == null) {
                    synchronized(AppDatabase::class) {
                        chatRoomInstance = Room.databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            db
                        ).allowMainThreadQueries().build()
                    }
                }
                return chatRoomInstance
            }

        }
    }

}