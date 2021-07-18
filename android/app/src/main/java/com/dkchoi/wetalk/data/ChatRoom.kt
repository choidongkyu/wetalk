package com.dkchoi.wetalk.data
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class ChatRoom(
    @PrimaryKey
    var roomName: String, //서버에 통신위한 room name - 사용자의 번호로 구성
    var roomTitle: String, // 채팅방 제목 - 사용자의 이름으로 구성
    var messageDatas: String, //채팅방에서 주고받은 대화들
    var chatProfileImg: String, // 채팅방 이미지
    var lastTime:String?,
    var unReadCount:Int = 0,
    val userList: MutableList<User>
) : Parcelable