package com.mas.mobile.repository.budget

import com.mas.mobile.BuildConfig
import com.mas.mobile.domain.budget.ExchangeRepository
import com.mas.mobile.domain.budget.ExchangeRepositoryException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Currency
import javax.inject.Singleton

@Singleton
class FreeCurrencyAPIRepositoryImpl : ExchangeRepository {
    private val client: FreeCurrencyAPIClient by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.freecurrencyapi.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FreeCurrencyAPIClient::class.java)
    }

    override suspend fun getRate(base: Currency, foreign: Currency): Result<Double> =
        try {
            val result = client.getExchangeRate(BuildConfig.FREE_CURRENCY_API_KEY, foreign.currencyCode, base.currencyCode)
            if (result.isSuccessful) {
                Result.success(result.body()?.data?.get(base.currencyCode)!!)
            } else {
                Result.failure(ExchangeRepositoryException(result.errorBody()?.string() ?: ERROR_TEXT))
            }
        } catch (e: Exception) {
            Result.failure(ExchangeRepositoryException(ERROR_TEXT, e))
        }

    interface FreeCurrencyAPIClient {
        @GET("/v1/latest")
        suspend fun getExchangeRate(
            @Query("apikey")
            apiKey: String,
            @Query("base_currency")
            base: String,
            @Query("currencies")
            foreign: String
        ): Response<ExchangeRateResponse>
    }

    data class ExchangeRateResponse(val data: Map<String, Double>)

    companion object {
        const val ERROR_TEXT = "Exchange request failed."
    }
}


