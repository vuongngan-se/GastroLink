package tech.davidmartinezmuelas.gastrolink.domain

enum class RecommendationSource {
    LOCAL_RULES,
    AI,
    NONE
}

data class RecommendationResult(
    val source: RecommendationSource,
    val messages: List<String>,
    val debugInfo: String? = null
)
