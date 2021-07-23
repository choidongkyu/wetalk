package com.dkchoi.wetalk.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.dkchoi.wetalk.data.ChatRoom

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM ChatRoom")
    fun getAll(): LiveData<List<ChatRoom>> // 모든 방 리스트를 가져오는 쿼리

    @Query("SELECT * FROM ChatRoom WHERE roomName = :roomName") //roomName 으로 하나의 룸만 가져오는 쿼리
    fun getRoomFromName(roomName: String): ChatRoom

    @Query("SELECT * FROM ChatRoom WHERE roomId = :id") //roomName 으로 하나의 룸만 가져오는 쿼리
    fun getRoomFromId(id: Int): ChatRoom

    @Update
    fun updateChatRoom(chatRoom: ChatRoom) // 채팅방 업데이트

    @Delete
    fun deleteChatRoom(chatRoom: ChatRoom) // 채팅방 삭제

    @Insert(onConflict = OnConflictStrategy.IGNORE) // primarykey가 중복되면 무시함
    fun insertChatRoom(chatRoom: ChatRoom) // 채팅방 생성
}