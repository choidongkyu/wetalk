package com.dkchoi.wetalk.util

import android.app.Activity
import android.util.Log
import com.dkchoi.wetalk.data.User
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class ChatClientReceiveThread(private val user: User, private val listener: ReceiveListener, private val activity: Activity) : Thread() {

    private var running = true;
    private lateinit var pw: PrintWriter
    private lateinit var socket: Socket
    companion object {
        const val SERVER_IP = "49.247.19.12"
        //const val SERVER_IP = "192.168.0.9"
        const val SERVER_PORT = 5002
        const val JOIN_KEY = "cfc3cf70-c9fc-11eb-9345-0800200c9a66"
    }

    interface ReceiveListener {
        fun onReceive(msg: String)
    }

    override fun run() {
        //socket.connect(InetSocketAddress(SERVER_IP, SERVER_PORT)) // 서버 연결
        socket = Socket(SERVER_IP, SERVER_PORT)
        pw = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        Log.d("test11", "thread start")
        val request = "join::${user.name}::${user.id}\r\n"
        pw.println(request)

        val br = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        while (running) {
            val msg:String? = br.readLine()
            msg?.let { listener.onReceive(it) }
        }
    }

    fun stopThread() {
        Log.d("test11", "stopThread called")
        running = false;
        Thread(Runnable {
            val request = "quit\r\n"
            pw.println(request)
        }).start()
    }
}