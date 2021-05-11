package com.dkchoi.wetalk.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.room.Room
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.UserDatabase
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application) {



    private val friendList: MutableList<String> by lazy {
        mutableListOf()
    }
    val friendListLiveData: MutableLiveData<List<User>> by lazy { // friend list를 observe하기 위한 livedata 생성
        MutableLiveData<List<User>>()
    }

    val myNameLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    init {
        loadUsers(application)
        loadMyName(application)
    }

    private fun loadUsers(application: Application) {
        val db = Room.databaseBuilder(application, UserDatabase::class.java, "user-database").build()
        viewModelScope.launch(Dispatchers.Default) {
            val userList = db.userDao().getAll()
            friendListLiveData.postValue(userList) // live data setting
        }
    }

    private fun loadMyName(application: Application) {
        val server = ServiceGenerator.retrofitSignUp.create(BackendInterface::class.java)
        val phone = Util.getSession(application)
        viewModelScope.launch {
            phone?.let {
                val name = server.getName(it)
                myNameLiveData.value = name
            }
        }
    }

}