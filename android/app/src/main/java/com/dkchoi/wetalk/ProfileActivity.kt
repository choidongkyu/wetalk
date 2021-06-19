package com.dkchoi.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.databinding.ActivityProfileBinding
import com.dkchoi.wetalk.util.Util

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityProfileBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_profile)
        val user = intent.getParcelableExtra<User>("user")


        // 프로필 이미지 set
        val imgPath = "${Util.profileImgPath}/${user.id}.jpg"
        val imgKey = if (user.profileImage != null) {
            user.profileImage
        } else {
            "null"
        } //프로파일 이미지가 null 일 경우 text "null"을 glide key로 설정
        Glide.with(this)
            .load(imgPath)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(imgKey!!))
            .into(binding.profileImg)

        //프로필 이름 set
        binding.profileName.text = user.name

        //
        binding.profileTxt.text = // null이라면 ""
            if (user.profileText != null)
                user.profileText
            else ""


        binding.closeBtn.setOnClickListener { //닫기 버튼을 눌렀을때 activity 종료
            finish()
        }
    }
}