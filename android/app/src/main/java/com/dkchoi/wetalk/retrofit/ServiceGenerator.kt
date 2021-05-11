package com.dkchoi.wetalk.retrofit

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

//retrofit 객체를 전역 사용하기 위한 싱글톤 객체
object ServiceGenerator {
    //retrofit init
    //signup.php에 쓰이는 retrofit
    val retrofitSignUp: Retrofit = Retrofit.Builder()
        .baseUrl(BackendInterface.BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    val retrofitUser: Retrofit = Retrofit.Builder()
        .baseUrl(BackendInterface.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}