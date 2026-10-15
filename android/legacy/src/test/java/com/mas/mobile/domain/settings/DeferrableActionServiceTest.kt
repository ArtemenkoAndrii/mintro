package com.mas.mobile.domain.settings

import com.mas.mobile.DummyTaskService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeferrableActionServiceTest {
    private val actionMock = mockk<DeferrableAction>(relaxed = true)
    private val deferrableActionRepositoryMock = mockk<DeferrableActionRepository>(relaxed = true)

    private val underTest = DeferrableActionService(deferrableActionRepositoryMock, DummyTaskService)

    @BeforeEach
    fun setUp() {
        every { deferrableActionRepositoryMock.getByKey(ACTION_KEY) } returns actionMock
    }

    @Test
    fun `should defer`() {
        underTest.defer(ACTION_KEY)

        verify { actionMock.defer() }
        coVerify { deferrableActionRepositoryMock.save(actionMock) }
        coVerify { deferrableActionRepositoryMock.purge(any()) }
    }

    @Test
    fun `should not be deferred for new action`() {
        val newAction = UniformDaily("test")
        assertFalse(underTest.isDeferred(newAction))
    }

    @Test
    fun `should not`() {
        underTest.remove(ACTION_KEY)

        verify { deferrableActionRepositoryMock.getByKey(ACTION_KEY) }
        coVerify { deferrableActionRepositoryMock.remove(actionMock) }
    }

    companion object {
        val ACTION_KEY = UniformDaily("test")
    }
}