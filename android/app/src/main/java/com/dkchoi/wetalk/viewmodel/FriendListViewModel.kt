package com.dkchoi.wetalk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.util.Util

class FriendListViewModel(application: Application) : AndroidViewModel(application) {
    private val friendList: MutableList<String> by lazy {
        mutableListOf()
    }
    val friendListLiveData: MutableLiveData<List<String>> by lazy { // friend list를 observe하기 위한 livedata 생성
        MutableLiveData<List<String>>()
    }

    init {
        loadUsers(application)
    }

    fun loadUsers(application: Application) {
        val phoneBooks: MutableList<PhoneBook> = Util.getContacts(application) //전화번호부 가져옴
        for (phoneBook in phoneBooks) {
            phoneBook.name?.let { friendList.add(it) }
        }
        friendListLiveData.value = friendList // live data setting
    }

}