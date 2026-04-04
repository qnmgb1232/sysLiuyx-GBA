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