package com.mas.mobile.domain.budget

import com.mas.mobile.DummyTaskService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CategoryServiceTest {
    private val mockCategoryRepository = mockk<CategoryRepository>(relaxed = true)

    private val testInstance = CategoryService(DummyTaskService, mockCategoryRepository)

    @BeforeEach
    fun setUp() {
        every { mockCategoryRepository.getAll() } returns listOf(
            Category(
                id = CategoryId(1),
                iconId = null,
                name = "Food",
                plan = 100.00,
                isActive = true,
                description = "All about food",
                merchants = mutableListOf(Merchant("McDonalds"), Merchant("Starbucks")),
                displayOrder = 0
            ),
            Category(
                id = CategoryId(2),
                name = "Petrol",
                iconId = null,
                plan = 200.00,
                isActive = true,
                description = "All about petrol",
                merchants = mutableListOf(Merchant("Shell")),
                displayOrder = 0
            )
        )
    }

    @Test
    fun `should find category by name ignoring case` () {
        assertEquals("Food", testInstance.findCategoryByName("food")!!.name)
        assertNull(testInstance.findCategoryByName("non-existing"))
    }

    @Test
    fun `should find category by merchant ignoring case` () {
        assertEquals("Petrol", testInstance.findCategoryByMerchant(Merchant("shell"))!!.name)
        assertNull(testInstance.findCategoryByMerchant(Merchant("non-existing")))
    }

    @Test
    fun `should reorder down up` () {
        val old = listOf(
            create(1, 1),
            create(2, 7),
            create(3, 8),
            create(4, 9),
        )
        val new = listOf(
            create(2, 7),
            create(1, 1),
            create(3, 8),
            create(4, 9),
        )

        testInstance.reorder(old, new)

        val result = listOf(
            create(2, 1),
            create(1, 7),
            create(3, 8),
            create(4, 9),
        )

        assertEquals(new, result)
    }

    @Test
    fun `should reorder up down` () {
        val old = listOf(
            create(1, 1),
            create(2, 7),
            create(3, 8),
            create(4, 9),
        )
        val new = listOf(
            create(2, 7),
            create(3, 8),
            create(1, 1),
            create(4, 9),
        )

        testInstance.reorder(old, new)

        val result = listOf(
            create(2, 1),
            create(3, 7),
            create(1, 8),
            create(4, 9),
        )

        assertEquals(new, result)
    }

    private fun create(id: Int, order: Int) =
        CATEGORY.copy(id = CategoryId(id), displayOrder = order)


    companion object {
        val CATEGORY = Category(
            id = CategoryId(0),
            iconId = null,
            name = "",
            plan = 0.00,
            isActive = false,
            description = "",
            merchants= mutableListOf(),
            displayOrder = 0
        )
    }
}