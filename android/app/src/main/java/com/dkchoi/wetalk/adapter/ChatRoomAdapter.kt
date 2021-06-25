package com.dkchoi.wetalk.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dkchoi.wetalk.ChatActivity
import com.dkchoi.wetalk.ProfileActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.toDate
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel
import com.google.gson.Gson

class ChatRoomAdapter(): RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>() {
    private lateinit var context: Context
    private var chatRoomList: List<ChatRoom> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.chatroom_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatRoom = chatRoomList[position]
        Glide.with(context)
            .load(chatRoom.chatProfileImg)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .into(holder.roomImage)

        holder.roomTitle.text = chatRoom.roomTitle

        if(chatRoom.messageDatas != "") {
            val message: String = chatRoom.messageDatas.substring(0, chatRoom.messageDatas.length -1) //맨 끝에 '|' 제거
            val messages:List<String> = message.split("|") // room에 있는 메시지 "|" 기준 parsing

            val lastDataMessage = gson.fromJson(messages[messages.size -1], MessageData::class.java) //데이터 마지막 메시지 값 구함

            holder.roomLastMessage.text = lastDataMessage.content
            holder.roomLastTime.text = lastDataMessage.sendTime.toDate()
        }
    }

    override fun getItemCount(): Int {
        return chatRoomList.size
    }

    fun updateItem(chatRoomList: List<ChatRoom>) {
        this.chatRoomList = chatRoomList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomImage: ImageView = itemView.findViewById(R.id.room_image)
        val roomTitle: TextView = itemView.findViewById(R.id.room_title)
        val roomLastMessage: TextView = itemView.findViewById(R.id.last_message)
        val roomLastTime: TextView = itemView.findViewById(R.id.last_time)
        init {
            itemView.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                val chatRoom: ChatRoom = chatRoomList[adapterPosition]
                intent.putExtra("chatRoom", chatRoom)
                context.startActivity(intent)
            }
        }
    }
}
