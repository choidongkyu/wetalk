package com.dkchoi.wetalk

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SmsMessage
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.RegisterActivity.Companion.toEditable
import com.dkchoi.wetalk.databinding.ActivityCertificationBinding
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Matcher
import java.util.regex.Pattern

//인증화면 액티비티

class CertificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCertificationBinding
    private lateinit var progressDialog: ProgressDialog
    private var mVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var smsReceiver: BroadcastReceiver

    private lateinit var server: BackendInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCertificationBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 제거
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 버튼 생성

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("잠시만 기다려주세요")
        progressDialog.setCanceledOnTouchOutside(false)

        if (intent.getStringExtra("verification") != null) {
            mVerificationId = intent.getStringExtra("verification")
        }


        firebaseAuth = FirebaseAuth.getInstance()

        binding.nextImageView.setOnClickListener {
            showSetNameDialog()
        }


        server = ServiceGenerator.retrofit.create(BackendInterface::class.java)

        //문자 받는 브로드캐스트리시버
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    val bundle = intent.extras
                    val messages = parseSmsMessage(bundle!!)

                    if (messages!!.isNotEmpty()) {
                        val content = messages[0]!!.messageBody.toString()
                        val pattern: Pattern =
                            Pattern.compile("\\d\\d\\d\\d\\d\\d") // 숫자 6자리 패턴만 가져오도록 패턴 설정
                        val matcher: Matcher = pattern.matcher(content)
                        while (matcher.find()) {
                            binding.codeEt.text = matcher.group(0).toEditable()
                            if (matcher.group(0) == null) break
                        }

                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // 툴바에서 뒤로가기 버튼을 눌렀을때
            finish() // 현재 액티비티 종료
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String, userName: String) {
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential, userName)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, userName: String) {
        progressDialog.setMessage("회원가입 처리중 ...")
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                //success
                progressDialog.dismiss()

                val phone = firebaseAuth.currentUser.phoneNumber
                //서버에 회원가입 요청
                server.setUserRegister(phone, userName).enqueue(object : Callback<Int> {
                    override fun onResponse(call: Call<Int>, response: Response<Int>) {
                        if (response.body() == RESPONSE_OK) {
                            //회원가입 완료시 home 화면 이동
                            Toast.makeText(
                                this@CertificationActivity,
                                "회원가입이 완료 되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()

                            lifecycleScope.launch {
                                Util.fetchMyData(application)
                                val intent: Intent = Intent(this@CertificationActivity, HomeActivity::class.java)

                                Util.setSession(
                                    firebaseAuth.currentUser.phoneNumber,
                                    this@CertificationActivity
                                ) //session setting

                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(intent)
                            }

                        } else if (response.body() == RESPONSE_DUPLICATE) { //중복된 핸드폰 번호가 있을 경우
                            lifecycleScope.launch {
                                Util.fetchMyData(application)
                            }
                            Toast.makeText(
                                this@CertificationActivity,
                                "가입된 핸드폰번호가 있습니다. 해당 번호로 로그인 합니다.",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent: Intent =
                                Intent(this@CertificationActivity, HomeActivity::class.java)

                            Util.setSession(phone, this@CertificationActivity) //session setting

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this@CertificationActivity,
                                "회원가입에 실패하였습니다. 관리자에게 문의해 주세요. : ${response.body()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }

                    override fun onFailure(call: Call<Int>, t: Throwable) {
                        Toast.makeText(
                            this@CertificationActivity,
                            "회원가입에 실패하였습니다. 인터넷을 확인해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })

            }
            .addOnFailureListener { e ->
                //login faile
                progressDialog.dismiss()
                Toast.makeText(this, "인증번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseSmsMessage(bundle: Bundle): Array<SmsMessage?>? {
        // PDU: Protocol Data Units
        val objs = bundle["pdus"] as Array<*>?
        val messages: Array<SmsMessage?> = arrayOfNulls<SmsMessage>(objs!!.size)
        for (i in objs!!.indices) {
            messages[i] = SmsMessage.createFromPdu(objs[i] as ByteArray)
        }
        return messages
    }

    override fun onDestroy() {
        unregisterReceiver(smsReceiver)
        super.onDestroy()
    }

    //사용자 이름을 setting 하는 다이어로그
    private fun showSetNameDialog() {
        val code: String = binding.codeEt.text.toString().trim()
        val editText = EditText(this)
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setTitle("사용자 이름 설정")
        dialog.setMessage("프로필 이름을 입력 해주세요.")
        dialog.setView(editText)
        dialog.setPositiveButton("확인") { _, _ ->
            val userName = editText.text.toString().trim()
            verifyPhoneNumberWithCode(mVerificationId, code, userName)
        }

        dialog.setNegativeButton("취소") { _, _ ->
            //nothing
        }

        dialog.show()
    }

    companion object {
        const val RESPONSE_OK = 200
        const val RESPONSE_DUPLICATE = -100
        const val RESPONSE_FAIL = -1
    }
}