package com.dkchoi.wetalk.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.dkchoi.wetalk.data.ChatRoom

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM ChatRoom")
    fun getAll(): LiveData<List<ChatRoom>> // 모든 방 리스트를 가져오는 쿼리

    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoom) // 채팅방 업데이트

    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoom) // 채팅방 삭제

    @Insert(onConflict = OnConflictStrategy.IGNORE) // primarykey가 중복되면 무시함
    suspend fun insertChatRoom(chatRoom: ChatRoom) // 채팅방 생성
}