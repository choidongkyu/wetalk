package com.example.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

//인증화면 액티비티

class CertificationActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certification)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // toolbar 제목 제거
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //뒤로가기 버튼 생성
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // 툴바에서 뒤로가기 버튼을 눌렀을때
            finish() // 현재 액티비티 종료
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}