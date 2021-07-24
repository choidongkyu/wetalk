package com.dkchoi.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.dkchoi.wetalk.adapter.InviteFriendListAdapter
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityInviteBinding
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class InviteActivity : AppCompatActivity() {

    private val userDb: AppDatabase? by lazy {
        AppDatabase.getInstance(this, "user-database")
    }

    private val chatRoomDb: AppDatabase? by lazy {
        AppDatabase.getInstance(this, "chatRoom-database")
    }

    private lateinit var chatRoom: ChatRoom

    lateinit var binding: ActivityInviteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_invite)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val friendList = userDb?.userDao()?.getList() // 친구목록

        //초대 친구 리스트
        friendList?.let {
            val inviteFriendListAdapter = InviteFriendListAdapter(it)
            binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            binding.recyclerView.adapter = inviteFriendListAdapter
            binding.recyclerView.addItemDecoration(RecyclerViewDecoration(40)) // 아이템간 간격 설정
        }

        val chatRoomId = intent.getStringExtra("chatRoomId") // 전달 받은 chatroom
        chatRoom = chatRoomDb?.chatRoomDao()?.getRoomFromId(chatRoomId)!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_confirm -> {
                val list = (binding.recyclerView.adapter as InviteFriendListAdapter).getCheckedList()
                Handler().postDelayed({
                    inviteUser(list)
                },1000)
                finish()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.invite_menu, menu)
        return true
    }

    private fun inviteUser(list: MutableList<User>) {
        chatRoom.userList.addAll(list)
        chatRoom.updateRoomInfo()// userlist가 바뀜에 따라 roomName, roomTitle도 바뀌어야 하므로 updateRoomInfo

        //socket으로 메시지 send
        Thread(Runnable {
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            for(user in list) {
                val inviteMessage = makeInviteMessage("${Util.getMyName(this)}님이 ${user.name}님을 초대하였습니다.")
                val messageData = Util.gson.toJson(inviteMessage) // message data를 json형태로 변환
                chatRoom.messageDatas =
                    chatRoom.messageDatas + messageData + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌
                chatRoomDb?.chatRoomDao()?.updateChatRoom(chatRoom) //로컬db에 메시지 저장
                val message = "invite::${chatRoom.roomName}::${user.id}::${messageData}"
                pw.println(message)
            }
        }).start()
    }

    private fun makeInviteMessage(request: String): MessageData {
        return MessageData(
            MessageType.CENTER_MESSAGE,
            Util.getMyName(this)!!,
            Util.getPhoneNumber(this),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle,
            chatRoom.roomId
        )
    }
}