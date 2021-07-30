package com.dkchoi.wetalk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.data.CallAction
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityHomeBinding
import com.dkchoi.wetalk.fragment.ChatRoomFragment
import com.dkchoi.wetalk.fragment.HomeFragment
import com.dkchoi.wetalk.fragment.ProfileFragment
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.getRoomImagePath
import com.dkchoi.wetalk.util.Util.Companion.hasPermissions
import com.dkchoi.wetalk.util.Util.Companion.openVideoActivity
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class HomeActivity : AppCompatActivity(), SocketReceiveService.IReceiveListener {
    private val HOME_CONTAINER = 0
    private val CHAT_CONTAINER = 1
    private val PROFILE_CONTAINER = 2

    private val permissions = arrayOf<String>(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    private lateinit var binding: ActivityHomeBinding
    private lateinit var homeFragment: HomeFragment
    private lateinit var chatRoomFragment: ChatRoomFragment
    private lateinit var profileFragment: ProfileFragment

    private val chatRoomViewModel: ChatRoomViewModel by viewModels()

    private val server by lazy {
        ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
    }

    companion object {
        var service: SocketReceiveService? = null
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SocketReceiveService.LocalBinder
            service = binder.getService()
            service?.registerListener(this@HomeActivity)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
        }

    }

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
        bindService(Intent(this, SocketReceiveService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        service?.registerListener(this@HomeActivity)
    }

    override fun onPause() {
        super.onPause()
        service?.registerListener(null)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //소켓 통신으로 오는 콜백
    override fun onReceive(msg: String) {
        receiveMessage(msg)
    }

    private fun receiveMessage(msg: String) {
        val message = msg.replace("\r\n", "")// \r\n 제거
        if (msg.split("::")[0] == "call") { //전화 관련 메시지 일경우
            if (!hasPermissions(permissions, this)) {
                Toast.makeText(this, "영상통화가 수신되었으나 권한이 없으므로 영상통화를 이용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val videoMsg = message.split("::") // msg[1] 채널 id, msg[2] user data
            val user = Util.gson.fromJson(videoMsg[2], User::class.java)
            openVideoActivity(this, videoMsg[1], CallAction.RECEIVE, user)
            return
        }

        val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)

        if (messageData.name == Util.getMyName(this)) { // 자신이 보낸 메시지라면 무시
            return
        }
        //서스펜드 함수이므로 코루틴 내에서 실행
        lifecycleScope.launch(Dispatchers.Default) {
            if (chatRoomViewModel.getChatRoom(messageData.roomId) == null) { // 로컬 db에 존재하는 방이 없다면
                val ids: List<String> = Util.getUserIdsFromRoomName(messageData.roomName)
                val userList = server.getUserListByIds(ids) // room에 소속된 user list 가져옴
                val imgPath = getRoomImagePath(messageData.roomName, applicationContext)
                val chatRoom = ChatRoom(
                    messageData.roomId, "$message|", imgPath,
                    null, 1, userList.toMutableList()
                ) //adapter에서 끝에 '|' 문자를 제거하므로 |를 붙여줌 안붙인다면 괄호가 삭제되는 있으므로 | 붙여줌
                chatRoomViewModel.insertRoom(chatRoom)
            } else { //기존에 방이 존재한다면
                val chatRoom = chatRoomViewModel.getChatRoom(messageData.roomId)
                //chatroom에 메시지 추가
                chatRoom.messageDatas =
                    chatRoom.messageDatas + message + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌
                chatRoom.unReadCount += 1
                chatRoomViewModel.updateRoom(chatRoom)
            }
            service?.showNotification(message) // 노티 띄워줌
        }
    }
}

