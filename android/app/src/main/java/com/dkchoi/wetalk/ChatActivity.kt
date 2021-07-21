package com.dkchoi.wetalk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dkchoi.wetalk.adapter.ChatAdapter
import com.dkchoi.wetalk.adapter.RoomFriendListAdapter
import com.dkchoi.wetalk.data.*
import com.dkchoi.wetalk.databinding.ActivityChatBinding
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.JOIN_KEY
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_IP
import com.dkchoi.wetalk.service.SocketReceiveService.Companion.SERVER_PORT
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.getMyName
import com.dkchoi.wetalk.util.Util.Companion.getPhoneNumber
import com.dkchoi.wetalk.util.Util.Companion.getRoomImagePath
import com.dkchoi.wetalk.util.Util.Companion.getUserIdsFromRoomName
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.saveMsgToLocalRoom
import com.dkchoi.wetalk.util.Util.Companion.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*

class ChatActivity : AppCompatActivity(), SocketReceiveService.IReceiveListener {
    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var roomFriendListAdapter: RoomFriendListAdapter
    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var chatRoom: ChatRoom

    private val db: AppDatabase? by lazy {
        AppDatabase.getInstance(this, "chatRoom-database")
    }

    private val server by lazy {
        ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
    }

    //갤러리에서 사진 선택 후 불리는 result activity
    private val requestActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data?.data != null) { //갤러리 캡쳐 결과값
                val clipData = it?.data?.clipData
                val clipDataSize = clipData?.itemCount
                if (clipData == null) { //이미지를 하나만 선택할 경우 clipData가 null이 올수 있음
                    val selectedImageUri = it?.data?.data!!
                    var bitmap: Bitmap? = null
                    bitmap = if (Build.VERSION.SDK_INT < 29) { // uri 이미지를 bitmap으로 변환
                        MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                    } else {
                        val source: ImageDecoder.Source = ImageDecoder.createSource(
                            contentResolver,
                            selectedImageUri
                        )
                        ImageDecoder.decodeBitmap(source)
                    }

                    bitmap?.let { uploadUriImage(bitmap!!) } //bitmap을 이미지 저장후 서버에 업로드


                    val chatItem = ChatItem(
                        "",
                        "",
                        selectedImageUri.toString(),
                        System.currentTimeMillis().toDate(),
                        ViewType.RIGHT_IMAGE
                    )
                    chatAdapter.addItem(chatItem)
                    binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
                    binding.contentEdit.setText("")
                } else {
                    clipData.let { clipData ->
                        for (i in 0 until clipDataSize!!) { //선택 한 사진수만큼 반복
                            val selectedImageUri = clipData.getItemAt(i).uri
                            var bitmap: Bitmap? = null
                            bitmap = if (Build.VERSION.SDK_INT < 29) { // uri 이미지를 bitmap으로 변환
                                MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                            } else {
                                val source: ImageDecoder.Source = ImageDecoder.createSource(
                                    contentResolver,
                                    selectedImageUri
                                )
                                ImageDecoder.decodeBitmap(source)
                            }

                            bitmap?.let { uploadUriImage(bitmap!!) } //bitmap을 이미지 저장후 서버에 업로드


                            val chatItem = ChatItem(
                                "",
                                "",
                                selectedImageUri.toString(),
                                System.currentTimeMillis().toDate(),
                                ViewType.RIGHT_IMAGE
                            )
                            chatAdapter.addItem(chatItem)
                            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
                            binding.contentEdit.setText("")
                        }
                    }
                }

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        val chatRoomName = intent.getStringExtra("chatRoom") // 전달 받은 chatroom
        chatRoom = db?.chatRoomDao()?.getRoom(chatRoomName)!!

        toolBarSetting()
        recyclerViewBinding()

        roomFriendListAdapter.setOnItemClickListener(object :
            RoomFriendListAdapter.OnItemClickEventListener {
            override fun onItemClick(view: View, pos: Int) {
                val friendList = roomFriendListAdapter.getList()
                val user: User = friendList[pos]

                if (user.id == getPhoneNumber(this@ChatActivity)) {
                    return
                }

                if (pos == 0) {
                    val intent = Intent(this@ChatActivity, InviteActivity::class.java)
                    intent.putExtra("chatRoom", chatRoom.roomName)
                    startActivity(intent)
                    return
                }

                val intent = Intent(this@ChatActivity, ProfileActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
            }

        })

        binding.sendBtn.setOnClickListener {
            sendMessage(binding.contentEdit.text.toString())
            val chatItem = ChatItem(
                "", "", binding.contentEdit.text.toString(),
                System.currentTimeMillis().toDate(), ViewType.RIGHT_MESSAGE
            )
            chatAdapter.addItem(chatItem)
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함
            binding.contentEdit.setText("")
        }

        binding.imageBtn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.action = Intent.ACTION_PICK
            requestActivity.launch(intent)
        }

        with(NotificationManagerCompat.from(this)) { // 채팅방 들어갈때 notification 메시지 삭제
            cancel(1002)
        }
    }

    private fun toolBarSetting() {
        setSupportActionBar(binding.toolbar) // 툴바 생성
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 생성
        supportActionBar?.title = "채팅방"
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onResume() {
        super.onResume()
        HomeActivity.service?.registerListener(this@ChatActivity)
        chatRoom.unReadCount = 0
        db?.chatRoomDao()?.updateChatRoom(chatRoom)
    }

    override fun onPause() {
        super.onPause()
        HomeActivity.service?.registerListener(null)
    }

    private fun sendImage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.IMAGE_MESSAGE,
            Util.getMyName(this)!!,
            getPhoneNumber(this),
            "${Util.chatImgPath}/${chatRoom.roomName}/$request.jpg", // ex) +821026595819,+15555215558/2999919293.jpg
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle
        )
        val jsonMessage = gson.toJson(messageData) // message data를 json형태로 변환


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        lifecycleScope.launch(Dispatchers.Default) {
            db?.chatRoomDao()?.updateChatRoom(chatRoom) //로컬db에 메시지 저장
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음

        //socket으로 메시지 send
        Thread(Runnable {
            val socket =
                Socket(SERVER_IP, SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }).start()
    }

    private fun sendMessage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.TEXT_MESSAGE,
            Util.getMyName(this)!!,
            getPhoneNumber(this),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle
        )
        val jsonMessage = gson.toJson(messageData) // message data를 json형태로 변환


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

        lifecycleScope.launch(Dispatchers.Default) {
            db?.chatRoomDao()?.updateChatRoom(chatRoom) //로컬db에 메시지 저장
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n을 메시지 끝에 붙여야 java에서 메시지의 끝임을 알 수 있음

        //socket으로 메시지 send
        Thread(Runnable {
            val socket =
                Socket(SERVER_IP, SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }).start()
    }

    override fun onReceive(msg: String) {
        runOnUiThread {
            var message = msg.replace("\r\n", "")
            if (message.contains(JOIN_KEY)) { // join_key가 있다면 유저 입장 or 퇴장 메시지
                message = message.replace(JOIN_KEY, "") // 조인키 삭제
                chatAdapter.addItem(
                    ChatItem(
                        "",
                        "",
                        message,
                        "",
                        ViewType.CENTER_MESSAGE
                    )
                )
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            } else {
                val messageData: MessageData = gson.fromJson(message, MessageData::class.java)
                db?.let { db ->
                    if (messageData.name == getMyName(applicationContext)) return@runOnUiThread // 자기 자신이 보낸 메시지도 소켓으로 통해 들어오므로 필터링
                    lifecycleScope.launch(Dispatchers.Main) {
                        saveMsgToLocalRoom(message, db, applicationContext)
                        if (messageData.roomName == chatRoom.roomName) { // 현재 activity에 있는 방과 소켓으로 들어온 메시지의 room이 같다면 ui에 추가
                            addChat(messageData) // 리사이클러뷰에 추가
                            chatRoom.unReadCount = 0
                            db.chatRoomDao().updateChatRoom(chatRoom)
                        }
                    }

                }
            }
        }
    }

    //상대방이 메시지 보낼 경우
    private fun addChat(messageData: MessageData) {
        if (messageData.type == MessageType.TEXT_MESSAGE) {
            chatAdapter.addItem(
                ChatItem(
                    messageData.name, messageData.id, messageData.content,
                    messageData.sendTime.toDate(), ViewType.LEFT_MESSAGE
                )
            )
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        } else {
            chatAdapter.addItem(
                ChatItem(
                    messageData.name, messageData.id, messageData.content,
                    messageData.sendTime.toDate(), ViewType.LEFT_IMAGE
                )
            )
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }


    private fun uploadImage(path: String) {
        val file = File(path)
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), file)
        val requestFile: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestBody)

        //저장될 이미지의 이름을 고유 uuid로 지정
        val content = UUID.randomUUID().toString()

        val name = RequestBody.create(MediaType.parse("text/plain"), content)
        val roomName = RequestBody.create(MediaType.parse("text/plain"), chatRoom.roomName)

        val server = ServiceGenerator.retrofit.create(BackendInterface::class.java)

        server.uploadImage(requestFile, name, roomName).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.body() == 200) {
                    Log.d("test11", "업로드 성공")
                    sendImage(content)
                } else {
                    Log.d("test11", "업로드 실패 result = ${response.body()}")
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Log.d("test11", "업로드 실패 - ${t.printStackTrace()}")
            }
        })
    }

    private fun saveBitmapToJpeg(bitmap: Bitmap) {
        Log.d("test11", "saveBitmaptoJpeg called")

        //내부 저장소 캐쉬 경로 받아옴
        val storage: File = cacheDir

        //저장할 파일 이름
        val fileName = "temp.jpg"

        //파일 인스턴스 생성
        val tempFile = File(storage, fileName)

        //빈파일 자동으로 생성
        tempFile.createNewFile()

        //stream 생성
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.close()

        uploadImage(tempFile.path) //서버에 업로드
    }

    private fun uploadUriImage(bitmap: Bitmap) { //uri 이미지를 서버에 전송
        Log.d("test11", "uploadUriImage called")
        binding.progressbar.visibility = View.VISIBLE
        saveBitmapToJpeg(bitmap)
        binding.progressbar.visibility = View.GONE
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.action_settings -> {
                binding.drawerLayout.openDrawer(GravityCompat.END)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun recyclerViewBinding() {
        // 채팅 리스트
        chatAdapter = ChatAdapter(chatRoom.messageDatas, this)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // 리스트의 마지막으로 포커스 가도록 함

        //대화상대 리스트
        roomFriendListAdapter = RoomFriendListAdapter(chatRoom.userList)
        val headerView = binding.navView.getHeaderView(0)
        conversationRecyclerView = headerView.findViewById(R.id.conversation_recyclerView)
        conversationRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        conversationRecyclerView.adapter = roomFriendListAdapter
        conversationRecyclerView.addItemDecoration(RecyclerViewDecoration(40)) // 아이템간 간격 설정
    }
}