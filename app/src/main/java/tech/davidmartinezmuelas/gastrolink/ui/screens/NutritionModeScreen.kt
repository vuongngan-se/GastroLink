package tech.davidmartinezmuelas.gastrolink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.davidmartinezmuelas.gastrolink.model.SavedProfile
import tech.davidmartinezmuelas.gastrolink.ui.theme.GastroSpacing
import tech.davidmartinezmuelas.gastrolink.ui.theme.NutritionCalories
import tech.davidmartinezmuelas.gastrolink.ui.theme.PillShape

@Composable
fun NutritionModeScreen(
    isPremiumEnabled: Boolean,
    savedProfiles: List<SavedProfile> = emptyList(),
    onChooseWithoutProfile: () -> Unit,
    onChooseWithProfile: () -> Unit,
    onSelectProfile: (String) -> Unit = {},
    onContinueWithoutProfile: () -> Unit,
    onOpenPlans: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val showPremiumDialog = remember { mutableStateOf(false) }
    val showProfilePicker = remember { mutableStateOf(false) }
    var selectedProfileId = remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            GastroTopBar(
                title = "Chế độ dinh dưỡng",
                onBack = onBack,
                onSettings = onOpenSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(GastroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GastroSpacing.md)
        ) {
            Text(
                text = "Bạn muốn đặt món với mức độ chi tiết dinh dưỡng nào?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)) {
                            Text(
                                text = "Không có thông tin hồ sơ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hiển thị kcal và macro cho từng món và tổng cộng trong giỏ hàng.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onChooseWithoutProfile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Tiếp tục")
                    }
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumEnabled) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(GastroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isPremiumEnabled) {
                                NutritionCalories
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(GastroSpacing.xs)) {
                            Text(
                                text = "Có thông tin hồ sơ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nhận các đề xuất cá nhân hóa dựa trên mục tiêu và hồ sơ của bạn.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isPremiumEnabled) {
                                Surface(
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "Yêu cầu Premium",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(
                                            horizontal = GastroSpacing.sm,
                                            vertical = GastroSpacing.xs
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!isPremiumEnabled) {
                                showPremiumDialog.value = true
                            } else if (savedProfiles.isNotEmpty()) {
                                selectedProfileId.value = null
                                showProfilePicker.value = true
                            } else {
                                onChooseWithProfile()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Tiếp tục")
                    }
                }
            }
        }
    }


    if (showPremiumDialog.value) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog.value = false },
            title = { Text(text = "Tính năng Premium") },
            text = {
                Text(
                    text = "Hồ sơ dinh dưỡng và các đề xuất cá nhân hóa chỉ có sẵn trong gói Premium.\n\n" +
                        "Bạn có thể kích hoạt chế độ demo miễn phí trong phần Gói dịch vụ."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPremiumDialog.value = false
                        onOpenPlans()
                    }
                ) {
                    Text(text = "Xem các gói")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPremiumDialog.value = false
                        onContinueWithoutProfile()
                    }
                ) {
                    Text(text = "Tiếp tục không cần hồ sơ")
                }
            }
        )
    }


    if (showProfilePicker.value) {
        AlertDialog(
            onDismissRequest = { showProfilePicker.value = false },
            title = { Text("Sử dụng hồ sơ nào?") },
            text = {
                LazyColumn {
                    items(savedProfiles, key = { it.id }) { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProfileId.value = profile.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            RadioButton(
                                selected = selectedProfileId.value == profile.id,
                                onClick = { selectedProfileId.value = profile.id }
                            )
                            Column {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                val summary = listOfNotNull(
                                    profile.profile.age?.let { "$it tuổi" },
                                    profile.profile.weightKg?.let { "$it kg" }
                                ).joinToString(" · ")
                                if (summary.isNotEmpty()) {
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProfileId.value = "new" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GastroSpacing.sm)
                        ) {
                            RadioButton(
                                selected = selectedProfileId.value == "new",
                                onClick = { selectedProfileId.value = "new" }
                            )
                            Text(
                                text = "Hồ sơ mới",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = selectedProfileId.value
                        showProfilePicker.value = false
                        if (id == "new" || id == null) {
                            onChooseWithProfile()
                        } else {
                            onSelectProfile(id)
                            onChooseWithProfile()
                        }
                    },
                    enabled = selectedProfileId.value != null
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfilePicker.value = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
