package com.dkchoi.wetalk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.data.CallAction
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityVoiceReceiveBinding
import com.dkchoi.wetalk.service.SocketReceiveService
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*

class VoiceReceiveActivity : AppCompatActivity() {
    lateinit var user: User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityVoiceReceiveBinding = DataBindingUtil.setContentView(this, R.layout.activity_voice_receive)
        user = intent.getParcelableExtra("user") as User
        val channelId = intent.getStringExtra("channelId")

        binding.nameText.text = user.name


        val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
        Glide.with(this)
            .load(imgPath)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.profileImg)

        binding.callBtn.setOnClickListener {
            Util.openVoiceActivity(this, channelId, CallAction.RECEIVE, user)
            finish()
        }

        binding.endCallBtn.setOnClickListener {
            sendRejectCall()
            finish()
        }
    }

    private fun sendRejectCall() {
        lifecycleScope.launch(Dispatchers.Default) {
            val message = "rejectCall::${user.id}"
            val socket =
                Socket(SocketReceiveService.SERVER_IP, SocketReceiveService.SERVER_PORT)
            val pw = PrintWriter(
                OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            )
            pw.println(message)
        }
    }
}