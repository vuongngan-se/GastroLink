package tech.davidmartinezmuelas.gastrolink.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.davidmartinezmuelas.gastrolink.model.Branch
import tech.davidmartinezmuelas.gastrolink.model.Dish

class RemoteCatalogRepository(
    private val baseUrl: String,
    private val proxyToken: String = "",
    private val client: HttpClient = defaultHttpClient()
) {

    suspend fun loadCatalog(): RepositoryResult<CatalogData> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Tải danh sách chi nhánh
            val branchesResponse = client.get("${baseUrl.trimEnd('/')}/catalog/branches") {
                if (proxyToken.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $proxyToken")
                }
            }
            if (branchesResponse.status != HttpStatusCode.OK) {
                return@withContext RepositoryResult.Error("Không thể tải danh sách chi nhánh từ máy chủ")
            }
            val branches = branchesResponse.body<List<Branch>>()

            // 2. Tải danh sách món ăn đã được AI dịch thuật và định lượng từ TheMealDB
            val dishesResponse = client.get("${baseUrl.trimEnd('/')}/catalog/external-dishes") {
                if (proxyToken.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $proxyToken")
                }
            }
            if (dishesResponse.status != HttpStatusCode.OK) {
                return@withContext RepositoryResult.Error("Không thể tải danh sách món ăn từ máy chủ")
            }
            val dishes = dishesResponse.body<List<Dish>>()

            RepositoryResult.Success(CatalogData(branches = branches, dishes = dishes))
        }.getOrElse { error ->
            RepositoryResult.Error("Lỗi kết nối mạng: ${error.localizedMessage ?: "Không rõ nguyên nhân"}")
        }
    }

    companion object {
        private fun defaultHttpClient(): HttpClient {
            return HttpClient(Android) {
                install(ContentNegotiation) {
                    gson {
                        setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 45_000 // Tăng timeout cho AI phân tích
                    connectTimeoutMillis = 15_000
                }
            }
        }
    }
}
