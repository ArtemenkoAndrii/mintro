package com.mas.mobile.domain.settings

import android.content.Context
import android.util.Log
import com.mas.mobile.BuildConfig
import com.mas.mobile.service.CoroutineService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsService @Inject constructor(
    private val context: Context,
    private val coroutineService: CoroutineService,
    val repository: SettingsRepository
) {
    var autodetect: Boolean
        get() = repository.get().discoveryMode
        set(value) {
            coroutineService.backgroundTask {
                val allSettings = repository.get()
                allSettings.discoveryMode = value
                repository.save(allSettings)
            }
        }

    fun isFirstLaunch(): Boolean =
        context.getSharedPreferences(BuildConfig.APPLICATION_ID, 0).getBoolean(FIRST_RUN_KEY, true)

    fun completeFirstLaunch() {
        if (isFirstLaunch()) {
            Log.i(this::class.simpleName, "Committing first app run.")
            context.getSharedPreferences(BuildConfig.APPLICATION_ID, 0).edit().also {
                it.putBoolean(FIRST_RUN_KEY, false)
                it.commit()
            }
        }
    }

    fun isMessageCapturingEnabled() =
        with(repository.get()) {
            captureSms || captureNotifications
        }

    fun needToShowPolicy() =
        repository.get().policyVersion != POLICY_VERSION

    fun confirmPolicyReading() {
        coroutineService.backgroundTask {
            val settings = repository.get()
            settings.policyVersion = POLICY_VERSION
            repository.save(settings)
        }
    }

    companion object {
        private const val FIRST_RUN_KEY = "firstRun"
        const val POLICY_VERSION = "privacy_policy_v3"
    }
}
