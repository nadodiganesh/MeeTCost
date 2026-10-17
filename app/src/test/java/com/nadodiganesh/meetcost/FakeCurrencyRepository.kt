package com.nadodiganesh.meetcost

import com.nadodiganesh.meetcost.data.CurrencyRepository

class FakeCurrencyRepository(private val rates: Map<String, Double>) : CurrencyRepository {
    override suspend fun fetchRates(base: String): Map<String, Double>? {
        return rates
    }
}
