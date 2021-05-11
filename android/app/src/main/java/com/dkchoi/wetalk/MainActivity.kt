package com.dkchoi.wetalk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    private lateinit var server: BackendInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        server = ServiceGenerator.retrofitSignUp.create(BackendInterface::class.java)

        val registerButton = findViewById<MaterialButton>(R.id.register_button)
        registerButton.setOnClickListener {
            val intent: Intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val phoneNumber: String = getPhoneNumber()

        val loginButton = findViewById<MaterialButton>(R.id.login_button)
        loginButton.setOnClickListener {
            server.duplicateCheck(phoneNumber).enqueue(object : Callback<Int> { //id 존재하는지 서버에 요청
                override fun onResponse(call: Call<Int>, response: Response<Int>) {
                    if (response.body() == -1) { //id가 존재하는 경우
                        Util.setSession(phoneNumber, this@MainActivity) //세션 설정
                        val intent: Intent =
                            Intent(this@MainActivity, HomeActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "회원가입이 필요합니다. 회원가입 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                        val intent: Intent = Intent(this@MainActivity, RegisterActivity::class.java)
                        startActivity(intent)
                    }
                }

                override fun onFailure(call: Call<Int>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "로그인이 실패하였습니다. 인터넷을 확인해 주세요", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun getPhoneNumber(): String {
        val telManager: TelephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        var phoneNumber: String = telManager.line1Number
        //kt의 경우 국가번호 +82가 붙지만 그외에 통신사는 붙지 않음 때문에 국가번호를 붙여줘야함
        if (!phoneNumber.contains("+82")) {
            phoneNumber = phoneNumber.replaceFirst("0", "+82")
        }
        return phoneNumber
    }
}