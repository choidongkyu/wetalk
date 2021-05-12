package com.dkchoi.wetalk.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dkchoi.wetalk.HomeActivity
import com.dkchoi.wetalk.ProfileEditActivity
import com.dkchoi.wetalk.databinding.FragmentProfileBinding
import com.dkchoi.wetalk.util.Util

class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("test11", "oncreateView")
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding!!.profileTxt.setOnClickListener {
            startActivity(Intent(requireActivity(), ProfileEditActivity::class.java))
        }
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        binding!!.profileTxt.text = Util.getMyStatusMsg(requireContext()) // Pref에 저장되어 있는 상태메시지 setting
        binding!!.profileName.text = Util.getMyName(requireContext()) // Pref에 저장되어 있는 이름 setting
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}