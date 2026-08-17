package tech.davidmartinezmuelas.gastrolink.domain

import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithItems
import tech.davidmartinezmuelas.gastrolink.model.Dish

data class NutritionStats(
    val averageCaloriesPerOrder: Double,
    val averageProtein: Double,
    val averageCarbs: Double,
    val averageFat: Double,
    val mostOrderedDishId: String?
)

object NutritionStatsCalculator {

    fun averageCaloriesPerOrder(orders: List<OrderWithItems>, dishesById: Map<String, Dish>): Double {
        if (orders.isEmpty()) return 0.0
        val totalCalories = orders.sumOf { order ->
            order.items.sumOf { item ->
                (dishesById[item.dishId]?.kcal ?: 0) * item.quantity
            }
        }
        return totalCalories.toDouble() / orders.size
    }

    fun averageProtein(orders: List<OrderWithItems>, dishesById: Map<String, Dish>): Double {
        if (orders.isEmpty()) return 0.0
        val totalProtein = orders.sumOf { order ->
            order.items.sumOf { item ->
                (dishesById[item.dishId]?.proteinG ?: 0) * item.quantity
            }
        }
        return totalProtein.toDouble() / orders.size
    }

    fun averageCarbs(orders: List<OrderWithItems>, dishesById: Map<String, Dish>): Double {
        if (orders.isEmpty()) return 0.0
        val totalCarbs = orders.sumOf { order ->
            order.items.sumOf { item ->
                (dishesById[item.dishId]?.carbsG ?: 0) * item.quantity
            }
        }
        return totalCarbs.toDouble() / orders.size
    }

    fun averageFat(orders: List<OrderWithItems>, dishesById: Map<String, Dish>): Double {
        if (orders.isEmpty()) return 0.0
        val totalFat = orders.sumOf { order ->
            order.items.sumOf { item ->
                (dishesById[item.dishId]?.fatG ?: 0) * item.quantity
            }
        }
        return totalFat.toDouble() / orders.size
    }

    fun mostOrderedDish(orders: List<OrderWithItems>): String? {
        val totalsByDish = orders
            .flatMap { it.items }
            .groupBy { it.dishId }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }

        return totalsByDish
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key
    }

    fun calculate(orders: List<OrderWithItems>, dishesById: Map<String, Dish>): NutritionStats {
        return NutritionStats(
            averageCaloriesPerOrder = averageCaloriesPerOrder(orders, dishesById),
            averageProtein = averageProtein(orders, dishesById),
            averageCarbs = averageCarbs(orders, dishesById),
            averageFat = averageFat(orders, dishesById),
            mostOrderedDishId = mostOrderedDish(orders)
        )
    }
}
