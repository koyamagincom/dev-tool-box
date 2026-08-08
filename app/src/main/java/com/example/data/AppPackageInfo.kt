package com.example.data

import android.graphics.drawable.Drawable

enum class PackageFilterType {
    ALL,        // Tất cả ứng dụng
    SYSTEM,     // Ứng dụng hệ thống
    USER,       // Ứng dụng người dùng
    BLOATWARE   // Đề xuất gỡ Bloatware rác
}

data class AppPackageInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val icon: Drawable? = null,
    val isKnownBloatware: Boolean = false,
    val bloatwareDescription: String = "",
    val installTime: Long = 0L,
    val targetSdkVersion: Int = 0,
    val sourceDir: String = "",
    val apkSizeMb: Double = 0.0
)

object KnownBloatwareList {
    val BLOATWARE_PACKAGES = mapOf(
        "com.facebook.appmanager" to "Facebook System Service (Chạy ngầm & tốn pin)",
        "com.facebook.system" to "Facebook App Installer (Cài đặt ngầm)",
        "com.facebook.services" to "Facebook Services (Thu thập dữ liệu)",
        "com.heytap.browser" to "Trình duyệt mặc định OPPO/Realme",
        "com.heytap.cloud" to "Dịch vụ đám mây OPPO Cloud",
        "com.heytap.datamigration" to "Chuyển đổi dữ liệu OPPO",
        "com.heytap.usercenter" to "Tài khoản HeyTap OPPO",
        "com.oppo.market" to "Chợ ứng dụng OPPO App Market",
        "com.coloros.phonemanager" to "Quản lý thiết bị ColorOS",
        "com.coloros.gamespace" to "ColorOS Game Space",
        "com.miui.analytics" to "Dịch vụ phân tích & quảng cáo MIUI",
        "com.xiaomi.mipicks" to "Chợ ứng dụng Xiaomi GetApps",
        "com.miui.msa.global" to "Dịch vụ quảng cáo hệ thống MSA Xiaomi",
        "com.miui.hybrid" to "Xiaomi Quick Apps (Ứng dụng rác)",
        "com.miui.cleanmaster" to "Trình dọn dẹp MIUI Cleaner",
        "com.samsung.android.app.spage" to "Samsung Daily / Bixby Home",
        "com.samsung.android.bixby.agent" to "Trợ lý ảo Bixby Voice Agent",
        "com.sec.android.app.samsungapps" to "Cửa hàng Galaxy Store",
        "com.samsung.android.game.gamehome" to "Samsung Game Launcher",
        "com.google.android.videos" to "Google TV / Phim ảnh",
        "com.google.android.apps.tachyon" to "Google Duo / Meet",
        "com.google.android.music" to "Google Play Music",
        "com.android.bookmarkprovider" to "Trình cung cấp Bookmark nhà mạng",
        "com.android.partnerbrowsercustomizations" to "Tùy biến trình duyệt đối tác",
        "com.netflix.partner.activation" to "Dịch vụ kích hoạt Netflix trả phí",
        "com.ebay.carrier" to "Ứng dụng eBay preinstall nhà mạng",
        "com.amazon.mShop.android.shopping" to "Ứng dụng mua sắm Amazon preinstall"
    )

    fun isBloatware(packageName: String): Boolean {
        if (BLOATWARE_PACKAGES.containsKey(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("analytics") ||
                lower.contains("telemetry") ||
                lower.contains("msa.global") ||
                (lower.contains("facebook") && !lower.equals("com.facebook.katana"))
    }

    fun getDescription(packageName: String): String {
        return BLOATWARE_PACKAGES[packageName] ?: "Ứng dụng hệ thống/quảng cáo preinstall có thể gỡ an toàn"
    }
}
