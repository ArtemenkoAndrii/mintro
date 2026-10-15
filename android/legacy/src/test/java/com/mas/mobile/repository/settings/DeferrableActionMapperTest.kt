package com.mas.mobile.repository.settings

import com.mas.mobile.domain.settings.DeferrableAction
import com.mas.mobile.domain.settings.ProgressiveDaily
import com.mas.mobile.domain.settings.UniformDaily
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import com.mas.mobile.repository.db.entity.DeferrableAction as DeferredActionDTO

class DeferrableActionMapperTest {
    
    @Test
    fun `should map DTO`() {
        val model = DeferrableActionMapper.toModel(DTO)
        assertEquals(KEY, model.key.value)
        assertEquals(DATE, model.activeAfter)
        assertEquals(INCREMENT, model.increment.value)
        assertTrue(model.key is UniformDaily)
    }

    @Test
    fun `should map model`() {
        val dto = DeferrableActionMapper.toDTO(MODEL)
        assertEquals(KEY, dto.key)
        assertEquals(DATE, dto.activeAfter)
        assertEquals(INCREMENT, dto.increment)
        assertEquals(DeferredActionDTO.UNIFORM, dto.type)
    }

    @Test
    fun `should map progressive`() {
        val model = DeferrableActionMapper.toModel(DTO.copy(type = DeferredActionDTO.PROGRESSIVE))
        assertTrue(model.key is ProgressiveDaily)

        val dto = DeferrableActionMapper.toDTO(MODEL.copy(key = ProgressiveDaily(KEY)))
        assertEquals(DeferredActionDTO.PROGRESSIVE, dto.type)
    }

    private companion object {
        const val KEY = "test"
        const val INCREMENT = 1L
        val DATE: LocalDateTime = LocalDateTime.now()

        val MODEL = DeferrableAction(
            key = UniformDaily(KEY),
            activeAfter = DATE,
            increment = DeferrableAction.Hours(INCREMENT)
        )
        val DTO = DeferredActionDTO(
            key = KEY,
            type = DeferredActionDTO.UNIFORM,
            increment = INCREMENT,
            activeAfter = DATE
        )
    }
}