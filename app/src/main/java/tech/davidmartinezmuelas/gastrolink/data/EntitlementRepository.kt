package tech.davidmartinezmuelas.gastrolink.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.davidmartinezmuelas.gastrolink.model.Entitlement

interface EntitlementRepository {
    val entitlementFlow: Flow<Entitlement>

    suspend fun setPremiumDemoEnabled(enabled: Boolean)

    suspend fun getCurrentEntitlement(): Entitlement
}

class DataStoreEntitlementRepository(
    private val context: Context
) : EntitlementRepository {

    private val premiumDemoEnabledKey = booleanPreferencesKey("premium_demo_enabled")

    override val entitlementFlow: Flow<Entitlement> = context.appDataStore.data
        .map { preferences ->
            if (preferences[premiumDemoEnabledKey] == true) {
                Entitlement.PREMIUM_DEMO
            } else {
                Entitlement.FREE
            }
        }

    override suspend fun setPremiumDemoEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[premiumDemoEnabledKey] = enabled
        }
    }

    override suspend fun getCurrentEntitlement(): Entitlement {
        return entitlementFlow.first()
    }
}
