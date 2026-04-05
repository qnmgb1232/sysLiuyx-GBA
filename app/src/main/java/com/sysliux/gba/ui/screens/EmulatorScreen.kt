package com.sysliux.gba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sysliux.gba.core.GbaCoreManager
import com.sysliux.gba.data.preferences.UserPreferences
import com.sysliux.gba.ui.components.VirtualGamepad
import com.sysliux.gba.ui.theme.DarkPurpleBlack
import com.sysliux.gba.ui.theme.NeonCyan
import com.sysliux.gba.ui.theme.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    romPath: String,
    onBack: () -> Unit,
    viewModel: GbaCoreManager = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }
    var gameSurfaceSize by remember { mutableStateOf(IntSize.Zero) }

    val speeds = listOf(0.5f, 1.0f, 2.0f, 3.0f, 4.0f)

    // 加载游戏
    DisposableEffect(romPath) {
        viewModel.loadGame(romPath)
        viewModel.start()
        onDispose {
            viewModel.pause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurpleBlack)
            .statusBarsPadding()
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    text = state.currentGame?.let { it.substringAfterLast("/") } ?: "Loading...",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.pause()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
            },
            actions = {
                // FPS 显示
                Text(
                    text = "${state.fps} FPS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // 速度选择
                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = NeonPink
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        speeds.forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${speed}x",
                                        color = if (speed == state.currentSpeed) NeonCyan else Color.White
                                    )
                                },
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // 播放/暂停
                IconButton(onClick = {
                    if (state.isRunning) viewModel.pause() else viewModel.start()
                }) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isRunning) "Pause" else "Play",
                        tint = NeonPink
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkPurpleBlack
            )
        )

        // 游戏画面区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            GameSurface(
                viewModel = viewModel,
                onSizeChanged = { gameSurfaceSize = it }
            )
        }

        // 虚拟按键
        VirtualGamepad(
            buttonAlpha = 0.5f,
            onButtonDown = viewModel::keyDown,
            onButtonUp = viewModel::keyUp
        )
    }
}

@Composable
private fun GameSurface(
    viewModel: GbaCoreManager,
    onSizeChanged: (IntSize) -> Unit
) {
    val gameWidth = 240
    val gameHeight = 160

    // 计算缩放比例
    var maxScale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .onSizeChanged { containerSize ->
                val scaleX = containerSize.width.toFloat() / gameWidth
                val scaleY = containerSize.height.toFloat() / gameHeight
                maxScale = minOf(scaleX, scaleY)
                onSizeChanged(containerSize)
            },
        contentAlignment = Alignment.Center
    ) {
        // 帧缓冲渲染
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameBuffer = viewModel.getFrameBuffer()
            if (frameBuffer.isNotEmpty()) {
                val width = gameWidth
                val height = gameHeight
                val pixels = IntArray(width * height)

                // 转换 RGBA bytes 到 Int pixels
                for (i in 0 until width * height) {
                    val offset = i * 4
                    if (offset + 3 < frameBuffer.size) {
                        val r = frameBuffer[offset].toInt() and 0xFF
                        val g = frameBuffer[offset + 1].toInt() and 0xFF
                        val b = frameBuffer[offset + 2].toInt() and 0xFF
                        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }

                // 绘制像素（整数缩放）
                val scale = maxScale.toInt().coerceAtLeast(1)

                pixels.forEachIndexed { index, color ->
                    val x = (index % width) * scale
                    val y = (index / width) * scale
                    drawRect(
                        color = Color(color),
                        topLeft = Offset(x.toFloat(), y.toFloat()),
                        size = androidx.compose.ui.geometry.Size(scale.toFloat(), scale.toFloat())
                    )
                }
            }
        }
    }
}
