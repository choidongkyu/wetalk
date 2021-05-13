package com.dkchoi.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.databinding.ActivityProfileEditBinding
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileMsgEditActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileEditBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeBtn.setOnClickListener {
            finish()
        }

        binding.saveBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.progressbar.visibility = View.VISIBLE
                val msg = binding.statusEt.text.toString()
                val server = ServiceGenerator.retrofit.create(BackendInterface::class.java)
                withContext(Dispatchers.Default) {
                    val result = server.setStatusMsg(Util.getSession(this@ProfileMsgEditActivity), msg)
                    if(result == 200) { // 정상적으로 update가 되었다면
                        Util.setMyStatueMsg(msg, this@ProfileMsgEditActivity) // 상태메시지 Pref에 저장
                    }
                }
                binding.progressbar.visibility = View.GONE
                Toast.makeText(this@ProfileMsgEditActivity, "상태 메시지가 변경되었습니다.",Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    }
}