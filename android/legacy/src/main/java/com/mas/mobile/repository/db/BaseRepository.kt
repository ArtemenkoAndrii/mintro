package com.mas.mobile.repository.db

interface BaseRepository<T: Any> {
    fun getById(id: Int): T?
    fun clone(item: T): T
    fun createNew(): T
    suspend fun insert(item: T): Long
    suspend fun update(item: T)
    suspend fun delete(item: T)
}