package com.dkchoi.wetalk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.room.Room
import com.dkchoi.wetalk.ChatActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.*
import java.io.*
import java.lang.Runnable
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets


class SocketReceiveService : Service() {

    private val binder = LocalBinder()

    private lateinit var user: User
    private var receiveThread: MainReceiveThread? = null
    private var listener: IReceiveListener? = null
    private var serviceHandler: Handler? = null

    private val db: AppDatabase? by lazy {
        AppDatabase.getInstance(this, "chatRoom-database")
    }

    interface IReceiveListener {
        fun onReceive(msg: String)
    }

    companion object {
        const val SERVER_IP = "49.247.19.12"
        const val SERVER_PORT = 5002
        const val JOIN_KEY = "cfc3cf70-c9fc-11eb-9345-0800200c9a66"
    }

    override fun onCreate() {
        Log.d("test11", "service onCreate ")
        serviceHandler = Handler() // queing 동작을 하기 위한 핸들러 생성
        user = Util.getMyUser(this)
        if (receiveThread == null) {
            Log.d("test11", "receivethread == null and make thread")
            receiveThread = MainReceiveThread()
        }
        createNotificationChannel()
        receiveThread!!.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onDestroy() {
        Log.d("test11", "service onDestroy called ")
        receiveThread?.stopThread()
    }

    fun registerListener(cb: IReceiveListener?) {
        listener = cb
    }

    inner class LocalBinder : Binder() {
        fun getService(): SocketReceiveService = this@SocketReceiveService
    }

    inner class MainReceiveThread() : Thread() {
        private var running = true
        private lateinit var pw: PrintWriter
        private lateinit var socket: Socket


        override fun run() {
            socket = Socket(SERVER_IP, SERVER_PORT)
            pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            Log.d("test11", "thread start")
            val request = "join::${user.name}::${user.id}\r\n"
            pw.println(request)

            val br =
                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            while (running) {
                val msg: String? = br.readLine()
                msg?.let {
                    if (listener != null) {
                        listener!!.onReceive(it) //listener이 null이 아닌경우 app이 현재 화면에 띄워진 상태이므로 콜백 불러줌
                    } else {
                        saveMsgToLocalDB(it) // 앱이 백그라운드에 있으므로 단순 db저장
                    }

                }
            }
        }

        fun stopThread() {
            Log.d("test11", "stopThread called")
            running = false
            Thread(Runnable {
                val pw = PrintWriter(
                    OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                )
                val request = "quit\r\n"
                pw.println(request)
            }).start()
        }
    }

    private fun saveMsgToLocalDB(message: String) {
        val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)
        if (messageData.name.equals(user.name)) return // 자기 자신이 보낸 메시지도 소켓으로 통해 들어오므로 필터링

        if (db?.chatRoomDao()
                ?.getRoom(messageData.roomName) == null
        ) { // 로컬 db에 존재하는 방이 없다면
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
                ChatRoom(
                    messageData.roomName,
                    messageData.roomTitle,
                    "$message|",
                    imgPath,
                    null,
                    1
                ) //adapter에서 끝에 '|' 문자를 제거하므로 |를 붙여줌 안붙인다면 괄호가 삭제되는 있으므로 | 붙여줌


            db?.chatRoomDao()?.insertChatRoom(chatRoom)
        } else { //기존에 방이 존재한다면
            val chatRoom = db?.chatRoomDao()?.getRoom(messageData.roomName)
            //chatroom에 메시지 추가
            chatRoom?.let {
                chatRoom.messageDatas =
                    chatRoom.messageDatas + message + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

                chatRoom.unReadCount += 1
                db?.chatRoomDao()?.updateChatRoom(it)
            }
        }
        showNotification(messageData)
    }

    private fun createNotificationChannel() {
        val channelId = "$packageName-${getString(R.string.app_name)}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = "App notification channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(messageData: MessageData) {

        val channelId = "$packageName-${getString(R.string.app_name)}"

        val title = messageData.roomTitle
        //방이름은 자신을 제외한 상대방의 이름으로 구성
        val names = title.split(",") //방 제목은 최동규,채혜인,에뮬레이터 식으로 구성되므로 , 기준으로 파싱
        var roomTitle = ""
        for (name in names) {
            if (name == Util.getMyName(this)) { //자신의 이름일 경우 건너 뜀
                continue
            }
            roomTitle += "$name,"
        }

        roomTitle = roomTitle.substring(0, roomTitle.length - 1)//마지막 , 제거
        CoroutineScope(Dispatchers.Main).launch {
            var userId = ""
            val users = messageData.roomName.split(",") //room name에 포함된 userid 파싱
            for (user in users) {
                if (user != Util.getPhoneNumber(applicationContext)) {//자신이 아닌 다른 user의 프로필 사진으로 채팅방 구성
                    userId = user
                    break
                }
            }
            val imgPath = "${Util.profileImgPath}/${userId}.jpg"

            val bitmap = withContext(Dispatchers.Default) {
                getImage(imgPath)
            }
            val content = messageData.content
            val notificationId = 1002
            val builder = NotificationCompat.Builder(this@SocketReceiveService, channelId)
                .setSmallIcon(R.drawable.ic_baseline_message_24) // 아이콘
                .setContentTitle(roomTitle) // 제목
                .setContentText(content) // 내용
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 노티의 중요도
                .setAutoCancel(true) // true라면 사용자가 노티를 터치했을때 사라지게함
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            if (messageData.type == MessageType.IMAGE_MESSAGE) { //이미지를 보낼경우
                builder.setContentText("${messageData.name}님이 이미지를 보냈습니다")
            }

            bitmap?.let {
                builder.setLargeIcon(it)
            }

            val chatIntent = Intent(this@SocketReceiveService, ChatActivity::class.java)

            val chatRoom = withContext(Dispatchers.Default) { // db에서 chatroom 가져옴
                db?.chatRoomDao()?.getRoom(messageData.roomName)
            }

            chatIntent.putExtra("chatRoom", chatRoom)
            val pendingIntent: PendingIntent? =
                TaskStackBuilder.create(this@SocketReceiveService).run {
                    // Add the intent, which inflates the back stack
                    addNextIntentWithParentStack(chatIntent)
                    // Get the PendingIntent containing the entire back stack
                    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                }

            builder.setContentIntent(pendingIntent)

            with(NotificationManagerCompat.from(this@SocketReceiveService)) {
                notify(notificationId, builder.build())
            }
        }

    }

    private fun getImage(url: String): Bitmap? {
        var bitmap: Bitmap? = null

        val urlConnection = URL(url)
        val connection = urlConnection.openConnection()
        connection.doInput = true
        connection.connect()
        val input = try {
            connection.getInputStream()
        } catch (e: Exception) {
            return null // 프로필 이미지가 없다면 null 반환
        }
        bitmap = BitmapFactory.decodeStream(input)

        return bitmap
    }
}

