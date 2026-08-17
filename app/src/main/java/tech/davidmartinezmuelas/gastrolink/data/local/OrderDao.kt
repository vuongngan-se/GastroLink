package tech.davidmartinezmuelas.gastrolink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getOrdersWithItems(): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithParticipants(orderId: String): OrderWithParticipants?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithProfile(orderId: String): OrderWithProfile?

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Transaction
    suspend fun insertCompleteOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        participants: List<ParticipantEntity>,
        profile: ProfileEntity?
    ) {
        insertOrder(order)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
        if (participants.isNotEmpty()) {
            insertParticipants(participants)
        }
        if (profile != null) {
            insertProfile(profile)
        }
    }
}
