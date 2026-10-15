package com.mas.mobile.domain.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


class DeferrableActionTest {
    @Test
    fun `should be deferred`() {
        assertTrue(DEFERRED_ACTION.isDeferred())
    }

    @Test
    fun `should be active`() {
        assertFalse(ACTIVE_ACTION.isDeferred())
    }

    @Test
    fun `should defer`() {
        val activeAction = ACTIVE_ACTION.copy()
        activeAction.defer()
        assertTrue(activeAction.isDeferred())
    }

    @Test
    fun `should not defer twice`() {
        val activeAction = ACTIVE_ACTION.copy()

        activeAction.defer()
        val deferredFirst = activeAction.activeAfter
        activeAction.defer()
        val deferredSecond = activeAction.activeAfter

        assertEquals(deferredFirst, deferredSecond)
    }

    @Test
    fun `should defer uniformly`() {
        val action = DeferrableAction(
            key = UniformDaily("test"),
            activeAfter = LocalDateTime.now().minusDays(1L),
            increment = ONE_DAY
        ).also {
            it.defer()
        }

        assertEquals(ONE_DAY, action.increment)
    }

    @Test
    fun `should defer progressively`() {
        val action = DeferrableAction(
            key = ProgressiveDaily("test"),
            activeAfter = LocalDateTime.now().minusDays(1L),
            increment = ONE_DAY
        ).also {
            it.defer()
        }

        assertEquals(TWO_DAY, action.increment)
    }

    companion object {
        val ONE_DAY = DeferrableAction.Hours(1)
        val TWO_DAY = DeferrableAction.Hours(2)

        val ACTIVE_ACTION = DeferrableAction(
            key = UniformDaily("test"),
            activeAfter = LocalDateTime.now().minusDays(1L),
            increment = ONE_DAY
        )
        val DEFERRED_ACTION = DeferrableAction(
            key = UniformDaily("test"),
            activeAfter = LocalDateTime.now().plusDays(1L),
            increment = ONE_DAY
        )
    }
}