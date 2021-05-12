package com.dkchoi.wetalk.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.UserDatabase
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.launch

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application) {
    val db = Room.databaseBuilder(application, UserDatabase::class.java, "user-database").build()

    private val friendList: MutableList<String> by lazy {
        mutableListOf()
    }
//    val friendListLiveData: MutableLiveData<List<User>> by lazy { // friend list를 observe하기 위한 livedata 생성
//        MutableLiveData<List<User>>()
//    }

    val myNameLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    init {
        loadMyName(application)
    }

    fun getUserList(): LiveData<List<User>> {
        return db.userDao().getAll()
    }

    suspend fun refreshUserList(application: Application) {
        val phoneBooks: List<PhoneBook> = Util.getContacts(application) //전화번호부 가져옴

        //유저정보를 받기위한 retrofit 객체 생성
        val server = ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
        val friendList = mutableListOf<User>()
        val userList = server.getUserList()

        //서버에서 받아온 user 리스트와 전화번호부 비교하여 친구리스트 생성
        for (user in userList) {
            for (phoneBook in phoneBooks) {
                var result = phoneBook.tel?.replace("-", "") // '-' 제거
                result = result?.replaceFirst("0", "+82")
                if (user.id == result) { //서버에 있는 유저가 전화번호부에 있다면
                    friendList.add(user) // 추가
                }

                //에뮬레이터 번호 예외처리, 테스트용으로 list에 추가
                if (user.name == "에뮬레이터" && phoneBook.name == "에뮬레이터") {
                    friendList.add(user)
                }
            }
        }
        if(friendList.size != 0) {
            db.userDao().insertAll(*friendList.toTypedArray())
        }
    }

    private fun loadMyName(application: Application) {
        val server = ServiceGenerator.retrofit.create(BackendInterface::class.java)
        val phone = Util.getSession(application)
        viewModelScope.launch {
            phone?.let {
                val name = server.getName(it)
                myNameLiveData.value = name
            }
        }
    }
}