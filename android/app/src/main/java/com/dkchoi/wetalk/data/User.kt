package com.dkchoi.wetalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class User(
    @PrimaryKey
    var id: String, // 유저 번호
    @field:Json(name = "profile_image")
    var profileImage: String?, // 유저 프로필 사진
    @field:Json(name = "profile_text")
    var profileText: String?, // 유저 상태 메시지
    var name: String // 유저 이름
)
