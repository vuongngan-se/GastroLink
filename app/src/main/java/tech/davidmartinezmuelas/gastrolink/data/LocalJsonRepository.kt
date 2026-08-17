package tech.davidmartinezmuelas.gastrolink.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import tech.davidmartinezmuelas.gastrolink.model.Branch
import tech.davidmartinezmuelas.gastrolink.model.Dish

sealed interface RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>
    data class Error(val message: String) : RepositoryResult<Nothing>
}

class LocalJsonRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    fun loadBranches(): RepositoryResult<List<Branch>> {
        return loadListFromAsset(
            path = "sample_data/branches.json",
            typeToken = object : TypeToken<List<Branch>>() {}
        )
    }

    fun loadDishes(): RepositoryResult<List<Dish>> {
        return loadListFromAsset(
            path = "sample_data/dishes.json",
            typeToken = object : TypeToken<List<Dish>>() {}
        )
    }

    private fun <T> loadListFromAsset(
        path: String,
        typeToken: TypeToken<List<T>>
    ): RepositoryResult<List<T>> {
        return runCatching {
            context.assets.open(path).bufferedReader().use { reader ->
                gson.fromJson<List<T>>(reader, typeToken.type)
            }
        }.fold(
            onSuccess = { data ->
                RepositoryResult.Success(data ?: emptyList())
            },
            onFailure = {
                RepositoryResult.Error("Không thể tải dữ liệu cục bộ từ $path")
            }
        )
    }

    fun loadCatalog(): RepositoryResult<CatalogData> {
        val branches = loadBranches()
        val dishes = loadDishes()

        return when {
            branches is RepositoryResult.Error -> branches
            dishes is RepositoryResult.Error -> dishes
            branches is RepositoryResult.Success && dishes is RepositoryResult.Success -> {
                RepositoryResult.Success(
                    CatalogData(branches = branches.data, dishes = dishes.data)
                )
            }
            else -> RepositoryResult.Error("Không thể hoàn tất tải danh mục")
        }
    }
}

data class CatalogData(
    val branches: List<Branch>,
    val dishes: List<Dish>
)
