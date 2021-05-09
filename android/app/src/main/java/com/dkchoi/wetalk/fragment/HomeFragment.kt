package com.dkchoi.wetalk.fragment

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.Observable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.FriendListAdapter
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.viewmodel.FriendListViewModel

class HomeFragment : Fragment() {
    private lateinit var mContext: Context
    private val friendListViewModel: FriendListViewModel by viewModels() //친구목록 viewmodel 생성


    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val friendNumber: TextView = view.findViewById(R.id.friend_number)
        friendNumber.text = "${friendListViewModel.friendListLiveData.value?.size}명"
        val friendRecyclerView: RecyclerView = view.findViewById(R.id.friend_recyclerView)
        val friendListAdapter = FriendListAdapter()
        friendRecyclerView.apply {
            adapter = friendListAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        friendListViewModel.apply {
            friendListLiveData.observe(requireActivity(), Observer {
                Log.d("test11", "observe!!" )
                friendListAdapter.updateItem(it)
            })
        }

        friendRecyclerView.addItemDecoration(RecyclerViewDecoration(30)) // 아이템간 간격 설정
        return view
    }
}
