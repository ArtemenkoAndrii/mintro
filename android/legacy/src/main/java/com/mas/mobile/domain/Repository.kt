package com.mas.mobile.domain

interface Repository<T> {
    suspend fun save(item: T)
    suspend fun remove(item: T)
}