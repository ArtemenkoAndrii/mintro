package com.mas.mobile.repository.settings

import com.mas.mobile.domain.settings.Settings
import com.mas.mobile.domain.settings.SettingsRepository
import com.mas.mobile.repository.db.config.AppDatabase
import javax.inject.Singleton
import com.mas.mobile.repository.db.entity.Settings as SettingsData

@Singleton
class SettingsRepositoryImpl(val db: AppDatabase) : SettingsRepository {
    override fun get(): Settings =
        db.settingsDao().getAll().toModel()

    override suspend fun save(item: Settings) {
        item.toDto().forEach {
            db.settingsDao().upsert(it)
        }
    }

    override suspend fun remove(item: Settings) {
        TODO("Not supported!")
    }

    private fun List<SettingsData>.toModel() = SettingsMapper.toModel(this)
    private fun Settings.toDto() = SettingsMapper.toDto(this)
}