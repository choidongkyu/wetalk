package com.dkchoi.wetalk

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dkchoi.wetalk.databinding.ActivityCertificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

//인증화면 액티비티

class CertificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCertificationBinding
    private lateinit var progressDialog: ProgressDialog
    private var mVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth

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

        mVerificationId = intent.getStringExtra("verification")

        firebaseAuth = FirebaseAuth.getInstance()

        binding.nextImageView.setOnClickListener {
            var code: String = binding.codeEt.text.toString().trim()

            verifyPhoneNumberWithCode(mVerificationId, code)
        }
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
}