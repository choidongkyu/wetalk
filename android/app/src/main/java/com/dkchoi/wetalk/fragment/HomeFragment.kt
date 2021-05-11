package com.dkchoi.wetalk.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.FriendListAdapter
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.room.UserDatabase
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.viewmodel.HomeFragmentViewModel
import kotlinx.coroutines.*

class HomeFragment : Fragment() {
    private val TAG: String = javaClass.simpleName
    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels() //친구목록 viewmodel 생성


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
        val myName: TextView = view.findViewById(R.id.myProfileName)
        val friendRecyclerView: RecyclerView = view.findViewById(R.id.friend_recyclerView)
        val friendListAdapter = FriendListAdapter()
        friendRecyclerView.apply {
            adapter = friendListAdapter
            layoutManager = LinearLayoutManager(activity)
        }


        homeFragmentViewModel.apply {
            friendListLiveData.observe(viewLifecycleOwner, Observer {
                friendListAdapter.updateItem(it)
                friendNumber.text = "${homeFragmentViewModel.friendListLiveData.value?.size}명"
            })
            myNameLiveData.observe(viewLifecycleOwner, Observer {
                myName.text = it
            })

        }

        friendRecyclerView.addItemDecoration(RecyclerViewDecoration(30)) // 아이템간 간격 설정

        return view
    }
}
