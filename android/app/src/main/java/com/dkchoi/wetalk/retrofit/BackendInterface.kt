package com.dkchoi.wetalk.retrofit

import com.dkchoi.wetalk.HomeActivity
import com.dkchoi.wetalk.data.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @GET("user.php?request=getUser")
    suspend fun getUser(
        @Query("phone") phone: String
    ): User

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

    @Multipart
    @POST("upload.php")
    suspend fun uploadFile( // 프로필 이미지지 업로드시 사용
        @Part file: MultipartBody.Part,
        @Part("id") requestBody: RequestBody
    ): Int

    @Multipart
    @POST("upload.php")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("roomName") roomName: RequestBody
    ): Int
}