package com.dkchoi.wetalk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class DirectReplyReceiver : BroadcastReceiver() {
    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    private lateinit var context: Context
    private lateinit var chatRoom: ChatRoom
    private val db: AppDatabase? by lazy {
        AppDatabase.getInstance(context, "chatRoom-database")
    }

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        val chatRoomId = intent.getStringExtra("chatRoomId") // 전달 받은 chatroom name
        db?.let { db ->
            chatRoom = db.chatRoomDao().getRoomFromId(chatRoomId)
        }
        remoteInput?.let {
            val replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY)
            sendMessage(replyText.toString())
            with(NotificationManagerCompat.from(context)) { // notification 메시지 삭제
                cancel(1002)
            }
        }
    }

    private fun sendMessage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.TEXT_MESSAGE,
            Util.getMyName(context)!!,
            Util.getPhoneNumber(context),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle,
            chatRoom.roomId
        )
        val jsonMessage = Util.gson.toJson(messageData) // message data를 json형태로 변환


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        chatRoom.unReadCount = 0

        //socket으로 메시지 send
        Thread(Runnable {
            db?.chatRoomDao()?.updateChatRoom(chatRoom) //로컬db에 메시지 저장
            val message =
                "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }).start()
    }
}