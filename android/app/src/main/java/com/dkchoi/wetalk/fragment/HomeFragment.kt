package com.dkchoi.wetalk.fragment

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.FriendListAdapter
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util

class HomeFragment : Fragment() {
    lateinit var mContext: Context
    lateinit var friendList: MutableList<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phoneBooks: MutableList<PhoneBook> = Util.getContacts(mContext) //전화번호부 가져옴
        friendList = mutableListOf()
        for(phoneBook in phoneBooks) {
            phoneBook.name?.let { friendList.add(it) }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val friendNumber: TextView = view.findViewById(R.id.friend_number)
        friendNumber.text = "${friendList.size}명"
        val friendRecyclerView: RecyclerView = view.findViewById(R.id.friend_recyclerView)
        val friendListAdapter: FriendListAdapter = FriendListAdapter(friendList, mContext)
        friendRecyclerView.adapter = friendListAdapter
        val linearLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        friendRecyclerView.layoutManager = linearLayoutManager

        friendRecyclerView.addItemDecoration(RecyclerViewDecoration(30)) // 아이템간 간격 설정
        return view
    }
}