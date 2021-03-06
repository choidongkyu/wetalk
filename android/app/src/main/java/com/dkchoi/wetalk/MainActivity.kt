package com.dkchoi.wetalk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.viewmodel.ChatRoomViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    private lateinit var server: BackendInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        server = ServiceGenerator.retrofit.create(BackendInterface::class.java)

        val registerButton = findViewById<Button>(R.id.register_button)
        registerButton.setOnClickListener {
            val intent: Intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val phoneNumber: String = Util.getPhoneNumber(this)

        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener {
            server.duplicateCheck(phoneNumber).enqueue(object : Callback<Int> { //id 존재하는지 서버에 요청
                override fun onResponse(call: Call<Int>, response: Response<Int>) {
                    if (response.body() == -1) { //id가 존재하는 경우
                        Util.setSession(phoneNumber, this@MainActivity) //세션 설정
                        lifecycleScope.launch {
                            Util.fetchMyData(application)
                            val intent: Intent =
                                Intent(this@MainActivity, HomeActivity::class.java)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "회원가입이 필요합니다. 회원가입 화면으로 이동합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent: Intent = Intent(this@MainActivity, RegisterActivity::class.java)
                        startActivity(intent)
                    }
                }

                override fun onFailure(call: Call<Int>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "로그인이 실패하였습니다. 인터넷을 확인해 주세요",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
        }
    }
}