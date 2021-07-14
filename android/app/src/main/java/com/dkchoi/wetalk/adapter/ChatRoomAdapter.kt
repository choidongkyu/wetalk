package com.dkchoi.wetalk.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dkchoi.wetalk.ChatActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.util.Util.Companion.getMyName
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.toDate

class ChatRoomAdapter() : RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>() {
    private lateinit var context: Context
    private var chatRoomList: List<ChatRoom> = listOf()

    private var longItemClickListener: OnItemLongClickEventListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.chatroom_list_item, parent, false)
        return ViewHolder(view)
    }

    interface OnItemLongClickEventListener {
        fun onItemLongClick(view: View, pos: Int)
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickEventListener) {
        this.longItemClickListener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatRoom = chatRoomList[position]
        Glide.with(context)
            .load(chatRoom.chatProfileImg)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .into(holder.roomImage)

        val roomTitle = getRoomTitle(chatRoom.roomTitle)

        holder.roomTitle.text = roomTitle

        if (chatRoom.messageDatas != "") {
            val message: String =
                chatRoom.messageDatas.substring(0, chatRoom.messageDatas.length - 1) //맨 끝에 '|' 제거
            val messages: List<String> = message.split("|") // room에 있는 메시지 "|" 기준 parsing

            val lastDataMessage = gson.fromJson(
                messages[messages.size - 1],
                MessageData::class.java
            ) //데이터 마지막 메시지 값 구함


            holder.roomLastMessage.text = if (lastDataMessage.type == MessageType.IMAGE_MESSAGE) {
                "사진을 보냈습니다."
            } else {
                lastDataMessage.content
            }
            holder.roomLastTime.text = lastDataMessage.sendTime.toDate()


            holder.unReadCount.visibility =
                if (chatRoom.unReadCount != 0) { //읽지 않은 메시지가 0이 아니라면
                    View.VISIBLE
                } else {
                    View.GONE
                }
            holder.unReadCount.text = chatRoom.unReadCount.toString()
        }
    }

    override fun getItemCount(): Int {
        return chatRoomList.size
    }

    fun getRoomTitle(pos: Int): String {
        return getRoomTitle(chatRoomList[pos].roomTitle)
    }

    fun getChatRoom(pos: Int): ChatRoom {
        return chatRoomList[pos]
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
        val unReadCount: TextView = itemView.findViewById(R.id.unread_num)

        init {
            itemView.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                val chatRoom: ChatRoom = chatRoomList[adapterPosition]
                intent.putExtra("chatRoom", chatRoom.roomName)
                context.startActivity(intent)
            }

            itemView.setOnLongClickListener {
                longItemClickListener?.onItemLongClick(it, adapterPosition)
                return@setOnLongClickListener true
            }
        }
    }

    private fun getRoomTitle(roomTitle: String): String { // roomtitle 파싱하여 반환해주는 메소드
        //방이름은 자신을 제외한 상대방의 이름으로 구성
        val names = roomTitle.split(",") //방 제목은 최동규,채혜인,에뮬레이터 식으로 구성되므로 , 기준으로 파싱
        var result = ""
        for (name in names) {
            if (name == getMyName(context)) { //자신의 이름일 경우 건너 뜀
                continue
            }
            result += "$name,"
        }

        result = result.substring(0, result.length - 1)//마지막 , 제거
        return result
    }
}
