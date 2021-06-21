package com.dkchoi.wetalk.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.ChatRoomAdapter
import com.dkchoi.wetalk.databinding.FragmentChatRoomBinding
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel

class ChatRoomFragment : Fragment() {
    private lateinit var binding: FragmentChatRoomBinding
    val viewModel: ChatRoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_chat_room, container, false)
        val view = binding.root

        val chatRoomAdapter = ChatRoomAdapter()
        binding.recyclerView.apply {
            adapter = chatRoomAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewModel.getChatRooms().observe(viewLifecycleOwner, Observer {
            chatRoomAdapter.updateItem(it)
        })

        binding.recyclerView.addItemDecoration(RecyclerViewDecoration(40)) // 아이템간 간격 설정
        return view
    }
}