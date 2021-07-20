package com.dkchoi.wetalk.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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

class InviteFriendListAdapter(private val friendList: List<User>) :
    RecyclerView.Adapter<InviteFriendListAdapter.ViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.invite_friends_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: User = friendList[position]
        holder.friendName.text = user.name
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById(R.id.friend_name)
        val friendImg: ImageView = itemView.findViewById(R.id.friend_image)
        val checkBox: CheckBox = itemView.findViewById(R.id.chkSelected)
    }
}