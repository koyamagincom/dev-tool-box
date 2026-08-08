package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * AutomationReceiver: BroadcastReceiver lắng nghe action "com.aistudio.devtoolbox.ACTION_AUTO"
 * Cho phép AGY Assistant / Termux điều khiển và thực thi script tự động hóa qua lệnh `am broadcast`.
 * Tự động ưu tiên Shizuku process để thực thi không cần root/PC.
 */
class AutomationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_AUTO = "com.aistudio.devtoolbox.ACTION_AUTO"
        private const val TAG = "AutomationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO) return

        val action = intent.getStringExtra("action") ?: run {
            Log.e(TAG, "Thiếu tham số --es action")
            return
        }

        Log.d(TAG, "Nhận lệnh điều phối Automation từ Broadcast: $action")

        val (success, message) = when (action.lowercase()) {
            "tap" -> {
                val x = intent.getIntExtra("x", 0)
                val y = intent.getIntExtra("y", 0)
                val cmd = "input tap $x $y"
                execCmd(cmd) { AdbBridge.tap(x, y) }
            }
            "text" -> {
                val content = intent.getStringExtra("content") ?: ""
                val sanitized = content.replace(" ", "%s")
                val cmd = "input text \"$sanitized\""
                execCmd(cmd) { AdbBridge.typeText(content) }
            }
            "key" -> {
                val code = intent.getIntExtra("code", 66) // 66: KEYCODE_ENTER
                val cmd = "input keyevent $code"
                execCmd(cmd) { AdbBridge.sendKeyEvent(code) }
            }
            "open" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                if (pkg.isNotBlank()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        true to "Đã mở ứng dụng $pkg qua Package Manager"
                    } else {
                        val cmd = "monkey -p $pkg -c android.intent.category.LAUNCHER 1"
                        execCmd(cmd) { AdbBridge.openApp(pkg) }
                    }
                } else {
                    false to "Thiếu tham số pkg"
                }
            }
            "screenshot" -> {
                val path = intent.getStringExtra("path") ?: "/sdcard/DCIM/AGY/screen.png"
                File(path).parentFile?.mkdirs()
                val cmd = "screencap -p \"$path\""
                execCmd(cmd) { AdbBridge.takeScreenshot(path) }
            }
            "share_adb", "enable_adb" -> {
                val port = intent.getIntExtra("port", 5555)
                ShizukuAdbManager.enableAdbTcp5555(port)
            }
            "debloat", "uninstall" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                if (pkg.isNotBlank()) {
                    val cmd = "pm uninstall -k --user 0 $pkg"
                    execCmd(cmd) { AdbBridge.executeShell(cmd) }
                } else {
                    false to "Thiếu tham số pkg"
                }
            }
            "disable" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                if (pkg.isNotBlank()) {
                    val cmd = "pm disable-user --user 0 $pkg"
                    execCmd(cmd) { AdbBridge.executeShell(cmd) }
                } else {
                    false to "Thiếu tham số pkg"
                }
            }
            "enable" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                if (pkg.isNotBlank()) {
                    val cmd = "pm enable $pkg"
                    execCmd(cmd) { AdbBridge.executeShell(cmd) }
                } else {
                    false to "Thiếu tham số pkg"
                }
            }
            "clear" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                if (pkg.isNotBlank()) {
                    val cmd = "pm clear $pkg"
                    execCmd(cmd) { AdbBridge.executeShell(cmd) }
                } else {
                    false to "Thiếu tham số pkg"
                }
            }
            else -> {
                false to "Hành động không được hỗ trợ: $action"
            }
        }

        val statusText = if (success) {
            "✅ [Automation API] $action thành công: $message"
        } else {
            "❌ [Automation API] $action thất bại: $message"
        }

        Log.d(TAG, statusText)
        Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
    }

    private inline fun execCmd(
        cmd: String,
        fallback: () -> Pair<Boolean, String>
    ): Pair<Boolean, String> {
        return if (ShizukuAdbManager.hasShizukuPermission()) {
            ShizukuAdbManager.execCommand(cmd)
        } else {
            fallback()
        }
    }
}
