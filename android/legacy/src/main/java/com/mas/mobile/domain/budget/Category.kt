package com.mas.mobile.domain.budget

data class Category(
    val id: CategoryId,
    var iconId: IconId?,
    var name: String,
    var plan: Double,
    var isActive: Boolean,
    var description: String,
    var merchants: MutableList<Merchant>,
    var displayOrder: Int
)

@JvmInline
value class CategoryId(val value: Int)

@JvmInline
value class Merchant(val value: String)

@JvmInline
value class IconId(val value: Int)