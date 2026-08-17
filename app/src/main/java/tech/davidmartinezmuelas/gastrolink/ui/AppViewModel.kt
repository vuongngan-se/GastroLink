package tech.davidmartinezmuelas.gastrolink.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.davidmartinezmuelas.gastrolink.BuildConfig
import tech.davidmartinezmuelas.gastrolink.data.DataStoreEntitlementRepository
import tech.davidmartinezmuelas.gastrolink.data.EntitlementRepository
import tech.davidmartinezmuelas.gastrolink.data.LocalJsonRepository
import tech.davidmartinezmuelas.gastrolink.data.RemoteCatalogRepository
import tech.davidmartinezmuelas.gastrolink.data.OrderExportRepository
import tech.davidmartinezmuelas.gastrolink.data.OrderRepository
import tech.davidmartinezmuelas.gastrolink.data.RepositoryResult
import tech.davidmartinezmuelas.gastrolink.data.SettingsRepository
import tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationServiceImpl
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.AiDishScanResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudProfileSync
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudSavedProfileSync
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudOrderSync
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudOrderItemSync
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadRequest
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncUploadResponse
import tech.davidmartinezmuelas.gastrolink.data.ai.CloudSyncDownloadResponse
import tech.davidmartinezmuelas.gastrolink.data.local.GastroLinkDatabase
import tech.davidmartinezmuelas.gastrolink.data.local.OrderEntity
import tech.davidmartinezmuelas.gastrolink.data.local.OrderItemEntity
import tech.davidmartinezmuelas.gastrolink.data.local.ParticipantEntity
import tech.davidmartinezmuelas.gastrolink.data.local.ProfileEntity
import tech.davidmartinezmuelas.gastrolink.domain.EntitlementUseCase
import tech.davidmartinezmuelas.gastrolink.domain.GetRecommendationUseCase
import tech.davidmartinezmuelas.gastrolink.domain.NutritionCalculator
import tech.davidmartinezmuelas.gastrolink.domain.NutritionStatsCalculator
import tech.davidmartinezmuelas.gastrolink.domain.RecommendationResult
import tech.davidmartinezmuelas.gastrolink.domain.RecommendationSource
import tech.davidmartinezmuelas.gastrolink.model.ActivityLevel
import tech.davidmartinezmuelas.gastrolink.model.Branch
import tech.davidmartinezmuelas.gastrolink.model.CartItem
import tech.davidmartinezmuelas.gastrolink.model.Dish
import tech.davidmartinezmuelas.gastrolink.model.Entitlement
import tech.davidmartinezmuelas.gastrolink.model.Goal
import tech.davidmartinezmuelas.gastrolink.model.GroupNutritionProfile
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.NutritionTotals
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.model.Participant
import tech.davidmartinezmuelas.gastrolink.model.RecommendationContext
import tech.davidmartinezmuelas.gastrolink.model.Sex
import tech.davidmartinezmuelas.gastrolink.model.SavedProfile
import tech.davidmartinezmuelas.gastrolink.model.SoloNutritionProfile
import tech.davidmartinezmuelas.gastrolink.model.UserProfile
import java.util.UUID

data class AppUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val isHistoryLoading: Boolean = false,
    val isSavingOrder: Boolean = false,
    val historyErrorMessage: String? = null,
    val branches: List<Branch> = emptyList(),
    val dishes: List<Dish> = emptyList(),
    val entitlement: Entitlement = Entitlement.FREE,
    val orderMode: OrderMode? = null,
    val nutritionMode: NutritionMode? = null,
    val selectedBranch: Branch? = null,
    val participants: List<Participant> = emptyList(),
    val selectedParticipantId: String? = null,
    val soloProfile: SoloNutritionProfile = SoloNutritionProfile(),
    val groupProfiles: Map<String, GroupNutritionProfile> = emptyMap(),
    val savedProfiles: List<SavedProfile> = emptyList(),
    val profileResetKey: Int = 0,
    val cartItems: List<CartItem> = emptyList(),
    val orderHistory: List<OrderHistoryItemUi> = emptyList(),
    val selectedOrderDetail: OrderDetailUi? = null,
    val stats: StatsUi = StatsUi(),
    val useAiRecommendations: Boolean = false,
    val isRecommendationLoading: Boolean = false,
    val recommendationResult: RecommendationResult = RecommendationResult(
        source = RecommendationSource.NONE,
        messages = emptyList()
    ),
    val chatMessages: List<tech.davidmartinezmuelas.gastrolink.model.ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null
)

data class SummaryUiState(
    val totals: NutritionTotals,
    val recommendations: List<String>,
    val totalsByParticipant: Map<String, NutritionTotals>,
    val recommendationSource: RecommendationSource,
    val isRecommendationLoading: Boolean,
    val totalAmount: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val catalogRepository = RemoteCatalogRepository(
        baseUrl = BuildConfig.AI_BASE_URL,
        proxyToken = BuildConfig.AI_PROXY_TOKEN
    )
    private val entitlementRepository: EntitlementRepository = DataStoreEntitlementRepository(application)
    private val database = GastroLinkDatabase.getInstance(application)
    private val orderRepository = OrderRepository(
        database.orderDao()
    )
    private val orderExportRepository = OrderExportRepository()
    private val entitlementUseCase = EntitlementUseCase(
        entitlementRepository = entitlementRepository,
        isAiEnabledByBuild = BuildConfig.AI_ENABLED
    )
    private val settingsRepository = SettingsRepository(application)
    private val aiService = AiRecommendationServiceImpl(
        baseUrl = BuildConfig.AI_BASE_URL,
        proxyToken = BuildConfig.AI_PROXY_TOKEN
    )
    private val getRecommendationUseCase = GetRecommendationUseCase(
        aiRecommendationService = aiService,
        isAiEnabledByBuild = BuildConfig.AI_ENABLED,
        includeDebugInfo = BuildConfig.DEBUG
    )
    private val gson = Gson()
    private var recommendationJob: Job? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val summaryUiState: StateFlow<SummaryUiState> = _uiState
        .map { state ->
            val totals = NutritionCalculator.calculateTotals(state.cartItems)
            val totalsByParticipant = NutritionCalculator.calculateTotalsByParticipant(state.cartItems)
            val totalAmount = state.cartItems.sumOf { it.dish.price * it.qty }
            SummaryUiState(
                totals = totals,
                recommendations = state.recommendationResult.messages,
                totalsByParticipant = totalsByParticipant,
                recommendationSource = state.recommendationResult.source,
                isRecommendationLoading = state.isRecommendationLoading,
                totalAmount = totalAmount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SummaryUiState(
                totals = NutritionCalculator.calculateTotals(emptyList()),
                recommendations = emptyList(),
                totalsByParticipant = emptyMap(),
                recommendationSource = RecommendationSource.NONE,
                isRecommendationLoading = false
            )
        )

    init {
        loadCatalog()
        observeEntitlement()
        observeSettings()
        observeProfileData()
        observeRecommendationInputs()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }

            when (val result = catalogRepository.loadCatalog()) {
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = result.message
                        )
                    }
                }
                is RepositoryResult.Success -> {
                    val defaultBranch = result.data.branches.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = null,
                            branches = result.data.branches,
                            dishes = result.data.dishes,
                            selectedBranch = defaultBranch
                        )
                    }
                    loadOrderHistory()
                }
            }
        }
    }

    fun retryLoadCatalog() {
        loadCatalog()
    }

    suspend fun wipeAllLocalData(): DataWipeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val currentState = _uiState.value
            database.clearAllTables()
            settingsRepository.clearAllPreferences()

            _uiState.update {
                AppUiState(
                    isLoading = false,
                    branches = currentState.branches,
                    dishes = currentState.dishes,
                    selectedBranch = currentState.branches.firstOrNull()
                )
            }
            DataWipeResult(success = true, message = "Dữ liệu đã được xóa")
        }.getOrElse {
            DataWipeResult(success = false, message = "Không thể xóa dữ liệu")
        }
    }

    suspend fun exportOrderHistory(format: HistoryExportFormat): HistoryExportResult {
        return runCatching {
            val orders = orderRepository.getOrders()
            if (orders.isEmpty()) {
                return HistoryExportResult.EmptyHistory
            }

            val state = _uiState.value
            val dishesById = state.dishes.associateBy { it.id }
            val branchesById = state.branches.associateBy { it.id }

            val content = when (format) {
                HistoryExportFormat.JSON -> {
                    orderExportRepository.exportOrdersToJson(
                        ordersWithItems = orders,
                        dishesById = dishesById,
                        branchesById = branchesById
                    )
                }

                HistoryExportFormat.CSV -> {
                    orderExportRepository.exportOrdersToCsv(
                        ordersWithItems = orders,
                        dishesById = dishesById,
                        branchesById = branchesById
                    )
                }
            }

            val payload = when (format) {
                HistoryExportFormat.JSON -> HistoryExportPayload(
                    fileName = "orders_export.json",
                    mimeType = "application/json",
                    content = content
                )

                HistoryExportFormat.CSV -> HistoryExportPayload(
                    fileName = "orders_export.csv",
                    mimeType = "text/csv",
                    content = content
                )
            }

            HistoryExportResult.Success(payload)
        }.getOrElse {
            HistoryExportResult.Error
        }
    }

    fun isPremiumModeEnabled(): Boolean {
        return entitlementUseCase.isPremiumEnabled(_uiState.value.entitlement)
    }

    fun canUseAiRecommendations(): Boolean {
        return entitlementUseCase.canUseAiRecommendations(_uiState.value.entitlement)
    }

    fun setPremiumDemoEnabled(enabled: Boolean) {
        val entitlement = if (enabled) Entitlement.PREMIUM_DEMO else Entitlement.FREE
        val canUseAi = entitlementUseCase.canUseAiRecommendations(entitlement)
        _uiState.update { current ->
            current.copy(
                entitlement = entitlement,
                nutritionMode = if (!entitlementUseCase.canUseNutritionWithProfile(entitlement) &&
                    current.nutritionMode == NutritionMode.WITH_PROFILE
                ) {
                    NutritionMode.WITHOUT_PROFILE
                } else {
                    current.nutritionMode
                },
                useAiRecommendations = current.useAiRecommendations && canUseAi
            )
        }

        viewModelScope.launch {
            entitlementRepository.setPremiumDemoEnabled(enabled)
            if (!canUseAi) {
                settingsRepository.setUseAiRecommendations(false)
            }
        }
    }

    fun setUseAiRecommendations(enabled: Boolean) {
        viewModelScope.launch {
            val allowed = entitlementUseCase.canUseAiRecommendations(_uiState.value.entitlement)
            settingsRepository.setUseAiRecommendations(enabled && allowed)
        }
    }

    fun setOrderMode(mode: OrderMode) {
        _uiState.update { state ->
            val participants = if (mode == OrderMode.GROUP) {
                state.participants.ifEmpty { defaultParticipants() }
            } else {
                emptyList()
            }

            state.copy(
                orderMode = mode,
                nutritionMode = null,
                participants = participants,
                selectedParticipantId = participants.firstOrNull()?.id,
                groupProfiles = participants.associate { participant ->
                    participant.id to GroupNutritionProfile(participantId = participant.id)
                },
                cartItems = emptyList()
            )
        }
    }

    fun chooseNutritionWithoutProfile() {
        _uiState.update { it.copy(nutritionMode = NutritionMode.WITHOUT_PROFILE) }
    }

    fun chooseNutritionWithProfile(): Boolean {
        val allowed = entitlementUseCase.canUseNutritionWithProfile(_uiState.value.entitlement)
        if (!allowed) {
            return false
        }

        _uiState.update { it.copy(nutritionMode = NutritionMode.WITH_PROFILE) }
        return true
    }

    fun updateSoloProfile(
        age: String,
        sex: Sex?,
        heightCm: String,
        weightKg: String,
        goal: Goal?,
        activityLevel: ActivityLevel?,
        allergies: String
    ) {
        val updated = SoloNutritionProfile(
            age = age.toIntOrNull(),
            sex = sex,
            heightCm = heightCm.toIntOrNull(),
            weightKg = weightKg.toIntOrNull(),
            goal = goal,
            activityLevel = activityLevel,
            allergies = allergies.trim()
        )
        _uiState.update { it.copy(soloProfile = updated) }
        viewModelScope.launch {
            settingsRepository.saveSoloProfileJson(gson.toJson(updated))
            uploadDataToCloudSilent()
        }
    }

    fun hasSoloProfileData(): Boolean {
        val p = _uiState.value.soloProfile
        return p.age != null || p.sex != null || p.heightCm != null ||
            p.weightKg != null || p.goal != null
    }

    fun saveCurrentProfileAs(name: String) {
        val trimmedName = name.trim().ifBlank { return }
        val newSaved = SavedProfile(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            profile = _uiState.value.soloProfile
        )
        val updated = _uiState.value.savedProfiles + newSaved
        _uiState.update { it.copy(savedProfiles = updated) }
        viewModelScope.launch {
            settingsRepository.saveSavedProfilesJson(gson.toJson(updated))
            uploadDataToCloudSilent()
        }
    }

    fun deleteSavedProfile(id: String) {
        val updated = _uiState.value.savedProfiles.filterNot { it.id == id }
        _uiState.update { it.copy(savedProfiles = updated) }
        viewModelScope.launch {
            settingsRepository.saveSavedProfilesJson(gson.toJson(updated))
            uploadDataToCloudSilent()
        }
    }

    fun loadSavedProfile(id: String) {
        val saved = _uiState.value.savedProfiles.find { it.id == id } ?: return
        _uiState.update { state ->
            state.copy(
                soloProfile = saved.profile,
                profileResetKey = state.profileResetKey + 1
            )
        }
        viewModelScope.launch {
            settingsRepository.saveSoloProfileJson(gson.toJson(saved.profile))
            uploadDataToCloudSilent()
        }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim().ifBlank { return }
        val userMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage("user", trimmed)
        val history = _uiState.value.chatMessages + userMsg
        _uiState.update { it.copy(chatMessages = history, isChatLoading = true) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val profilePayload = if (state.soloProfile.let {
                        it.age != null || it.weightKg != null || it.goal != null
                    }) {
                    val summaryMap = mutableMapOf<String, Any?>()
                    state.soloProfile.age?.let { summaryMap["edad"] = it }
                    state.soloProfile.weightKg?.let { summaryMap["pesoKg"] = it }
                    state.soloProfile.heightCm?.let { summaryMap["alturaCm"] = it }
                    state.soloProfile.sex?.name?.let { summaryMap["sexo"] = it }
                    state.soloProfile.goal?.name?.let { summaryMap["objetivo"] = it }
                    state.soloProfile.activityLevel?.name?.let { summaryMap["actividad"] = it }
                    tech.davidmartinezmuelas.gastrolink.data.ai.AiRecommendationRequest.ProfilePayload(
                        type = "SOLO",
                        summary = summaryMap
                    )
                } else null

                val request = tech.davidmartinezmuelas.gastrolink.data.ai.AiChatRequest(
                    messages = history.map {
                        tech.davidmartinezmuelas.gastrolink.data.ai.AiChatRequest.ChatMessagePayload(it.role, it.content)
                    },
                    profile = profilePayload,
                    availableDishes = state.dishes.map { it.name }.ifEmpty { null }
                )

                val response = aiService.chat(request)
                val assistantMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage("assistant", response.reply)
                _uiState.update { it.copy(chatMessages = it.chatMessages + assistantMsg, isChatLoading = false) }
            } catch (e: Exception) {
                val errorMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage(
                    "assistant", "Rất tiếc, tôi không thể kết nối với trợ lý ảo. Vui lòng kiểm tra kết nối mạng và thử lại."
                )
                _uiState.update { it.copy(chatMessages = it.chatMessages + errorMsg, isChatLoading = false) }
            }
        }
    }

    private fun generateSmartWarnings(
        name: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        ingredients: List<String>
    ): String {
        val state = _uiState.value
        val profile = state.soloProfile
        val warnings = mutableListOf<String>()

        // 1. Kiểm tra dị ứng (Allergies)
        if (profile.allergies.isNotBlank()) {
            val allergyList = profile.allergies.split(",").map { it.trim().lowercase() }
            for (allergy in allergyList) {
                if (allergy.isNotBlank()) {
                    val matchesName = name.lowercase().contains(allergy)
                    val matchesIngredient = ingredients.any { it.lowercase().contains(allergy) }
                    if (matchesName || matchesIngredient) {
                        warnings.add("🚨 CẢNH BÁO DỊ ỨNG: Phát hiện nguyên liệu có thể chứa '${allergy.uppercase()}' gây nguy hiểm với tiền sử dị ứng của bạn!")
                    }
                }
            }
        }

        // 2. Kiểm tra Mục tiêu dinh dưỡng (Goal)
        val goal = profile.goal
        if (goal != null) {
            when (goal) {
                tech.davidmartinezmuelas.gastrolink.model.Goal.LOSE_WEIGHT -> {
                    if (kcal > 400) {
                        warnings.add("⚠️ Lưu ý Giảm cân: Lượng Calo khá cao ($kcal Kcal). Hãy cân nhắc kiểm soát khẩu phần ăn nhỏ hơn!")
                    }
                    if (carbsG > 30) {
                        warnings.add("⚠️ Giới hạn giảm cân: Hàm lượng Carbs khá cao (${carbsG}g), hãy cẩn thận để không ảnh hưởng đến mục tiêu giảm mỡ.")
                    }
                }
                tech.davidmartinezmuelas.gastrolink.model.Goal.GAIN_MUSCLE -> {
                    if (proteinG < 15) {
                        warnings.add("💪 Lưu ý Tăng cơ: Lượng đạm trong món ăn này khá thấp (${proteinG}g). Bạn nên ăn kèm thêm món giàu protein để kích thích cơ bắp phát triển!")
                    } else {
                        warnings.add("💪 Rất tốt cho Tăng cơ: Hàm lượng đạm dồi dào (${proteinG}g) cực kỳ tối ưu cho cơ bắp của bạn!")
                    }
                }
                tech.davidmartinezmuelas.gastrolink.model.Goal.MAINTAIN -> {
                    // Cân đối
                }
            }
        }

        if (warnings.isEmpty()) return ""

        return "\n🛡️ **Phân tích sức khỏe cá nhân (AI Health Warning):**\n" + warnings.joinToString("\n") + "\n"
    }

    private fun formatRecipes(recipes: List<String>): String {
        if (recipes.isEmpty()) return ""
        return "\n🍳 **Gợi ý công thức chế biến lành mạnh từ AI:**\n" + 
                recipes.joinToString("\n") + "\n"
    }

    fun scanDishImage(base64Image: String) {
        val userMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage("user", "📸 [Đã gửi một hình ảnh món ăn để quét dinh dưỡng]")
        val history = _uiState.value.chatMessages + userMsg
        _uiState.update { it.copy(chatMessages = history, isChatLoading = true) }

        viewModelScope.launch {
            try {
                val response = aiService.scanDish(AiDishScanRequest(image = base64Image))
                val warningsText = generateSmartWarnings(
                    response.name, response.kcal, response.proteinG, response.carbsG, response.fatG, response.ingredients
                )
                val recipesText = formatRecipes(response.recipes)
                
                val replyText = """
                    🔍 Kết quả phân tích món ăn từ ảnh chụp:
                    
                    🥗 Tên món: ${response.name}
                    🔥 Năng lượng: ${response.kcal} Kcal
                    💪 Đạm (Protein): ${response.proteinG}g
                    🌾 Đường bột (Carbs): ${response.carbsG}g
                    🥑 Chất béo (Fat): ${response.fatG}g
                    
                    🥕 Thành phần chính: ${response.ingredients.joinToString(", ")}
                    $warningsText$recipesText
                    👉 Đây là một lựa chọn cực kỳ dinh dưỡng! Tôi có thể giúp bạn tìm thêm các món ăn lành mạnh tương tự trên thực đơn hôm nay của nhà hàng không?
                """.trimIndent()
                
                val assistantMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage("assistant", replyText)
                _uiState.update { it.copy(chatMessages = it.chatMessages + assistantMsg, isChatLoading = false) }
            } catch (e: Exception) {
                kotlinx.coroutines.delay(1500)
                
                val mockResponses = listOf(
                    AiDishScanResponse(
                        name = "Cá hồi nướng củ thì là & cà chua",
                        kcal = 450,
                        proteinG = 35,
                        carbsG = 12,
                        fatG = 28,
                        ingredients = listOf("Cá hồi phi lê", "Thì là tươi", "Cà chua bi", "Dầu ô liu"),
                        recipes = listOf(
                            "• Công thức 1: Cá hồi áp chảo sốt chanh leo - Áp chảo cá hồi với tỏi, rim lửa nhỏ cùng nước cốt chanh leo chua ngọt tự nhiên.",
                            "• Công thức 2: Cháo cá hồi yến mạch - Nấu yến mạch cùng cá hồi phi lê dầm nhỏ, hành lá và gừng tươi giúp giữ ấm bụng."
                        )
                    ),
                    AiDishScanResponse(
                        name = "Salad tôm tươi Bang Bang cay nồng",
                        kcal = 320,
                        proteinG = 24,
                        carbsG = 15,
                        fatG = 12,
                        ingredients = listOf("Tôm sú lột vỏ", "Xà lách", "Cà chua bi", "Sốt bơ lạc cay"),
                        recipes = listOf(
                            "• Công thức 1: Salad tôm sốt sữa chua Hy Lạp - Luộc chín tôm, trộn cùng xà lách tươi và sốt sữa chua Hy Lạp ít béo để giảm calo.",
                            "• Công thức 2: Tôm áp chảo muối ớt - Áp chảo tôm với 1 thìa dầu ô liu và bột ớt cay, ăn kèm rau luộc để tăng xơ."
                        )
                    ),
                    AiDishScanResponse(
                        name = "Bánh kẹp Tacos cá chiên sốt Cajun",
                        kcal = 410,
                        proteinG = 22,
                        carbsG = 38,
                        fatG = 18,
                        ingredients = listOf("Bánh Tortilla dẻo", "Cá basa chiên giòn", "Gia vị Cajun", "Xà lách cải bắp"),
                        recipes = listOf(
                            "• Công thức 1: Tacos cá áp chảo ăn kiêng - Áp chảo cá thay vì chiên ngập dầu, dùng bánh tortilla lúa mạch nguyên cám.",
                            "• Công thức 2: Salad tacos cá - Bỏ vỏ bánh Tortilla, trộn thịt cá áp chảo cùng bắp cải thái nhỏ và sốt sữa chua tỏi."
                        )
                    ),
                    AiDishScanResponse(
                        name = "Thịt gà ta tươi sống (Chưa chế biến)",
                        kcal = 215,
                        proteinG = 27,
                        carbsG = 0,
                        fatG = 11,
                        ingredients = listOf("Thịt gà tươi nguyên con", "Da gà thô"),
                        recipes = listOf(
                            "• Công thức 1: Gà hấp hành thảo mộc - Hấp gà cách thủy với gừng tươi, hành lá và một chút muối để giữ nguyên độ ngọt thanh và ít calo.",
                            "• Công thức 2: Gà áp chảo sốt tiêu đen - Lọc bỏ da gà, áp chảo với 1 thìa dầu ô liu, tiêu đen băm nhuyễn và hành tỏi thơm."
                        )
                    )
                )
                
                val mock = mockResponses.random()
                val warningsText = generateSmartWarnings(
                    mock.name, mock.kcal, mock.proteinG, mock.carbsG, mock.fatG, mock.ingredients
                )
                val recipesText = formatRecipes(mock.recipes)
                
                val replyText = """
                    🔍 [Trợ lý dự phòng] Kết quả phân tích ảnh chụp:
                    
                    🥗 Món ăn nhận diện: ${mock.name}
                    🔥 Năng lượng: ${mock.kcal} Kcal
                    💪 Đạm (Protein): ${mock.proteinG}g
                    🌾 Đường bột (Carbs): ${mock.carbsG}g
                    🥑 Chất béo (Fat): ${mock.fatG}g
                    
                    🥕 Thành phần chính: ${mock.ingredients.joinToString(", ")}
                    $warningsText$recipesText
                    👉 Hệ thống phát hiện đây là một lựa chọn cực tốt cho mục tiêu dinh dưỡng hôm nay của bạn! Tôi đã tìm thấy món này trong thực đơn sẵn có, bạn có muốn thêm món này vào giỏ hàng không?
                """.trimIndent()
                
                val assistantMsg = tech.davidmartinezmuelas.gastrolink.model.ChatMessage("assistant", replyText)
                _uiState.update { it.copy(chatMessages = it.chatMessages + assistantMsg, isChatLoading = false) }
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(chatMessages = emptyList()) }
    }

    fun addParticipant() {
        _uiState.update { state ->
            val newName = "Thành viên ${state.participants.size + 1}"
            val participant = Participant(id = UUID.randomUUID().toString(), name = newName)
            state.copy(
                participants = state.participants + participant,
                selectedParticipantId = state.selectedParticipantId ?: participant.id,
                groupProfiles = state.groupProfiles + (participant.id to GroupNutritionProfile(participantId = participant.id))
            )
        }
    }

    fun removeParticipant(participantId: String) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.filterNot { it.id == participantId }
            if (updatedParticipants.isEmpty()) {
                return@update state
            }

            state.copy(
                participants = updatedParticipants,
                selectedParticipantId = if (state.selectedParticipantId == participantId) {
                    updatedParticipants.first().id
                } else {
                    state.selectedParticipantId
                },
                groupProfiles = state.groupProfiles - participantId,
                cartItems = state.cartItems.filterNot { it.participantId == participantId }
            )
        }
    }

    fun renameParticipant(participantId: String, name: String) {
        _uiState.update { state ->
            state.copy(
                participants = state.participants.map { participant ->
                    if (participant.id == participantId) participant.copy(name = name) else participant
                }
            )
        }
    }

    fun selectParticipant(participantId: String) {
        _uiState.update { it.copy(selectedParticipantId = participantId) }
    }

    fun updateGroupProfile(
        participantId: String,
        allergies: String,
        preferences: String,
        generalGoal: Goal?
    ) {
        _uiState.update { state ->
            state.copy(
                groupProfiles = state.groupProfiles + (
                    participantId to GroupNutritionProfile(
                        participantId = participantId,
                        allergies = allergies.trim(),
                        preferences = preferences.trim(),
                        generalGoal = generalGoal
                    )
                )
            )
        }
    }

    fun selectBranch(branch: Branch) {
        _uiState.update { it.copy(selectedBranch = branch) }
    }

    fun addDishToCart(dish: Dish, participantId: String? = null) {
        val state = _uiState.value
        val participantForItem = if (state.orderMode == OrderMode.GROUP) {
            participantId ?: state.selectedParticipantId
        } else {
            null
        }
        val existing = state.cartItems.find {
            it.dish.id == dish.id && it.participantId == participantForItem
        }

        val updatedItems = if (existing == null) {
            state.cartItems + CartItem(dish = dish, qty = 1, participantId = participantForItem)
        } else {
            state.cartItems.map {
                if (it.dish.id == dish.id && it.participantId == participantForItem) {
                    it.copy(qty = it.qty + 1)
                } else {
                    it
                }
            }
        }

        _uiState.update { it.copy(cartItems = updatedItems) }
    }

    fun confirmOrder(address: String, phoneNumber: String, paymentMethod: String) {
        val state = _uiState.value
        val selectedBranch = state.selectedBranch ?: return
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingOrder = true, historyErrorMessage = null) }

            runCatching {
                val orderId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val orderEntity = OrderEntity(
                    id = orderId,
                    branchId = selectedBranch.id,
                    orderMode = mapOrderMode(state.orderMode),
                    nutritionMode = mapNutritionMode(state.nutritionMode),
                    createdAt = now,
                    address = address,
                    phoneNumber = phoneNumber,
                    paymentMethod = paymentMethod
                )

                val participantIdMap = state.participants.associate { participant ->
                    participant.id to "${orderId}_${participant.id}"
                }

                val itemEntities = state.cartItems.map {
                    OrderItemEntity(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        dishId = it.dish.id,
                        quantity = it.qty,
                        participantId = it.participantId?.let { participantId ->
                            participantIdMap[participantId] ?: participantId
                        }
                    )
                }

                val participantEntities = state.participants.map {
                    ParticipantEntity(
                        id = participantIdMap[it.id] ?: it.id,
                        orderId = orderId,
                        name = it.name
                    )
                }

                val profileEntity = buildProfileEntity(orderId, state)

                orderRepository.saveOrder(
                    order = orderEntity,
                    items = itemEntities,
                    participants = participantEntities,
                    profile = profileEntity
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        cartItems = emptyList(),
                        isSavingOrder = false
                    )
                }
                loadOrderHistory()
                uploadDataToCloudSilent()
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSavingOrder = false,
                        historyErrorMessage = "Không thể lưu đơn hàng"
                    )
                }
            }
        }
    }

    fun loadOrderHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryLoading = true, historyErrorMessage = null) }

            runCatching {
                val orders = orderRepository.getOrders()
                val state = _uiState.value
                val dishesById = state.dishes.associateBy { it.id }
                val branchesById = state.branches.associateBy { it.id }

                val historyItems = orders.map { orderWithItems ->
                    val dishCount = orderWithItems.items.sumOf { it.quantity }
                    val totalCalories = orderWithItems.items.sumOf { item ->
                        (dishesById[item.dishId]?.kcal ?: 0) * item.quantity
                    }
                    OrderHistoryItemUi(
                        id = orderWithItems.order.id,
                        createdAt = orderWithItems.order.createdAt,
                        branchName = branchesById[orderWithItems.order.branchId]?.name ?: "Chi nhánh không xác định",
                        dishCount = dishCount,
                        totalCalories = totalCalories
                    )
                }

                val recentCalList = historyItems.take(7).map { it.totalCalories }.reversed()

                val stats = NutritionStatsCalculator
                    .calculate(orders, dishesById)
                    .toStatsUi(recentCalList) { dishId ->
                        dishesById[dishId]?.name ?: "Không có dữ liệu"
                    }

                historyItems to stats
            }.onSuccess { (history, stats) ->
                _uiState.update {
                    it.copy(
                        isHistoryLoading = false,
                        orderHistory = history,
                        stats = stats
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isHistoryLoading = false,
                        historyErrorMessage = "Không thể tải lịch sử"
                    )
                }
            }
        }
    }

    fun clearSelectedOrderDetail() {
        _uiState.update { it.copy(selectedOrderDetail = null) }
    }

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(historyErrorMessage = null) }
            runCatching {
                val detailRecord = orderRepository.getOrderDetails(orderId)
                val withItems = detailRecord.orderWithItems ?: return@runCatching null
                val state = _uiState.value
                val dishesById = state.dishes.associateBy { it.id }
                val branchesById = state.branches.associateBy { it.id }
                val participants = detailRecord
                    .orderWithParticipants
                    ?.participants
                    ?.map { Participant(id = it.id, name = it.name) }
                    .orEmpty()
                val participantNameById = participants.associateBy({ it.id }, { it.name })

                val itemsUi = withItems.items.map { item ->
                    OrderDetailItemUi(
                        dishName = dishesById[item.dishId]?.name ?: item.dishId,
                        quantity = item.quantity,
                        participantName = item.participantId?.let { participantNameById[it] ?: it },
                        price = dishesById[item.dishId]?.price ?: 0
                    )
                }

                val cartItems = withItems.items.mapNotNull { item ->
                    val dish = dishesById[item.dishId] ?: return@mapNotNull null
                    CartItem(
                        dish = dish,
                        qty = item.quantity,
                        participantId = item.participantId
                    )
                }

                val totalAmount = withItems.items.sumOf { item ->
                    (dishesById[item.dishId]?.price ?: 0) * item.quantity
                }

                val profileAudit = buildProfileAudit(
                    profile = detailRecord.profile,
                    canExposeRawProfile = BuildConfig.DEBUG || state.entitlement == Entitlement.PREMIUM_DEMO
                )

                OrderDetailUi(
                    id = withItems.order.id,
                    createdAt = withItems.order.createdAt,
                    branchName = branchesById[withItems.order.branchId]?.name ?: "Chi nhánh không xác định",
                    orderMode = withItems.order.orderMode,
                    nutritionMode = withItems.order.nutritionMode,
                    participants = participants,
                    items = itemsUi,
                    totals = NutritionCalculator.calculateTotals(cartItems),
                    totalAmount = totalAmount,
                    profileType = profileAudit.profileType,
                    profileSummaryLines = profileAudit.summaryLines,
                    profileParseable = profileAudit.parseable,
                    rawProfileJson = profileAudit.rawJson,
                    canShowRawProfileJson = profileAudit.canExposeRaw,
                    address = withItems.order.address,
                    phoneNumber = withItems.order.phoneNumber,
                    paymentMethod = withItems.order.paymentMethod
                )
            }.onSuccess { detail ->
                _uiState.update { it.copy(selectedOrderDetail = detail) }
            }.onFailure {
                _uiState.update { it.copy(historyErrorMessage = "Không thể tải chi tiết đơn hàng") }
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            runCatching {
                orderRepository.deleteOrder(orderId)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        selectedOrderDetail = if (state.selectedOrderDetail?.id == orderId) {
                            null
                        } else {
                            state.selectedOrderDetail
                        }
                    )
                }
                loadOrderHistory()
            }.onFailure {
                _uiState.update { it.copy(historyErrorMessage = "Không thể xóa đơn hàng") }
            }
        }
    }

    fun increaseItem(dishId: String, participantId: String?) {
        val updatedItems = _uiState.value.cartItems.map {
            if (it.dish.id == dishId && it.participantId == participantId) {
                it.copy(qty = it.qty + 1)
            } else {
                it
            }
        }
        _uiState.update { it.copy(cartItems = updatedItems) }
    }

    fun decreaseItem(dishId: String, participantId: String?) {
        val updatedItems = _uiState.value.cartItems.mapNotNull {
            if (it.dish.id != dishId || it.participantId != participantId) {
                return@mapNotNull it
            }

            val newQty = it.qty - 1
            if (newQty <= 0) null else it.copy(qty = newQty)
        }

        _uiState.update { it.copy(cartItems = updatedItems) }
    }

    private fun buildRecommendationContext(state: AppUiState): RecommendationContext {
        val userProfile = when {
            state.nutritionMode != NutritionMode.WITH_PROFILE -> null
            state.orderMode == OrderMode.SOLO -> UserProfile.Solo(state.soloProfile)
            state.orderMode == OrderMode.GROUP -> UserProfile.Group(
                profiles = state.participants.map { participant ->
                    state.groupProfiles[participant.id] ?: GroupNutritionProfile(participant.id)
                }
            )
            else -> null
        }

        return RecommendationContext(
            orderMode = state.orderMode ?: OrderMode.SOLO,
            nutritionMode = state.nutritionMode ?: NutritionMode.WITHOUT_PROFILE,
            userProfile = userProfile
        )
    }

    private fun defaultParticipants(): List<Participant> {
        return listOf(
            Participant(id = UUID.randomUUID().toString(), name = "Thành viên 1"),
            Participant(id = UUID.randomUUID().toString(), name = "Thành viên 2")
        )
    }

    private fun mapOrderMode(orderMode: OrderMode?): String {
        return if (orderMode == OrderMode.GROUP) "GRUPO" else "SOLITARIO"
    }

    private fun mapNutritionMode(nutritionMode: NutritionMode?): String {
        return if (nutritionMode == NutritionMode.WITH_PROFILE) "CON_DATOS" else "SIN_DATOS"
    }

    private fun buildProfileEntity(orderId: String, state: AppUiState): ProfileEntity? {
        if (state.nutritionMode != NutritionMode.WITH_PROFILE) {
            return null
        }

        val (type, json) = if (state.orderMode == OrderMode.GROUP) {
            "GROUP_LIGHT" to gson.toJson(state.groupProfiles.values.toList())
        } else {
            "SOLO" to gson.toJson(state.soloProfile)
        }

        return ProfileEntity(
            id = UUID.randomUUID().toString(),
            orderId = orderId,
            profileType = type,
            dataJson = json
        )
    }

    private fun buildProfileAudit(
        profile: ProfileEntity?,
        canExposeRawProfile: Boolean
    ): ProfileAudit {
        if (profile == null) {
            return ProfileAudit(
                profileType = null,
                summaryLines = emptyList(),
                parseable = true,
                rawJson = null,
                canExposeRaw = false
            )
        }

        return runCatching {
            when (profile.profileType) {
                "SOLO" -> {
                    val parsed = gson.fromJson(profile.dataJson, SoloNutritionProfile::class.java)
                    ProfileAudit(
                        profileType = profile.profileType,
                        summaryLines = listOfNotNull(
                            parsed.age?.let { "Tuổi: $it" },
                            parsed.sex?.let { "Giới tính: ${it.name}" },
                            parsed.goal?.let { "Mục tiêu: ${it.name}" },
                            parsed.activityLevel?.let { "Mức độ hoạt động: ${it.name}" },
                            parsed.allergies.takeIf { it.isNotBlank() }?.let { "Dị ứng: $it" }
                        ),
                        parseable = true,
                        rawJson = profile.dataJson,
                        canExposeRaw = canExposeRawProfile
                    )
                }
                "GROUP_LIGHT" -> {
                    val listType = object : TypeToken<List<GroupNutritionProfile>>() {}.type
                    val parsedList = gson.fromJson<List<GroupNutritionProfile>>(profile.dataJson, listType).orEmpty()
                    val summary = mutableListOf<String>()
                    parsedList.forEachIndexed { index, groupProfile ->
                        summary += "Thành viên ${index + 1}"
                        if (groupProfile.generalGoal != null) {
                            summary += "Mục tiêu: ${groupProfile.generalGoal.name}"
                        }
                        if (groupProfile.preferences.isNotBlank()) {
                            summary += "Sở thích: ${groupProfile.preferences}"
                        }
                        if (groupProfile.allergies.isNotBlank()) {
                            summary += "Dị ứng: ${groupProfile.allergies}"
                        }
                    }

                    ProfileAudit(
                        profileType = profile.profileType,
                        summaryLines = summary,
                        parseable = true,
                        rawJson = profile.dataJson,
                        canExposeRaw = canExposeRawProfile
                    )
                }
                else -> {
                    ProfileAudit(
                        profileType = profile.profileType,
                        summaryLines = listOf("Đã lưu hồ sơ (loại không xác định)"),
                        parseable = false,
                        rawJson = profile.dataJson,
                        canExposeRaw = canExposeRawProfile
                    )
                }
            }
        }.getOrElse {
            ProfileAudit(
                profileType = profile.profileType,
                summaryLines = listOf("Đã lưu hồ sơ (không thể phân tích)"),
                parseable = false,
                rawJson = profile.dataJson,
                canExposeRaw = canExposeRawProfile
            )
        }
    }

    private fun observeProfileData() {
        viewModelScope.launch {
            settingsRepository.soloProfileJsonFlow.collect { json ->
                if (json != null) {
                    val profile = runCatching {
                        gson.fromJson(json, SoloNutritionProfile::class.java)
                    }.getOrNull() ?: SoloNutritionProfile()
                    _uiState.update { it.copy(soloProfile = profile) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.savedProfilesJsonFlow.collect { json ->
                if (json != null) {
                    val type = object : TypeToken<List<SavedProfile>>() {}.type
                    val profiles = runCatching {
                        gson.fromJson<List<SavedProfile>>(json, type)
                    }.getOrNull() ?: emptyList()
                    _uiState.update { it.copy(savedProfiles = profiles) }
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.useAiRecommendationsFlow.collect { enabled ->
                _uiState.update { it.copy(useAiRecommendations = enabled) }
            }
        }
    }

    private fun observeEntitlement() {
        viewModelScope.launch {
            entitlementRepository.entitlementFlow.collect { entitlement ->
                val canUseAi = entitlementUseCase.canUseAiRecommendations(entitlement)
                _uiState.update { current ->
                    current.copy(
                        entitlement = entitlement,
                        nutritionMode = if (!entitlementUseCase.canUseNutritionWithProfile(entitlement) &&
                            current.nutritionMode == NutritionMode.WITH_PROFILE
                        ) {
                            NutritionMode.WITHOUT_PROFILE
                        } else {
                            current.nutritionMode
                        },
                        useAiRecommendations = current.useAiRecommendations && canUseAi
                    )
                }

                if (!canUseAi && _uiState.value.useAiRecommendations) {
                    settingsRepository.setUseAiRecommendations(false)
                }
            }
        }
    }

    private fun observeRecommendationInputs() {
        viewModelScope.launch {
            _uiState
                .map { state ->
                    RecommendationTrigger(
                        entitlement = state.entitlement,
                        nutritionMode = state.nutritionMode,
                        orderMode = state.orderMode,
                        cartItems = state.cartItems,
                        soloProfile = state.soloProfile,
                        groupProfiles = state.groupProfiles,
                        participants = state.participants,
                        useAiRecommendations = state.useAiRecommendations
                    )
                }
                .distinctUntilChanged()
                .collect {
                    refreshRecommendations()
                }
        }
    }

    private fun refreshRecommendations() {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            val state = _uiState.value
            val context = buildRecommendationContext(state)
            val totals = NutritionCalculator.calculateTotals(state.cartItems)

            if (state.cartItems.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isRecommendationLoading = false,
                        recommendationResult = RecommendationResult(
                            source = RecommendationSource.NONE,
                            messages = emptyList()
                        )
                    )
                }
                return@launch
            }

            val shouldTryAi = state.useAiRecommendations &&
                entitlementUseCase.canUseAiRecommendations(state.entitlement) &&
                context.nutritionMode == NutritionMode.WITH_PROFILE

            if (shouldTryAi) {
                _uiState.update { it.copy(isRecommendationLoading = true) }
            }

            val result = getRecommendationUseCase.execute(
                totals = totals,
                cartItems = state.cartItems,
                context = context,
                entitlement = state.entitlement,
                useAiEnabledByUser = state.useAiRecommendations
            )

            _uiState.update {
                it.copy(
                    isRecommendationLoading = false,
                    recommendationResult = result
                )
            }
        }
    }

    private fun uploadDataToCloudSilent() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val state = _uiState.value
                val profile = state.soloProfile
                val orders = orderRepository.getOrders()
                val dishesById = state.dishes.associateBy { it.id }

                val cloudProfile = CloudProfileSync(
                    age = profile.age,
                    sex = profile.sex?.name,
                    weight = profile.weightKg?.toDouble(),
                    height = profile.heightCm?.toDouble(),
                    goal = profile.goal?.name,
                    activityLevel = profile.activityLevel?.name,
                    allergies = profile.allergies
                )

                val cloudSavedProfiles = state.savedProfiles.map { saved ->
                    CloudSavedProfileSync(
                        id = saved.id,
                        name = saved.name,
                        profile = CloudProfileSync(
                            age = saved.profile.age,
                            sex = saved.profile.sex?.name,
                            weight = saved.profile.weightKg?.toDouble(),
                            height = saved.profile.heightCm?.toDouble(),
                            goal = saved.profile.goal?.name,
                            activityLevel = saved.profile.activityLevel?.name,
                            allergies = saved.profile.allergies
                        )
                    )
                }

                val cloudOrders = orders.map { orderWithItems ->
                    val items = orderWithItems.items.mapNotNull { item ->
                        val dish = dishesById[item.dishId] ?: return@mapNotNull null
                        CloudOrderItemSync(
                            dishName = dish.name,
                            qty = item.quantity,
                            kcal = dish.kcal
                        )
                    }

                    val totalKcal = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.kcal ?: 0) * item.quantity }
                    val totalProtein = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.proteinG ?: 0) * item.quantity }
                    val totalCarbs = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.carbsG ?: 0) * item.quantity }
                    val totalFat = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.fatG ?: 0) * item.quantity }

                    CloudOrderSync(
                        orderId = orderWithItems.order.id,
                        orderDate = orderWithItems.order.createdAt.toString(),
                        totalKcal = totalKcal,
                        totalProtein = totalProtein,
                        totalCarbs = totalCarbs,
                        totalFat = totalFat,
                        items = items
                    )
                }

                val request = CloudSyncUploadRequest(
                    userId = "demo_user",
                    profile = cloudProfile,
                    savedProfiles = cloudSavedProfiles,
                    orders = cloudOrders
                )

                aiService.uploadSync(request)
            } catch (e: Exception) {
                // Silent fail to not disrupt offline usage
            }
        }
    }

    fun uploadDataToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatusMessage = "Đang sao lưu dữ liệu lên đám mây...") }
            try {
                val state = _uiState.value
                val profile = state.soloProfile
                val orders = orderRepository.getOrders()
                val dishesById = state.dishes.associateBy { it.id }

                val cloudProfile = CloudProfileSync(
                    age = profile.age,
                    sex = profile.sex?.name,
                    weight = profile.weightKg?.toDouble(),
                    height = profile.heightCm?.toDouble(),
                    goal = profile.goal?.name,
                    activityLevel = profile.activityLevel?.name,
                    allergies = profile.allergies
                )

                val cloudSavedProfiles = state.savedProfiles.map { saved ->
                    CloudSavedProfileSync(
                        id = saved.id,
                        name = saved.name,
                        profile = CloudProfileSync(
                            age = saved.profile.age,
                            sex = saved.profile.sex?.name,
                            weight = saved.profile.weightKg?.toDouble(),
                            height = saved.profile.heightCm?.toDouble(),
                            goal = saved.profile.goal?.name,
                            activityLevel = saved.profile.activityLevel?.name,
                            allergies = saved.profile.allergies
                        )
                    )
                }

                val cloudOrders = orders.map { orderWithItems ->
                    val items = orderWithItems.items.mapNotNull { item ->
                        val dish = dishesById[item.dishId] ?: return@mapNotNull null
                        CloudOrderItemSync(
                            dishName = dish.name,
                            qty = item.quantity,
                            kcal = dish.kcal
                        )
                    }

                    val totalKcal = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.kcal ?: 0) * item.quantity }
                    val totalProtein = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.proteinG ?: 0) * item.quantity }
                    val totalCarbs = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.carbsG ?: 0) * item.quantity }
                    val totalFat = orderWithItems.items.sumOf { item -> (dishesById[item.dishId]?.fatG ?: 0) * item.quantity }

                    CloudOrderSync(
                        orderId = orderWithItems.order.id,
                        orderDate = orderWithItems.order.createdAt.toString(),
                        totalKcal = totalKcal,
                        totalProtein = totalProtein,
                        totalCarbs = totalCarbs,
                        totalFat = totalFat,
                        items = items
                    )
                }

                val request = CloudSyncUploadRequest(
                    userId = "demo_user",
                    profile = cloudProfile,
                    savedProfiles = cloudSavedProfiles,
                    orders = cloudOrders
                )

                aiService.uploadSync(request)
                _uiState.update { it.copy(isSyncing = false, syncStatusMessage = "Đã đồng bộ thành công lên MySQL!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncStatusMessage = "Lỗi sao lưu: ${e.localizedMessage ?: "Không kết nối được database"}") }
            }
        }
    }

    fun downloadDataFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatusMessage = "Đang khôi phục dữ liệu từ đám mây...") }
            try {
                val state = _uiState.value
                val response = aiService.downloadSync("demo_user")
                
                 // 1. Restore Profile
                response.profile?.let { profile ->
                    val restoredProfile = SoloNutritionProfile(
                        age = profile.age,
                        sex = profile.sex?.let { s -> Sex.values().find { it.name == s } },
                        heightCm = profile.height?.toInt(),
                        weightKg = profile.weight?.toInt(),
                        goal = Goal.values().find { it.name == profile.goal },
                        activityLevel = profile.activityLevel?.let { a -> ActivityLevel.values().find { it.name == a } },
                        allergies = profile.allergies ?: ""
                    )
                    _uiState.update { it.copy(soloProfile = restoredProfile) }
                    settingsRepository.saveSoloProfileJson(gson.toJson(restoredProfile))
                }

                // 1.5 Restore Saved Profiles
                val restoredSavedProfiles = response.savedProfiles?.map { cloudSaved ->
                    SavedProfile(
                        id = cloudSaved.id,
                        name = cloudSaved.name,
                        profile = SoloNutritionProfile(
                            age = cloudSaved.profile.age,
                            sex = cloudSaved.profile.sex?.let { s -> Sex.values().find { it.name == s } },
                            heightCm = cloudSaved.profile.height?.toInt(),
                            weightKg = cloudSaved.profile.weight?.toInt(),
                            goal = Goal.values().find { it.name == cloudSaved.profile.goal },
                            activityLevel = cloudSaved.profile.activityLevel?.let { a -> ActivityLevel.values().find { it.name == a } },
                            allergies = cloudSaved.profile.allergies ?: ""
                        )
                    )
                } ?: emptyList()
                if (restoredSavedProfiles.isNotEmpty()) {
                    _uiState.update { it.copy(savedProfiles = restoredSavedProfiles) }
                    settingsRepository.saveSavedProfilesJson(gson.toJson(restoredSavedProfiles))
                }

                // 2. Restore Orders
                val dishesByName = state.dishes.associateBy { it.name.lowercase() }
                val defaultBranchId = state.branches.firstOrNull()?.id ?: "branch_1"

                for (cloudOrder in response.orders ?: emptyList()) {
                    val orderEntity = OrderEntity(
                        id = cloudOrder.orderId,
                        branchId = defaultBranchId,
                        orderMode = "SOLO",
                        nutritionMode = "WITH_PROFILE",
                        createdAt = cloudOrder.orderDate.toLongOrNull() ?: System.currentTimeMillis()
                    )

                    val itemEntities = cloudOrder.items.mapNotNull { item ->
                        val dish = dishesByName[item.dishName.lowercase()] ?: return@mapNotNull null
                        OrderItemEntity(
                            id = UUID.randomUUID().toString(),
                            orderId = cloudOrder.orderId,
                            dishId = dish.id,
                            quantity = item.qty,
                            participantId = null
                        )
                    }

                    orderRepository.saveOrder(
                        order = orderEntity,
                        items = itemEntities,
                        participants = emptyList(),
                        profile = null
                    )
                }

                loadOrderHistory()
                _uiState.update { it.copy(isSyncing = false, syncStatusMessage = "Khôi phục dữ liệu thành công từ MySQL!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncStatusMessage = "Lỗi khôi phục: ${e.localizedMessage ?: "Không kết nối được database"}") }
            }
        }
    }
}

private data class ProfileAudit(
    val profileType: String?,
    val summaryLines: List<String>,
    val parseable: Boolean,
    val rawJson: String?,
    val canExposeRaw: Boolean
)

private data class RecommendationTrigger(
    val entitlement: Entitlement,
    val nutritionMode: NutritionMode?,
    val orderMode: OrderMode?,
    val cartItems: List<CartItem>,
    val soloProfile: SoloNutritionProfile,
    val groupProfiles: Map<String, GroupNutritionProfile>,
    val participants: List<Participant>,
    val useAiRecommendations: Boolean
)
