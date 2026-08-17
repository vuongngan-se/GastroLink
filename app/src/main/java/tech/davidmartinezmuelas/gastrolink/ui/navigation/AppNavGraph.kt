package tech.davidmartinezmuelas.gastrolink.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import tech.davidmartinezmuelas.gastrolink.model.NutritionMode
import tech.davidmartinezmuelas.gastrolink.model.OrderMode
import tech.davidmartinezmuelas.gastrolink.ui.AppViewModel
import tech.davidmartinezmuelas.gastrolink.ui.BuildInfoProvider
import tech.davidmartinezmuelas.gastrolink.ui.screens.BranchScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.ChatScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.CartScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.MenuScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.NutritionModeScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.OrderDetailScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.OrderHistoryScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.PlansScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.ProfileScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.SettingsScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.StartModeScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.StatsScreen
import tech.davidmartinezmuelas.gastrolink.ui.screens.SummaryScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val summaryState by viewModel.summaryUiState.collectAsState()
    val currentRoute by navController.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route
    val hiddenChatRoutes = setOf(AppRoute.CART, AppRoute.CHAT)
    val showChatFab = route != null && route !in hiddenChatRoutes
    val showPremiumDialog = remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.START_MODE,
            modifier = Modifier.fillMaxSize(),
            enterTransition    = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition     = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
            popExitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
        composable(AppRoute.START_MODE) {
            StartModeScreen(
                selectedMode = state.orderMode,
                savedProfiles = state.savedProfiles,
                hasProfileData = viewModel.hasSoloProfileData(),
                onSelectSolo = {
                    viewModel.setOrderMode(OrderMode.SOLO)
                    navController.navigate(AppRoute.NUTRITION_MODE)
                },
                onSelectGroup = {
                    viewModel.setOrderMode(OrderMode.GROUP)
                    navController.navigate(AppRoute.NUTRITION_MODE)
                },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) },
                onManageProfiles = {
                    viewModel.setOrderMode(OrderMode.SOLO)
                    navController.navigate(AppRoute.PROFILE_MANAGE)
                }
            )
        }

        composable(AppRoute.NUTRITION_MODE) {
            NutritionModeScreen(
                isPremiumEnabled = viewModel.isPremiumModeEnabled(),
                savedProfiles = state.savedProfiles,
                onChooseWithoutProfile = {
                    viewModel.chooseNutritionWithoutProfile()
                    navController.navigate(AppRoute.BRANCH)
                },
                onChooseWithProfile = {
                    val allowed = viewModel.chooseNutritionWithProfile()
                    if (allowed) {
                        if (state.orderMode == OrderMode.SOLO && viewModel.hasSoloProfileData()) {
                            navController.navigate(AppRoute.BRANCH)
                        } else {
                            navController.navigate(AppRoute.PROFILE)
                        }
                    }
                },
                onSelectProfile = { profileId ->
                    viewModel.loadSavedProfile(profileId)
                },
                onContinueWithoutProfile = {
                    viewModel.chooseNutritionWithoutProfile()
                    navController.navigate(AppRoute.BRANCH)
                },
                onOpenPlans = { navController.navigate(AppRoute.PLANS) },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
        }

        composable(AppRoute.PROFILE) {
            ProfileScreen(
                orderMode = state.orderMode,
                soloProfile = state.soloProfile,
                savedProfiles = state.savedProfiles,
                profileResetKey = state.profileResetKey,
                participants = state.participants,
                groupProfiles = state.groupProfiles,
                onSaveSoloProfile = viewModel::updateSoloProfile,
                onSaveProfileAs = viewModel::saveCurrentProfileAs,
                onLoadSavedProfile = viewModel::loadSavedProfile,
                onDeleteSavedProfile = viewModel::deleteSavedProfile,
                onAddParticipant = viewModel::addParticipant,
                onRemoveParticipant = viewModel::removeParticipant,
                onRenameParticipant = viewModel::renameParticipant,
                onUpdateGroupProfile = viewModel::updateGroupProfile,
                onContinue = { navController.navigate(AppRoute.BRANCH) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.BRANCH) {
            BranchScreen(
                isLoading = state.isLoading,
                errorMessage = state.loadErrorMessage,
                branches = state.branches,
                selectedBranchId = state.selectedBranch?.id,
                onRetry = viewModel::retryLoadCatalog,
                onSelectBranch = {
                    viewModel.selectBranch(it)
                    navController.navigate(AppRoute.MENU)
                },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
        }

        composable(AppRoute.MENU) {
            MenuScreen(
                orderMode = state.orderMode,
                selectedBranchName = state.selectedBranch?.name,
                dishes = state.dishes,
                cartItems = state.cartItems,
                participants = state.participants,
                selectedParticipantId = state.selectedParticipantId,
                onSelectParticipant = viewModel::selectParticipant,
                onAddDish = { dish, participantId -> viewModel.addDishToCart(dish, participantId) },
                onGoToCart = { navController.navigate(AppRoute.CART) },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
        }

        composable(AppRoute.CART) {
            CartScreen(
                orderMode = state.orderMode,
                items = state.cartItems,
                participants = state.participants,
                onIncrease = { dishId, participantId -> viewModel.increaseItem(dishId, participantId) },
                onDecrease = { dishId, participantId -> viewModel.decreaseItem(dishId, participantId) },
                onGoToSummary = { navController.navigate(AppRoute.SUMMARY) },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
        }

        composable(AppRoute.SUMMARY) {
            val allergies = state.soloProfile.allergies
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            val allergyDishes = if (allergies.isNotEmpty()) {
                state.cartItems.mapNotNull { item ->
                    val dishName = item.dish.name.lowercase()
                    val ingredients = item.dish.ingredients?.map { it.lowercase() } ?: emptyList()
                    val matchedAllergens = allergies.filter { allergy ->
                        dishName.contains(allergy) || ingredients.any { it.contains(allergy) }
                    }
                    if (matchedAllergens.isNotEmpty()) {
                        "${item.dish.name} (chứa ${matchedAllergens.joinToString(", ")})"
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }

            SummaryScreen(
                nutritionMode = state.nutritionMode ?: NutritionMode.WITHOUT_PROFILE,
                totals = summaryState.totals,
                recommendations = summaryState.recommendations,
                recommendationSource = summaryState.recommendationSource,
                isRecommendationLoading = summaryState.isRecommendationLoading,
                participants = state.participants,
                totalsByParticipant = summaryState.totalsByParticipant,
                isSavingOrder = state.isSavingOrder,
                totalAmount = summaryState.totalAmount,
                allergyDishes = allergyDishes,
                onConfirmOrder = { address, phoneNumber, paymentMethod ->
                    viewModel.confirmOrder(address, phoneNumber, paymentMethod)
                    navController.navigate(AppRoute.HISTORY) {
                        popUpTo(AppRoute.START_MODE) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) }
            )
        }

        composable(AppRoute.SETTINGS) {
            SettingsScreen(
                isPremiumDemoEnabled = viewModel.isPremiumModeEnabled(),
                canUseAiRecommendations = viewModel.canUseAiRecommendations(),
                nutritionMode = state.nutritionMode,
                useAiRecommendations = state.useAiRecommendations,
                buildInfo = BuildInfoProvider.current(),
                isSyncing = state.isSyncing,
                syncStatusMessage = state.syncStatusMessage,
                onUploadSync = viewModel::uploadDataToCloud,
                onDownloadSync = viewModel::downloadDataFromCloud,
                onTogglePremiumDemo = viewModel::setPremiumDemoEnabled,
                onToggleUseAiRecommendations = viewModel::setUseAiRecommendations,
                onOpenPlans = { navController.navigate(AppRoute.PLANS) },
                onExportHistory = viewModel::exportOrderHistory,
                onDeleteAllData = viewModel::wipeAllLocalData,
                onOpenHistory = {
                    viewModel.loadOrderHistory()
                    navController.navigate(AppRoute.HISTORY)
                },
                onNavigateStart = {
                    navController.navigate(AppRoute.START_MODE) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.PLANS) {
            PlansScreen(
                isPremiumEnabled = viewModel.isPremiumModeEnabled(),
                isDebugBuild = tech.davidmartinezmuelas.gastrolink.BuildConfig.DEBUG,
                onActivatePremiumDemo = { viewModel.setPremiumDemoEnabled(true) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.HISTORY) {
            OrderHistoryScreen(
                isLoading = state.isHistoryLoading,
                errorMessage = state.historyErrorMessage,
                orders = state.orderHistory,
                onRefresh = viewModel::loadOrderHistory,
                onExportHistory = viewModel::exportOrderHistory,
                onViewDetails = { orderId ->
                    navController.navigate("${AppRoute.DETAIL}/$orderId")
                },
                onOpenStats = { navController.navigate(AppRoute.STATS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoute.DETAIL_WITH_ARG,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            LaunchedEffect(orderId) {
                viewModel.clearSelectedOrderDetail()
                if (!orderId.isNullOrBlank()) {
                    viewModel.loadOrderDetails(orderId)
                }
            }

            OrderDetailScreen(
                detail = state.selectedOrderDetail,
                onDeleteOrder = { deleteId ->
                    viewModel.deleteOrder(deleteId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.STATS) {
            StatsScreen(
                stats = state.stats,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.PROFILE_MANAGE) {
            ProfileScreen(
                orderMode = OrderMode.SOLO,
                soloProfile = state.soloProfile,
                savedProfiles = state.savedProfiles,
                profileResetKey = state.profileResetKey,
                isStandalone = true,
                participants = emptyList(),
                groupProfiles = emptyMap(),
                onSaveSoloProfile = viewModel::updateSoloProfile,
                onSaveProfileAs = viewModel::saveCurrentProfileAs,
                onLoadSavedProfile = viewModel::loadSavedProfile,
                onDeleteSavedProfile = viewModel::deleteSavedProfile,
                onAddParticipant = {},
                onRemoveParticipant = {},
                onRenameParticipant = { _, _ -> },
                onUpdateGroupProfile = { _, _, _, _ -> },
                onContinue = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.CHAT) {
            ChatScreen(
                messages = state.chatMessages,
                isLoading = state.isChatLoading,
                onSendMessage = viewModel::sendChatMessage,
                onScanImage = viewModel::scanDishImage,
                onClearChat = viewModel::clearChat,
                onBack = { navController.popBackStack() }
            )
        }
    }

        AnimatedVisibility(
            visible = showChatFab,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                    slideInVertically(animationSpec = androidx.compose.animation.core.tween(200)) { it / 2 },
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                   slideOutVertically(animationSpec = androidx.compose.animation.core.tween(150)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (viewModel.isPremiumModeEnabled()) {
                        navController.navigate(AppRoute.CHAT)
                    } else {
                        showPremiumDialog.value = true
                    }
                },
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                text = { Text("Trợ lý AI") }
            )
        }

        if (showPremiumDialog.value) {
            AlertDialog(
                onDismissRequest = { showPremiumDialog.value = false },
                title = { Text(text = "Tính năng Premium") },
                text = {
                    Text(
                        text = "Trợ lý AI Pro chỉ có sẵn trong gói Premium.\n\n" +
                                "Bạn có thể kích hoạt chế độ demo miễn phí trong phần Gói dịch vụ."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPremiumDialog.value = false
                            navController.navigate(AppRoute.PLANS)
                        }
                    ) {
                        Text(text = "Xem các gói")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPremiumDialog.value = false
                        }
                    ) {
                        Text(text = "Đóng")
                    }
                }
            )
        }
    }
}
