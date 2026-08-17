package tech.davidmartinezmuelas.gastrolink.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.davidmartinezmuelas.gastrolink.data.local.OrderEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderItemEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithItems
import tech.davidmartinezmuelas.gastrolink.model.Branch
import tech.davidmartinezmuelas.gastrolink.model.Dish

class OrderExportRepositoryTest {

    private val repository = OrderExportRepository()

    private val branch = Branch(id = "b1", name = "Sucursal Central", city = "Madrid")
    private val dishA = Dish("d1", "Ensalada", kcal = 200, proteinG = 10, carbsG = 20, fatG = 5)
    private val dishB = Dish("d2", "Pasta", kcal = 500, proteinG = 20, carbsG = 80, fatG = 10)

    private val branchesById = mapOf("b1" to branch)
    private val dishesById = mapOf("d1" to dishA, "d2" to dishB)

    private val singleOrder = listOf(
        OrderWithItems(
            order = OrderEntity(
                id = "order-1",
                branchId = "b1",
                orderMode = "SOLO",
                nutritionMode = "WITHOUT_PROFILE",
                createdAt = 1_700_000_000_000L
            ),
            items = listOf(
                OrderItemEntity("i1", "order-1", "d1", quantity = 2, participantId = null),
                OrderItemEntity("i2", "order-1", "d2", quantity = 1, participantId = null)
            )
        )
    )

    // ── JSON ──────────────────────────────────────────────────────────────────

    @Test
    fun exportToJson_containsFormatoField() {
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("orders_export_v1"))
    }

    @Test
    fun exportToJson_containsEstructuraField() {
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("\"estructura\""))
        assertTrue(json.contains("una_fila_por_plato"))
    }

    @Test
    fun exportToJson_containsOrderId() {
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("order-1"))
    }

    @Test
    fun exportToJson_containsBranchName() {
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("Sucursal Central"))
    }

    @Test
    fun exportToJson_containsDishName() {
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("Ensalada"))
        assertTrue(json.contains("Pasta"))
    }

    @Test
    fun exportToJson_totalKcalMatchesExpected() {
        // dishA × 2 = 400 kcal, dishB × 1 = 500 kcal → total = 900
        val json = repository.exportOrdersToJson(singleOrder, dishesById, branchesById)
        assertTrue(json.contains("900"))
    }

    @Test
    fun exportToJson_withEmptyOrders_containsEmptyPedidosList() {
        val json = repository.exportOrdersToJson(emptyList(), dishesById, branchesById)
        assertTrue(json.contains("\"pedidos\": []"))
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    @Test
    fun exportToCsv_firstLineIsHeader() {
        val csv = repository.exportOrdersToCsv(singleOrder, dishesById, branchesById)
        val header = csv.lineSequence().first()
        assertTrue(header.contains("orderId"))
        assertTrue(header.contains("fecha"))
        assertTrue(header.contains("sucursal"))
        assertTrue(header.contains("modoPedido"))
        assertTrue(header.contains("modoNutricional"))
        assertTrue(header.contains("plato"))
    }

    @Test
    fun exportToCsv_rowCountMatchesDishCount() {
        // 1 order with 2 dishes → header + 2 data rows (+ possibly trailing newline)
        val csv = repository.exportOrdersToCsv(singleOrder, dishesById, branchesById)
        val dataRows = csv.lineSequence()
            .drop(1)        // skip header
            .filter { it.isNotBlank() }
            .toList()
        assertEquals(2, dataRows.size)
    }

    @Test
    fun exportToCsv_containsBranchName() {
        val csv = repository.exportOrdersToCsv(singleOrder, dishesById, branchesById)
        assertTrue(csv.contains("Sucursal Central"))
    }

    @Test
    fun exportToCsv_withEmptyOrders_containsOnlyHeader() {
        val csv = repository.exportOrdersToCsv(emptyList(), dishesById, branchesById)
        val nonBlankLines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        assertEquals(1, nonBlankLines.size) // only header
    }

    // ── CSV escaping ──────────────────────────────────────────────────────────

    @Test
    fun exportToCsv_escapesCommasInValues() {
        val dishWithComma = Dish("d3", "Sopa, caliente", kcal = 100, proteinG = 5, carbsG = 10, fatG = 3)
        val orders = listOf(
            OrderWithItems(
                order = OrderEntity("o2", "b1", "SOLO", "WITHOUT_PROFILE", 1_700_000_000_000L),
                items = listOf(OrderItemEntity("i3", "o2", "d3", quantity = 1, participantId = null))
            )
        )
        val csv = repository.exportOrdersToCsv(
            orders,
            mapOf("d3" to dishWithComma),
            branchesById
        )
        // Comma inside value must be wrapped in quotes
        assertTrue(csv.contains("\"Sopa, caliente\""))
    }

    @Test
    fun exportToCsv_escapesDoubleQuotesInValues() {
        val dishWithQuote = Dish("d4", "Burger \"especial\"", kcal = 600, proteinG = 30, carbsG = 40, fatG = 25)
        val orders = listOf(
            OrderWithItems(
                order = OrderEntity("o3", "b1", "SOLO", "WITHOUT_PROFILE", 1_700_000_000_000L),
                items = listOf(OrderItemEntity("i4", "o3", "d4", quantity = 1, participantId = null))
            )
        )
        val csv = repository.exportOrdersToCsv(
            orders,
            mapOf("d4" to dishWithQuote),
            branchesById
        )
        // Double-quote in value must be escaped as ""
        assertTrue(csv.contains("\"\"especial\"\""))
    }
}
