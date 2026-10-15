package com.mas.mobile.domain.message

interface MessageAnalyzer {
    suspend fun buildPattern(message: String): Pattern?
}
