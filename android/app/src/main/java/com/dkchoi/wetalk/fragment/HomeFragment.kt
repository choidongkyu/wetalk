package com.dkchoi.wetalk.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.adapter.FriendListAdapter
import com.dkchoi.wetalk.databinding.FragmentHomeBinding
import com.dkchoi.wetalk.util.RecyclerViewDecoration
import com.dkchoi.wetalk.util.Util
import com.dkchoi.wetalk.viewmodel.HomeFragmentViewModel
import kotlinx.coroutines.*

class HomeFragment : Fragment() {
    private val TAG: String = javaClass.simpleName
    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels() //친구목록 viewmodel 생성
    private lateinit var binding: FragmentHomeBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        val view = binding.root


        val friendListAdapter = FriendListAdapter()
        binding.friendRecyclerView.apply {
            adapter = friendListAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        homeFragmentViewModel.apply {
            getUserList().observe(viewLifecycleOwner, Observer {
                friendListAdapter.updateItem(it)
                binding.friendNumber.text = "${it.size}명"
            })
        }

        binding.addFriendBt.setOnClickListener {
            showAddFriendDialog()
        }


        //새로고침 버튼을 누를시 user refresh
        binding.refreshButton.setOnClickListener {
            activity?.let { it ->
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.progressbar.visibility = View.VISIBLE
                    withContext(Dispatchers.IO) {
                        homeFragmentViewModel.refreshUserList(it.application)
                    }
                    binding.progressbar.visibility = View.GONE
                }
            }
        }

        binding.friendRecyclerView.addItemDecoration(RecyclerViewDecoration(30)) // 아이템간 간격 설정

        return view
    }

    override fun onResume() {
        super.onResume()
        val imgPath = "${Util.profileImgPath}/${Util.getPhoneNumber(requireContext())}.jpg"
        Glide.with(requireContext())
            .load(imgPath)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(Util.getMyImg(requireContext())!!))
            .into(binding.myProfileImg)
        loadMyName()
    }

    private fun loadMyName() {
        binding.myProfileName.text = Util.getMyName(requireContext())
        binding.myProfileStatus.text = Util.getMyStatusMsg(requireContext())
    }

    private fun showAddFriendDialog() {
        val editText = EditText(requireContext())
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        dialog.setTitle("친구 추가")
        dialog.setMessage("번호를 입력하세요.")
        dialog.setView(editText)
        dialog.setPositiveButton("확인") { _, _ ->
            var id = editText.text.toString().trim()
            if(!id.contains("+82")) {
                id = id.replaceFirst("0", "+82")
            }
            activity?.let { it ->
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.progressbar.visibility = View.VISIBLE
                    withContext(Dispatchers.IO) {
                        homeFragmentViewModel.addFriend(it.application, id)
                    }
                    binding.progressbar.visibility = View.GONE
                }
            }
        }

        dialog.setNegativeButton("취소") { _, _ ->
            //nothing
        }

        dialog.show()
    }
}
