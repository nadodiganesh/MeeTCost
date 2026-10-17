package com.nadodiganesh.meetcost.data

import retrofit2.http.GET
import retrofit2.http.Path

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

interface CurrencyService {
    @GET("latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeRateResponse
}
