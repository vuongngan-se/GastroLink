package tech.davidmartinezmuelas.gastrolink.data.ai

data class AiRecommendationRequest(
    val orderMode: String,
    val nutritionMode: String,
    val totals: TotalsPayload,
    val dishes: List<DishPayload>,
    val profile: ProfilePayload?
) {
    data class TotalsPayload(
        val kcal: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int
    )

    data class DishPayload(
        val name: String,
        val qty: Int,
        val kcal: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int
    )

    data class ProfilePayload(
        val type: String,
        val summary: Map<String, Any?>
    )
}

data class AiRecommendationResponse(
    val recommendationText: String,
    val model: String? = null,
    val requestId: String? = null
)

data class AiChatRequest(
    val messages: List<ChatMessagePayload>,
    val profile: AiRecommendationRequest.ProfilePayload? = null,
    val availableDishes: List<String>? = null
) {
    data class ChatMessagePayload(val role: String, val content: String)
}

data class AiChatResponse(val reply: String)

data class AiDishScanRequest(val image: String)

data class AiDishScanResponse(
    val name: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val ingredients: List<String>,
    val recipes: List<String> = emptyList()
)

data class CloudProfileSync(
    val age: Int?,
    val sex: String?,
    val weight: Double?,
    val height: Double?,
    val goal: String?,
    val activityLevel: String?,
    val allergies: String?
)

data class CloudOrderItemSync(
    val dishName: String,
    val qty: Int,
    val kcal: Int
)

data class CloudOrderSync(
    val orderId: String,
    val orderDate: String,
    val totalKcal: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val items: List<CloudOrderItemSync>
)

data class CloudSavedProfileSync(
    val id: String,
    val name: String,
    val profile: CloudProfileSync
)

data class CloudSyncUploadRequest(
    val userId: String,
    val profile: CloudProfileSync?,
    val savedProfiles: List<CloudSavedProfileSync> = emptyList(),
    val orders: List<CloudOrderSync>
)

data class CloudSyncUploadResponse(
    val status: String,
    val message: String
)

data class CloudSyncDownloadResponse(
    val status: String,
    val profile: CloudProfileSync?,
    val savedProfiles: List<CloudSavedProfileSync>? = emptyList(),
    val orders: List<CloudOrderSync>? = emptyList()
)
