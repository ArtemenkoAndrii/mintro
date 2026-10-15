package com.mas.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.mas.mobile.appComponent
import com.mas.mobile.domain.message.MessageService
import java.time.LocalDateTime
import javax.inject.Inject

const val SMS_FORMAT = "format"
const val SMS_BUNDLE = "pdus"
const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"

class SmsListener: BroadcastReceiver() {
    @Inject
    lateinit var messageService: MessageService

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.injectSmsListener(this)

        if (intent.action == SMS_RECEIVED && intent.extras != null) {
            val intentExtras = intent.extras!!
            val sms = intentExtras.get(SMS_BUNDLE) as Array<*>

            (sms.indices).forEach { i ->
                val format = intentExtras.getString(SMS_FORMAT)
                val smsMessage = SmsMessage.createFromPdu(sms[i] as ByteArray, format)

                messageService.handleRawMessage(
                    sender = smsMessage.originatingAddress.orEmpty(),
                    text = smsMessage.messageBody.trim(),
                    date = LocalDateTime.now()
                )
            }
        }
    }
}