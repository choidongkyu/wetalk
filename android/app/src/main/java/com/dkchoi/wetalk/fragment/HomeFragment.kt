package com.dkchoi.wetalk.fragment

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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
        val refreshButton: ImageView = view.findViewById(R.id.refresh_button)
        val progressBar: ProgressBar = view.findViewById(R.id.progressbar)

        val friendListAdapter = FriendListAdapter()
        friendRecyclerView.apply {
            adapter = friendListAdapter
            layoutManager = LinearLayoutManager(activity)
        }


        homeFragmentViewModel.apply {
            getUserList().observe(viewLifecycleOwner, Observer {
                friendListAdapter.updateItem(it)
                friendNumber.text = "${it.size}명"
            })
            myNameLiveData.observe(viewLifecycleOwner, Observer {
                myName.text = it
            })
        }


        //새로고침 버튼을 누를시 user refresh
        refreshButton.setOnClickListener {
            activity?.let { it ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                    withContext(Dispatchers.IO) {
                        homeFragmentViewModel.refreshUserList(it.application)
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }

        friendRecyclerView.addItemDecoration(RecyclerViewDecoration(30)) // 아이템간 간격 설정

        return view
    }
}
