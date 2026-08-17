package tech.davidmartinezmuelas.gastrolink.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.delay
import tech.davidmartinezmuelas.gastrolink.domain.AiRecommendationService
import java.io.IOException

class AiRecommendationServiceImpl(
    private val baseUrl: String,
    private val proxyToken: String = "",
    private val client: HttpClient = defaultHttpClient()
) : AiRecommendationService {

    override suspend fun generate(request: AiRecommendationRequest): AiRecommendationResponse {
        check(baseUrl.isNotBlank()) { "Dịch vụ AI chưa được cấu hình" }

        var lastError: Throwable? = null
        repeat(2) { attempt ->
            runCatching {
                val response = client.post("${baseUrl.trimEnd('/')}/ai/recommendation") {
                    contentType(ContentType.Application.Json)
                    if (proxyToken.isNotBlank()) {
                        header(HttpHeaders.Authorization, "Bearer $proxyToken")
                    }
                    setBody(request)
                }

                check(response.status == HttpStatusCode.OK) { "Dịch vụ AI không khả dụng" }

                response.body<AiRecommendationResponse>()
            }.onSuccess { return it }
                .onFailure { error ->
                    lastError = error
                    if (error is IOException && attempt == 0) {
                        delay(250)
                    }
                }
        }

        throw lastError ?: error("Không thể nhận đề xuất từ AI")
    }

    override suspend fun chat(request: AiChatRequest): AiChatResponse {
        check(baseUrl.isNotBlank()) { "Dịch vụ AI chưa được cấu hình" }

        val response = client.post("${baseUrl.trimEnd('/')}/ai/chat") {
            contentType(ContentType.Application.Json)
            if (proxyToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $proxyToken")
            }
            setBody(request)
        }

        check(response.status == HttpStatusCode.OK) { "Dịch vụ AI không khả dụng" }
        return response.body()
    }

    override suspend fun scanDish(request: AiDishScanRequest): AiDishScanResponse {
        check(baseUrl.isNotBlank()) { "Dịch vụ AI chưa được cấu hình" }

        val response = client.post("${baseUrl.trimEnd('/')}/ai/scan-dish") {
            contentType(ContentType.Application.Json)
            if (proxyToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $proxyToken")
            }
            setBody(request)
        }

        check(response.status == HttpStatusCode.OK) { "Dịch vụ AI không khả dụng" }
        return response.body()
    }

    override suspend fun uploadSync(request: CloudSyncUploadRequest): CloudSyncUploadResponse {
        check(baseUrl.isNotBlank()) { "Dịch vụ AI chưa được cấu hình" }
        val response = client.post("${baseUrl.trimEnd('/')}/ai/sync/upload") {
            contentType(ContentType.Application.Json)
            if (proxyToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $proxyToken")
            }
            setBody(request)
        }
        check(response.status == HttpStatusCode.OK) { "Không thể đồng bộ dữ liệu lên đám mây" }
        return response.body()
    }

    override suspend fun downloadSync(userId: String): CloudSyncDownloadResponse {
        check(baseUrl.isNotBlank()) { "Dịch vụ AI chưa được cấu hình" }
        val response = client.get("${baseUrl.trimEnd('/')}/ai/sync/download?userId=$userId") {
            if (proxyToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $proxyToken")
            }
        }
        check(response.status == HttpStatusCode.OK) { "Không thể tải dữ liệu đồng bộ từ đám mây" }
        return response.body()
    }

    companion object {
        private fun defaultHttpClient(): HttpClient {
            return HttpClient(Android) {
                install(ContentNegotiation) {
                    gson()
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 30_000
                }
            }
        }
    }
}
