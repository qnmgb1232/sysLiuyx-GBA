# sysLiux-GBA 模拟器实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建完整的 Android GBA 模拟器，基于 mGBA 核心，采用 Jetpack Compose 和霓虹复古 UI

**Architecture:** 客户端 Android 应用，mGBA 通过 JNI 集成，Compose UI 层与模拟器核心分离

**Tech Stack:** Kotlin, Jetpack Compose, mGBA (JNI), Room, DataStore, Hilt, OpenGL ES 3.0+

---

## 项目结构

```
app/
├── src/main/
│   ├── java/com/sysliux/gba/
│   │   ├── SysLiuxGbaApp.kt           # Application 类，Hilt 入口
│   │   ├── MainActivity.kt            # 单 Activity
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt          # 霓虹配色
│   │   │   │   ├── Type.kt           # 字体
│   │   │   │   └── Theme.kt          # Compose Theme
│   │   │   ├── screens/
│   │   │   │   ├── GameLibraryScreen.kt   # 游戏库
│   │   │   │   ├── EmulatorScreen.kt      # 模拟器
│   │   │   │   └── SettingsScreen.kt      # 设置
│   │   │   ├── components/
│   │   │   │   ├── VirtualGamepad.kt      # 虚拟按键
│   │   │   │   └── GameCard.kt            # 游戏卡片
│   │   │   └── navigation/
│   │   │       └── AppNavigation.kt       # 导航
│   │   ├── core/
│   │   │   ├── GbaCore.kt            # JNI 封装
│   │   │   └── GbaCoreManager.kt     # 核心生命周期管理
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── GameDao.kt        # ROM 元数据 DAO
│   │   │   │   ├── SaveDao.kt        # 即时存档 DAO
│   │   │   │   └── AppDatabase.kt    # Room 数据库
│   │   │   ├── entity/
│   │   │   │   ├── GameEntity.kt     # 游戏实体
│   │   │   │   └── SaveEntity.kt     # 存档实体
│   │   │   └── preferences/
│   │   │       └── UserPreferences.kt # DataStore
│   │   └── di/
│   │       └── AppModule.kt          # Hilt 模块
│   └── jni/                          # NDK
│       ├── Android.mk
│       ├── Application.mk
│       └── mgba/                     # mGBA submodule
├── build.gradle.kts                  # 根构建
└── settings.gradle.kts
```

---

## 阶段一：项目初始化

### Task 1: 创建 Gradle Wrapper 和基本项目结构

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (根)
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`, `gradlew.bat`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 创建 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "sysLiux-GBA"
include(":app")
```

- [ ] **Step 2: 创建根 build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

- [ ] **Step 3: 创建 gradle-wrapper.properties**

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
```

- [ ] **Step 4: 创建 app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sysliux.gba"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

- [ ] **Step 5: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="true" />
    <uses-feature android:name="android.hardware.gamepad" android:required="false" />
    <application
        android:name=".SysLiuxGbaApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="sysLiux-GBA"
        android:theme="@style/Theme.sysLiuxGBA">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.sysLiuxGBA">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: 创建 Application 类**

```kotlin
package com.sysliux.gba

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SysLiuxGbaApp : Application()
```

- [ ] **Step 7: 创建 MainActivity**

```kotlin
package com.sysliux.gba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sysliux.gba.ui.navigation.AppNavigation
import com.sysliux.gba.ui.theme.SysLiuxGbaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SysLiuxGbaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git init
git add .
git commit -m "feat: initial project setup with Gradle wrapper"
```

---

### Task 2: 创建霓虹复古主题

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/ui/theme/Color.kt`
- Create: `app/src/main/java/com/sysliux/gba/ui/theme/Type.kt`
- Create: `app/src/main/java/com/sysliux/gba/ui/theme/Theme.kt`

- [ ] **Step 1: 创建 Color.kt**

```kotlin
package com.sysliux.gba.ui.theme

import androidx.compose.ui.graphics.Color

// 主背景
val DarkPurpleBlack = Color(0xFF0D0D1A)

// 霓虹色
val NeonCyan = Color(0xFF00FFFF)
val NeonPink = Color(0xFFFF00FF)
val ElectricPurple = Color(0xFF9D00FF)

// 文字
val LightPurpleWhite = Color(0xFFE0E0FF)

// 遮罩
val MaskBackground = Color(0xB3000000) // 70% 黑色

// 按键
val ButtonBackground = Color(0x80000000) // 50% 黑色
val ButtonBorder = NeonPink
val ButtonPressed = NeonCyan
```

- [ ] **Step 2: 创建 Type.kt**

```kotlin
package com.sysliux.gba.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
```

- [ ] **Step 3: 创建 Theme.kt**

```kotlin
package com.sysliux.gba.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeonRetroColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    tertiary = ElectricPurple,
    background = DarkPurpleBlack,
    surface = DarkPurpleBlack,
    onPrimary = DarkPurpleBlack,
    onSecondary = DarkPurpleBlack,
    onTertiary = DarkPurpleBlack,
    onBackground = LightPurpleWhite,
    onSurface = LightPurpleWhite,
)

@Composable
fun SysLiuxGbaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NeonRetroColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkPurpleBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add neon retro theme"
```

---

### Task 3: 创建导航框架

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: 创建 AppNavigation.kt**

```kotlin
package com.sysliux.gba.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sysliux.gba.ui.screens.EmulatorScreen
import com.sysliux.gba.ui.screens.GameLibraryScreen
import com.sysliux.gba.ui.screens.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val LIBRARY = "/"
    const val PLAY = "/play/{romPath}"
    const val SETTINGS = "/settings"

    fun play(romPath: String): String {
        val encoded = URLEncoder.encode(romPath, StandardCharsets.UTF_8.toString())
        return "/play/$encoded"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        composable(Routes.LIBRARY) {
            GameLibraryScreen(
                onNavigateToGame = { romPath ->
                    navController.navigate(Routes.play(romPath))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(
            route = Routes.PLAY,
            arguments = listOf(navArgument("romPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("romPath") ?: ""
            val romPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            EmulatorScreen(
                romPath = romPath,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: 创建占位 Screen 文件**

```kotlin
package com.sysliux.gba.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun GameLibraryScreen(
    onNavigateToGame: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Game Library - TODO")
    }
}

@Composable
fun EmulatorScreen(
    romPath: String,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Emulator - $romPath - TODO")
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings - TODO")
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add navigation framework"
```

---

## 阶段二：数据层

### Task 4: Room 数据库和 Entity

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/data/local/entity/GameEntity.kt`
- Create: `app/src/main/java/com/sysliux/gba/data/local/entity/SaveEntity.kt`
- Create: `app/src/main/java/com/sysliux/gba/data/local/GameDao.kt`
- Create: `app/src/main/java/com/sysliux/gba/data/local/SaveDao.kt`
- Create: `app/src/main/java/com/sysliux/gba/data/local/AppDatabase.kt`
- Modify: `app/build.gradle.kts` (添加 Room 依赖已在上阶段完成)

- [ ] **Step 1: 创建 GameEntity.kt**

```kotlin
package com.sysliux.gba.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val lastPlayedAt: Long? = null,
    val totalPlayTime: Long = 0,
    val coverPath: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 SaveEntity.kt**

```kotlin
package com.sysliux.gba.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saves",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["path"],
            childColumns = ["gamePath"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gamePath")]
)
data class SaveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gamePath: String,
    val slot: Int, // 1-10
    val name: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val playTimeAtSave: Long = 0
)
```

- [ ] **Step 3: 创建 GameDao.kt**

```kotlin
package com.sysliux.gba.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sysliux.gba.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayedAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%'")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE path = :path")
    suspend fun getGameByPath(path: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("UPDATE games SET lastPlayedAt = :timestamp, totalPlayTime = totalPlayTime + :additionalTime WHERE path = :path")
    suspend fun updatePlayTime(path: String, timestamp: Long, additionalTime: Long)
}
```

- [ ] **Step 4: 创建 SaveDao.kt**

```kotlin
package com.sysliux.gba.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sysliux.gba.data.local.entity.SaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM saves WHERE gamePath = :gamePath ORDER BY slot")
    fun getSavesForGame(gamePath: String): Flow<List<SaveEntity>>

    @Query("SELECT * FROM saves WHERE gamePath = :gamePath AND slot = :slot")
    suspend fun getSave(gamePath: String, slot: Int): SaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: SaveEntity): Long

    @Delete
    suspend fun deleteSave(save: SaveEntity)

    @Query("DELETE FROM saves WHERE gamePath = :gamePath AND slot = :slot")
    suspend fun deleteSaveSlot(gamePath: String, slot: Int)
}
```

- [ ] **Step 5: 创建 AppDatabase.kt**

```kotlin
package com.sysliux.gba.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sysliux.gba.data.local.entity.GameEntity
import com.sysliux.gba.data.local.entity.SaveEntity

@Database(
    entities = [GameEntity::class, SaveEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveDao(): SaveDao
}
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add Room database entities and DAOs"
```

---

### Task 5: DataStore 用户偏好设置

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/data/preferences/UserPreferences.kt`
- Create: `app/src/main/java/com/sysliux/gba/di/AppModule.kt`

- [ ] **Step 1: 创建 UserPreferences.kt**

```kotlin
package com.sysliux.gba.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val volume: Int = 100,
    val showFps: Boolean = false,
    val buttonAlpha: Float = 0.5f,
    val defaultSpeed: Float = 1.0f,
    val lastRomPath: String? = null
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val VOLUME = intPreferencesKey("volume")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val BUTTON_ALPHA = floatPreferencesKey("button_alpha")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val LAST_ROM_PATH = stringPreferencesKey("last_rom_path")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            volume = prefs[Keys.VOLUME] ?: 100,
            showFps = prefs[Keys.SHOW_FPS] ?: false,
            buttonAlpha = prefs[Keys.BUTTON_ALPHA] ?: 0.5f,
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            lastRomPath = prefs[Keys.LAST_ROM_PATH]
        )
    }

    suspend fun setVolume(volume: Int) {
        context.dataStore.edit { it[Keys.VOLUME] = volume }
    }

    suspend fun setShowFps(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FPS] = show }
    }

    suspend fun setButtonAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.BUTTON_ALPHA] = alpha }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed }
    }

    suspend fun setLastRomPath(path: String) {
        context.dataStore.edit { it[Keys.LAST_ROM_PATH] = path }
    }
}
```

- [ ] **Step 2: 创建 AppModule.kt (Hilt)**

```kotlin
package com.sysliux.gba.di

import android.content.Context
import androidx.room.Room
import com.sysliux.gba.data.local.AppDatabase
import com.sysliux.gba.data.local.GameDao
import com.sysliux.gba.data.local.SaveDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sysliux-gba.db"
        ).build()
    }

    @Provides
    fun provideGameDao(database: AppDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideSaveDao(database: AppDatabase): SaveDao {
        return database.saveDao()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add DataStore preferences and Hilt module"
```

---

## 阶段三：模拟器核心 JNI 绑定

### Task 6: mGBA JNI 绑定层

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/core/GbaCore.kt`
- Create: `app/src/main/java/com/sysliux/gba/core/GbaCoreManager.kt`
- Create: `app/src/main/jni/Android.mk` (模板)
- Create: `app/src/main/jni/Application.mk` (模板)

**Note:** mGBA 源码将通过 submodule 引入，此处先创建 JNI 绑定接口

- [ ] **Step 1: 创建 GbaCore.kt (JNI 封装)**

```kotlin
package com.sysliux.gba.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * GBA 模拟器核心 JNI 接口
 * 实际实现依赖于 mGBA 源码编译的 native library
 */
@Singleton
class GbaCore @Inject constructor() {

    /**
     * 加载 ROM 文件
     * @param path ROM 文件路径
     * @return 是否加载成功
     */
    external fun loadRom(path: String): Boolean

    /**
     * 卸载当前 ROM
     */
    external fun unloadRom()

    /**
     * 开始模拟
     */
    external fun start()

    /**
     * 暂停模拟
     */
    external fun pause()

    /**
     * 重置模拟器状态
     */
    external fun reset()

    /**
     * 单步执行一帧
     */
    external fun stepFrame()

    /**
     * 设置模拟速度倍率
     * @param speed 速度倍率 (0.5, 1.0, 2.0, 3.0, 4.0)
     */
    external fun setSpeed(speed: Float)

    /**
     * 设置帧跳过
     * @param frames 跳过的帧数 (0-3)
     */
    external fun setFrameSkip(frames: Int)

    /**
     * 获取当前帧率
     * @return FPS
     */
    external fun getFps(): Int

    /**
     * 获取当前 CPU 周期数
     * @return cycles
     */
    external fun getCycles(): Long

    /**
     * 按下虚拟按键
     * @param button 按键代码 (A=0, B=1, SELECT=2, START=3, RIGHT=4, LEFT=5, UP=6, DOWN=7, R=8, L=9)
     */
    external fun keyDown(button: Int)

    /**
     * 释放虚拟按键
     * @param button 按键代码
     */
    external fun keyUp(button: Int)

    /**
     * 保存即时存档到指定槽位
     * @param slot 槽位编号 (0-9)
     * @return 是否保存成功
     */
    external fun saveState(slot: Int): Boolean

    /**
     * 从指定槽位加载即时存档
     * @param slot 槽位编号 (0-9)
     * @return 是否加载成功
     */
    external fun loadState(slot: Int): Boolean

    /**
     * 获取电池存档数据
     * @return 存档数据 ByteArray
     */
    external fun getBatterySave(): ByteArray

    /**
     * 加载电池存档数据
     * @param data 存档数据
     */
    external fun loadBatterySave(data: ByteArray)

    /**
     * 获取帧缓冲数据 (用于渲染)
     * @return RGBA 格式像素数据 (240 * 160 * 4 bytes)
     */
    external fun getFrameBuffer(): ByteArray

    /**
     * 获取音频采样数据
     * @return 音频采样数据
     */
    external fun getAudioSamples(): ShortArray

    /**
     * 设置音量
     * @param volume 音量 0-100
     */
    external fun setVolume(volume: Int)

    /**
     * 检查 ROM 是否已加载
     * @return 是否已加载
     */
    external fun isRomLoaded(): Boolean

    /**
     * 检查模拟器是否正在运行
     * @return 是否运行中
     */
    external fun isRunning(): Boolean

    companion object {
        const val BUTTON_A = 0
        const val BUTTON_B = 1
        const val BUTTON_SELECT = 2
        const val BUTTON_START = 3
        const val BUTTON_RIGHT = 4
        const val BUTTON_LEFT = 5
        const val BUTTON_UP = 6
        const val BUTTON_DOWN = 7
        const val BUTTON_R = 8
        const val BUTTON_L = 9
        const val BUTTON_X = 10
        const val BUTTON_Y = 11
    }
}
```

- [ ] **Step 2: 创建 GbaCoreManager.kt (生命周期管理)**

```kotlin
package com.sysliux.gba.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.GameDao
import com.sysliux.gba.data.local.entity.GameEntity
import com.sysliux.gba.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class EmulatorState(
    val isLoaded: Boolean = false,
    val isRunning: Boolean = false,
    val currentSpeed: Float = 1.0f,
    val fps: Int = 0,
    val currentGame: String? = null
)

@HiltViewModel
class GbaCoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val gbaCore = GbaCore()

    private val _state = MutableStateFlow(EmulatorState())
    val state: StateFlow<EmulatorState> = _state.asStateFlow()

    private var renderJob: Job? = null

    init {
        System.loadLibrary("gba") // 加载 native library
    }

    fun loadGame(romPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val loaded = gbaCore.loadRom(romPath)
                if (loaded) {
                    // 更新数据库
                    val game = GameEntity(
                        path = romPath,
                        name = File(romPath).nameWithoutExtension
                    )
                    gameDao.insertGame(game)
                    userPreferences.setLastRomPath(romPath)

                    _state.value = _state.value.copy(
                        isLoaded = true,
                        currentGame = romPath
                    )
                }
            }
        }
    }

    fun start() {
        gbaCore.start()
        _state.value = _state.value.copy(isRunning = true)
        startRenderLoop()
    }

    fun pause() {
        renderJob?.cancel()
        gbaCore.pause()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        pause()
        gbaCore.reset()
    }

    fun setSpeed(speed: Float) {
        gbaCore.setSpeed(speed)
        _state.value = _state.value.copy(currentSpeed = speed)
    }

    fun setVolume(volume: Int) {
        gbaCore.setVolume(volume)
        viewModelScope.launch {
            userPreferences.setVolume(volume)
        }
    }

    fun keyDown(button: Int) {
        gbaCore.keyDown(button)
    }

    fun keyUp(button: Int) {
        gbaCore.keyUp(button)
    }

    fun saveState(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            gbaCore.saveState(slot)
        }
    }

    fun loadState(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            gbaCore.loadState(slot)
        }
    }

    fun getFrameBuffer(): ByteArray {
        return gbaCore.getFrameBuffer()
    }

    fun getAudioSamples(): ShortArray {
        return gbaCore.getAudioSamples()
    }

    fun unload() {
        pause()
        gbaCore.unloadRom()
        _state.value = EmulatorState()
    }

    private fun startRenderLoop() {
        renderJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                gbaCore.stepFrame()
                _state.value = _state.value.copy(fps = gbaCore.getFps())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unload()
    }
}
```

- [ ] **Step 3: 创建 jni/Android.mk (模板)**

```makefile
# sysLiux-GBA native Android.mk
# mGBA 集成构建配置

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := gba
LOCAL_SRC_FILES := mgba/libgba.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/mgba
include $(PREBUILT_STATIC_LIBRARY)
```

- [ ] **Step 4: 创建 jni/Application.mk (模板)**

```makefile
# Application.mk for sysLiux-GBA
APP_PLATFORM := android-26
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_STL := c++_static
APP_CFLAGS := -O3 -ffast-math
```

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add mGBA JNI binding layer"
```

---

## 阶段四：游戏库界面

### Task 7: 游戏库 Screen 实现

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/ui/components/GameCard.kt`
- Modify: `app/src/main/java/com/sysliux/gba/ui/screens/GameLibraryScreen.kt`

- [ ] **Step 1: 创建 GameCard.kt**

```kotlin
package com.sysliux.gba.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sysliux.gba.data.local.entity.GameEntity
import com.sysliux.gba.ui.theme.DarkPurpleBlack
import com.sysliux.gba.ui.theme.ElectricPurple
import com.sysliux.gba.ui.theme.NeonCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameCard(
    game: GameEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkPurpleBlack
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 游戏封面/占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(ElectricPurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonCyan
                )
            }

            // 游戏信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                game.lastPlayedAt?.let { timestamp ->
                    Text(
                        text = "上次: ${formatDate(timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

- [ ] **Step 2: 实现 GameLibraryScreen**

```kotlin
package com.sysliux.gba.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.sysliux.gba.ui.components.GameCard
import com.sysliux.gba.ui.theme.DarkPurpleBlack
import com.sysliux.gba.ui.theme.NeonCyan
import com.sysliux.gba.ui.theme.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    onNavigateToGame: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: GameLibraryViewModel = hiltViewModel()
) {
    val games by viewModel.games.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addGame(it) }
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
                    text = "sysLiux-GBA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkPurpleBlack
            )
        )

        // 游戏列表
        if (games.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "还没有游戏",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("application/octet-stream", "application/x-zip-compressed", "application/zip", "*/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导入 ROM")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(games, key = { it.path }) { game ->
                    GameCard(
                        game = game,
                        onClick = { onNavigateToGame(game.path) }
                    )
                }

                // 添加游戏按钮
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                color = NeonCyan.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { filePicker.launch(arrayOf("application/octet-stream", "application/x-zip-compressed", "*/*")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Game",
                                tint = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 创建 GameLibraryViewModel**

```kotlin
package com.sysliux.gba.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.GameDao
import com.sysliux.gba.data.local.entity.GameEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) : ViewModel() {

    val games: StateFlow<List<GameEntity>> = gameDao.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGame(uri: Uri) {
        viewModelScope.launch {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = uri.lastPathSegment ?: "game_${System.currentTimeMillis()}.gba"
            val destFile = File(context.filesDir, "roms/$fileName")

            destFile.parentFile?.mkdirs()
            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val game = GameEntity(
                path = destFile.absolutePath,
                name = destFile.nameWithoutExtension
            )
            gameDao.insertGame(game)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: implement Game Library screen"
```

---

## 阶段五：模拟器界面

### Task 8: 虚拟按键组件

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/ui/components/VirtualGamepad.kt`

- [ ] **Step 1: 创建 VirtualGamepad.kt**

```kotlin
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sysliux.gba.core.GbaCore
import com.sysliux.gba.ui.theme.ButtonBackground
import com.sysliux.gba.ui.theme.ButtonBorder
import com.sysliux.gba.ui.theme.ButtonPressed
import com.sysliux.gba.ui.theme.MaskBackground
import com.sysliux.gba.ui.theme.NeonCyan
import kotlin.math.roundToInt

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
                    width = 60.dp
                )
                GameButton(
                    label = "START",
                    buttonCode = GbaCore.BUTTON_START,
                    alpha = buttonAlpha,
                    onPress = onButtonDown,
                    onRelease = onButtonUp,
                    width = 60.dp
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
    size: Int = 44,
    width: Int? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(width ?: size.dp)
            .size(size.dp)
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
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add VirtualGamepad component"
```

---

### Task 9: 模拟器 Screen 实现

**Files:**
- Modify: `app/src/main/java/com/sysliux/gba/ui/screens/EmulatorScreen.kt`

- [ ] **Step 1: 实现 EmulatorScreen.kt**

```kotlin
package com.sysliux.gba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sysliux.gba.core.GbaCoreManager
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
    val settings by viewModel.settings.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }
    var gameSurfaceSize by remember { mutableStateOf(IntSize.Zero) }

    val speeds = listOf(0.5f, 1.0f, 2.0f, 3.0f, 4.0f)

    // 加载游戏
    DisposableEffect(romPath) {
        viewModel.loadGame(romPath)
        viewModel.start()
        onDispose {
            viewModel.unload()
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
                if (settings.showFps) {
                    Text(
                        text = "${state.fps} FPS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

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
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
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
            buttonAlpha = settings.buttonAlpha,
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
    val density = LocalDensity.current
    val gameWidth = 240
    val gameHeight = 160

    // 计算缩放比例
    var maxScale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(gameWidth.toFloat() / gameHeight)
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
                val scaledWidth = width * scale
                val scaledHeight = height * scale

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
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: implement Emulator screen with game surface"
```

---

## 阶段六：设置界面

### Task 10: 设置 Screen 实现

**Files:**
- Modify: `app/src/main/java/com/sysliux/gba/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: 实现 SettingsScreen.kt**

```kotlin
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
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
```

- [ ] **Step 2: 创建 SettingsViewModel**

```kotlin
package com.sysliux.gba.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.preferences.UserPreferences
import com.sysliux.gba.data.preferences.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val settings: StateFlow<UserSettings> = userPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setVolume(volume: Int) {
        viewModelScope.launch {
            userPreferences.setVolume(volume)
        }
    }

    fun setShowFps(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowFps(show)
        }
    }

    fun setButtonAlpha(alpha: Float) {
        viewModelScope.launch {
            userPreferences.setButtonAlpha(alpha)
        }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferences.setDefaultSpeed(speed)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: implement Settings screen"
```

---

## 阶段七：收尾

### Task 11: 添加资源文件和最终配置

**Files:**
- Create: `app/src/main/res/values/styles.xml`
- Create: `app/src/main/res/mipmap-*/ic_launcher.png` (占位)
- Modify: `app/build.gradle.kts` (添加资源路径)

- [ ] **Step 1: 创建 styles.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.sysLiuxGBA" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/dark_purple_black</item>
        <item name="android:statusBarColor">@color/dark_purple_black</item>
        <item name="android:navigationBarColor">@color/dark_purple_black</item>
    </style>
</resources>
```

- [ ] **Step 2: 创建 colors.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="dark_purple_black">#FF0D0D1A</color>
    <color name="neon_cyan">#FF00FFFF</color>
    <color name="neon_pink">#FFFF00FF</color>
    <color name="electric_purple">#FF9D00FF</color>
</resources>
```

- [ ] **Step 3: 创建占位 Launcher 图标**

```bash
mkdir -p app/src/main/res/mipmap-hdpi app/src/main/res/mipmap-mdpi app/src/main/res/mipmap-xhdpi app/src/main/res/mipmap-xxhdpi app/src/main/res/mipmap-xxxhdpi
# 使用 ImageMagick 创建简单的占位图标
convert -size 48x48 xc:#9D00FF app/src/main/res/mipmap-mdpi/ic_launcher.png
convert -size 72x72 xc:#9D00FF app/src/main/res/mipmap-hdpi/ic_launcher.png
convert -size 96x96 xc:#9D00FF app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert -size 144x144 xc:#9D00FF app/src/main/res/mipmap-xxhdpi/ic_launcher.png
convert -size 192x192 xc:#9D00FF app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add resources and final configuration"
```

---

### Task 12: 存档管理界面

**Files:**
- Create: `app/src/main/java/com/sysliux/gba/ui/screens/SaveManagementScreen.kt`
- Create: `app/src/main/java/com/sysliux/gba/ui/screens/SaveManagementViewModel.kt`

- [ ] **Step 1: 创建 SaveManagementScreen.kt**

```kotlin
package com.sysliux.gba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
                    // 加载缩略图
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
```

- [ ] **Step 2: 创建 SaveManagementViewModel.kt**

```kotlin
package com.sysliux.gba.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.SaveDao
import com.sysliux.gba.data.local.entity.SaveEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaveManagementViewModel @Inject constructor(
    private val saveDao: SaveDao
) : ViewModel() {

    fun getSavesForGame(gamePath: String): Flow<List<SaveEntity>> {
        return saveDao.getSavesForGame(gamePath)
    }

    fun deleteSave(save: SaveEntity) {
        viewModelScope.launch {
            saveDao.deleteSave(save)
        }
    }

    suspend fun createSave(gamePath: String, slot: Int): SaveEntity {
        val save = SaveEntity(
            gamePath = gamePath,
            slot = slot,
            name = "存档 $slot",
            createdAt = System.currentTimeMillis()
        )
        saveDao.insertSave(save)
        return save
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add Save Management screen"
```

---

## 执行顺序

1. **Task 1**: 项目初始化 - 创建 Gradle Wrapper 和基本结构
2. **Task 2**: 霓虹复古主题
3. **Task 3**: 导航框架
4. **Task 4**: Room 数据库和 Entity
5. **Task 5**: DataStore 和 Hilt 模块
6. **Task 6**: mGBA JNI 绑定层 (核心接口)
7. **Task 7**: 游戏库 Screen
8. **Task 8**: 虚拟按键组件
9. **Task 9**: 模拟器 Screen
10. **Task 10**: 设置 Screen
11. **Task 11**: 资源文件和配置
12. **Task 12**: 存档管理界面

---

**下一步：mGBA 源码集成**

完成以上任务后，需要进行 mGBA 源码集成：
1. 初始化 mGBA git submodule
2. 配置 mGBA Android.mk 编译
3. 实现 JNI 绑定中的 native 方法
4. 配置 CMake/NDK 构建

这将在后续阶段完成。
