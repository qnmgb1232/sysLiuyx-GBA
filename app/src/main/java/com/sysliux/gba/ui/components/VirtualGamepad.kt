package com.sysliux.gba.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sysliux.gba.core.GbaCore
import com.sysliux.gba.ui.theme.ButtonBackground
import com.sysliux.gba.ui.theme.ButtonBorder
import com.sysliux.gba.ui.theme.ButtonPressed
import com.sysliux.gba.ui.theme.MaskBackground
import com.sysliux.gba.ui.theme.NeonCyan

data class DpadDirection(
    val offsetX: Int = 0,
    val offsetY: Int = 0
)

@Composable
fun VirtualGamepad(
    buttonAlpha: Float,
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaskBackground)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧: L 键 + 十字方向键
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // L 键
                GameButton(
                    label = "L",
                    buttonCode = GbaCore.BUTTON_L,
                    alpha = buttonAlpha,
                    onPress = onButtonDown,
                    onRelease = onButtonUp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 十字方向键
                Dpad(
                    alpha = buttonAlpha,
                    onDirectionDown = { direction ->
                        when (direction) {
                            DpadDirection(0, -1) -> onButtonDown(GbaCore.BUTTON_UP)
                            DpadDirection(0, 1) -> onButtonDown(GbaCore.BUTTON_DOWN)
                            DpadDirection(-1, 0) -> onButtonDown(GbaCore.BUTTON_LEFT)
                            DpadDirection(1, 0) -> onButtonDown(GbaCore.BUTTON_RIGHT)
                        }
                    },
                    onDirectionUp = { direction ->
                        when (direction) {
                            DpadDirection(0, -1) -> onButtonUp(GbaCore.BUTTON_UP)
                            DpadDirection(0, 1) -> onButtonUp(GbaCore.BUTTON_DOWN)
                            DpadDirection(-1, 0) -> onButtonUp(GbaCore.BUTTON_LEFT)
                            DpadDirection(1, 0) -> onButtonUp(GbaCore.BUTTON_RIGHT)
                        }
                    }
                )
            }

            // 中间: SELECT / START
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameButton(
                    label = "SELECT",
                    buttonCode = GbaCore.BUTTON_SELECT,
                    alpha = buttonAlpha,
                    onPress = onButtonDown,
                    onRelease = onButtonUp,
                    width = 60
                )
                GameButton(
                    label = "START",
                    buttonCode = GbaCore.BUTTON_START,
                    alpha = buttonAlpha,
                    onPress = onButtonDown,
                    onRelease = onButtonUp,
                    width = 60
                )
            }

            // 右侧: A/B/X/Y + R
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GameButton(
                        label = "X",
                        buttonCode = GbaCore.BUTTON_X,
                        alpha = buttonAlpha,
                        onPress = onButtonDown,
                        onRelease = onButtonUp,
                        size = 36.dp
                    )
                    GameButton(
                        label = "Y",
                        buttonCode = GbaCore.BUTTON_Y,
                        alpha = buttonAlpha,
                        onPress = onButtonDown,
                        onRelease = onButtonUp,
                        size = 36.dp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GameButton(
                        label = "A",
                        buttonCode = GbaCore.BUTTON_A,
                        alpha = buttonAlpha,
                        onPress = onButtonDown,
                        onRelease = onButtonUp,
                        size = 40.dp
                    )
                    GameButton(
                        label = "B",
                        buttonCode = GbaCore.BUTTON_B,
                        alpha = buttonAlpha,
                        onPress = onButtonDown,
                        onRelease = onButtonUp,
                        size = 40.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // R 键
                GameButton(
                    label = "R",
                    buttonCode = GbaCore.BUTTON_R,
                    alpha = buttonAlpha,
                    onPress = onButtonDown,
                    onRelease = onButtonUp
                )
            }
        }
    }
}

@Composable
private fun GameButton(
    label: String,
    buttonCode: Int,
    alpha: Float,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    width: Dp? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(width ?: size)
            .size(size)
            .alpha(alpha)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) ButtonPressed else ButtonBackground)
            .border(
                width = 2.dp,
                color = if (isPressed) NeonCyan else ButtonBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(buttonCode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress(buttonCode)
                        tryAwaitRelease()
                        isPressed = false
                        onRelease(buttonCode)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun Dpad(
    alpha: Float,
    onDirectionDown: (DpadDirection) -> Unit,
    onDirectionUp: (DpadDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSizePx = 36

    Box(
        modifier = modifier
            .size((buttonSizePx * 3).dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // 使用 Column + Row 布局四个方向键
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 上
            GameButton(
                label = "▲",
                buttonCode = -1,
                alpha = 1f,
                onPress = { onDirectionDown(DpadDirection(0, -1)) },
                onRelease = { onDirectionUp(DpadDirection(0, -1)) },
                size = buttonSizePx / 2
            )

            // 下排：左 | 中 | 右
            Row(
                horizontalArrangement = Arrangement.spacedBy((buttonSizePx * 2).dp - buttonSizePx.dp)
            ) {
                // 左
                GameButton(
                    label = "◀",
                    buttonCode = -1,
                    alpha = 1f,
                    onPress = { onDirectionDown(DpadDirection(-1, 0)) },
                    onRelease = { onDirectionUp(DpadDirection(-1, 0)) },
                    size = buttonSizePx / 2
                )

                // 中心占位
                Spacer(modifier = Modifier.size((buttonSizePx / 2).dp))

                // 右
                GameButton(
                    label = "▶",
                    buttonCode = -1,
                    alpha = 1f,
                    onPress = { onDirectionDown(DpadDirection(1, 0)) },
                    onRelease = { onDirectionUp(DpadDirection(1, 0)) },
                    size = buttonSizePx / 2
                )
            }

            // 下
            GameButton(
                label = "▼",
                buttonCode = -1,
                alpha = 1f,
                onPress = { onDirectionDown(DpadDirection(0, 1)) },
                onRelease = { onDirectionUp(DpadDirection(0, 1)) },
                size = buttonSizePx / 2
            )
        }
    }
}
