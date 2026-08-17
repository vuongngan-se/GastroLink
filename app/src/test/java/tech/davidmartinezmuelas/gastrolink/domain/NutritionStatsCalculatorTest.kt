package tech.davidmartinezmuelas.gastrolink.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.davidmartinezmuelas.gastrolink.data.local.OrderEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderItemEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithItems
import tech.davidmartinezmuelas.gastrolink.model.Dish

class NutritionStatsCalculatorTest {

    private val dishesById = mapOf(
        "d1" to Dish("d1", "A", kcal = 100, proteinG = 10, carbsG = 20, fatG = 5),
        "d2" to Dish("d2", "B", kcal = 300, proteinG = 30, carbsG = 15, fatG = 12),
        "d3" to Dish("d3", "C", kcal = 200, proteinG = 15, carbsG = 30, fatG = 8)
    )

    private val orders = listOf(
        OrderWithItems(
            order = OrderEntity("o1", "b1", "SOLITARIO", "SIN_DATOS", 1L),
            items = listOf(
                OrderItemEntity("i1", "o1", "d1", quantity = 2, participantId = null),
                OrderItemEntity("i2", "o1", "d2", quantity = 1, participantId = null)
            )
        ),
        OrderWithItems(
            order = OrderEntity("o2", "b1", "GRUPO", "CON_DATOS", 2L),
            items = listOf(
                OrderItemEntity("i3", "o2", "d1", quantity = 1, participantId = "p1"),
                OrderItemEntity("i4", "o2", "d3", quantity = 2, participantId = "p2")
            )
        )
    )

    @Test
    fun averageCaloriesPerOrder_returnsExpectedDoubleAverage() {
        val result = NutritionStatsCalculator.averageCaloriesPerOrder(orders, dishesById)
        assertEquals(500.0, result, 0.0001)
    }

    @Test
    fun averageProteinCarbsFat_returnsExpectedAverages() {
        val avgProtein = NutritionStatsCalculator.averageProtein(orders, dishesById)
        val avgCarbs = NutritionStatsCalculator.averageCarbs(orders, dishesById)
        val avgFat = NutritionStatsCalculator.averageFat(orders, dishesById)

        assertEquals(45.0, avgProtein, 0.0001)
        assertEquals(67.5, avgCarbs, 0.0001)
        assertEquals(21.5, avgFat, 0.0001)
    }

    @Test
    fun mostOrderedDish_breaksTiesByDishIdAscending() {
        val tieOrders = listOf(
            OrderWithItems(
                order = OrderEntity("o3", "b1", "SOLITARIO", "SIN_DATOS", 3L),
                items = listOf(
                    OrderItemEntity("i5", "o3", "d2", quantity = 3, participantId = null),
                    OrderItemEntity("i6", "o3", "d1", quantity = 3, participantId = null)
                )
            )
        )

        val mostOrdered = NutritionStatsCalculator.mostOrderedDish(tieOrders)

        assertEquals("d1", mostOrdered)
    }

    @Test
    fun calculate_withEmptyOrders_returnsZeroesAndNoDish() {
        val stats = NutritionStatsCalculator.calculate(emptyList(), dishesById)

        assertEquals(0.0, stats.averageCaloriesPerOrder, 0.0001)
        assertEquals(0.0, stats.averageProtein, 0.0001)
        assertEquals(0.0, stats.averageCarbs, 0.0001)
        assertEquals(0.0, stats.averageFat, 0.0001)
        assertEquals(null, stats.mostOrderedDishId)
    }
}
