package tech.davidmartinezmuelas.gastrolink.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.davidmartinezmuelas.gastrolink.model.ActivityLevel
import tech.davidmartinezmuelas.gastrolink.model.CartItem
import tech.davidmartinezmuelas.gastrolink.model.Dish
import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.Sex
import tech.davidmartinezmuelas.gastrolink.model.SoloNutritionProfile

class NutritionCalculatorTest {

    private val dishA = Dish(
        id = "d1",
        name = "Dish A",
        kcal = 100,
        proteinG = 10,
        carbsG = 20,
        fatG = 5
    )

    private val dishB = Dish(
        id = "d2",
        name = "Dish B",
        kcal = 250,
        proteinG = 25,
        carbsG = 15,
        fatG = 10
    )

    // ── calculateTotals ──────────────────────────────────────────────────────

    @Test
    fun calculateTotals_returnsExpectedGlobalTotals() {
        val items = listOf(
            CartItem(dish = dishA, qty = 2),
            CartItem(dish = dishB, qty = 3)
        )

        val totals = NutritionCalculator.calculateTotals(items)

        assertEquals(950, totals.kcal)
        assertEquals(95, totals.proteinG)
        assertEquals(85, totals.carbsG)
        assertEquals(40, totals.fatG)
    }

    @Test
    fun calculateTotalsByParticipant_groupsOnlyAssignedItems() {
        val items = listOf(
            CartItem(dish = dishA, qty = 2, participantId = "p1"),
            CartItem(dish = dishB, qty = 1, participantId = "p1"),
            CartItem(dish = dishB, qty = 2, participantId = "p2"),
            CartItem(dish = dishA, qty = 1, participantId = null)
        )

        val totalsByParticipant = NutritionCalculator.calculateTotalsByParticipant(items)

        val p1 = totalsByParticipant.getValue("p1")
        assertEquals(450, p1.kcal)
        assertEquals(45, p1.proteinG)
        assertEquals(55, p1.carbsG)
        assertEquals(20, p1.fatG)

        val p2 = totalsByParticipant.getValue("p2")
        assertEquals(500, p2.kcal)
        assertEquals(50, p2.proteinG)
        assertEquals(30, p2.carbsG)
        assertEquals(20, p2.fatG)

        assertEquals(2, totalsByParticipant.size)
    }

    @Test
    fun calculateTotals_withEmptyList_returnsZeros() {
        val totals = NutritionCalculator.calculateTotals(emptyList())

        assertEquals(
            NutritionTotals(kcal = 0, proteinG = 0, carbsG = 0, fatG = 0),
            totals
        )
    }

    // ── calculateTdee ────────────────────────────────────────────────────────

    @Test
    fun calculateTdee_youngMaleHighActivityGainMuscle_returnsAbove2500() {
        // Profile 1: 25yo male, 180cm, 80kg, HIGH activity, GAIN_MUSCLE
        val profile = SoloNutritionProfile(
            age = 25,
            sex = Sex.MALE,
            heightCm = 180,
            weightKg = 80,
            activityLevel = ActivityLevel.HIGH,
            goal = Goal.GAIN_MUSCLE
        )

        val tdee = NutritionCalculator.calculateTdee(profile)

        assertNotNull(tdee)
        // BMR ≈ 1904, TDEE ≈ 1904 * 1.725 ≈ 3284, +10% ≈ 3612
        assertTrue("Expected >2500, got $tdee", tdee!! > 2500)
    }

    @Test
    fun calculateTdee_adultFemaleLowActivityLoseWeight_returnsBelow1600() {
        // Profile 2: 40yo female, 163cm, 68kg, LOW activity, LOSE_WEIGHT
        val profile = SoloNutritionProfile(
            age = 40,
            sex = Sex.FEMALE,
            heightCm = 163,
            weightKg = 68,
            activityLevel = ActivityLevel.LOW,
            goal = Goal.LOSE_WEIGHT
        )

        val tdee = NutritionCalculator.calculateTdee(profile)

        assertNotNull(tdee)
        // BMR ≈ 1448, TDEE ≈ 1448 * 1.375 ≈ 1991, -25% ≈ 1493
        assertTrue("Expected <1600, got $tdee", tdee!! < 1600)
    }

    @Test
    fun calculateTdee_otherSexMaintainMediumActivity_returnsReasonableRange() {
        // Profile 3: 30yo OTHER sex, 170cm, 70kg, MEDIUM activity, MAINTAIN
        val profile = SoloNutritionProfile(
            age = 30,
            sex = Sex.OTHER,
            heightCm = 170,
            weightKg = 70,
            activityLevel = ActivityLevel.MEDIUM,
            goal = Goal.MAINTAIN
        )

        val tdee = NutritionCalculator.calculateTdee(profile)

        assertNotNull(tdee)
        assertTrue("Expected in 1800–2600, got $tdee", tdee!! in 1800..2600)
    }

    @Test
    fun calculateTdee_nullActivityLevel_usesModerateDefault() {
        // Profile 4: 35yo male, 175cm, 75kg, no activityLevel, MAINTAIN
        val withActivity = SoloNutritionProfile(
            age = 35, sex = Sex.MALE, heightCm = 175, weightKg = 75,
            activityLevel = ActivityLevel.MEDIUM, goal = Goal.MAINTAIN
        )
        val withoutActivity = withActivity.copy(activityLevel = null)

        // Both should return the same value since null defaults to MEDIUM (1.55)
        assertEquals(
            NutritionCalculator.calculateTdee(withActivity),
            NutritionCalculator.calculateTdee(withoutActivity)
        )
    }

    @Test
    fun calculateTdee_missingRequiredField_returnsNull() {
        // Profile 5: incomplete profile (no age)
        val noAge = SoloNutritionProfile(
            sex = Sex.FEMALE, heightCm = 165, weightKg = 60,
            activityLevel = ActivityLevel.MEDIUM, goal = Goal.MAINTAIN
        )
        assertNull(NutritionCalculator.calculateTdee(noAge))

        // Profile 6: no height
        val noHeight = SoloNutritionProfile(
            age = 28, sex = Sex.MALE, weightKg = 75,
            activityLevel = ActivityLevel.HIGH, goal = Goal.GAIN_MUSCLE
        )
        assertNull(NutritionCalculator.calculateTdee(noHeight))

        // Profile 7: no weight
        val noWeight = SoloNutritionProfile(
            age = 28, sex = Sex.MALE, heightCm = 175,
            activityLevel = ActivityLevel.HIGH, goal = Goal.MAINTAIN
        )
        assertNull(NutritionCalculator.calculateTdee(noWeight))
    }
}
