package com.dkchoi.wetalk.retrofit

import com.dkchoi.wetalk.HomeActivity
import com.dkchoi.wetalk.data.User
import retrofit2.Call
import retrofit2.http.*

interface BackendInterface {
    companion object {
        const val BASE_URL = "http://49.247.19.12/"
    }

    @FormUrlEncoded
    @POST("signup.php")
    fun setUserRegister(
        @Field("id") id: String,
        @Field("userName") userName: String
    ): Call<Int>

    @GET("signup.php")
    fun duplicateCheck(
        @Query("id") id: String
    ): Call<Int>

    @GET("user.php?request=getName")
    suspend fun getName(
        @Query("phone") phone: String
    ): String

    @GET("user.php?request=getUserList")
    suspend fun getUserList(): List<User>

    @GET("user.php?request=getStatusMsg")
    suspend fun getStatusMsg(
        @Query("phone") phone: String
    ): String?

    @FormUrlEncoded
    @POST("user.php")
    suspend fun setStatusMsg(
        @Field("phone") phone: String?,
        @Field("status_msg") msg: String
    ): Int
}