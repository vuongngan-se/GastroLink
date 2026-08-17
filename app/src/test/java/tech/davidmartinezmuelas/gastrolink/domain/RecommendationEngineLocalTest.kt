package tech.davidmartinezmuelas.gastrolink.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.model.RecommendationContext
import tech.davidmartinezmuelas.gastrolink.model.SoloNutritionProfile
import tech.davidmartinezmuelas.gastrolink.model.UserProfile

class RecommendationEngineLocalTest {

    @Test
    fun generateRecommendations_withoutProfileMode_returnsEmptyList() {
        val totals = NutritionTotals(kcal = 1200, proteinG = 20, carbsG = 100, fatG = 60)
        val context = RecommendationContext(
            orderMode = OrderMode.SOLO,
            nutritionMode = NutritionMode.WITHOUT_PROFILE,
            userProfile = null
        )

        val recommendations = RecommendationEngineLocal.generateRecommendations(totals, context)

        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun generateRecommendations_withProfileMode_returnsThresholdMessages() {
        val totals = NutritionTotals(kcal = 980, proteinG = 20, carbsG = 100, fatG = 25)
        val context = RecommendationContext(
            orderMode = OrderMode.SOLO,
            nutritionMode = NutritionMode.WITH_PROFILE,
            userProfile = UserProfile.Solo(
                SoloNutritionProfile(goal = Goal.MAINTAIN)
            )
        )

        val recommendations = RecommendationEngineLocal.generateRecommendations(totals, context)

        assertTrue(recommendations.any { it.contains("calo", ignoreCase = true) })
        assertTrue(recommendations.any { it.contains("đạm", ignoreCase = true) })
    }

    @Test
    fun generateRecommendations_doesNotRepeatMessages() {
        val totals = NutritionTotals(kcal = 1300, proteinG = 10, carbsG = 200, fatG = 90)
        val context = RecommendationContext(
            orderMode = OrderMode.GROUP,
            nutritionMode = NutritionMode.WITH_PROFILE,
            userProfile = UserProfile.Solo(SoloNutritionProfile(goal = Goal.GAIN_MUSCLE))
        )

        val recommendations = RecommendationEngineLocal.generateRecommendations(totals, context)

        assertEquals(recommendations.distinct().size, recommendations.size)
    }

    @Test
    fun generateRecommendations_withProfileMode_returnsAtMostTwoMessages() {
        val totals = NutritionTotals(kcal = 1800, proteinG = 5, carbsG = 250, fatG = 90)
        val context = RecommendationContext(
            orderMode = OrderMode.SOLO,
            nutritionMode = NutritionMode.WITH_PROFILE,
            userProfile = UserProfile.Solo(SoloNutritionProfile(goal = Goal.MAINTAIN))
        )

        val recommendations = RecommendationEngineLocal.generateRecommendations(totals, context)

        assertTrue(recommendations.size <= 2)
    }

    @Test
    fun generateRecommendations_withSameInputs_isDeterministic() {
        val totals = NutritionTotals(kcal = 1200, proteinG = 20, carbsG = 160, fatG = 55)
        val context = RecommendationContext(
            orderMode = OrderMode.GROUP,
            nutritionMode = NutritionMode.WITH_PROFILE,
            userProfile = UserProfile.Solo(SoloNutritionProfile(goal = Goal.GAIN_MUSCLE))
        )

        val first = RecommendationEngineLocal.generateRecommendations(totals, context)
        val second = RecommendationEngineLocal.generateRecommendations(totals, context)

        assertEquals(first, second)
    }
}
