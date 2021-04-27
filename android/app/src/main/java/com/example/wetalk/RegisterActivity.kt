package com.example.wetalk

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class RegisterActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val nextButton: ImageView = findViewById(R.id.next_imageView)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 제거
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 버튼 생성

        nextButton.setOnClickListener {
            Log.d("dkchoi", "nextbutton click")
            showDialog() // 인증번호 다이어로그
        }
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
        dialog.setTitle("+8210********")
        dialog.setMessage("이 전화번호로 SMS 인증번호를 보냅니다.")
        dialog.setPositiveButton("확인") { _, _ ->
            val intent : Intent = Intent(this, CertificationActivity::class.java)
            startActivity(intent)
        }

        dialog.setNegativeButton("취소") { _, _ ->
            //nothing
        }

        dialog.show()
    }
}