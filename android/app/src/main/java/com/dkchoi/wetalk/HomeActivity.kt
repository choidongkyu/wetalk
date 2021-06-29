package com.dkchoi.wetalk

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dkchoi.wetalk.databinding.ActivityHomeBinding
import com.dkchoi.wetalk.fragment.ChatRoomFragment
import com.dkchoi.wetalk.fragment.HomeFragment
import com.dkchoi.wetalk.fragment.ProfileFragment
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {
    private val HOME_CONTAINER = 0
    private val CHAT_CONTAINER = 1
    private val PROFILE_CONTAINER = 2

    lateinit var binding: ActivityHomeBinding
    lateinit var homeFragment: HomeFragment
    lateinit var chatRoomFragment: ChatRoomFragment
    lateinit var profileFragment: ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        homeFragment = HomeFragment()
        chatRoomFragment = ChatRoomFragment()
        profileFragment = ProfileFragment()

        supportFragmentManager.beginTransaction().replace(R.id.container, homeFragment).commit()

        binding.mainTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { //tab 선택 될때
                val pos = tab?.position
                var fragment: Fragment? = null
                when (pos) {
                    HOME_CONTAINER -> fragment = HomeFragment()
                    CHAT_CONTAINER -> fragment = ChatRoomFragment()
                    PROFILE_CONTAINER -> fragment = ProfileFragment()
                }

                fragment?.let {
                    supportFragmentManager.beginTransaction().replace(
                        R.id.container,
                        it
                    ).commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                //nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //nothing
            }
        })
    }
}

