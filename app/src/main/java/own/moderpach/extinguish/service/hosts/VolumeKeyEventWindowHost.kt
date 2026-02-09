package own.moderpach.extinguish.service.hosts

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
import androidx.core.view.isVisible
import own.moderpach.extinguish.util.ext.addFlags

private const val TAG = "VolumeKeyEventWindowHost"

class VolumeKeyEventWindowHost(
    private val owner: Context,
    var onKeyEvent: () -> Unit
) {
    val windowManager = owner.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val mLayoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.RGBA_8888
        gravity = Gravity.END or Gravity.BOTTOM
        width = 1
        height = 1
        addFlags(FLAG_NOT_TOUCH_MODAL)
        addFlags(FLAG_SPLIT_TOUCH)
    }

    // 双击检测（分别记录音量上、下键）
    private var lastVolumeDownTime = 0L
    private var lastVolumeUpTime = 0L
    private val DOUBLE_TAP_INTERVAL = 300L // 300ms内算双击

    private val listener = View.OnKeyListener { _, keyCode, event ->
        val isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP

        if (!isVolumeDown && !isVolumeUp) return@OnKeyListener true

        // 只在按下时检测
        if (event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()

            if (isVolumeDown) {
                // 检查是否是双击（音量下）
                if (currentTime - lastVolumeDownTime < DOUBLE_TAP_INTERVAL) {
                    Log.d(TAG, "Double tap VOLUME_DOWN - triggering screen off")
                    onKeyEvent() // 双击触发息屏
                    lastVolumeDownTime = 0 // 重置，防止连续触发
                } else {
                    // 第一次按下，记录时间
                    lastVolumeDownTime = currentTime
                    Log.d(TAG, "Single tap VOLUME_DOWN - waiting for second tap")
                }
            } else if (isVolumeUp) {
                // 检查是否是双击（音量上）
                if (currentTime - lastVolumeUpTime < DOUBLE_TAP_INTERVAL) {
                    Log.d(TAG, "Double tap VOLUME_UP - triggering screen off")
                    onKeyEvent()
                    lastVolumeUpTime = 0
                } else {
                    lastVolumeUpTime = currentTime
                    Log.d(TAG, "Single tap VOLUME_UP - waiting for second tap")
                }
            }
        }
        false // 返回 false，让系统继续正常处理音量键（显示音量条）
    }

    val mView = View(owner).apply {
        setOnKeyListener(listener)
    }

    fun create() {
        if (!mView.isAttachedToWindow) {
            windowManager.addView(mView, mLayoutParams)
        }
    }

    fun sleep() {
        if (mView.isAttachedToWindow) mView.isVisible = false
    }

    fun wake() {
        if (mView.isAttachedToWindow) mView.isVisible = true
    }

    fun destroy() {
        if (mView.isAttachedToWindow) {
            windowManager.removeView(mView)
        }
    }
}