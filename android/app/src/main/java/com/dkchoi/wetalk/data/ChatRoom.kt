package com.dkchoi.wetalk.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity
data class ChatRoom(
    @PrimaryKey()
    val roomId: String,
    var messageDatas: String, //채팅방에서 주고받은 대화들
    var chatProfileImg: String, // 채팅방 이미지
    var lastTime: String?,
    var unReadCount: Int = 0,
    val userList: MutableList<User>
) : Parcelable {
    lateinit var roomName: String //서버에 통신위한 room name - 사용자의 번호로 구성
    lateinit var roomTitle: String // 채팅방 제목 - 사용자의 이름으로 구성

    init {
        updateRoomInfo()
    }

    fun updateRoomInfo() { // userList에 있는 user 정보에 맞춰 roomName, roomTitle을 update해주는 메소드
        val userIds: MutableList<String> = mutableListOf()
        val userNames: MutableList<String> = mutableListOf()

        for (user in userList) {
            userIds.add(user.id)
            userNames.add(user.name)
        }
        userNames.sort() // 통일성 위한 sort
        userIds.sort()

        roomName = userIds.toString().replace(" ", "") // 공백제거
        roomName = roomName.substring(1, roomName.length - 1) //[] 괄호 삭제

        roomTitle = userNames.toString().replace(" ", "")
        roomTitle = roomTitle.substring(1, roomTitle.length - 1)

    }
}