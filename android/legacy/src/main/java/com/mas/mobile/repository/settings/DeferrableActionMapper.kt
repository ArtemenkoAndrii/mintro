package com.mas.mobile.repository.settings

import com.mas.mobile.domain.settings.DeferrableAction
import com.mas.mobile.domain.settings.ProgressiveDaily
import com.mas.mobile.domain.settings.UniformDaily
import com.mas.mobile.repository.db.entity.DeferrableAction as DeferredActionDTO

object DeferrableActionMapper {
    fun toModel(dto: DeferredActionDTO): DeferrableAction =
        DeferrableAction(
            key = when(dto.type) {
                DeferredActionDTO.PROGRESSIVE -> ProgressiveDaily(dto.key)
                else -> UniformDaily(dto.key)
            },
            activeAfter = dto.activeAfter,
            increment = DeferrableAction.Hours(dto.increment)
        )

    fun toDTO(model:  DeferrableAction): DeferredActionDTO =
        DeferredActionDTO(
            key = model.key.value,
            type = when(model.key) {
                is ProgressiveDaily -> DeferredActionDTO.PROGRESSIVE
                else -> DeferredActionDTO.UNIFORM
            },
            increment = model.increment.value,
            activeAfter = model.activeAfter
        )
}
