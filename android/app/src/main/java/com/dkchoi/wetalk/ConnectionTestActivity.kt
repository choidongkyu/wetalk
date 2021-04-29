package com.dkchoi.wetalk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dkchoi.wetalk.databinding.ActivityConnectionTestBinding
import com.dkchoi.wetalk.retrofit.BackendInterface
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class ConnectionTestActivity : AppCompatActivity() {
    lateinit var binding: ActivityConnectionTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BackendInterface.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val server: BackendInterface = retrofit.create(BackendInterface::class.java)

        binding.submitBt.setOnClickListener {
            var id = binding.idEt.text.toString().trim()
            server.setUserRegister(id).enqueue(object: Callback<Int> {
                override fun onResponse(call: Call<Int>, response: Response<Int>) {
                    Toast.makeText(this@ConnectionTestActivity, "respons = ${response.body()}", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<Int>, t: Throwable) {
                    Toast.makeText(this@ConnectionTestActivity, "respons = ${t.printStackTrace()}", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }
}