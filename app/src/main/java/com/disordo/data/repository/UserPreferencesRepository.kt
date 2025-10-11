package com.disordo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    suspend fun saveLoginState(isLoggedIn: Boolean, userId: String?) {
        dataStore.edit {
            it[IS_LOGGED_IN_KEY] = isLoggedIn
            if (isLoggedIn && userId != null) {
                it[USER_ID_KEY] = userId
            } else {
                it.remove(USER_ID_KEY)
            }
        }
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map {
        it[IS_LOGGED_IN_KEY] ?: false
    }

    val userIdFlow: Flow<String?> = dataStore.data.map {
        it[USER_ID_KEY]
    }
}
