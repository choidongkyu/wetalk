package com.dkchoi.wetalk

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dkchoi.wetalk.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"

    //view binding
    private lateinit var binding: ActivityRegisterBinding

    //for resending
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    //for verification
    private var mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 제거
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 버튼 생성

        binding.nextImageView.setOnClickListener {
            showDialog() // 인증번호 다이어로그
        }

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.languageCode = "ko"

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("잠시만 기다려주세요")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                //signInWithPhoneAuthCredential(phoneAuthCredential)

            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Toast.makeText(this@RegisterActivity, "\$(e.message)", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: $verificationId")
                forceResendingToken = token
                progressDialog.dismiss()

                Toast.makeText(this@RegisterActivity, "인증번호를 발송하였습니다", Toast.LENGTH_SHORT).show()
                var intent: Intent =
                    Intent(this@RegisterActivity, CertificationActivity::class.java)
                //code 비교 위한 verification id 전달
                intent.putExtra("verification", verificationId)
                startActivity(intent)
            }
        }

        //디바이스 번호 자동입력
        binding.inputEt.text = getPhoneNumber()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // 툴바에서 뒤로가기 버튼을 눌렀을때
            finish() // 현재 액티비티 종료
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDialog() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setTitle(binding.inputEt.text.toString())
        dialog.setMessage("이 전화번호로 SMS 인증번호를 보냅니다.")
        dialog.setPositiveButton("확인") { _, _ ->
            startPhoneNumberVerification(binding.inputEt.text.toString().trim())
        }

        dialog.setNegativeButton("취소") { _, _ ->
            //nothing
        }

        dialog.show()
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        var pi: PendingIntent = PendingIntent.getActivities(
            this, 0,
            arrayOf(Intent(this, "RegisterActivity"::class.java)), 0
        )

        val sms: SmsManager = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber, null, message, pi, null)
    }

    private fun startPhoneNumberVerification(phone: String) {
        progressDialog.setMessage("sms를 보내는 중입니다...")
        progressDialog.show()

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallback)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    @SuppressLint("MissingPermission")
    private fun getPhoneNumber(): Editable? {
        val telManager:TelephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val phoneNumber: String = telManager.line1Number
        return phoneNumber.toEditable()
    }



    companion object {
        //String을 editable 객체로 만들어주는 메소드
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    }

}