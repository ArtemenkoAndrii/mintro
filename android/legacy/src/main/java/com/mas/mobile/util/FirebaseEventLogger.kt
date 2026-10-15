package com.mas.mobile.util

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.mas.mobile.domain.analytics.Event
import com.mas.mobile.domain.analytics.EventLogger
import javax.inject.Singleton

@Singleton
class FirebaseEventLogger : EventLogger {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    override fun log(event: Event) {
        try {
            val paramBundle = Bundle()
            event.getParams().forEach {
                paramBundle.putString(it.key.name, it.value.toString())
            }
            firebaseAnalytics.logEvent(event.getName(), paramBundle)
        } catch (e: Throwable) {
            Log.e(this::class.java.name, "FirebaseAnalytics failed", e)
        }
    }
}

