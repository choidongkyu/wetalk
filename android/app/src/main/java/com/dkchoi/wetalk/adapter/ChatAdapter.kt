package com.dkchoi.wetalk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.ChatItem
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.data.ViewType
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.toDate

class ChatAdapter(private val messageDatas: String, private var context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: MutableList<ChatItem> = mutableListOf()

    init {
        if (messageDatas != "") { //기존 대화가 존재한다면
            val messageList = messageDatas.substring(0, messageDatas.length -1) //맨 끝에 '|' 제거
            addMessageDataList(messageList)
        }
    }

    private fun addMessageDataList(messageDatas: String) {
        val messages = messageDatas.split("|")
        for (data in messages) {
            val messageData = gson.fromJson(data, MessageData::class.java)
            addMessageData(messageData)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        return when (viewType) {
            ViewType.CENTER_MESSAGE -> {
                view = inflater.inflate(R.layout.chat_center_item, parent, false)
                CenterViewHolder(view)
            }
            ViewType.LEFT_MESSAGE -> {
                view = inflater.inflate(R.layout.chat_left_item, parent, false)
                LeftViewHolder(view)
            }
            ViewType.RIGHT_MESSAGE -> {
                view = inflater.inflate(R.layout.chat_right_item, parent, false)
                RightViewHolder(view)
            }
            ViewType.LEFT_IMAGE -> {
                view = inflater.inflate(R.layout.chat_left_image, parent, false)
                LeftImageViewHolder(view)
            }
            else -> {
                view = inflater.inflate(R.layout.chat_right_image, parent, false)
                RightImageViewHolder(view)
            }
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CenterViewHolder -> {
                val item = items[position]
                (holder as CenterViewHolder).setItem(item)
            }
            is LeftViewHolder -> {
                val item = items[position]
                (holder as LeftViewHolder).setItem(item, context)
            }
            is RightViewHolder -> {
                val item = items[position]
                (holder as RightViewHolder).setItem(item)
            }
            is LeftImageViewHolder -> {
                val item = items[position]
                (holder as LeftImageViewHolder).setItem(item, context)
            }
            else -> {
                val item = items[position]
                (holder as RightImageViewHolder).setItem(item, context)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    class CenterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.content_text)

        fun setItem(item: ChatItem) {
            contentText.text = item.content
        }
    }


    class LeftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameText: TextView = itemView.findViewById(R.id.name_text)
        var contentText: TextView = itemView.findViewById(R.id.msg_text)
        var sendTimeText: TextView = itemView.findViewById(R.id.send_time_text)
        var profileImg: ImageView = itemView.findViewById(R.id.profile_img)

        fun setItem(item: ChatItem, context: Context) {
            nameText.text = item.name
            contentText.text = item.content
            sendTimeText.text = item.sendTime
            Glide.with(context)
                .load("${Util.profileImgPath}/${item.id}.jpg")
                .apply(RequestOptions.circleCropTransform())
                .into(profileImg)
        }

    }

    class RightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contentText: TextView = itemView.findViewById(R.id.msg_text)
        var sendTimeText: TextView = itemView.findViewById(R.id.send_time_text)

        fun setItem(item: ChatItem) {
            contentText.text = item.content
            sendTimeText.text = item.sendTime
        }
    }

    class LeftImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameText: TextView = itemView.findViewById(R.id.name_text)
        var image: ImageView = itemView.findViewById(R.id.image_view)
        var sendTimeText: TextView = itemView.findViewById(R.id.send_time_text)
        var profileImg: ImageView = itemView.findViewById(R.id.profile_img)

        fun setItem(item: ChatItem, context: Context) {
            val option = MultiTransformation(CenterCrop(), RoundedCorners(8))
            Glide.with(context)
                .load(item.content)
                .apply(RequestOptions.bitmapTransform(option))
                .into(image)

            nameText.text = item.name
            sendTimeText.text = item.sendTime

            Glide.with(context)
                .load("${Util.profileImgPath}/${item.id}.jpg")
                .apply(RequestOptions.circleCropTransform())
                .into(profileImg)
        }

    }

    class RightImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view)
        var sendTimeText: TextView = itemView.findViewById(R.id.send_time_text)

        fun setItem(item: ChatItem, context: Context) {
            val option = MultiTransformation(CenterCrop(), RoundedCorners(8))
            Glide.with(context)
                .load(item.content)
                .apply(RequestOptions.bitmapTransform(option))
                .into(image)
            sendTimeText.text = item.sendTime
        }
    }

    fun addItem(item: ChatItem) {
        items.add(item)
        notifyDataSetChanged()
    }

    private fun addMessageData(messageData: MessageData) {
        if(messageData.type == MessageType.CENTER_MESSAGE) {
            items.add(
                ChatItem(messageData.name, messageData.id, messageData.content, messageData.sendTime.toDate(), ViewType.CENTER_MESSAGE)
            )
            return
        }

        if (Util.getMyName(context) == messageData.name) { //자기 자신이 보낸 메시지라면
            if (messageData.type == MessageType.TEXT_MESSAGE) {
                items.add(
                    ChatItem(
                        messageData.name,
                        messageData.id,
                        messageData.content,
                        messageData.sendTime.toDate(),
                        ViewType.RIGHT_MESSAGE
                    )
                )

            } else {
                items.add(
                    ChatItem(
                        messageData.name,
                        messageData.id,
                        messageData.content,
                        messageData.sendTime.toDate(),
                        ViewType.RIGHT_IMAGE
                    )
                )
            }
        } else { // 상대방이 보낸 메시지라면
            if (messageData.type == MessageType.TEXT_MESSAGE) {
                items.add(
                    ChatItem(
                        messageData.name,
                        messageData.id,
                        messageData.content,
                        messageData.sendTime.toDate(),
                        ViewType.LEFT_MESSAGE
                    )
                )

            } else {
                items.add(
                    ChatItem(
                        messageData.name,
                        messageData.id,
                        messageData.content,
                        messageData.sendTime.toDate(),
                        ViewType.LEFT_IMAGE
                    )
                )
            }
        }
    }
}