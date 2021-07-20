package com.dkchoi.wetalk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.dkchoi.wetalk.adapter.ChatRoomAdapter
import com.dkchoi.wetalk.adapter.InviteFriendListAdapter
import com.dkchoi.wetalk.adapter.RoomFriendListAdapter
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.databinding.ActivityInviteBinding
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.util.RecyclerViewDecoration

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

        val chatRoomName = intent.getStringExtra("chatRoom") // 전달 받은 chatroom
        chatRoom = chatRoomDb?.chatRoomDao()?.getRoom(chatRoomName)!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_confirm -> {
                val list = (binding.recyclerView.adapter as InviteFriendListAdapter).getCheckedList()
                chatRoom.userList.addAll(list)
                chatRoomDb?.chatRoomDao()?.updateChatRoom(chatRoom)
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("chatRoom", chatRoom.roomName)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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