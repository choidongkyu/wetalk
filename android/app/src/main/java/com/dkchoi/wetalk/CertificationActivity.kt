package com.dkchoi.wetalk

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dkchoi.wetalk.RegisterActivity.Companion.toEditable
import com.dkchoi.wetalk.databinding.ActivityCertificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.regex.Matcher
import java.util.regex.Pattern

//인증화면 액티비티

class CertificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCertificationBinding
    private lateinit var progressDialog: ProgressDialog
    private var mVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var smsReceiver: BroadcastReceiver

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
            var code: String = binding.codeEt.text.toString().trim()

            verifyPhoneNumberWithCode(mVerificationId, code)
        }

        //문자 받는 브로드캐스트리시버
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    val bundle = intent.extras
                    val messages = parseSmsMessage(bundle!!)

                    if (messages!!.size > 0) {
                        val content = messages[0]!!.messageBody.toString()
                        //인증코드 메시지에 국외발신 키워드가 없다면 우리가 보낸 메시지가 아니므로 return
                        if(!content.contains("[국외발신]")) {
                            return
                        }
                        val pattern: Pattern = Pattern.compile("\\d\\d\\d\\d\\d\\d") // 숫자 6자리 패턴만 가져오도록 패턴 설정
                        val matcher: Matcher = pattern.matcher(content)
                        while(matcher.find()) {
                            binding.codeEt.text = matcher.group(0).toEditable()
                            if(matcher.group(0) == null) break
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

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressDialog.setMessage("회원가입 처리중 ...")
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                //success
                progressDialog.dismiss()
                val phone = firebaseAuth.currentUser.phoneNumber
                var intent: Intent = Intent(this@CertificationActivity, HomeActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                //login faile
                progressDialog.dismiss()
                Toast.makeText(this, "인증번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseSmsMessage(bundle: Bundle): Array<SmsMessage?>? {
        // PDU: Protocol Data Units
        val objs = bundle["pdus"] as Array<Any>?
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
}