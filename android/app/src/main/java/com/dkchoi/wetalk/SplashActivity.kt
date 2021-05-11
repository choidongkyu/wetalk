package com.dkchoi.wetalk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.UserDatabase
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : AppCompatActivity() {

    private val TAG = javaClass.simpleName

    private val permissions = arrayOf<String>(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (!hasPermissions(*permissions)) { //권한이 없다면
            ActivityCompat.requestPermissions(this@SplashActivity, permissions, 1) //권한 요청
        } else { //권한이 있다면
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                var intent: Intent? = null
                lifecycleScope.launch(Dispatchers.Main) {
                    fetchUserData() //친구리스트 생성
                    intent =
                        if (Util.getSession(this@SplashActivity) != null) {// 세션이 존재한다면 바로 home 화면으로 이동
                            Intent(this@SplashActivity, HomeActivity::class.java)
                        } else { //그렇지 않다면 로그인 화면 으로 이동
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }
                    intent!!.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }

            }, 900)
        }
    }

    // 해당 기능의 권한이 있는지 확인할 수 있는 메소드
    private fun hasPermissions(vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            )
                return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var allGrant: Boolean = true;
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allGrant = false
            }
        }

        if (allGrant) {//전체 권한이 승인된 경우 activity 이동
            lifecycleScope.launch(Dispatchers.Main) {
                fetchUserData()
                val intent: Intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
        } else { //승인되지 않은 권한이 있을 경우
            Toast.makeText(this@SplashActivity, "앱을 실행시키기 위해서는 모든 권한이 필요합니다.", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private suspend fun fetchUserData() {
        // 서버에서 받아온 유저 정보를 데이터베이스에 저장하기 위해 db객체 생성
        val db = Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java, "user-database"
        )
            .allowMainThreadQueries()
            .build()

        val phoneBooks: List<PhoneBook> = Util.getContacts(application) //전화번호부 가져옴

        //유저정보를 받기위한 retrofit 객체 생성
        val server = ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
        val friendList = mutableListOf<User>()
        val userList = server.getUserList()

        //서버에서 받아온 user 리스트와 전화번호부 비교하여 친구리스트 생성
        for (user in userList) {
            for (phoneBook in phoneBooks) {
                var result = phoneBook.tel?.replace("-", "") // '-' 제거
                result = result?.replaceFirst("0", "+82")
                if (user.id == result) { //서버에 있는 유저가 전화번호부에 있다면
                    friendList.add(user) // 추가
                }

                //에뮬레이터 번호 예외처리, 테스트용으로 list에 추가
                if (user.name == "에뮬레이터" && phoneBook.name == "에뮬레이터") {
                    friendList.add(user)
                }
            }
        }
        db.userDao().insertAll(*friendList.toTypedArray())
    }
}