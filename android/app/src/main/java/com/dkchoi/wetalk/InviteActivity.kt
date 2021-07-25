package com.dkchoi.wetalk

import android.content.Intent
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
                //inviteUser(list)
                val intent = Intent(this, ChatActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("inviteList", list)
                intent.putExtra("chatRoomId", chatRoom.roomId)
                startActivity(intent)
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
}
