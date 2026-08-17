package tech.davidmartinezmuelas.gastrolink.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderDaoTest {

    private lateinit var database: GastroLinkDatabase
    private lateinit var dao: OrderDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GastroLinkDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.orderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertCompleteOrder_andReadRelations_returnsItemsAndParticipants() = runBlocking {
        val order = OrderEntity(
            id = "order-1",
            branchId = "b1",
            orderMode = "SOLITARIO",
            nutritionMode = "SIN_DATOS",
            createdAt = 1000L
        )
        val items = listOf(
            OrderItemEntity("item-1", "order-1", "dish-1", 2, null),
            OrderItemEntity("item-2", "order-1", "dish-2", 1, "participant-1")
        )
        val participants = listOf(
            ParticipantEntity("participant-1", "order-1", "Guest 1")
        )

        dao.insertCompleteOrder(order, items, participants, profile = null)

        val orderWithItems = dao.getOrderWithItems("order-1")
        val orderWithParticipants = dao.getOrderWithParticipants("order-1")

        assertNotNull(orderWithItems)
        assertNotNull(orderWithParticipants)
        assertEquals(2, orderWithItems?.items?.size)
        assertEquals(1, orderWithParticipants?.participants?.size)
    }

    @Test
    fun deleteOrder_cascadesToItemsAndParticipants() = runBlocking {
        val order = OrderEntity(
            id = "order-2",
            branchId = "b2",
            orderMode = "GRUPO",
            nutritionMode = "CON_DATOS",
            createdAt = 2000L
        )
        val items = listOf(
            OrderItemEntity("item-3", "order-2", "dish-3", 3, "participant-2")
        )
        val participants = listOf(
            ParticipantEntity("participant-2", "order-2", "Guest 2")
        )

        dao.insertCompleteOrder(order, items, participants, profile = null)
        dao.deleteOrder("order-2")

        val deletedOrderWithItems = dao.getOrderWithItems("order-2")
        val deletedOrderWithParticipants = dao.getOrderWithParticipants("order-2")

        assertNull(deletedOrderWithItems)
        assertNull(deletedOrderWithParticipants)
        assertEquals(0, dao.getOrdersWithItems().size)
    }
}
