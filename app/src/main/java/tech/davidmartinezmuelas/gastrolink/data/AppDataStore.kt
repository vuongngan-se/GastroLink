package tech.davidmartinezmuelas.gastrolink.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.appDataStore by preferencesDataStore(name = "gastrolink_settings")
