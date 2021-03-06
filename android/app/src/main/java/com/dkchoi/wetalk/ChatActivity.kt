package com.dkchoi.wetalk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
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

    //??????????????? ?????? ?????? ??? ????????? result activity
    private val requestActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data?.data != null) { //????????? ?????? ?????????
                val clipData = it?.data?.clipData
                val clipDataSize = clipData?.itemCount
                if (clipData == null) { //???????????? ????????? ????????? ?????? clipData??? null??? ?????? ??????
                    val selectedImageUri = it?.data?.data!!
                    var bitmap: Bitmap? = null
                    bitmap = getBitmapFromUri(selectedImageUri)

                    bitmap?.let { uploadUriImage(bitmap!!) } //bitmap??? ????????? ????????? ????????? ?????????
                    addImage(selectedImageUri.toString())
                } else {
                    clipData.let { clipData ->
                        for (i in 0 until clipDataSize!!) { //?????? ??? ??????????????? ??????
                            val selectedImageUri = clipData.getItemAt(i).uri
                            var bitmap: Bitmap? = null
                            bitmap = getBitmapFromUri(selectedImageUri)

                            bitmap?.let { uploadUriImage(bitmap!!) } //bitmap??? ????????? ????????? ????????? ?????????
                            addImage(selectedImageUri.toString())
                        }
                    }
                }
            }
        }

    private fun getBitmapFromUri(selectedImageUri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT < 29) { // uri ???????????? bitmap?????? ??????
            MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
        } else {
            val source: ImageDecoder.Source = ImageDecoder.createSource(
                contentResolver,
                selectedImageUri
            )
            ImageDecoder.decodeBitmap(source)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        val chatRoomId = intent.getStringExtra("chatRoomId") // ?????? ?????? chatroom
        chatRoom = db?.chatRoomDao()?.getRoomFromId(chatRoomId)!!

        toolBarSetting()
        recyclerViewBinding()

        if (intent.getSerializableExtra("inviteList") != null) {
            val list = intent.getSerializableExtra("inviteList") as ArrayList<User>
            inviteUser(list)
        }

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
                    intent.putExtra("chatRoomId", chatRoom.roomId)
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
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // ???????????? ??????????????? ????????? ????????? ???
            binding.contentEdit.setText("")
        }

        binding.imageBtn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.action = Intent.ACTION_PICK
            requestActivity.launch(intent)
        }

        with(NotificationManagerCompat.from(this)) { // ????????? ???????????? notification ????????? ??????
            cancel(1002)
        }
    }

    private fun toolBarSetting() {
        setSupportActionBar(binding.toolbar) // ?????? ??????
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //???????????? ??????
        supportActionBar?.title = "?????????"
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onResume() {
        super.onResume()
        HomeActivity.service?.registerListener(this@ChatActivity)
        db?.let { db ->
            chatRoom = db.chatRoomDao().getRoomFromId(chatRoom.roomId)
            chatRoom.unReadCount = 0
            db.chatRoomDao().updateChatRoom(chatRoom)
        }
    }

    override fun onPause() {
        super.onPause()
        HomeActivity.service?.registerListener(null)
    }

    private fun addImage(selectedImageUri: String) {
        val chatItem = ChatItem(
            "",
            "",
            selectedImageUri,
            System.currentTimeMillis().toDate(),
            ViewType.RIGHT_IMAGE
        )
        chatAdapter.addItem(chatItem)
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // ???????????? ??????????????? ????????? ????????? ???
        binding.contentEdit.setText("")
    }

    private fun sendImage(request: String) {
        val messageData: MessageData = MessageData(
            MessageType.IMAGE_MESSAGE,
            Util.getMyName(this)!!,
            getPhoneNumber(this),
            "${Util.chatImgPath}/${chatRoom.roomName}/$request.jpg", // ex) +821026595819,+15555215558/2999919293.jpg
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle,
            chatRoom.roomId
        )
        val jsonMessage = gson.toJson(messageData) // message data??? json????????? ??????


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," ???????????? message??? ???????????? ?????? ?????? | ??? ?????????

        lifecycleScope.launch(Dispatchers.Default) {
            db?.chatRoomDao()?.updateChatRoom(chatRoom) //??????db??? ????????? ??????
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n??? ????????? ?????? ????????? java?????? ???????????? ????????? ??? ??? ??????

        //socket?????? ????????? send
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
            chatRoom.roomTitle,
            chatRoom.roomId
        )
        val jsonMessage = gson.toJson(messageData) // message data??? json????????? ??????


        chatRoom.messageDatas =
            chatRoom.messageDatas + jsonMessage + "|" //"," ???????????? message??? ???????????? ?????? ?????? | ??? ?????????

        lifecycleScope.launch(Dispatchers.Default) {
            db?.chatRoomDao()?.updateChatRoom(chatRoom) //??????db??? ????????? ??????
        }

        val message =
            "message::${chatRoom.roomName}::${jsonMessage}\r\n" // \r\n??? ????????? ?????? ????????? java?????? ???????????? ????????? ??? ??? ??????

        //socket?????? ????????? send
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
        lifecycleScope.launch(Dispatchers.Main) {
            db?.let { db ->
                var message = msg
                if(message.split("::")[0] == "videoCall" || message.split("::")[0] == "voiceCall") { //?????? ?????? ????????? ?????????
                    return@launch
                }
                lateinit var messageData: MessageData
                if (msg.contains(JOIN_KEY)) {
                    val tokens = msg.replace(JOIN_KEY, "").split("::")
                    message = tokens[0] // message Data
                    messageData = gson.fromJson(message, MessageData::class.java)
                    if (messageData.id == getPhoneNumber(this@ChatActivity)) return@launch
                    val user = gson.fromJson(tokens[1], User::class.java)
                    chatRoom.userList.add(user)
                    chatRoom.updateRoomInfo()
                    (conversationRecyclerView.adapter as RoomFriendListAdapter).updateList(chatRoom.userList) // ?????? ????????? ????????????
                    db.chatRoomDao().updateChatRoom(chatRoom)
                }

                messageData = gson.fromJson(message, MessageData::class.java)
                if (messageData.name == getMyName(applicationContext) && messageData.type != MessageType.CENTER_MESSAGE) return@launch // ?????? ????????? ?????? ???????????? ???????????? ?????? ??????????????? ?????????

                saveMsgToLocalRoom(message, db, applicationContext)
                if (messageData.roomId == chatRoom.roomId) { // ?????? activity??? ?????? ?????? ???????????? ????????? ???????????? room??? ????????? ui??? ??????
                    addChat(messageData) // ????????????????????? ??????
                    chatRoom = db.chatRoomDao().getRoomFromId(messageData.roomId)
                    chatRoom.unReadCount = 0
                    db.chatRoomDao().updateChatRoom(chatRoom)
                }
            }
        }
    }

    //???????????? ????????? ?????? ??????
    private fun addChat(messageData: MessageData) {
        when (messageData.type) {
            MessageType.CENTER_MESSAGE -> {
                chatAdapter.addItem(
                    ChatItem(
                        "",
                        "",
                        messageData.content,
                        "",
                        ViewType.CENTER_MESSAGE
                    )
                )
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                return
            }

            MessageType.TEXT_MESSAGE -> {
                chatAdapter.addItem(
                    ChatItem(
                        messageData.name, messageData.id, messageData.content,
                        messageData.sendTime.toDate(), ViewType.LEFT_MESSAGE
                    )
                )
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                return
            }

            MessageType.IMAGE_MESSAGE -> {
                chatAdapter.addItem(
                    ChatItem(
                        messageData.name, messageData.id, messageData.content,
                        messageData.sendTime.toDate(), ViewType.LEFT_IMAGE
                    )
                )
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                return
            }
        }
    }


    private fun uploadImage(path: String) {
        val file = File(path)
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), file)
        val requestFile: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestBody)

        //????????? ???????????? ????????? ?????? uuid??? ??????
        val content = UUID.randomUUID().toString()

        val name = RequestBody.create(MediaType.parse("text/plain"), content)
        val roomName = RequestBody.create(MediaType.parse("text/plain"), chatRoom.roomName)

        val server = ServiceGenerator.retrofit.create(BackendInterface::class.java)

        server.uploadImage(requestFile, name, roomName).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.body() == 200) {
                    sendImage(content)
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                //nothing...
            }
        })
    }

    private fun saveBitmapToJpeg(bitmap: Bitmap) {
        //?????? ????????? ?????? ?????? ?????????
        val storage: File = cacheDir

        //????????? ?????? ??????
        val fileName = "temp.jpg"

        //?????? ???????????? ??????
        val tempFile = File(storage, fileName)

        //????????? ???????????? ??????
        tempFile.createNewFile()

        //stream ??????
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.close()

        uploadImage(tempFile.path) //????????? ?????????
    }

    private fun uploadUriImage(bitmap: Bitmap) { //uri ???????????? ????????? ??????
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
        // ?????? ?????????
        chatAdapter = ChatAdapter(chatRoom.messageDatas, this)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1) // ???????????? ??????????????? ????????? ????????? ???

        //???????????? ?????????
        roomFriendListAdapter = RoomFriendListAdapter(chatRoom.userList)
        val headerView = binding.navView.getHeaderView(0)
        conversationRecyclerView = headerView.findViewById(R.id.conversation_recyclerView)
        conversationRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        conversationRecyclerView.adapter = roomFriendListAdapter
        conversationRecyclerView.addItemDecoration(RecyclerViewDecoration(40)) // ???????????? ?????? ??????
    }

    private fun inviteUser(list: ArrayList<User>) {
        chatRoom.userList.addAll(list)
        chatRoom.updateRoomInfo()// userlist??? ????????? ?????? roomName, roomTitle??? ???????????? ????????? updateRoomInfo
        (conversationRecyclerView.adapter as RoomFriendListAdapter).updateList(chatRoom.userList) // ?????? ????????? ????????????
        db?.chatRoomDao()?.updateChatRoom(chatRoom) //??????db??? ????????? ??????

        //socket?????? ????????? send
        Thread(Runnable {
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            for (user in list) {
                val inviteMessage =
                    makeInviteMessage("${Util.getMyName(this)}?????? ${user.name}?????? ?????????????????????.")
                val messageData = gson.toJson(inviteMessage) // message data??? json????????? ??????
                val userJson = gson.toJson(user)
                chatRoom.messageDatas =
                    chatRoom.messageDatas + messageData + "|" //"," ???????????? message??? ???????????? ?????? ?????? | ??? ?????????
                db?.chatRoomDao()?.updateChatRoom(chatRoom) //??????db??? ????????? ??????
                val message =
                    "invite::${chatRoom.roomName}::${user.id}::${messageData}::${userJson}"
                pw.println(message)
            }
        }).start()
    }

    private fun makeInviteMessage(request: String): MessageData {
        return MessageData(
            MessageType.CENTER_MESSAGE,
            Util.getMyName(this)!!,
            Util.getPhoneNumber(this),
            request,
            System.currentTimeMillis(),
            chatRoom.roomName,
            chatRoom.roomTitle,
            chatRoom.roomId
        )
    }
}