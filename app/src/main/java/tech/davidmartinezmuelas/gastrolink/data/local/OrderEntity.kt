package tech.davidmartinezmuelas.gastrolink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val orderMode: String,
    val nutritionMode: String,
    val createdAt: Long,
    val address: String? = null,
    val phoneNumber: String? = null,
    val paymentMethod: String? = null
)
