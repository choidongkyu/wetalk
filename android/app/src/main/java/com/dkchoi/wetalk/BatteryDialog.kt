package com.dkchoi.wetalk

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment


class BatteryDialog : DialogFragment() {
    override fun onCreateDialog(a_savedInstanceState: Bundle?): Dialog {
        val clickListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                val intent: Intent = permissionIntent
                startActivity(intent)
            }
        val message = "정상적인 메시지 수신을 위해서는 해당 어플을 \"배터리 사용량 최적화\" 목록에서 \"제외\"해야 합니다.\n weTalk 앱을 찾아 제외시켜 주세요."
        val builder: android.app.AlertDialog.Builder =
            android.app.AlertDialog.Builder(getActivity())
        builder.setMessage(message)
            .setPositiveButton("네", clickListener)
        return builder.create()
    }

    private val permissionIntent: Intent
        get() {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            return intent
        }
}