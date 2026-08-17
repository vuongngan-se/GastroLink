package tech.davidmartinezmuelas.gastrolink.data

import tech.davidmartinezmuelas.gastrolink.data.local.OrderDao
import tech.davidmartinezmuelas.gastrolink.data.local.OrderEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderItemEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithItems
import tech.davidmartinezmuelas.gastrolink.data.local.OrderWithParticipants
import tech.davidmartinezmuelas.gastrolink.data.local.ParticipantEntity
import tech.davidmartinezmuelas.gastrolink.data.local.ProfileEntity

data class OrderDetailsRecord(
    val orderWithItems: OrderWithItems?,
    val orderWithParticipants: OrderWithParticipants?,
    val profile: ProfileEntity?
)

class OrderRepository(private val orderDao: OrderDao) {

    suspend fun saveOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        participants: List<ParticipantEntity>,
        profile: ProfileEntity? = null
    ) {
        orderDao.insertCompleteOrder(order, items, participants, profile)
    }

    suspend fun getOrders(): List<OrderWithItems> {
        return orderDao.getOrdersWithItems()
    }

    suspend fun getOrderDetails(orderId: String): OrderDetailsRecord {
        val withItems = orderDao.getOrderWithItems(orderId)
        val withParticipants = orderDao.getOrderWithParticipants(orderId)
        val profile = orderDao.getOrderWithProfile(orderId)?.profiles?.firstOrNull()
        return OrderDetailsRecord(
            orderWithItems = withItems,
            orderWithParticipants = withParticipants,
            profile = profile
        )
    }

    suspend fun deleteOrder(orderId: String) {
        orderDao.deleteOrder(orderId)
    }
}
