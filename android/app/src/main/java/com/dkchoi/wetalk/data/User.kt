package com.dkchoi.wetalk.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class User(
    @PrimaryKey
    var id: String, // 유저 번호
    @field:Json(name = "profile_image")
    var profileImage: String?, // 유저 프로필 캐시 키
    @field:Json(name = "profile_text")
    var profileText: String?, // 유저 상태 메시지
    var name: String // 유저 이름
) : Parcelable
