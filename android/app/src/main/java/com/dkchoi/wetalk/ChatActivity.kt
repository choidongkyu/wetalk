package com.dkchoi.wetalk

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.dkchoi.wetalk.adapter.ChatAdapter
import com.dkchoi.wetalk.data.*
import com.dkchoi.wetalk.databinding.ActivityChatBinding
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.toDate
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var chatRoom: ChatRoom

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "chatRoom-database")
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        chatRoom = intent.getParcelableExtra("chatRoom") // 전달 받은 chatroom

        adapter = ChatAdapter(chatRoom.messageDatas, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        binding.recyclerView.adapter = adapter

        binding.recyclerView.scrollToPosition(adapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함

        binding.sendBtn.setOnClickListener {
            sendMessage(binding.contentEdit.text.toString())
            val chatItem = ChatItem(
                "",
                binding.contentEdit.text.toString(),
                System.currentTimeMillis().toDate(),
                ViewType.RIGHT_MESSAGE
            )
            adapter.addItem(chatItem)
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
            binding.contentEdit.setText("")
        }
    }

    private fun sendMessage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.TEXT_MESSAGE,
            Util.getMyName(this)!!,
            request,
            System.currentTimeMillis(),
            chatRoom.roomName
        )
        val jsonMessage = gson.toJson(messageData) // message data를 json형태로 변환

        chatRoom.messageDatas = chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        lifecycleScope.launch(Dispatchers.Default) {
            db.chatRoomDao().updateChatRoom(chatRoom) //db update
        }

        val message =
            "message::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음
    }
}