package com.dkchoi.wetalk.retrofit

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

//retrofit 객체를 전역 사용하기 위한 싱글톤 객체
object ServiceGenerator {
    //retrofit init
    //일반적으로 상요하는 retrofit 객체
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BackendInterface.BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    //user list를 받을 때 사용하는 retrofit 객체
    val retrofitUser: Retrofit = Retrofit.Builder()
        .baseUrl(BackendInterface.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}