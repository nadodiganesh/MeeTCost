package com.nadodiganesh.meetcost.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meetcost_preferences")

interface MeetingHistoryRepository {
    val meetings: Flow<List<MeetingRecord>>
    val isIntroDismissed: Flow<Boolean>
    suspend fun addMeeting(record: MeetingRecord)
    suspend fun deleteMeeting(id: String)
    suspend fun clearHistory()
    suspend fun setIntroDismissed(dismissed: Boolean)
}

class RealMeetingHistoryRepository(private val context: Context) : MeetingHistoryRepository {
    private val gson = Gson()
    private val listType = object : TypeToken<List<MeetingRecord>>() {}.type

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("meetcost.history.v1")
        private val KEY_INTRO_DISMISSED = booleanPreferencesKey("meetcost.intro.dismissed")
    }

    override val meetings: Flow<List<MeetingRecord>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                gson.fromJson<List<MeetingRecord>>(json, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override val isIntroDismissed: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_INTRO_DISMISSED] ?: false
    }

    override suspend fun addMeeting(record: MeetingRecord) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[KEY_HISTORY]
            val currentList: List<MeetingRecord> = if (!currentJson.isNullOrBlank()) {
                try {
                    gson.fromJson(currentJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            val updated = listOf(record) + currentList
            prefs[KEY_HISTORY] = gson.toJson(updated)
        }
    }

    override suspend fun deleteMeeting(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[KEY_HISTORY]
            if (!currentJson.isNullOrBlank()) {
                val currentList: List<MeetingRecord> = try {
                    gson.fromJson(currentJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                val updated = currentList.filter { it.id != id }
                prefs[KEY_HISTORY] = gson.toJson(updated)
            }
        }
    }

    override suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }

    override suspend fun setIntroDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INTRO_DISMISSED] = dismissed
        }
    }
}
