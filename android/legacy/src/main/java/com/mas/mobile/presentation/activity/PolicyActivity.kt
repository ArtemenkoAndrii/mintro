package com.mas.mobile.presentation.activity

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.domain.settings.SettingsService
import javax.inject.Inject

class PolicyActivity : Activity() {
    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.appComponent.injectPolicyActivity(this)

        setContentView(R.layout.policy_activity)

        findViewById<WebView?>(R.id.policy_web_view)?.let {
            it.settings.allowContentAccess = true
            it.settings.allowFileAccess = true
            it.loadUrl("file:///android_res/raw/${SettingsService.POLICY_VERSION}.html")
        }

        findViewById<Button>(R.id.btn_policy_agree).setOnClickListener {
            settingsService.confirmPolicyReading()
            this.finish()
        }

        findViewById<Button>(R.id.btn_policy_decline).setOnClickListener {
            this.finishAffinity()
        }
    }
}