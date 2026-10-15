package com.mas.mobile.domain.budget

import java.util.Currency

interface ExchangeRepository {
    suspend fun getRate(base: Currency, foreign: Currency): Result<Double>
}

class ExchangeRepositoryException : Exception {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}