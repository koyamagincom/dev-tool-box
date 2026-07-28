package com.example.automation

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * ShizukuAdbManager: Quản lý kết nối Binder IPC qua Shizuku
 * Cho phép kích hoạt ADB TCP Port 5555 & thực thi lệnh Tự động hóa
 * không cần PC hay Root trên Android 9+ (ColorOS, MIUI, OneUI, v.v.)
 */
object ShizukuAdbManager {

    private const val TAG = "ShizukuAdbManager"
    const val SHIZUKU_REQUEST_CODE = 5555

    /**
     * Kiểm tra Shizuku Service có đang chạy trên thiết bị không
     */
    fun isShizukuInstalledAndRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            Log.e(TAG, "Shizuku binder ping failed: ${e.message}")
            false
        }
    }

    /**
     * Kiểm tra ứng dụng đã có quyền Shizuku hay chưa
     */
    fun hasShizukuPermission(): Boolean {
        if (!isShizukuInstalledAndRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            Log.e(TAG, "Check Shizuku permission error: ${e.message}")
            false
        }
    }

    /**
     * Yêu cầu cấp quyền Shizuku cho ứng dụng
     */
    fun requestShizukuPermission(requestCode: Int = SHIZUKU_REQUEST_CODE) {
        if (!isShizukuInstalledAndRunning()) {
            Log.w(TAG, "Shizuku chưa chạy, không thể xin quyền.")
            return
        }
        try {
            if (!hasShizukuPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Request Shizuku permission error: ${e.message}")
        }
    }

    /**
     * Khởi tạo Process thông qua Shizuku Binder IPC bằng reflection
     */
    private fun createShizukuProcess(cmdArray: Array<String>): Process? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, cmdArray, null, null) as? Process
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to invoke Shizuku.newProcess via reflection: ${e.message}")
            null
        }
    }

    /**
     * Khởi chạy lệnh Shell với đặc quyền Shizuku ADB
     */
    fun execCommand(command: String): Pair<Boolean, String> {
        if (!isShizukuInstalledAndRunning()) {
            return false to "Dịch vụ Shizuku chưa chạy trên thiết bị. Hãy mở app Shizuku để kích hoạt."
        }
        if (!hasShizukuPermission()) {
            requestShizukuPermission()
            return false to "Chưa có quyền Shizuku! Đã gửi yêu cầu xin cấp quyền."
        }

        return try {
            val cmdArray = arrayOf("sh", "-c", command)
            val process = createShizukuProcess(cmdArray)
                ?: return false to "Không thể tạo Shizuku process."

            val output = process.inputStream.bufferedReader().readText().trim()
            val error = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                true to (output.ifEmpty { "Thành công (Code 0)" })
            } else {
                false to (error.ifEmpty { "Thất bại với exit code $exitCode" })
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Lỗi thực thi lệnh qua Shizuku: ${e.message}", e)
            false to (e.localizedMessage ?: "Lỗi Shizuku process execution")
        }
    }

    /**
     * Bật mở cổng ADB TCP 5555 qua Shizuku mà không cần PC hay Root
     */
    fun enableAdbTcp5555(port: Int = 5555): Pair<Boolean, String> {
        val command = "setprop service.adb.tcp.port $port; stop adbd; start adbd"
        Log.d(TAG, "Executing setprop via Shizuku: $command")
        
        val (success, message) = execCommand(command)
        return if (success) {
            true to "Đã mở cổng ADB TCP $port qua Shizuku thành công! Termux/AGY có thể kết nối: adb connect 127.0.0.1:$port"
        } else {
            // Fallback sang AdbBridge bình thường nếu Shizuku thất bại
            val fallback = AdbBridge.shareAdbToAgy(port)
            fallback.first to "Shizuku: $message\nStandard Fallback: ${fallback.second}"
        }
    }
}
