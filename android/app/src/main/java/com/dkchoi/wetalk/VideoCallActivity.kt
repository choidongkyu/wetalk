package com.dkchoi.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.data.CallAction
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityVideoCallBinding
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util.Companion.getMyUser
import com.dkchoi.wetalk.util.Util.Companion.gson
import com.remotemonster.sdk.RemonCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class VideoCallActivity : AppCompatActivity(), SocketReceiveService.IReceiveListener {
    lateinit var binding: ActivityVideoCallBinding
    private var remonCall: RemonCall? = null
    private lateinit var action: CallAction
    private lateinit var opponentUser: User
    private lateinit var channelId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_call)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // 영상 통화중 화면이 꺼지지 않도록 flag 추가
        action = intent.getSerializableExtra("action") as CallAction
        opponentUser = intent.getParcelableExtra("user") as User
        channelId = intent.getStringExtra("channelId")

        updateView(false)

        remonCall = RemonCall.builder()
            .context(this)
            .serviceId("SERVICEID1")
            .key("1234567890")
            .videoCodec("VP8")
            .videoWidth(640)
            .videoHeight(400)
            .localView(binding.localView)
            .remoteView(binding.remoteView)
            .build()
        remonCall?.connect(channelId)


        remonCall?.onClose {
            //상대방이 종료했을 경우
            Toast.makeText(this, "통화를 종료 합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.endCallBtn.setOnClickListener {
            remonCall?.close()
            finish()
        }

        binding.callBtn.setOnClickListener { //통화버튼 누를경우
            sendReceiveCall()
        }
    }

    private fun updateView(connected: Boolean) {
        binding.nameText.text = opponentUser.name
        if (connected) { //연결이 됬을 경우
            binding.connectionText.visibility = View.GONE
            binding.receiveText.visibility = View.GONE
            binding.callBtn.visibility = View.GONE
            binding.remoteView.visibility = View.VISIBLE
            binding.localView.visibility = View.VISIBLE
            return
        }
        //연결 중일 경우
        if (action == CallAction.CALL) { //전화거는 사용자일 경우
            binding.receiveText.visibility = View.GONE
            binding.connectionText.visibility = View.VISIBLE
            binding.callBtn.visibility = View.GONE
            binding.remoteView.visibility = View.GONE
            binding.localView.visibility = View.GONE
            sendVideoCall()
        } else { // 수신하는 사용자일 경우
            binding.receiveText.visibility = View.VISIBLE
            binding.connectionText.visibility = View.GONE
            binding.callBtn.visibility = View.VISIBLE
            binding.localView.visibility = View.INVISIBLE
        }
    }

    private fun sendVideoCall() {
        lifecycleScope.launch(Dispatchers.Default) {
            val user = gson.toJson(getMyUser(this@VideoCallActivity))
            val message =
                "videoCall::${channelId}::${user}::${opponentUser.id}" // 소켓에 채널 id와 user객체 전달
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }
    }

    private fun sendReceiveCall() {
        updateView(true)
        lifecycleScope.launch(Dispatchers.Default) {
            val message = "receiveCall::${opponentUser.id}"
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }
    }

    override fun onDestroy() {
        remonCall?.close()
        super.onDestroy()
    }

    override fun onReceive(msg: String) {
        if(msg == "receiveCall") {
            runOnUiThread {
                updateView(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HomeActivity.service?.registerListener(this)
    }

    override fun onPause() {
        super.onPause()
        HomeActivity.service?.registerListener(null)
    }
}