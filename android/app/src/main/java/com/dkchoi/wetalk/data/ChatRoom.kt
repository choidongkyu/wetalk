package com.dkchoi.wetalk.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatRoom(
    @PrimaryKey
    var roomName: String, //서버에 통신위한 room name - 사용자의 번호로 구성
    var roomTitle: String, // 채팅방 제목 - 사용자의 이름으로 구성
    var lastMessage: String?, //마지막 메시지
    var chatProfileImg: String, // 채팅방 이미지
    var lastTime:String?
)
