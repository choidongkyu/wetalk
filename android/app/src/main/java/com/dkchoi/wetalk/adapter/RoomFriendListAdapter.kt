package com.dkchoi.wetalk.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.InviteActivity
import com.dkchoi.wetalk.ProfileActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.util.Util

class RoomFriendListAdapter(private val userList: MutableList<User>) : RecyclerView.Adapter<RoomFriendListAdapter.ViewHolder>() {
    private lateinit var context: Context
    private val friendList: MutableList<User> = mutableListOf<User>()
    private var itemClickListener: OnItemClickEventListener? = null

    init {
        val inviteUser = User("0", null, null, "대화상대 초대") // 대화상대 초대 위한 dummy data
        friendList.add(inviteUser)
        friendList.addAll(userList)
    }

    interface OnItemClickEventListener {
        fun onItemClick(view: View, pos: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickEventListener) {
        this.itemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.conversation_list_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: User = friendList[position]
        val friendName = if (user.id == Util.getPhoneNumber(context)) { //자신인 경우 이름옆에 (나) 추가
            "${user.name} <b>(나)</b>"
        } else {
            user.name
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.friendName.text = Html.fromHtml(friendName, Html.FROM_HTML_MODE_LEGACY)
        } else {
            holder.friendName.text = Html.fromHtml(friendName)
        }
        val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
        val imgKey = if (user.profileImage != null) {
            user.profileImage
        } else {
            "null"
        } //프로파일 이미지가 null 일 경우 text "null"을 glide key로 설정

        if(position == 0) { // 대화상대추가 아이템인 경우
            holder.friendImg.background = context.getDrawable(R.drawable.ic_baseline_add_circle_24)
            return
        }

        Glide.with(context)
            .load(imgPath)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(imgKey!!))
            .into(holder.friendImg)
    }

    override fun getItemCount(): Int {
        return friendList.size
    }

    fun getList(): MutableList<User> {
        return friendList
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById(R.id.friend_name)
        val friendImg: ImageView = itemView.findViewById(R.id.friend_image)

        init {
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(it, adapterPosition)
                return@setOnClickListener
            }
        }
    }
}