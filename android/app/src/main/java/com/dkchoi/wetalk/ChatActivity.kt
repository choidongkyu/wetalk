package com.dkchoi.wetalk

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.dkchoi.wetalk.adapter.ChatAdapter
import com.dkchoi.wetalk.data.ChatItem
import com.dkchoi.wetalk.data.ViewType
import com.dkchoi.wetalk.databinding.ActivityChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        val chatItems: MutableList<ChatItem> = mutableListOf()
        adapter = ChatAdapter(chatItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        binding.recyclerView.adapter = adapter

        binding.sendBtn.setOnClickListener {
            val chatItem = ChatItem(
                "",
                binding.contentEdit.text.toString(),
                toDate(System.currentTimeMillis()),
                ViewType.RIGHT_MESSAGE
            )
            adapter.addItem(chatItem)
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
            binding.contentEdit.setText("")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun toDate(currentMillis: Long): String {
        return SimpleDateFormat("hh:mm a").format(Date(currentMillis))
    }

}