package com.dkchoi.wetalk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.dkchoi.wetalk.adapter.ChatAdapter
import com.dkchoi.wetalk.data.*
import com.dkchoi.wetalk.databinding.ActivityChatBinding
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.JOIN_KEY
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_IP
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_PORT
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.getMyName
import com.dkchoi.wetalk.util.Util.Companion.getPhoneNumber
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class ChatActivity : AppCompatActivity(), SocketReceiveService.IReceiveListener {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var chatRoom: ChatRoom

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "chatRoom-database")
            .build()
    }

    //갤러리에서 사진 선택 후 불리는 result activity
    private val requestActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data?.data != null) { //갤러리 캡쳐 결과값
                val selectedImageUri = it.data!!.data

                val chatItem = ChatItem(
                    "",
                    "",
                    selectedImageUri.toString(),
                    System.currentTimeMillis().toDate(),
                    ViewType.RIGHT_IMAGE
                )
                adapter.addItem(chatItem)
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
                binding.contentEdit.setText("")
            }
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
                "",
                binding.contentEdit.text.toString(),
                System.currentTimeMillis().toDate(),
                ViewType.RIGHT_MESSAGE
            )
            adapter.addItem(chatItem)
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
            binding.contentEdit.setText("")
        }

        binding.imageBtn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            requestActivity.launch(intent)
        }

        with(NotificationManagerCompat.from(this)) { // 채팅방 들어갈때 notification 메시지 삭제
            cancel(1002)
        }

        HomeActivity.service?.registerListener(this@ChatActivity)

//        val user = getMyUser(this)
//        MainReceiveThread.getInstance(user)
//            .setListener(this)// 소켓으로 메시지가 들어온다면 chatactivity가 받을수 있도록 리스너 설정
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        HomeActivity.service?.registerListener(null)
    }

    private fun sendImage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.IMAGE_MESSAGE,
            Util.getMyName(this)!!,
            getPhoneNumber(this),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle
        )
        val jsonMessage = gson.toJson(messageData) // message data를 json형태로 변환


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        lifecycleScope.launch(Dispatchers.Default) {
            db.chatRoomDao().updateChatRoom(chatRoom) //로컬db에 메시지 저장
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음

        //socket으로 메시지 send
        Thread(Runnable {
            val socket =
                Socket(SERVER_IP, SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }).start()
    }

    private fun sendMessage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.TEXT_MESSAGE,
            Util.getMyName(this)!!,
            getPhoneNumber(this),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle
        )
        val jsonMessage = gson.toJson(messageData) // message data를 json형태로 변환


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        lifecycleScope.launch(Dispatchers.Default) {
            db.chatRoomDao().updateChatRoom(chatRoom) //로컬db에 메시지 저장
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음

        //socket으로 메시지 send
        Thread(Runnable {
            val socket =
                Socket(SERVER_IP, SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }).start()
    }

    override fun onReceive(msg: String) {
        runOnUiThread {
            var message = msg.replace("\r\n", "")
            if (message.contains(JOIN_KEY)) { // join_key가 있다면 유저 입장 or 퇴장 메시지
                message = message.replace(JOIN_KEY, "") // 조인키 삭제
                adapter.addItem(
                    ChatItem(
                        "",
                        "",
                        message,
                        "",
                        ViewType.CENTER_MESSAGE
                    )
                )
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
            } else {
                saveMsgToLocalDB(message)
            }
        }
    }

    //상대방이 메시지 보낼 경우
    private fun addChat(messageData: MessageData) {
        if (messageData.type == MessageType.TEXT_MESSAGE) {
            adapter.addItem(
                ChatItem(
                    messageData.name,
                    messageData.id,
                    messageData.content,
                    messageData.sendTime.toDate(),
                    ViewType.LEFT_MESSAGE
                )
            )
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
        } else {
            adapter.addItem(
                ChatItem(
                    messageData.name,
                    messageData.id,
                    messageData.content,
                    messageData.sendTime.toDate(),
                    ViewType.LEFT_IMAGE
                )
            )
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun saveMsgToLocalDB(message: String) {
        val messageData: MessageData = gson.fromJson(message, MessageData::class.java)
        if (messageData.name.equals(getMyName(this))) return // 자기 자신이 보낸 메시지도 소켓으로 통해 들어오므로 필터링
        lifecycleScope.launch(Dispatchers.Default) {
            if (db.chatRoomDao()
                    .getRoom(messageData.roomName) == null
            ) { // 로컬 db에 존재하는 방이 없다면
                var userId = ""
                val users = messageData.roomName.split(",") //room name에 포함된 userid 파싱
                for (user in users) {
                    if (user != Util.getPhoneNumber(applicationContext)) {//자신이 아닌 다른 user의 프로필 사진으로 채팅방 구성
                        userId = user
                        break
                    }
                }
                val imgPath = "${Util.profileImgPath}/${userId}.jpg"
                val chatRoom =
                    ChatRoom(
                        messageData.roomName,
                        messageData.roomTitle,
                        "$message|",
                        imgPath,
                        null
                    ) //adapter에서 끝에 '|' 문자를 제거하므로 |를 붙여줌 안붙인다면 괄호가 삭제되는 있으므로 | 붙여줌


                db.chatRoomDao().insertChatRoom(chatRoom)
            } else { //기존에 방이 존재한다면
                val chatRoom = db.chatRoomDao().getRoom(messageData.roomName)
                //chatroom에 메시지 추가
                chatRoom.messageDatas =
                    chatRoom.messageDatas + message + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

                db.chatRoomDao().updateChatRoom(chatRoom)
            }
        }
        if (messageData.roomName == chatRoom.roomName) { // 현재 activity에 있는 방과 소켓으로 들어온 메시지의 room이 같다면 ui에 추가
            addChat(messageData) // 리사이클러뷰에 추가
        }

    }
}