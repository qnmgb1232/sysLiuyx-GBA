package com.sysliux.gba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sysliux.gba.data.local.entity.SaveEntity
import com.sysliux.gba.ui.theme.DarkPurpleBlack
import com.sysliux.gba.ui.theme.ElectricPurple
import com.sysliux.gba.ui.theme.NeonCyan
import com.sysliux.gba.ui.theme.NeonPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveManagementScreen(
    gamePath: String,
    gameName: String,
    onBack: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    viewModel: SaveManagementViewModel = hiltViewModel()
) {
    val saves by viewModel.getSavesForGame(gamePath).collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf<SaveEntity?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurpleBlack)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "存档管理",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = gameName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "新建存档",
                        tint = NeonPink
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkPurpleBlack
            )
        )

        if (saves.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无存档",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右上角按钮创建新存档",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(saves, key = { it.id }) { save ->
                    SaveSlotCard(
                        save = save,
                        onLoad = { onLoad(save.slot) },
                        onDelete = { showDeleteDialog = save }
                    )
                }
            }
        }
    }

    // 保存对话框
    if (showSaveDialog) {
        SaveSlotSelectionDialog(
            existingSlots = saves.map { it.slot },
            onSelectSlot = { slot ->
                onSave(slot)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    // 删除确认对话框
    showDeleteDialog?.let { save ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除存档", color = NeonCyan) },
            text = { Text("确定要删除存档 ${save.name} 吗？", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSave(save)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = NeonPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消", color = NeonCyan)
                }
            },
            containerColor = DarkPurpleBlack
        )
    }
}

@Composable
private fun SaveSlotCard(
    save: SaveEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad),
        colors = CardDefaults.cardColors(containerColor = DarkPurpleBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElectricPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (save.thumbnailPath != null) {
                    Text(
                        text = "预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                } else {
                    Text(
                        text = "S${save.slot}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = save.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateTime(save.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = NeonPink.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SaveSlotSelectionDialog(
    existingSlots: List<Int>,
    onSelectSlot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val allSlots = (1..10).toList()
    val availableSlots = allSlots.filter { it !in existingSlots }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择存档槽位", color = NeonCyan) },
        text = {
            Column {
                if (availableSlots.isEmpty()) {
                    Text(
                        text = "所有槽位都已使用，请先删除旧存档",
                        color = Color.White
                    )
                } else {
                    availableSlots.forEach { slot ->
                        TextButton(
                            onClick = { onSelectSlot(slot) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "槽位 $slot (空)",
                                color = NeonPink,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (existingSlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已使用的槽位: ${existingSlots.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = NeonCyan)
            }
        },
        containerColor = DarkPurpleBlack
    )
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
