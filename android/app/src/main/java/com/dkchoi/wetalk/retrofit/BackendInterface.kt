package com.dkchoi.wetalk.retrofit

import retrofit2.Call
import retrofit2.http.*

interface BackendInterface {
    companion object {
        const val BASE_URL = "http://49.247.19.12/"
    }

    @FormUrlEncoded
    @POST("signup.php")
    fun setUserRegister(
        @Field("id") id: String
    ): Call<Int>

    @GET("signup.php")
    fun duplicateCheck(
        @Query("id") id: String
    ): Call<Int>
}