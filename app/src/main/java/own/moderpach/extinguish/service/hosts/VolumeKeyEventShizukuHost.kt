package own.moderpach.extinguish.service.hosts

import android.content.Context
import android.util.Log
import extinguish.ipc.result.EventResult
import extinguish.shizuku_service.IEventsListener
import extinguish.shizuku_service.IEventsProvider

private const val TAG = "VolumeKeyEventShizukuHost"

class VolumeKeyEventShizukuHost(
    private val owner: Context,
    val service: IEventsProvider,
    var onKeyEvent: () -> Unit = {},
) {
    var isRegister = false
    var isAwake = false

    // 双击检测
    private var lastVolumeKeyTime = 0L
    private val DOUBLE_TAP_INTERVAL = 300L

    private val listener = object : IEventsListener.Stub() {
        override fun onEvent(event: EventResult) {
            Log.d(TAG, "get event - $event")

            // 过滤：只处理音量键按下事件
            // v0="0001" 是键盘事件，v1="0072"是音量下，"0073"是音量上，v2="00000000"是按下
            val isVolumeKey = event.v0 == "0001" &&
                    (event.v1 == "0072" || event.v1 == "0073")
            val isKeyDown = event.v2 == "00000000"

            if (!isVolumeKey || !isKeyDown) return

            if (isAwake) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastVolumeKeyTime < DOUBLE_TAP_INTERVAL) {
                    Log.d(TAG, "Double tap detected - triggering screen off")
                    onKeyEvent() // 双击触发
                    lastVolumeKeyTime = 0 // 重置
                } else {
                    Log.d(TAG, "Single tap detected - waiting for second tap")
                    lastVolumeKeyTime = currentTime
                }
            }
        }
    }

    fun register() {
        if (!isRegister) {
            isRegister = true
            service.registerListener(listener)
            isAwake = true
        }
    }

    fun unregister() {
        if (isRegister) {
            isRegister = false
            service.unregisterListener(listener)
            isAwake = false
        }
    }

    fun sleep() {
        isAwake = false
        // 重置时间，避免息屏后回来误判
        lastVolumeKeyTime = 0L
    }

    fun wake() {
        isAwake = true
    }
}