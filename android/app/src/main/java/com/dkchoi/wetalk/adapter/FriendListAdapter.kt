package com.dkchoi.wetalk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dkchoi.wetalk.R

class FriendListAdapter(private val friendList: MutableList<String>, private val context: Context): RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view: View = LayoutInflater.from(parent.context).inflate(R.layout.friends_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var name: String = friendList.get(position)
        holder.friendName.text = name
    }

    override fun getItemCount(): Int {
        return friendList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val friendName: TextView = itemView.findViewById<TextView>(R.id.friend_name)
    }
}