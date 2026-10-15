package com.mas.mobile.domain.analytics

interface EventLogger {
    fun log(event: Event)
}