package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automation.ShizukuAdbManager
import com.example.data.AppPackageInfo
import com.example.data.PackageFilterType
import com.example.ui.DevToolboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageManagerDebloatScreen(
    viewModel: DevToolboxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allPackages by viewModel.installedPackages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingPackages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.packageSearchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.packageFilterType.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackageNames.collectAsStateWithLifecycle()
    val isBatchMode by viewModel.isBatchDebloatMode.collectAsStateWithLifecycle()
    val statusText by viewModel.packageOperationStatus.collectAsStateWithLifecycle()

    var selectedAppForDetail by remember { mutableStateOf<AppPackageInfo?>(null) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    var singleDebloatTarget by remember { mutableStateOf<AppPackageInfo?>(null) }
    var showSystemPrivilegeGuideSheet by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restorePackageInput by remember { mutableStateOf("") }
    var isHeaderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (allPackages.isEmpty()) {
            viewModel.loadInstalledPackages()
        }
    }

    // Calculated Statistics
    val totalCount = allPackages.size
    val systemCount = remember(allPackages) { allPackages.count { it.isSystemApp } }
    val userCount = remember(allPackages) { allPackages.count { !it.isSystemApp } }
    val bloatwareCount = remember(allPackages) { allPackages.count { it.isKnownBloatware } }

    // Filtered List
    val filteredPackages = remember(allPackages, searchQuery, filterType) {
        allPackages.filter { pkg ->
            val matchesFilter = when (filterType) {
                PackageFilterType.ALL -> true
                PackageFilterType.SYSTEM -> pkg.isSystemApp
                PackageFilterType.USER -> !pkg.isSystemApp
                PackageFilterType.BLOATWARE -> pkg.isKnownBloatware
            }
            val matchesQuery = searchQuery.isEmpty() ||
                    pkg.appName.contains(searchQuery, ignoreCase = true) ||
                    pkg.packageName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Clean Modern Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input with Action Icons
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setPackageSearchQuery(it) },
                    placeholder = { Text("Tìm ứng dụng hoặc package (ví dụ: com.oppo)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setPackageSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(
                                onClick = { viewModel.loadInstalledPackages() },
                                modifier = Modifier.testTag("refresh_packages_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_package_input")
                )

                // Scrollable Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterType == PackageFilterType.ALL,
                            onClick = { viewModel.setPackageFilterType(PackageFilterType.ALL) },
                            label = { Text("Tất cả ($totalCount)") },
                            leadingIcon = if (filterType == PackageFilterType.ALL) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == PackageFilterType.BLOATWARE,
                            onClick = { viewModel.setPackageFilterType(PackageFilterType.BLOATWARE) },
                            label = { Text("🔥 Bloatware rác ($bloatwareCount)") },
                            leadingIcon = if (filterType == PackageFilterType.BLOATWARE) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == PackageFilterType.SYSTEM,
                            onClick = { viewModel.setPackageFilterType(PackageFilterType.SYSTEM) },
                            label = { Text("System ($systemCount)") },
                            leadingIcon = if (filterType == PackageFilterType.SYSTEM) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == PackageFilterType.USER,
                            onClick = { viewModel.setPackageFilterType(PackageFilterType.USER) },
                            label = { Text("User ($userCount)") },
                            leadingIcon = if (filterType == PackageFilterType.USER) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Compact Quick Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { viewModel.toggleBatchDebloatMode() },
                            label = { Text(if (isBatchMode) "Thoát hàng loạt" else "Chọn hàng loạt") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isBatchMode) Icons.Default.Checklist else Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isBatchMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        AssistChip(
                            onClick = { showRestoreDialog = true },
                            label = { Text("Khôi phục") },
                            leadingIcon = { Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        AssistChip(
                            onClick = { showSystemPrivilegeGuideSheet = true },
                            label = { Text("Quyền ADB") },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Text(
                        text = "${filteredPackages.size} ứng dụng",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Batch Banner Actions
        AnimatedVisibility(visible = isBatchMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Đã chọn: ${selectedPackages.size} gói",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (statusText.isNotEmpty()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { viewModel.selectAllPackages(filteredPackages) }) {
                            Text("Chọn hết")
                        }
                        TextButton(onClick = { viewModel.clearPackageSelection() }) {
                            Text("Bỏ chọn")
                        }
                        Button(
                            onClick = { showBatchConfirmDialog = true },
                            enabled = selectedPackages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Gỡ Bỏ (${selectedPackages.size})")
                        }
                    }
                }
            }
        }

        // Package List View
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Đang quét danh sách gói ứng dụng...")
                }
            }
        } else if (filteredPackages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không tìm thấy ứng dụng phù hợp",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPackages, key = { it.packageName }) { appInfo ->
                    AppPackageItemRow(
                        appInfo = appInfo,
                        isBatchMode = isBatchMode,
                        isSelected = selectedPackages.contains(appInfo.packageName),
                        onToggleSelect = { viewModel.toggleSelectPackage(appInfo.packageName) },
                        onDebloatClick = { singleDebloatTarget = appInfo },
                        onDisableClick = { viewModel.disablePackage(appInfo.packageName) },
                        onEnableClick = { viewModel.enablePackage(appInfo.packageName) },
                        onClearDataClick = { viewModel.clearPackageData(appInfo.packageName) },
                        onInfoClick = { selectedAppForDetail = appInfo }
                    )
                }
            }
        }
    }

    // Detail BottomSheet
    selectedAppForDetail?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForDetail = null }
        ) {
            AppDetailBottomSheetContent(
                appInfo = app,
                onDismiss = { selectedAppForDetail = null },
                onDebloat = {
                    singleDebloatTarget = app
                    selectedAppForDetail = null
                },
                onDisable = {
                    viewModel.disablePackage(app.packageName)
                    selectedAppForDetail = null
                },
                onEnable = {
                    viewModel.enablePackage(app.packageName)
                    selectedAppForDetail = null
                },
                onClearData = {
                    viewModel.clearPackageData(app.packageName)
                    selectedAppForDetail = null
                }
            )
        }
    }

    // Batch Confirm Dialog
    if (showBatchConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatchConfirmDialog = false },
            title = { Text("⚠️ XÁC NHẬN GỠ HÀNG LOẠT") },
            text = {
                Text(
                    "Bạn có chắc muốn gỡ bỏ / debloat ${selectedPackages.size} ứng dụng đã chọn khỏi người dùng hiện tại (pm uninstall --user 0)?\n\nLưu ý: Có thể khôi phục lại bất kỳ lúc nào bằng lệnh 'pm install-existing <package>'."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchConfirmDialog = false
                        viewModel.executeBatchDebloat(useUser0Uninstall = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đồng Ý Gỡ Bỏ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBatchConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Single App Debloat Confirm Dialog
    singleDebloatTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { singleDebloatTarget = null },
            title = { Text("🧹 Gỡ Ứng Dụng: ${app.appName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Package: ${app.packageName}")
                    if (app.isKnownBloatware) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Cảnh báo Bloatware: ${app.bloatwareDescription}",
                                color = Color(0xFFE65100),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Text(
                        if (app.isSystemApp)
                            "Ứng dụng hệ thống sẽ được gỡ bỏ cho người dùng hiện tại (pm uninstall -k --user 0)."
                        else
                            "Ứng dụng người dùng sẽ được yêu cầu gỡ cài đặt tiêu chuẩn."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = singleDebloatTarget
                        singleDebloatTarget = null
                        if (target != null) {
                            if (target.isSystemApp) {
                                viewModel.debloatSystemPackage(target.packageName)
                            } else {
                                viewModel.uninstallUserPackage(target.packageName)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác Nhận Gỡ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { singleDebloatTarget = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // System Privilege Guide Modal Bottom Sheet
    if (showSystemPrivilegeGuideSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSystemPrivilegeGuideSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Phương Pháp Truy Cập Hệ Thống & Debloat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Để gỡ bỏ ứng dụng hệ thống (System Apps) hoặc Bloatware của nhà sản xuất (OPPO, Xiaomi, Samsung, Facebook...), Android yêu cầu đặc quyền Shell / ADB / Root. Ứng dụng hỗ trợ 4 phương pháp chính:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Method 1: Built-in App ADB Engine
                    MethodGuideCard(
                        title = "1. Trình Thực Thi ADB Tích Hợp App (Khuyên dùng)",
                        statusText = "⚡ Đã sẵn sàng hoạt động trong App",
                        isSuccess = true,
                        description = "Ứng dụng gọi trực tiếp tiến trình ADB Shell nội bộ (`pm uninstall -k --user 0`, `pm disable-user`, `pm clear`). Bạn có thể gỡ ứng dụng hệ thống rác ngay trên app mà không cần cài thêm Shizuku hay kết nối máy tính.",
                        actionLabel = null,
                        onAction = null
                    )

                    // Method 2: Root (su)
                    MethodGuideCard(
                        title = "2. Quyền Root SU (Magisk / KernelSU / APatch)",
                        statusText = "⚡ Tự động nhận diện khi thực thi",
                        isSuccess = true,
                        description = "Nếu thiết bị đã được Root, ứng dụng sẽ tự động chuyển sang thực thi lệnh `su -c pm uninstall ...` để gỡ hoàn toàn ứng dụng hệ thống mà không bị hạn chế.",
                        actionLabel = null,
                        onAction = null
                    )

                    // Method 3: Wireless ADB Local
                    MethodGuideCard(
                        title = "3. Kết nối ADB không dây Local (127.0.0.1:5555)",
                        statusText = "🌐 Cổng TCP 5555",
                        isSuccess = true,
                        description = "Mở cổng ADB Bridge TCP 5555 ở tab 'Wireless ADB' để gửi câu lệnh từ ứng dụng Termux hoặc LADB trực tiếp trên điện thoại.",
                        actionLabel = null,
                        onAction = null
                    )

                    // Method 4: Shizuku Service (Tuỳ chọn)
                    MethodGuideCard(
                        title = "4. Dịch Vụ Shizuku (Dự phòng tuỳ chọn)",
                        statusText = if (ShizukuAdbManager.hasShizukuPermission()) "✅ Đã kết nối Shizuku Binder" else "⚠️ Chưa cấp quyền Shizuku",
                        isSuccess = ShizukuAdbManager.hasShizukuPermission(),
                        description = "Sử dụng Shizuku làm dịch vụ ADB Binder dự phòng nếu hệ thống chặn tiến trình Shell chuẩn.",
                        actionLabel = if (!ShizukuAdbManager.hasShizukuPermission()) "Cấp Quyền Shizuku" else null,
                        onAction = {
                            showSystemPrivilegeGuideSheet = false
                            ShizukuAdbManager.requestShizukuPermission()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showSystemPrivilegeGuideSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đã Hiểu")
                }
            }
        }
    }

    // Restore System Package Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("🔄 Khôi Phục Ứng Dụng Hệ Thống") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập hoặc dán tên gói ứng dụng (Package Name) đã bị gỡ để khôi phục lại cho người dùng hiện tại (lệnh pm install-existing):",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = restorePackageInput,
                        onValueChange = { restorePackageInput = it.trim() },
                        placeholder = { Text("Ví dụ: com.facebook.system") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Ví dụ gói phổ biến:\n• com.facebook.system\n• com.google.android.videos\n• com.miui.analytics\n• com.heytap.browser",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPkg = restorePackageInput
                        showRestoreDialog = false
                        restorePackageInput = ""
                        if (targetPkg.isNotBlank()) {
                            viewModel.restoreSystemPackage(targetPkg)
                        }
                    },
                    enabled = restorePackageInput.isNotBlank()
                ) {
                    Text("Thực Thi Khôi Phục")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun MethodGuideCard(
    title: String,
    statusText: String,
    isSuccess: Boolean,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text(actionLabel, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AppPackageItemRow(
    appInfo: AppPackageInfo,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDebloatClick: () -> Unit,
    onDisableClick: () -> Unit,
    onEnableClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else if (appInfo.isKnownBloatware) Color(0xFFFFF8E1)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isBatchMode) onToggleSelect() else onInfoClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // App Icon
            AppIconImage(
                drawable = appInfo.icon,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appInfo.appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (appInfo.isKnownBloatware) {
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BLOATWARE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (appInfo.isSystemApp) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SYSTEM",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "USER",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!appInfo.isEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DISABLED",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (appInfo.isKnownBloatware) {
                    Text(
                        text = "💡 ${appInfo.bloatwareDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "v${appInfo.versionName} • Target SDK ${appInfo.targetSdkVersion}${if (appInfo.apkSizeMb > 0) " • ${"%.1f".format(appInfo.apkSizeMb)}MB" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Menu actions
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Gỡ Bỏ / Debloat", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDebloatClick()
                        }
                    )

                    if (appInfo.isEnabled) {
                        DropdownMenuItem(
                            text = { Text("Vô Hiệu Hoá") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDisableClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Bật Lại") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEnableClick()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Xoá Cache & Dữ Liệu") },
                        leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClearDataClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Sao chép Package Name") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            clipboardManager.setText(AnnotatedString(appInfo.packageName))
                            Toast.makeText(context, "Đã sao chép: ${appInfo.packageName}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Mở Cài Đặt Ứng Dụng") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${appInfo.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppDetailBottomSheetContent(
    appInfo: AppPackageInfo,
    onDismiss: () -> Unit,
    onDebloat: () -> Unit,
    onDisable: () -> Unit,
    onEnable: () -> Unit,
    onClearData: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                drawable = appInfo.icon,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailItem(label = "Phiên bản", value = "${appInfo.versionName} (${appInfo.versionCode})")
            DetailItem(label = "Loại ứng dụng", value = if (appInfo.isSystemApp) "Ứng dụng Hệ Thống (System App)" else "Ứng dụng Người Dùng (User App)")
            DetailItem(label = "Trạng thái", value = if (appInfo.isEnabled) "Đang kích hoạt (Enabled)" else "Đã vô hiệu hoá (Disabled)")
            DetailItem(label = "Target SDK", value = "Android ${appInfo.targetSdkVersion}")
            if (appInfo.sourceDir.isNotEmpty()) {
                DetailItem(label = "Đường dẫn APK", value = appInfo.sourceDir)
            }
            if (appInfo.isKnownBloatware) {
                DetailItem(label = "Mô tả Bloatware", value = appInfo.bloatwareDescription)
            }
        }

        // ADB Command helper snippet
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val adbCmd = "adb shell pm uninstall -k --user 0 ${appInfo.packageName}"
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "💻 Lệnh ADB Debloat thủ công:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = adbCmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(adbCmd))
                            Toast.makeText(context, "Đã copy lệnh ADB!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDebloat,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gỡ Bỏ")
            }

            OutlinedButton(
                onClick = { if (appInfo.isEnabled) onDisable() else onEnable() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (appInfo.isEnabled) Icons.Default.Block else Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (appInfo.isEnabled) "Tắt" else "Bật")
            }

            OutlinedButton(
                onClick = onClearData,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Xoá Data")
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            try {
                if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                    drawable.bitmap.asImageBitmap()
                } else {
                    val targetWidth = if (drawable.intrinsicWidth in 1..256) drawable.intrinsicWidth else 96
                    val targetHeight = if (drawable.intrinsicHeight in 1..256) drawable.intrinsicHeight else 96
                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier
            )
        } else {
            Icon(Icons.Default.Android, contentDescription = null, modifier = modifier, tint = MaterialTheme.colorScheme.primary)
        }
    } else {
        Icon(Icons.Default.Android, contentDescription = null, modifier = modifier, tint = MaterialTheme.colorScheme.primary)
    }
}
