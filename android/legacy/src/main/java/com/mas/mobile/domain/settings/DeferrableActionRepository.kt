package com.mas.mobile.domain.settings

import com.mas.mobile.domain.Repository
import java.time.LocalDateTime

interface DeferrableActionRepository : Repository<DeferrableAction> {
    fun getByKey(key: DeferrableAction.Key): DeferrableAction?
    suspend fun purge(olderThen: LocalDateTime)
}