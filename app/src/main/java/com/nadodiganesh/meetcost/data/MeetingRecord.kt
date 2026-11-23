package com.nadodiganesh.meetcost.data

import java.util.UUID

data class MeetingRecord(
    val id: String = UUID.randomUUID().toString(),
    val endedAt: Long = System.currentTimeMillis(),
    val durationSec: Long,
    val attendees: Int,
    val hourlyRate: Double,
    val currency: String,
    val cost: Double,
    val costUsd: Double? = null
)
