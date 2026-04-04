package com.sysliux.gba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sysliux.gba.ui.theme.DarkPurpleBlack
import com.sysliux.gba.ui.theme.ElectricPurple
import com.sysliux.gba.ui.theme.NeonCyan
import com.sysliux.gba.ui.theme.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurpleBlack)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkPurpleBlack
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 音量设置
            SettingsSection(title = "音频") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "音量",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(100.dp)
                        )
                        Slider(
                            value = settings.volume.toFloat(),
                            onValueChange = { viewModel.setVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 9,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPink,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = ElectricPurple
                            )
                        )
                        Text(
                            text = "${settings.volume}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 显示设置
            SettingsSection(title = "显示") {
                // FPS 显示
                SettingsSwitch(
                    title = "显示帧率",
                    subtitle = "在游戏画面上显示 FPS 计数器",
                    checked = settings.showFps,
                    onCheckedChange = viewModel::setShowFps
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 按键透明度
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "按键透明度",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(100.dp)
                        )
                        Slider(
                            value = settings.buttonAlpha,
                            onValueChange = viewModel::setButtonAlpha,
                            valueRange = 0.1f..0.8f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPink,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = ElectricPurple
                            )
                        )
                        Text(
                            text = "${(settings.buttonAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 模拟器设置
            SettingsSection(title = "模拟器") {
                // 默认倍速
                Column {
                    Text(
                        text = "默认倍速",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0.5f, 1.0f, 2.0f, 3.0f, 4.0f).forEach { speed ->
                            SpeedChip(
                                text = "${speed}x",
                                selected = settings.defaultSpeed == speed,
                                onClick = { viewModel.setDefaultSpeed(speed) },
                                modifier = Modifier.weight(1f)
                            )
                            if (speed != 4.0f) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 关于
            SettingsSection(title = "关于") {
                Text(
                    text = "sysLiux-GBA",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Text(
                    text = "基于 mGBA 核心的 GBA 模拟器",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "版本 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NeonPink,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonPink
            )
        )
    }
}

@Composable
private fun SpeedChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) NeonPink else ElectricPurple.copy(alpha = 0.3f)
    val textColor = if (selected) DarkPurpleBlack else NeonCyan

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}
