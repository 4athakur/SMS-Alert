package com.example.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.example.data.db.AppDatabase
import com.example.server.SmsDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra(SmsDispatcher.EXTRA_MESSAGE_ID) ?: return
        val db = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            when (intent.action) {
                SmsDispatcher.ACTION_SMS_SENT -> {
                    val status = when (resultCode) {
                        Activity.RESULT_OK -> "SENT"
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "FAILED: Generic Failure"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "FAILED: No Service"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "FAILED: Null PDU"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "FAILED: Radio Off"
                        else -> "FAILED: Code $resultCode"
                    }
                    val isSuccess = resultCode == Activity.RESULT_OK
                    db.smsLogDao().updateStatus(
                        messageId = messageId,
                        status = if (isSuccess) "SENT" else "FAILED",
                        errorMessage = if (!isSuccess) status else null
                    )
                }
                SmsDispatcher.ACTION_SMS_DELIVERED -> {
                    db.smsLogDao().updateStatus(
                        messageId = messageId,
                        status = "DELIVERED"
                    )
                }
            }
        }
    }
}
