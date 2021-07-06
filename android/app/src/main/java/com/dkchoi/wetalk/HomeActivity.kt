package com.dkchoi.wetalk

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityHomeBinding
import com.dkchoi.wetalk.fragment.ChatRoomFragment
import com.dkchoi.wetalk.fragment.HomeFragment
import com.dkchoi.wetalk.fragment.ProfileFragment
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private val HOME_CONTAINER = 0
    private val CHAT_CONTAINER = 1
    private val PROFILE_CONTAINER = 2

    private lateinit var binding: ActivityHomeBinding
    private lateinit var homeFragment: HomeFragment
    private lateinit var chatRoomFragment: ChatRoomFragment
    private lateinit var profileFragment: ProfileFragment

    private val chatRoomViewModel: ChatRoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                BatteryDialog().show(supportFragmentManager, "dialog")
            }
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        homeFragment = HomeFragment()
        chatRoomFragment = ChatRoomFragment()
        profileFragment = ProfileFragment()

        supportFragmentManager.beginTransaction().replace(R.id.container, homeFragment).commit()

        binding.mainTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { //tab 선택 될때
                val pos = tab?.position
                var fragment: Fragment? = null
                when (pos) {
                    HOME_CONTAINER -> fragment = homeFragment
                    CHAT_CONTAINER -> fragment = chatRoomFragment
                    PROFILE_CONTAINER -> fragment = profileFragment
                }

                fragment?.let {
                    supportFragmentManager.beginTransaction().replace(
                        R.id.container,
                        it
                    ).commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                //nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //nothing
            }
        })

        startService(Intent(applicationContext, SocketReceiveService::class.java)) // 소켓 서비스 시작

//        user = Util.getMyUser(this)
//        mainReceiveThread = MainReceiveThread.getInstance(user) // 소켓 통신위한 쓰레드 생성
//        mainReceiveThread.setListener(this)
//        mainReceiveThread.start() // 소켓 연결
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //소켓 통신으로 오는 콜백
    fun onReceive(msg: String) {
        receiveMessage(msg)
    }

    private fun receiveMessage(msg: String) {
        val message = msg.replace("\r\n", "")// \r\n 제거
        val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)

        if (messageData.name == Util.getMyName(this)) { // 자신이 보낸 메시지라면 무시
            return
        }
        //서스펜드 함수이므로 코루틴 내에서 실행
        lifecycleScope.launch(Dispatchers.Default) {
            if (chatRoomViewModel.getChatRoom(messageData.roomName) == null) { // 로컬 db에 존재하는 방이 없다면
                var userId = ""
                val users = messageData.roomName.split(",") //room name에 포함된 userid 파싱
                for (user in users) {
                    if (user != Util.getPhoneNumber(applicationContext)) {//자신이 아닌 다른 user의 프로필 사진으로 채팅방 구성
                        userId = user
                        break
                    }
                }
                val imgPath = "${Util.profileImgPath}/${userId}.jpg"
                val chatRoom =
                    ChatRoom(messageData.roomName, messageData.roomTitle, "$message|", imgPath, null) //adapter에서 끝에 '|' 문자를 제거하므로 |를 붙여줌 안붙인다면 괄호가 삭제되는 있으므로 | 붙여줌


                chatRoomViewModel.insertRoom(chatRoom)
            } else { //기존에 방이 존재한다면
                val chatRoom = chatRoomViewModel.getChatRoom(messageData.roomName)
                //chatroom에 메시지 추가
                chatRoom.messageDatas =
                    chatRoom.messageDatas + message + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

                chatRoomViewModel.updateRoom(chatRoom)
            }
        }
    }
}

