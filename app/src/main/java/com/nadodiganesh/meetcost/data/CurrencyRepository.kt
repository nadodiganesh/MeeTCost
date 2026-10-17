package com.nadodiganesh.meetcost.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface CurrencyRepository {
    suspend fun fetchRates(base: String = "USD"): Map<String, Double>?
}

class RealCurrencyRepository : CurrencyRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/v6/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(CurrencyService::class.java)

    override suspend fun fetchRates(base: String): Map<String, Double>? {
        return try {
            val response = service.getLatestRates(base)
            if (response.result == "success") response.rates else null
        } catch (e: Exception) {
            null
        }
    }
}
