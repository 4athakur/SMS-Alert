package com.example.server

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val carrierName: String,
    val displayName: String,
    val phoneNumber: String?
)

object SmsDispatcher {

    const val ACTION_SMS_SENT = "com.example.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.example.SMS_DELIVERED"
    const val EXTRA_MESSAGE_ID = "extra_message_id"

    fun getAvailableSimCards(context: Context): List<SimCardInfo> {
        val simList = mutableListOf<SimCardInfo>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return simList
        }

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

            activeList?.forEachIndexed { index, info ->
                val number = if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
                    try { info.number } catch (e: Exception) { null }
                } else null

                simList.add(
                    SimCardInfo(
                        subscriptionId = info.subscriptionId,
                        slotIndex = info.simSlotIndex,
                        carrierName = info.carrierName?.toString() ?: "SIM ${index + 1}",
                        displayName = info.displayName?.toString() ?: "SIM ${index + 1}",
                        phoneNumber = number
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return simList
    }

    fun sendSms(
        context: Context,
        phoneNumber: String,
        message: String,
        messageId: String,
        simSlot: Int = 0
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        try {
            val availableSims = getAvailableSimCards(context)
            val selectedSim = availableSims.find { it.slotIndex == simSlot } ?: availableSims.firstOrNull()

            val smsManager: SmsManager = if (selectedSim != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(selectedSim.subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(selectedSim.subscriptionId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val sentIntent = Intent(ACTION_SMS_SENT).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
                setPackage(context.packageName)
            }
            val sentPI = PendingIntent.getBroadcast(
                context,
                messageId.hashCode(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
                setPackage(context.packageName)
            }
            val deliveredPI = PendingIntent.getBroadcast(
                context,
                messageId.hashCode(),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentPIs = ArrayList<PendingIntent>()
                val deliveredPIs = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentPIs.add(sentPI)
                    deliveredPIs.add(deliveredPI)
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentPIs, deliveredPIs)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
