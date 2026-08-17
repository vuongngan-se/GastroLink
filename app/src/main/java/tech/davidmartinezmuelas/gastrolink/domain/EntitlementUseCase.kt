package tech.davidmartinezmuelas.gastrolink.domain

import tech.davidmartinezmuelas.gastrolink.data.EntitlementRepository
import tech.davidmartinezmuelas.gastrolink.model.Entitlement

class EntitlementUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val isAiEnabledByBuild: Boolean
) {

    suspend fun isPremiumEnabled(): Boolean {
        return isPremiumEnabled(entitlementRepository.getCurrentEntitlement())
    }

    fun isPremiumEnabled(entitlement: Entitlement): Boolean {
        return entitlement == Entitlement.PREMIUM_DEMO
    }

    suspend fun canUseNutritionWithProfile(): Boolean {
        return canUseNutritionWithProfile(entitlementRepository.getCurrentEntitlement())
    }

    fun canUseNutritionWithProfile(entitlement: Entitlement): Boolean {
        return isPremiumEnabled(entitlement)
    }

    suspend fun canUseAiRecommendations(): Boolean {
        return canUseAiRecommendations(entitlementRepository.getCurrentEntitlement())
    }

    fun canUseAiRecommendations(entitlement: Entitlement): Boolean {
        return isAiEnabledByBuild && isPremiumEnabled(entitlement)
    }
}
