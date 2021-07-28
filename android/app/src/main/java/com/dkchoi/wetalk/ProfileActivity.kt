package com.dkchoi.wetalk

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.data.CallAction
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityProfileBinding
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_IP
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_PORT
import com.dkchoi.wetalk.util.Util
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private val permissions = arrayOf<String>(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    private val db: AppDatabase? by lazy {
        AppDatabase.getInstance(this, "chatRoom-database")
    }

    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityProfileBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_profile)
        user = intent.getParcelableExtra<User>("user")


        // 프로필 이미지 set
        val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
        val imgKey = if (user.profileImage != null) {
            user.profileImage
        } else {
            "null"
        } //프로파일 이미지가 null 일 경우 text "null"을 glide key로 설정
        Glide.with(this)
            .load(imgPath)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(imgKey!!))
            .into(binding.profileImg)

        //프로필 이름 set
        binding.profileName.text = user.name

        //
        binding.profileTxt.text = // null이라면 ""
            if (user.profileText != null)
                user.profileText
            else ""


        binding.closeBtn.setOnClickListener { //닫기 버튼을 눌렀을때 activity 종료
            finish()
        }

        binding.chatBtn.setOnClickListener {
            val chatRoom = createRoom()
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chatRoomId", chatRoom.roomId)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        binding.videoBtn.setOnClickListener {
            if(!Util.hasPermissions(permissions, this)) { //권한이 없는 경우
                ActivityCompat.requestPermissions(this@ProfileActivity, permissions, 1) //권한 요청
                return@setOnClickListener
            }
            Util.openVideoActivity(this, UUID.randomUUID().toString(), CallAction.CALL, user)
        }
    }

    private fun createRoom(): ChatRoom {
        var roomName =
            "${user.id},${Util.getPhoneNumber(this)}" //roomName ex - +821093230128,+821026595819
        val roomNameArray = roomName.split(",").sorted() // room name 통일위하여 sort
        roomName = "${roomNameArray[0]},${roomNameArray[1]}"
        var chatRoom = db?.chatRoomDao()?.getRoomFromName(roomName)

        if (chatRoom == null) { //기존에 존재하는 room이 없을경우 room 생성
            val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
            val userList: MutableList<User> = mutableListOf<User>()
            userList.add(Util.getMyUser(this))
            userList.add(user)
            chatRoom = ChatRoom(UUID.randomUUID().toString(), "", imgPath, null, 0, userList)
            db?.chatRoomDao()?.insertChatRoom(chatRoom)
        }

        //서버에 room 생성 요청
        Thread(Runnable {
            val socket =
                Socket(SERVER_IP, SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println("createRoom::${roomName}")
        }).start()
        return chatRoom
    }
}