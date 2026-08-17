package tech.davidmartinezmuelas.gastrolink.model

enum class OrderMode {
    SOLO,
    GROUP
}

enum class NutritionMode {
    WITHOUT_PROFILE,
    WITH_PROFILE
}

enum class Entitlement {
    FREE,
    PREMIUM_DEMO
}

enum class Sex {
    FEMALE,
    MALE,
    OTHER
}

enum class Goal {
    MAINTAIN,
    LOSE_WEIGHT,
    GAIN_MUSCLE
}

enum class ActivityLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class Branch(
    val id: String,
    val name: String,
    val city: String
)

data class Dish(
    val id: String,
    val name: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val price: Int = 0,
    val ingredients: List<String>? = null,
    val imageUrl: String? = null
)

data class Participant(
    val id: String,
    val name: String
)

data class CartItem(
    val dish: Dish,
    val qty: Int,
    val participantId: String? = null
)

data class NutritionTotals(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

data class SoloNutritionProfile(
    val age: Int? = null,
    val sex: Sex? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val goal: Goal? = null,
    val activityLevel: ActivityLevel? = null,
    val allergies: String = ""
)

data class GroupNutritionProfile(
    val participantId: String,
    val allergies: String = "",
    val preferences: String = "",
    val generalGoal: Goal? = null
)

sealed interface UserProfile {
    data class Solo(val profile: SoloNutritionProfile) : UserProfile
    data class Group(val profiles: List<GroupNutritionProfile>) : UserProfile
}

data class SavedProfile(
    val id: String,
    val name: String,
    val profile: SoloNutritionProfile
)

data class RecommendationContext(
    val orderMode: OrderMode,
    val nutritionMode: NutritionMode,
    val userProfile: UserProfile?
)

data class ChatMessage(
    val role: String,
    val content: String
)
