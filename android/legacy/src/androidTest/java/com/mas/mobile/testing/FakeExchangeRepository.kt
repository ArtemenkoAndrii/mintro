package com.mas.mobile.testing

import com.mas.mobile.domain.budget.ExchangeRepository
import java.util.Currency

/**
 * Deterministic [ExchangeRepository] for instrumented tests - never calls the real
 * currency-exchange API. Returns [rate] for every currency pair (default 1.0), unless
 * an override for a specific (base, foreign) pair is supplied via [rates].
 */
class FakeExchangeRepository(
    private val rate: Double = 1.0,
    private val rates: Map<Pair<String, String>, Double> = emptyMap()
) : ExchangeRepository {
    override suspend fun getRate(base: Currency, foreign: Currency): Result<Double> =
        Result.success(rates[base.currencyCode to foreign.currencyCode] ?: rate)
}
