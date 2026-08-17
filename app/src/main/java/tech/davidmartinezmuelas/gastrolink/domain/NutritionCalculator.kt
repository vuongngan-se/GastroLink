package tech.davidmartinezmuelas.gastrolink.domain

import tech.davidmartinezmuelas.gastrolink.model.ActivityLevel
import tech.davidmartinezmuelas.gastrolink.model.CartItem
import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.Sex
import tech.davidmartinezmuelas.gastrolink.model.SoloNutritionProfile

object NutritionCalculator {

    fun calculateTotals(items: List<CartItem>): NutritionTotals {
        val kcal = items.sumOf { it.dish.kcal * it.qty }
        val protein = items.sumOf { it.dish.proteinG * it.qty }
        val carbs = items.sumOf { it.dish.carbsG * it.qty }
        val fat = items.sumOf { it.dish.fatG * it.qty }

        return NutritionTotals(
            kcal = kcal,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat
        )
    }

    fun calculateTotalsByParticipant(items: List<CartItem>): Map<String, NutritionTotals> {
        return items
            .filter { !it.participantId.isNullOrBlank() }
            .groupBy { it.participantId.orEmpty() }
            .mapValues { (_, participantItems) -> calculateTotals(participantItems) }
    }

    fun calculateTdee(profile: SoloNutritionProfile): Int? {
        val age = profile.age
        val heightCm = profile.heightCm
        val weightKg = profile.weightKg
        if (age == null || heightCm == null || weightKg == null) return null

        val bmr = when (profile.sex) {
            Sex.MALE -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5).toInt()
            Sex.FEMALE -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161).toInt()
            Sex.OTHER, null -> (10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 78).toInt()
        }

        val activityMultiplier = when (profile.activityLevel) {
            ActivityLevel.LOW -> 1.375
            ActivityLevel.MEDIUM -> 1.55
            ActivityLevel.HIGH -> 1.725
            null -> 1.55
        }

        val tdee = (bmr * activityMultiplier).toInt()

        return when (profile.goal) {
            Goal.LOSE_WEIGHT -> (tdee * 0.75).toInt()
            Goal.GAIN_MUSCLE -> (tdee * 1.10).toInt()
            Goal.MAINTAIN, null -> tdee
        }
    }
}
