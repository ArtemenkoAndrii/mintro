package com.mas.mobile.domain.settings

import com.mas.mobile.domain.Repository

interface SettingsRepository : Repository<Settings> {
    fun get(): Settings
}