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
        serviceHandler = Handler() // queing ????????? ?????? ?????? ????????? ??????
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
                        listener!!.onReceive(it) //listener??? null??? ???????????? app??? ?????? ????????? ????????? ??????????????? ?????? ?????????
                    } else {
                        onReceive(it) // ?????? ?????????????????? ???????????? ?????? db??????
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
                if(message.split("::")[0] == "videoCall" || message.split("::")[0] == "voiceCall") { //?????? ?????? ????????? ?????????
                    return@launch
                }
                val messageData: MessageData = Util.gson.fromJson(message, MessageData::class.java)
                if (messageData.name == Util.getMyName(applicationContext)) return@launch // ?????? ????????? ?????? ???????????? ???????????? ?????? ??????????????? ?????????
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

        val roomTitle = getRoomTitle(messageData.roomTitle) // ?????????????????? ????????? ????????? get
        val bitmap = getLargeIcon(messageData.roomName) // ????????? ??? ????????? get

        CoroutineScope(Dispatchers.Main).launch {
            val content = messageData.content
            val notificationId = 1002
            val builder = NotificationCompat.Builder(this@SocketReceiveService, channelId)
                .setSmallIcon(R.drawable.ic_baseline_message_24) // ?????????
                .setContentTitle(roomTitle) // ??????
                .setContentText(content) // ??????
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // ????????? ?????????
                .setAutoCancel(true) // true?????? ???????????? ????????? ??????????????? ???????????????
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            bitmap?.let {
                builder.setLargeIcon(bitmap)   // ??? ?????????
            }

            if (messageData.type == MessageType.IMAGE_MESSAGE) { //???????????? ????????????
                builder.setContentText("${messageData.name}?????? ????????? ???????????????")
            }


            val chatRoom = db?.chatRoomDao()?.getRoomFromId(messageData.roomId)
            val pendingIntent =
                getContentIntent(chatRoom) //notification ????????? ??????????????? ???????????? ????????? intent ??????
            builder.setContentIntent(pendingIntent)

            val replyAction = getReplyAction(chatRoom) // notification ?????? ?????? ?????? ?????? ????????? ?????? ??????
            builder.addAction(replyAction)

            with(NotificationManagerCompat.from(this@SocketReceiveService)) {
                notify(notificationId, builder.build())
            }
        }

    }

    private fun getRoomTitle(title: String): String {
        //???????????? ????????? ????????? ???????????? ???????????? ??????
        val names = title.split(",") //??? ????????? ?????????,?????????,??????????????? ????????? ??????????????? , ???????????? ??????
        var roomTitle = ""
        for (name in names) {
            if (name == Util.getMyName(this)) { //????????? ????????? ?????? ?????? ???
                continue
            }
            roomTitle += "$name,"
        }

        roomTitle = roomTitle.substring(0, roomTitle.length - 1)//????????? , ??????
        return roomTitle
    }

    private fun getLargeIcon(roomName: String): Bitmap? { // ????????? ?????? ????????? ???????????? ????????? ?????????
        var userId = ""
        val users = roomName.split(",") //room name??? ????????? userid ??????
        for (user in users) {
            if (user != Util.getPhoneNumber(applicationContext)) {//????????? ?????? ?????? user??? ????????? ???????????? ????????? ??????
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
        val replyLabel = "??????"
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
            return null // ????????? ???????????? ????????? null ??????
        }
        bitmap = BitmapFactory.decodeStream(input)

        return bitmap
    }
}

