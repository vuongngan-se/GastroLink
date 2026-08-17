package tech.davidmartinezmuelas.gastrolink.domain

import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.RecommendationContext
import tech.davidmartinezmuelas.gastrolink.model.UserProfile

object RecommendationEngineLocal {

    fun generateRecommendations(
        totals: NutritionTotals,
        context: RecommendationContext
    ): List<String> {
        if (context.nutritionMode != NutritionMode.WITH_PROFILE) {
            return emptyList()
        }

        val kcalHighThreshold = when (val profile = context.userProfile) {
            is UserProfile.Solo -> {
                val tdee = NutritionCalculator.calculateTdee(profile.profile)
                tdee?.div(3) ?: when (profile.profile.goal) {
                    Goal.LOSE_WEIGHT -> 750
                    Goal.GAIN_MUSCLE -> 1100
                    else -> 900
                }
            }
            is UserProfile.Group -> 1100
            null -> 900
        }

        val proteinLowThreshold = when (val profile = context.userProfile) {
            is UserProfile.Solo -> when (profile.profile.goal) {
                Goal.GAIN_MUSCLE -> 40
                else -> 30
            }
            is UserProfile.Group -> 45
            null -> 30
        }

        val messages = mutableListOf<String>()

        if (totals.kcal > kcalHighThreshold) {
            messages += "Đơn hàng chứa lượng calo cao so với mục tiêu đã chọn"
        }

        if (totals.proteinG < proteinLowThreshold) {
            messages += "Tổng lượng chất đạm thấp so với cấu hình hồ sơ"
        }

        if (totals.fatG > 40) {
            messages += "Lượng chất béo cao, cân nhắc chọn các món ăn nhẹ hơn"
        }

        if (totals.carbsG > 120) {
            messages += "Hàm lượng carbohydrate cao cho một bữa ăn"
        }

        if (messages.isEmpty()) {
            messages += "Đơn hàng cân đối theo các quy tắc dinh dưỡng"
        }

        return messages.take(2)
    }
}
