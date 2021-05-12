package com.dkchoi.wetalk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.User

class FriendListAdapter(): RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {
    private var friendList: List<User> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.friends_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: User = friendList.get(position)
        holder.friendName.text = user.name
    }

    override fun getItemCount(): Int {
        return friendList.size
    }

    fun updateItem(friendList: List<User>) {
        this.friendList = friendList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById<TextView>(R.id.friend_name)
    }
}