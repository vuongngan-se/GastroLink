package tech.davidmartinezmuelas.gastrolink.domain

import tech.davidmartinezmuelas.gastrolink.data.ai.AiChatRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiChatResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanResponse

interface AiRecommendationService {
    suspend fun generate(request: AiRecommendationRequest): AiRecommendationResponse
    suspend fun chat(request: AiChatRequest): AiChatResponse
    suspend fun scanDish(request: AiDishScanRequest): AiDishScanResponse
    suspend fun uploadSync(request: tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadRequest): tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadResponse
    suspend fun downloadSync(userId: String): tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncDownloadResponse
}
