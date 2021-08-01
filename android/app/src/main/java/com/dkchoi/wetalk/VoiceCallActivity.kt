package com.dkchoi.wetalk

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dkchoi.wetalk.data.CallAction
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityVoiceCallBinding
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util
import com.remotemonster.sdk.RemonCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class VoiceCallActivity : AppCompatActivity(), SocketReceiveService.IReceiveListener {
    lateinit var binding: ActivityVoiceCallBinding
    private var remonCall: RemonCall? = null
    private lateinit var action: CallAction
    private lateinit var opponentUser: User
    private lateinit var channelId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_voice_call)
        action = intent.getSerializableExtra("action") as CallAction
        opponentUser = intent.getParcelableExtra("user") as User
        channelId = intent.getStringExtra("channelId")

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

        updateView(false)


        remonCall?.onClose {
            //상대방이 종료했을 경우
            Toast.makeText(this, "통화를 종료 합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.endCallBtn.setOnClickListener {
            remonCall?.close()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateView(connected: Boolean) {
        binding.nameText.text = opponentUser.name
        val imgPath = "${Util.profileImgPath}/${opponentUser.id}.jpg"
        Glide.with(this)
            .load(imgPath)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.profileImg)

        if (connected) { //연결이 됬을 경우
            binding.connText.text = "0.03" // todo 해당 부분 timer로 바꿔야 함
            return
        }

        if (action == CallAction.CALL) { //발신하는 사용자일 경우
            sendVoiceCall()
        } else { //수신하는 사용자일 경우
            sendReceiveCall()
        }
    }

    private fun sendVoiceCall() {
        lifecycleScope.launch(Dispatchers.Default) {
            val user = Util.gson.toJson(Util.getMyUser(this@VoiceCallActivity))
            val message =
                "voiceCall::${channelId}::${user}::${opponentUser.id}" // 소켓에 채널 id와 user객체 전달
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
            val message = "receiveVoiceCall::${opponentUser.id}"
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }
    }

    override fun onReceive(msg: String) {
        when (msg) {
            "receiveVoiceCall" -> { // 상대방이 통화 수락할 경우
                runOnUiThread {
                    updateView(true)
                }
            }
            "rejectCall" -> { // 상대방이 통화 거절할 경우
                remonCall?.close()
                finish()
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