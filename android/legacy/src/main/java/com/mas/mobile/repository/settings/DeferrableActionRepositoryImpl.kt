package com.mas.mobile.repository.settings

import com.mas.mobile.domain.settings.DeferrableAction
import com.mas.mobile.domain.settings.DeferrableActionRepository
import com.mas.mobile.repository.db.config.AppDatabase
import com.mas.mobile.repository.db.entity.DeferrableAction as DeferredActionDTO
import java.time.LocalDateTime
import javax.inject.Singleton

@Singleton
class DeferrableActionRepositoryImpl(val db: AppDatabase) : DeferrableActionRepository {
    override fun getByKey(key: DeferrableAction.Key): DeferrableAction? {
        return db.deferredActionDAO().getByKey(key.value)?.toModel()
    }

    override suspend fun purge(olderThen: LocalDateTime) {
        db.deferredActionDAO().purge(olderThen)
    }

    override suspend fun save(item: DeferrableAction) {
        db.deferredActionDAO().upsert(item.toDTO())
    }

    override suspend fun remove(item: DeferrableAction) {
        db.deferredActionDAO().remove(item.toDTO())
    }
}

fun DeferredActionDTO.toModel() = DeferrableActionMapper.toModel(this)
fun DeferrableAction.toDTO() = DeferrableActionMapper.toDTO(this)