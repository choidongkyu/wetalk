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
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.ProfileActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.util.Util

class FriendListAdapter() : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {
    private var friendList: List<User> = listOf()
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.friends_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: User = friendList.get(position)
        holder.friendName.text = user.name

        holder.friendStatus.text =
            if (user.profileText != null && !user.profileText.equals("")) user.profileText
            else "상태메시지"

        val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
        val imgKey = if (user.profileImage != null) {
            user.profileImage
        } else {
            "null"
        } //프로파일 이미지가 null 일 경우 text "null"을 glide key로 설정
        Glide.with(context)
            .load(imgPath)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(imgKey!!))
            .into(holder.friendImg)

    }

    override fun getItemCount(): Int {
        return friendList.size
    }

    fun updateItem(friendList: List<User>) {
        this.friendList = friendList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById(R.id.friend_name)
        val friendStatus: TextView = itemView.findViewById(R.id.friend_status)
        val friendImg: ImageView = itemView.findViewById(R.id.friend_image)

        init {
            itemView.setOnClickListener {
                val intent = Intent(context, ProfileActivity::class.java)
                val user: User = friendList[adapterPosition]
                intent.putExtra("user", user)
                context.startActivity(intent)
            }
        }

    }
}