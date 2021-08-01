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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import com.dkchoi.wetalk.ChatActivity
import com.dkchoi.wetalk.DirectReplyReceiver
import com.dkchoi.wetalk.DirectReplyReceiver.Companion.KEY_TEXT_REPLY
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.MessageType
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.AppDatabase
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.dkchoi.wetalk.util.Util.Companion.saveMsgToLocalRoom
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
    private val server by lazy {
        ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
    }

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
        serviceHandler = Handler() // queing 동작을 하기 위한 핸들러 생성
        user = Util.getMyUser(this)
        if (receiveThread == null) {
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
                        onReceive(it) // 앱이 백그라운드에 있으므로 단순 db저장
                    }

                }
            }
        }

        fun stopThread() {
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

    private fun onReceive(message: String) {
        db?.let {
            CoroutineScope(Dispatchers.IO).launch {
                if(message.split("::")[0] == "videoCall" || message.split("::")[0] == "voiceCall") { //전화 관련 메시지 일경우
                    return@launch
                }
                val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)
                if (messageData.name == Util.getMyName(applicationContext)) return@launch // 자기 자신이 보낸 메시지도 소켓으로 통해 들어오므로 필터링
                saveMsgToLocalRoom(message, it, applicationContext)
                showNotification(message)
            }
        }
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

    fun showNotification(message: String) {
        val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)
        val channelId = "$packageName-${getString(R.string.app_name)}"

        val roomTitle = getRoomTitle(messageData.roomTitle) // 알림제목으로 설정될 방제목 get
        val bitmap = getLargeIcon(messageData.roomName) // 프로필 방 이미지 get

        CoroutineScope(Dispatchers.Main).launch {
            val content = messageData.content
            val notificationId = 1002
            val builder = NotificationCompat.Builder(this@SocketReceiveService, channelId)
                .setSmallIcon(R.drawable.ic_baseline_message_24) // 아이콘
                .setContentTitle(roomTitle) // 제목
                .setContentText(content) // 내용
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 노티의 중요도
                .setAutoCancel(true) // true라면 사용자가 노티를 터치했을때 사라지게함
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            bitmap?.let {
                builder.setLargeIcon(bitmap)   // 방 이미지
            }

            if (messageData.type == MessageType.IMAGE_MESSAGE) { //이미지를 보낼경우
                builder.setContentText("${messageData.name}님이 사진을 보냈습니다")
            }


            val chatRoom = db?.chatRoomDao()?.getRoomFromId(messageData.roomId)
            val pendingIntent =
                getContentIntent(chatRoom) //notification 클릭시 액티비티로 이동할수 있도록 intent 생성
            builder.setContentIntent(pendingIntent)

            val replyAction = getReplyAction(chatRoom) // notification 에서 즉시 답장 할수 있도록 액션 만듬
            builder.addAction(replyAction)

            with(NotificationManagerCompat.from(this@SocketReceiveService)) {
                notify(notificationId, builder.build())
            }
        }

    }

    private fun getRoomTitle(title: String): String {
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
        return roomTitle
    }

    private fun getLargeIcon(roomName: String): Bitmap? { // 알림에 쓰일 프로필 이미지를 구하는 메소드
        var userId = ""
        val users = roomName.split(",") //room name에 포함된 userid 파싱
        for (user in users) {
            if (user != Util.getPhoneNumber(applicationContext)) {//자신이 아닌 다른 user의 프로필 사진으로 채팅방 구성
                userId = user
                break
            }
        }
        val imgPath = "${Util.profileImgPath}/${userId}.jpg"

        return getImage(imgPath)
    }

    private fun getContentIntent(chatRoom: ChatRoom?): PendingIntent? {
        val chatIntent = Intent(this@SocketReceiveService, ChatActivity::class.java)
        chatIntent.putExtra("chatRoomId", chatRoom?.roomId)

        return TaskStackBuilder.create(this@SocketReceiveService).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(chatIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun getReplyAction(chatRoom: ChatRoom?): NotificationCompat.Action {
        val replyLabel = "답장"
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }

        val replyIntent = Intent(applicationContext, DirectReplyReceiver::class.java)
        replyIntent.putExtra("chatRoomId", chatRoom?.roomId)
        val replyPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_reply_24,
            "REPLY", replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()
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

