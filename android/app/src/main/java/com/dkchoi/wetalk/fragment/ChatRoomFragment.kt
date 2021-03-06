package com.dkchoi.wetalk.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.ChatRoomAdapter
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.databinding.FragmentChatRoomBinding
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.getMyName
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel
import kotlinx.coroutines.launch

class ChatRoomFragment : Fragment() {
    private lateinit var binding: FragmentChatRoomBinding
    private val viewModel: ChatRoomViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_chat_room, container, false)
        val view = binding.root

        val chatRoomAdapter = ChatRoomAdapter()
        chatRoomAdapter.setOnItemLongClickListener(object : ChatRoomAdapter.OnItemLongClickEventListener {
            override fun onItemLongClick(view: View, pos: Int) {
                showDialog(pos)
            }

        })
        binding.recyclerView.apply {
            adapter = chatRoomAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        binding.recyclerView.addItemDecoration(RecyclerViewDecoration(40)) // ???????????? ?????? ??????
        return view
    }

    override fun onResume() {
        super.onResume()
        viewModel.getChatRooms().observe(viewLifecycleOwner, Observer {
            val adapter = binding.recyclerView.adapter as ChatRoomAdapter
            adapter.updateItem(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showDialog(position: Int) {
        val listItems = arrayOf<CharSequence>("??????", "?????????")
        val builder = AlertDialog.Builder(requireContext())
        val adapter = (binding.recyclerView.adapter as ChatRoomAdapter)
        builder.setTitle(adapter.getRoomTitle(position))
            .setItems(listItems, DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    1 -> lifecycleScope.launch {
                        viewModel.deleteRoom(adapter.getChatRoom(position))
                    }
                }
            })
        builder.show()
    }
}