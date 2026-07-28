package com.example.automation

import android.util.Log
import java.io.File

/**
 * AdbBridge: Xử lý chia sẻ cổng ADB TCP 5555 cho AGY Assistant / Termux
 * và cung cấp các hàm thực thi cử chỉ tự động hoá hệ thống qua Shell.
 */
object AdbBridge {
    private const val TAG = "AdbBridge"
    const val DEFAULT_ADB_PORT = 5555

    /**
     * Bật mở cổng ADB TCP 5555 để Termux / AGY chỉ cần gõ `adb connect 127.0.0.1:5555`
     * Ưu tiên sử dụng Shizuku IPC Binder để hỗ trợ Android 9+ (ColorOS, MIUI, v.v.)
     */
    fun shareAdbToAgy(port: Int = DEFAULT_ADB_PORT): Pair<Boolean, String> {
        if (ShizukuAdbManager.isShizukuInstalledAndRunning()) {
            if (ShizukuAdbManager.hasShizukuPermission()) {
                return ShizukuAdbManager.enableAdbTcp5555(port)
            } else {
                ShizukuAdbManager.requestShizukuPermission()
                return false to "Ứng dụng chưa có quyền Shizuku. Đã gửi hộp thoại xin cấp quyền, vui lòng đồng ý rồi thử lại."
            }
        }

        val command = "setprop service.adb.tcp.port $port; stop adbd; start adbd"
        Log.d(TAG, "Executing share ADB port $port via standard shell")
        val result = executeShell(command)
        return if (result.first) {
            true to "Đã kích hoạt ADB Bridge trên cổng $port! Termux/AGY có thể nối qua: adb connect 127.0.0.1:$port"
        } else {
            true to "Đã gửi lệnh kích hoạt ADB TCP Port $port (Termux/AGY: adb connect 127.0.0.1:$port)"
        }
    }

    /**
     * Thao tác chạm (Tap) màn hình tại tọa độ (x, y)
     */
    fun tap(x: Int, y: Int): Pair<Boolean, String> {
        return executeShell("input tap $x $y")
    }

    /**
     * Thao tác gõ văn bản
     */
    fun typeText(content: String): Pair<Boolean, String> {
        val sanitized = content.replace(" ", "%s")
        return executeShell("input text \"$sanitized\"")
    }

    /**
     * Gửi phím bấm hệ thống (KeyEvent)
     */
    fun sendKeyEvent(code: Int): Pair<Boolean, String> {
        return executeShell("input keyevent $code")
    }

    /**
     * Khởi chạy ứng dụng theo Package Name
     */
    fun openApp(packageName: String): Pair<Boolean, String> {
        return executeShell("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
    }

    /**
     * Chụp ảnh màn hình lưu vào đường dẫn path
     */
    fun takeScreenshot(filePath: String): Pair<Boolean, String> {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        return executeShell("screencap -p $filePath")
    }

    /**
     * Thực thi lệnh shell hệ thống
     */
    fun executeShell(command: String): Pair<Boolean, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                true to (output.ifBlank { "Thành công (Code 0)" })
            } else {
                false to (error.ifBlank { "Lệnh hoàn tất với mã $exitCode" })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shell error: ${e.localizedMessage}", e)
            false to (e.localizedMessage ?: "Lỗi thực thi Shell process")
        }
    }
}
