package com.mas.mobile.domain.budget

import androidx.lifecycle.LiveData
import com.mas.mobile.domain.Repository

interface CategoryRepository : Repository<Category> {
    val live: CategoryLiveData
    fun getAll(): List<Category>
    fun getById(id: CategoryId): Category?
    fun create(): Category
}

interface CategoryLiveData {
    fun getAll(): LiveData<List<Category>>
}
