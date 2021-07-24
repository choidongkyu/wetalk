package com.dkchoi.wetalk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.room.AppDatabase

class ChatRoomViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application, "chatRoom-database")


    fun getChatRooms(): LiveData<List<ChatRoom>> {
        return db!!.chatRoomDao().getAll()
    }

    fun getChatRoom(id: String): ChatRoom {
        return db!!.chatRoomDao().getRoomFromId(id)
    }

    suspend fun insertRoom(chatRoom: ChatRoom) {
        db!!.chatRoomDao().insertChatRoom(chatRoom)
    }

    suspend fun deleteRoom(chatRoom: ChatRoom) {
        db!!.chatRoomDao().deleteChatRoom(chatRoom)
    }

    suspend fun updateRoom(chatRoom: ChatRoom) {
        db!!.chatRoomDao().updateChatRoom(chatRoom)
    }
}