package tech.davidmartinezmuelas.gastrolink.data

import com.google.gson.GsonBuilder
import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithItems
import tech.davidmartinezmuelas.gastrolink.model.Branch
import tech.davidmartinezmuelas.gastrolink.model.Dish
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportOrder(
    val id: String,
    val fecha: String,
    val sucursal: String,
    val modoPedido: String,
    val modoNutricional: String,
    val totales: ExportTotals,
    val platos: List<ExportDish>
)

data class ExportTotals(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

data class ExportDish(
    val nombre: String,
    val qty: Int,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

class OrderExportRepository {

    fun exportOrdersToJson(
        ordersWithItems: List<OrderWithItems>,
        dishesById: Map<String, Dish>,
        branchesById: Map<String, Branch>
    ): String {
        val exportOrders = mapOrders(ordersWithItems, dishesById, branchesById)
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(
                mapOf(
                    "formato" to "orders_export_v1",
                    "estructura" to "una_fila_por_plato",
                    "pedidos" to exportOrders
                )
            )
    }

    fun exportOrdersToCsv(
        ordersWithItems: List<OrderWithItems>,
        dishesById: Map<String, Dish>,
        branchesById: Map<String, Branch>
    ): String {
        val exportOrders = mapOrders(ordersWithItems, dishesById, branchesById)
        val header = listOf(
            "orderId",
            "fecha",
            "sucursal",
            "modoPedido",
            "modoNutricional",
            "totalKcalPedido",
            "totalProteinPedido",
            "totalCarbsPedido",
            "totalFatPedido",
            "plato",
            "qty",
            "kcal",
            "proteinG",
            "carbsG",
            "fatG"
        ).joinToString(",")

        val rows = exportOrders.flatMap { order ->
            order.platos.map { dish ->
                listOf(
                    escapeCsv(order.id),
                    escapeCsv(order.fecha),
                    escapeCsv(order.sucursal),
                    escapeCsv(order.modoPedido),
                    escapeCsv(order.modoNutricional),
                    order.totales.kcal.toString(),
                    order.totales.proteinG.toString(),
                    order.totales.carbsG.toString(),
                    order.totales.fatG.toString(),
                    escapeCsv(dish.nombre),
                    dish.qty.toString(),
                    dish.kcal.toString(),
                    dish.proteinG.toString(),
                    dish.carbsG.toString(),
                    dish.fatG.toString()
                ).joinToString(",")
            }
        }

        return buildString {
            appendLine(header)
            rows.forEach { appendLine(it) }
        }
    }

    private fun mapOrders(
        ordersWithItems: List<OrderWithItems>,
        dishesById: Map<String, Dish>,
        branchesById: Map<String, Branch>
    ): List<ExportOrder> {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return ordersWithItems.map { orderWithItems ->
            val dishes = orderWithItems.items.map { item ->
                val dish = dishesById[item.dishId]
                ExportDish(
                    nombre = dish?.name ?: item.dishId,
                    qty = item.quantity,
                    kcal = (dish?.kcal ?: 0) * item.quantity,
                    proteinG = (dish?.proteinG ?: 0) * item.quantity,
                    carbsG = (dish?.carbsG ?: 0) * item.quantity,
                    fatG = (dish?.fatG ?: 0) * item.quantity
                )
            }

            val totals = ExportTotals(
                kcal = dishes.sumOf { it.kcal },
                proteinG = dishes.sumOf { it.proteinG },
                carbsG = dishes.sumOf { it.carbsG },
                fatG = dishes.sumOf { it.fatG }
            )

            ExportOrder(
                id = orderWithItems.order.id,
                fecha = formatter.format(Date(orderWithItems.order.createdAt)),
                sucursal = branchesById[orderWithItems.order.branchId]?.name ?: "Sucursal desconocida",
                modoPedido = orderWithItems.order.orderMode,
                modoNutricional = orderWithItems.order.nutritionMode,
                totales = totals,
                platos = dishes
            )
        }
    }

    private fun escapeCsv(value: String): String {
        val safe = value.replace("\"", "\"\"")
        return "\"$safe\""
    }
}
