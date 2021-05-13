package com.dkchoi.wetalk.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.dkchoi.wetalk.ProfileMsgEditActivity
import com.dkchoi.wetalk.R
import com.dkchoi.wetalk.databinding.FragmentProfileBinding
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                when (it.value) {
                    true -> showPhotoDialog() //권한이 있다면
                    false -> Toast.makeText(requireContext(), "카메라 권한이 필요합니다", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    //startForActivity result 결과
    private val requestActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data?.data != null) { //갤러리 캡쳐 결과값
                var bitmap: Bitmap? = null
                if (Build.VERSION.SDK_INT < 29) {
                    bitmap =
                        MediaStore.Images.Media.getBitmap(activity?.contentResolver, it.data?.data)
                } else {
                    val source: ImageDecoder.Source? =
                        activity?.let { it1 ->
                            ImageDecoder.createSource(
                                it1.contentResolver,
                                it.data?.data!!
                            )
                        }
                    bitmap = source?.let { it1 -> ImageDecoder.decodeBitmap(it1) }
                }
                val name = Util.getPhoneNumber(requireContext())
                processTheImage(bitmap!!, name) // 이미지 저장
            } else if (it.data?.extras?.get("data") != null) { // 카메라 캡쳐 결과갑
                val bitmap = it.data?.extras?.get("data") as Bitmap?
                binding.profileImg.setImageBitmap(bitmap)
                val name = Util.getPhoneNumber(requireContext())
                processTheImage(bitmap!!, name) //이미지 저장
            }
        }

    private val permissions = arrayOf<String>(
        Manifest.permission.CAMERA,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.profileTxt.setOnClickListener {
            startActivity(Intent(requireActivity(), ProfileMsgEditActivity::class.java))
        }

        binding.profileImg.setOnClickListener {
            if (!Util.hasPermissions(permissions, requireContext())) { // 카메라 권한이 없다면
                requestMultiplePermissions.launch(permissions) // 권한 요청
            } else {
                showPhotoDialog()
            }
        }

        val imgPath = "${Util.profileImgPath}/${Util.getPhoneNumber(requireContext())}.jpg"
        Glide.with(requireContext())
            .load(imgPath)
            .error(R.drawable.ic_baseline_account_circle_24_black)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(Util.getMyImg(requireContext())!!))
            .into(binding.profileImg)

        return binding.root
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        val statusMsg = Util.getMyStatusMsg(requireContext())
        if (statusMsg.equals("")) {
            binding.profileTxt.text = "상태 메시지를 입력해주세요."
        } else {
            binding.profileTxt.text = statusMsg
        }

        // Pref에 저장되어 있는 상태메시지 setting
        binding.profileName.text = Util.getMyName(requireContext()) // Pref에 저장되어 있는 이름 setting
    }

    private fun showPhotoDialog(): Unit {
        val options = arrayOf("갤러리에서 가져오기", "카메라 열기")
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("프로필 사진 설정")
        alertDialog.setSingleChoiceItems(options, -1) { dialog, item ->
            if (item == 0) { // 갤러리
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                requestActivity.launch(intent)
                dialog.dismiss()
            } else {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                requestActivity.launch(intent)
                dialog.dismiss()
            }
        }
        alertDialog.show()
    }

    private suspend fun saveBitmapToJpeg(bitmap: Bitmap, name: String) {

        //내부 저장소 캐쉬 경로 받아옴
        val storage: File? = activity?.cacheDir
        //저장할 파일 이름
        val fileName = "$name.jpg"
        //파일 인스턴스 생성
        val tempFile = File(storage, fileName)
        //빈파일 자동으로 생성
        tempFile.createNewFile()
        //stream 생성
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.close()

        uploadImage(tempFile.path) //서버에 업로드

    }

    private fun processTheImage(bitmap: Bitmap, name: String) {
        lifecycleScope.launch {
            binding.progressbar.visibility = View.VISIBLE
            saveBitmapToJpeg(bitmap, name)

            Util.fetchMyData(requireContext())

            val imgPath = "${Util.profileImgPath}/${Util.getPhoneNumber(requireContext())}.jpg"
            Glide.with(requireContext())
                .load(imgPath)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(Util.getMyImg(requireContext())!!))
                .into(binding.profileImg)
            binding.progressbar.visibility = View.GONE
        }
    }

    private suspend fun uploadImage(path: String) {
        val file = File(path)
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), file)
        val requestFile: MultipartBody.Part =
            MultipartBody.Part.createFormData("file", file.name, requestBody)

        val name =
            RequestBody.create(MediaType.parse("text/plain"), Util.getPhoneNumber(requireContext()))

        val server = ServiceGenerator.retrofit.create(BackendInterface::class.java)

        val result = server.uploadFile(requestFile, name)
        if (result == 200) {
            Log.d("test11", "업로드 성공")
        } else {
            Log.d("test11", "업로드 실패 result = $result")
        }

    }
}
