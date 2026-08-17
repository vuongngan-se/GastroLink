package tech.davidmartinezmuelas.gastrolink.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.AiChatRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiChatResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanResponse
import java.io.IOException
import tech.davidmartinezmuelas.gastrolink.model.Entitlement
import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.model.RecommendationContext
import tech.davidmartinezmuelas.gastrolink.model.SoloNutritionProfile
import tech.davidmartinezmuelas.gastrolink.model.UserProfile

class GetRecommendationUseCaseTest {

    @Test
    fun execute_withoutProfileMode_returnsNone() = runTest {
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success("IA: ok"),
            isAiEnabledByBuild = true,
            includeDebugInfo = false
        )

        val result = useCase.execute(
            totals = triggerTotals(),
            cartItems = emptyList(),
            context = baseContext(nutritionMode = NutritionMode.WITHOUT_PROFILE),
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = true
        )

        assertEquals(RecommendationSource.NONE, result.source)
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun execute_freeEntitlement_returnsNone() = runTest {
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success("IA: ok"),
            isAiEnabledByBuild = true,
            includeDebugInfo = false
        )

        val result = useCase.execute(
            totals = triggerTotals(),
            cartItems = emptyList(),
            context = baseContext(),
            entitlement = Entitlement.FREE,
            useAiEnabledByUser = true
        )

        assertEquals(RecommendationSource.NONE, result.source)
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun execute_premiumWithAiDisabled_returnsLocalRules() = runTest {
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success("IA: ok"),
            isAiEnabledByBuild = true,
            includeDebugInfo = true
        )

        val totals = triggerTotals()
        val context = baseContext()
        val expectedLocal = RecommendationEngineLocal
            .generateRecommendations(totals = totals, context = context)
            .take(2)

        val result = useCase.execute(
            totals = totals,
            cartItems = emptyList(),
            context = context,
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = false
        )

        assertEquals(RecommendationSource.LOCAL_RULES, result.source)
        assertEquals(expectedLocal, result.messages)
        assertTrue(result.debugInfo?.contains("tắt", ignoreCase = true) == true)
    }

    @Test
    fun execute_premiumWithAiEnabledAndSuccess_returnsAi() = runTest {
        val aiText = "Primera sugerencia. Segunda sugerencia."
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success(aiText),
            isAiEnabledByBuild = true,
            includeDebugInfo = true
        )

        val result = useCase.execute(
            totals = triggerTotals(),
            cartItems = emptyList(),
            context = baseContext(),
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = true
        )

        assertEquals(RecommendationSource.AI, result.source)
        assertEquals(listOf("Primera sugerencia", "Segunda sugerencia"), result.messages)
        assertTrue(result.debugInfo?.contains("Modelo", ignoreCase = true) == true)
    }

    @Test
    fun execute_premiumWithAiEnabledAndEmptyResponse_returnsLocalRules() = runTest {
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success(""),
            isAiEnabledByBuild = true,
            includeDebugInfo = true
        )

        val totals = triggerTotals()
        val context = baseContext()
        val expectedLocal = RecommendationEngineLocal
            .generateRecommendations(totals = totals, context = context)
            .take(2)

        val result = useCase.execute(
            totals = totals,
            cartItems = emptyList(),
            context = context,
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = true
        )

        assertEquals(RecommendationSource.LOCAL_RULES, result.source)
        assertEquals(expectedLocal, result.messages)
    }

    @Test
    fun execute_premiumWithAiEnabledAndLongResponse_sanitizesCorrectly() = runTest {
        val longText = "Primera sugerencia nutricional importante. " +
            "Segunda sugerencia sobre proteínas. " +
            "x".repeat(600)
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Success(longText),
            isAiEnabledByBuild = true,
            includeDebugInfo = false
        )

        val result = useCase.execute(
            totals = triggerTotals(),
            cartItems = emptyList(),
            context = baseContext(),
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = true
        )

        // Response is long but should be processed (not discarded) — returns AI source
        assertEquals(RecommendationSource.AI, result.source)
        assertTrue(result.messages.isNotEmpty())
        assertTrue(result.messages.size <= 2)
    }

    @Test
    fun execute_premiumWithAiEnabledAndFailure_returnsLocalRules() = runTest {
        val useCase = GetRecommendationUseCase(
            aiRecommendationService = FakeAiRecommendationService.Failure,
            isAiEnabledByBuild = true,
            includeDebugInfo = true
        )

        val totals = triggerTotals()
        val context = baseContext()
        val expectedLocal = RecommendationEngineLocal
            .generateRecommendations(totals = totals, context = context)
            .take(2)

        val result = useCase.execute(
            totals = totals,
            cartItems = emptyList(),
            context = context,
            entitlement = Entitlement.PREMIUM_DEMO,
            useAiEnabledByUser = true
        )

        assertEquals(RecommendationSource.LOCAL_RULES, result.source)
        assertEquals(expectedLocal, result.messages)
        assertTrue(result.debugInfo?.contains("phòng", ignoreCase = true) == true)
    }

    private fun baseContext(
        nutritionMode: NutritionMode = NutritionMode.WITH_PROFILE
    ): RecommendationContext {
        return RecommendationContext(
            orderMode = OrderMode.SOLO,
            nutritionMode = nutritionMode,
            userProfile = UserProfile.Solo(
                SoloNutritionProfile(goal = Goal.MAINTAIN)
            )
        )
    }

    private fun triggerTotals(): NutritionTotals {
        return NutritionTotals(
            kcal = 1100,
            proteinG = 20,
            carbsG = 120,
            fatG = 50
        )
    }

    private sealed class FakeAiRecommendationService : AiRecommendationService {
        override suspend fun chat(request: AiChatRequest): AiChatResponse {
            return AiChatResponse(reply = "")
        }
        override suspend fun scanDish(request: AiDishScanRequest): AiDishScanResponse {
            return AiDishScanResponse(name = "", kcal = 0, proteinG = 0, carbsG = 0, fatG = 0, ingredients = emptyList())
        }
        override suspend fun uploadSync(request: tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadRequest): tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadResponse {
            return tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadResponse(status = "success", message = "")
        }
        override suspend fun downloadSync(userId: String): tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncDownloadResponse {
            return tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncDownloadResponse(status = "success", profile = null, savedProfiles = emptyList(), orders = emptyList())
        }

        data class Success(private val text: String) : FakeAiRecommendationService() {
            override suspend fun generate(request: AiRecommendationRequest): AiRecommendationResponse {
                return AiRecommendationResponse(
                    recommendationText = text,
                    model = "fake-model"
                )
            }
        }

        object Failure : FakeAiRecommendationService() {
            override suspend fun generate(request: AiRecommendationRequest): AiRecommendationResponse {
                throw IOException("network error")
            }
        }
    }
}
