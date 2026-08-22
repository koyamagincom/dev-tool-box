package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.asImageBitmap
import com.example.automation.AdbBridgeDaemonManager
import com.example.ui.DevToolboxViewModel
import com.example.ui.MacroAction
import com.example.ui.MacroType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlAutomationScreen(
    viewModel: DevToolboxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubMode by remember { mutableIntStateOf(0) } // 0: AI Agent, 1: Touch Macro, 2: Daemon Server

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mode Selector Header Tabs
        TabRow(
            selectedTabIndex = selectedSubMode,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
        ) {
            Tab(
                selected = selectedSubMode == 0,
                onClick = { selectedSubMode = 0 },
                text = { Text("🤖 AI Agent", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Agent", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_ai_agent")
            )
            Tab(
                selected = selectedSubMode == 1,
                onClick = { selectedSubMode = 1 },
                text = { Text("⚡ Macro", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.TouchApp, contentDescription = "Macro", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_macro")
            )
            Tab(
                selected = selectedSubMode == 2,
                onClick = { 
                    selectedSubMode = 2 
                    viewModel.checkDaemonStatus()
                },
                text = { Text("🌐 Daemon Server", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Dns, contentDescription = "Daemon", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_daemon")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubMode) {
                0 -> AiAutomationTab(viewModel)
                1 -> MacroAutoClickerTab(viewModel)
                2 -> AdbBridgeDaemonTab(viewModel)
            }
        }
    }
}

@Composable
fun AiAutomationTab(viewModel: DevToolboxViewModel) {
    val prompt by viewModel.aiAutomationPrompt.collectAsStateWithLifecycle()
    val result by viewModel.aiAutomationResult.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingAiAutomation.collectAsStateWithLifecycle()

    val samplePrompts = listOf(
        "Tự động nhấp điểm (540, 1200) 5 lần mỗi 1 giây",
        "Mở Cài Đặt Wifi, chờ 2s rồi cuộn xuống 2 lần",
        "Tự động vuốt từ dưới lên để xem video ngắn TikTok",
        "Tạo lệnh ADB cấp quyền camera và vị trí cho ứng dụng"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Trợ lý AI Tự Động Hoá (AI Device Agent)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nhập yêu cầu bằng tiếng Việt, AI sẽ lập kịch bản và chuỗi lệnh điều khiển tự động.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { viewModel.setAiAutomationPrompt(it) },
                        label = { Text("Yêu cầu tự động hoá") },
                        placeholder = { Text("Ví dụ: Tự động nhấp điểm (500, 1000) 10 lần...") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (prompt.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setAiAutomationPrompt("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xoá")
                                }
                            }
                        }
                    )

                    Text(
                        text = "Gợi ý kịch bản mẫu:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(samplePrompts) { item ->
                            FilterChip(
                                selected = prompt == item,
                                onClick = { viewModel.setAiAutomationPrompt(item) },
                                label = { Text(item, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.generateAiAutomationPlan() },
                        enabled = !isGenerating && prompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("generate_ai_plan_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang phân tích kịch bản AI...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tạo Kịch Bản Tự Động Hoá AI")
                        }
                    }
                }
            }
        }

        if (result.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📋 Kịch Bản AI Được Phân Tích",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    viewModel.runMacroSequence()
                                }
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Thực thi", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SelectionContainer {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.runMacroSequence()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chạy Kịch Bản Trực Tiếp Trên Điện Thoại")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroAutoClickerTab(viewModel: DevToolboxViewModel) {
    val context = LocalContext.current
    val actions by viewModel.macroActions.collectAsStateWithLifecycle()
    val isRunning by viewModel.isMacroRunning.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentMacroStep.collectAsStateWithLifecycle()
    val repeatCount by viewModel.macroRepeatCount.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Clicker & Cử Chỉ Tự Động",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Chạy độc lập trên Android không cần PC thông qua Accessibility hoặc Shell/ADB.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = repeatCount.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { count -> viewModel.setMacroRepeatCount(count) }
                            },
                            label = { Text("Số lần lặp") },
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("repeat_count_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!isRunning) {
                            Button(
                                onClick = { viewModel.runMacroSequence() },
                                enabled = actions.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(56.dp).testTag("run_macro_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bắt đầu")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopMacroSequence() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(56.dp).testTag("stop_macro_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dừng lại")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Danh sách bước Macro (${actions.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.clearMacroActions() }) {
                        Text("Xoá hết", color = MaterialTheme.colorScheme.error)
                    }
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm bước")
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Chưa có bước Macro nào.",
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Nhấn 'Thêm bước' để tạo chạm, vuốt hoặc gõ chữ tự động.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(actions) { index, action ->
                val isCurrentExecuting = isRunning && currentStep == index
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentExecuting) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    border = if (isCurrentExecuting) 
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                    else 
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isCurrentExecuting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isCurrentExecuting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.description.ifEmpty { action.type.name },
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when (action.type) {
                                    MacroType.TAP -> "Tap (${action.x1}, ${action.y1}) | Delay: ${action.durationMs}ms"
                                    MacroType.SWIPE -> "Swipe (${action.x1},${action.y1}) ➜ (${action.x2},${action.y2}) | ${action.durationMs}ms"
                                    MacroType.TYPE_TEXT -> "Text: '${action.text}' | Delay: ${action.durationMs}ms"
                                    MacroType.WAIT -> "Đờ ${action.durationMs}ms"
                                    MacroType.LAUNCH_APP -> "Launch: ${action.text}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.removeMacroAction(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMacroActionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { action ->
                viewModel.addMacroAction(action)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddMacroActionDialog(
    onDismiss: () -> Unit,
    onAdd: (MacroAction) -> Unit
) {
    var selectedType by remember { mutableStateOf(MacroType.TAP) }
    var x1Text by remember { mutableStateOf("540") }
    var y1Text by remember { mutableStateOf("1200") }
    var x2Text by remember { mutableStateOf("540") }
    var y2Text by remember { mutableStateOf("400") }
    var textInput by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("500") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Bước Tự Động Hoá") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chọn loại thao tác:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MacroType.entries.toTypedArray()) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                when (selectedType) {
                    MacroType.TAP -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = x1Text,
                                onValueChange = { x1Text = it },
                                label = { Text("Tọa độ X") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = y1Text,
                                onValueChange = { y1Text = it },
                                label = { Text("Tọa độ Y") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                    MacroType.SWIPE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = x1Text,
                                onValueChange = { x1Text = it },
                                label = { Text("Từ X1") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = y1Text,
                                onValueChange = { y1Text = it },
                                label = { Text("Từ Y1") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = x2Text,
                                onValueChange = { x2Text = it },
                                label = { Text("Tới X2") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = y2Text,
                                onValueChange = { y2Text = it },
                                label = { Text("Tới Y2") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                    MacroType.TYPE_TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Văn bản cần gõ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MacroType.LAUNCH_APP -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Package / Action") },
                            placeholder = { Text("com.android.settings") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MacroType.WAIT -> {}
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Thời gian chờ / Thực thi (ms)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Mô tả bước (không bắt buộc)") },
                    placeholder = { Text("Ví dụ: Nhấp nút Tiếp tục") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val action = MacroAction(
                        type = selectedType,
                        x1 = x1Text.toIntOrNull() ?: 0,
                        y1 = y1Text.toIntOrNull() ?: 0,
                        x2 = x2Text.toIntOrNull() ?: 0,
                        y2 = y2Text.toIntOrNull() ?: 0,
                        text = textInput,
                        durationMs = durationText.toLongOrNull() ?: 500L,
                        description = descText
                    )
                    onAdd(action)
                }
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun WirelessAdbTerminalTab(viewModel: DevToolboxViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val ip by viewModel.wirelessAdbIp.collectAsStateWithLifecycle()
    val port by viewModel.wirelessAdbPort.collectAsStateWithLifecycle()
    val pairingCode by viewModel.wirelessAdbPairingCode.collectAsStateWithLifecycle()
    val isConnected by viewModel.isAdbConnected.collectAsStateWithLifecycle()

    val customCmd by viewModel.customShellCommand.collectAsStateWithLifecycle()
    val logs by viewModel.shellLogs.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecutingShell.collectAsStateWithLifecycle()

    var isGuideExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val presetCommands = listOf(
        "input tap 540 1200",
        "input swipe 540 1500 540 400 300",
        "input text HelloDevToolbox",
        "am start -a android.settings.SETTINGS",
        "dumpsys battery",
        "getprop ro.build.version.release",
        "screencap -p /sdcard/screen.png",
        "pm list packages -3",
        "id"
    )

    val quickKeys = listOf(
        "input keyevent 4" to "BACK",
        "input keyevent 3" to "HOME",
        "input keyevent 66" to "ENTER",
        "dumpsys battery" to "PIN",
        "getprop" to "PROP",
        "id" to "UID"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wireless_adb_terminal_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Kết Nối Wireless ADB & Shell",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isConnected) "Cổng kết nối đang hoạt động" else "Cần bật Gỡ lỗi qua Wi-Fi trong Cài đặt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isConnected) Color(0xFF1B5E20).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935))
                                )
                                Text(
                                    text = if (isConnected) "Đã Kết Nối" else "Chưa Nối",
                                    color = if (isConnected) Color(0xFF81C784) else MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // IP and Port input row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { viewModel.setWirelessAdbIp(it) },
                            label = { Text("Địa chỉ IP") },
                            placeholder = { Text("127.0.0.1 hoặc 192.168.x.x") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(2f).testTag("adb_ip_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.setWirelessAdbPort(it) },
                            label = { Text("Port") },
                            placeholder = { Text("5555") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1.1f).testTag("adb_port_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Quick Host presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Điền nhanh:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SuggestionChip(
                            onClick = {
                                viewModel.setWirelessAdbIp("127.0.0.1")
                                viewModel.setWirelessAdbPort("5555")
                            },
                            label = { Text("127.0.0.1:5555", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        SuggestionChip(
                            onClick = {
                                viewModel.setWirelessAdbIp("localhost")
                                viewModel.setWirelessAdbPort("5555")
                            },
                            label = { Text("localhost:5555", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // Pairing code field
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { viewModel.setWirelessAdbPairingCode(it) },
                        label = { Text("Mã Ghép Nối (Pairing Code - Tùy chọn)") },
                        placeholder = { Text("Nhập mã 6 chữ số nếu dùng Pairing Code") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (pairingCode.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setWirelessAdbPairingCode("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("adb_code_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Symmetrical Action Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.connectWirelessAdb() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("connect_adb_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kết Nối ADB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.shareAdbToAgy() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_adb_agy_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mở Cổng 5555", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 2. Expandable Automation Script API Guide
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGuideExpanded = !isGuideExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.IntegrationInstructions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Lệnh Tự Động Hoá Broadcast (AGY / Termux)",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { isGuideExpanded = !isGuideExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isGuideExpanded) "Thu gọn" else "Mở rộng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isGuideExpanded) {
                        Text(
                            text = "Termux, Tasker và AGY Assistant có thể gửi lệnh điều phối qua Android Broadcast:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val codeExample = """
# 1. Chạm màn hình
am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action tap --ei x 500 --ei y 1000

# 2. Gõ văn bản
am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action text --es content "hello"

# 3. Phím bấm (Code 66 = Enter, 3 = Home, 4 = Back)
am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action key --ei code 66

# 4. Mở ứng dụng
am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action open --es pkg "com.android.settings"

# 5. Chụp ảnh màn hình
am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action screenshot --es path "/sdcard/DCIM/out.png"
                        """.trimIndent()

                        Surface(
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Broadcast Command Script", color = Color(0xFF81C784), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(codeExample))
                                            Toast.makeText(context, "Đã sao chép kịch bản broadcast!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 6.dp))
                                SelectionContainer {
                                    Text(
                                        text = codeExample,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE0E0E0),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. ADB Terminal Console Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                border = BorderStroke(1.dp, Color(0xFF243048)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // macOS styled top bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Traffic lights
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ADB Shell Console",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }

                        // Status pill & Action buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isExecuting) Color(0xFFE65100).copy(alpha = 0.3f) else Color(0xFF2E7D32).copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFFFFB74D))
                                        Text("Đang chạy", fontSize = 10.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                                    } else {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF81C784)))
                                        Text("Sẵn sàng", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (logs.isNotEmpty()) {
                                        val fullLog = logs.joinToString("\n")
                                        clipboardManager.setText(AnnotatedString(fullLog))
                                        Toast.makeText(context, "📋 Đã sao chép ${logs.size} dòng log!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Chưa có log nào để sao chép", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", tint = Color(0xFF80D8FF), modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { viewModel.clearShellLogs() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF243048))

                    // Log output area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0E14))
                            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "Nhập lệnh bên dưới hoặc chọn lệnh mẫu để thực thi",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            SelectionContainer {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(logs) { line ->
                                        Text(
                                            text = line,
                                            color = when {
                                                line.startsWith("$") -> Color(0xFF4ADE80)
                                                line.startsWith("➜") -> Color(0xFF38BDF8)
                                                line.contains("❌") || line.contains("Error") || line.contains("denied") -> Color(0xFFF87171)
                                                line.startsWith("ℹ") -> Color(0xFFFBBF24)
                                                else -> Color(0xFFCBD5E1)
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick keys toolbar
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickKeys) { (cmd, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    viewModel.setCustomShellCommand(cmd)
                                    viewModel.executeShellCommand()
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Preset commands selector
                    Text(
                        text = "Lệnh mẫu chọn nhanh:",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelSmall
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetCommands) { cmd ->
                            SuggestionChip(
                                onClick = { viewModel.setCustomShellCommand(cmd) },
                                label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFE2E8F0)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            )
                        }
                    }

                    // Symmetrical & Perfectly Balanced Command Input & Execute Button Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customCmd,
                            onValueChange = { viewModel.setCustomShellCommand(it) },
                            placeholder = { Text("Nhập lệnh shell...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            leadingIcon = {
                                Text("$", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                            },
                            trailingIcon = {
                                if (customCmd.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setCustomShellCommand("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Xóa lệnh", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("shell_command_input"),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 12.5.sp
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0B0E14),
                                unfocusedContainerColor = Color(0xFF0B0E14),
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = Color(0xFF4ADE80)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.executeShellCommand() })
                        )

                        Button(
                            onClick = { viewModel.executeShellCommand() },
                            enabled = !isExecuting && customCmd.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("send_shell_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22C55E),
                                disabledContainerColor = Color(0xFF1E293B)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Chạy",
                                    tint = if (customCmd.isNotBlank()) Color.Black else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Chạy",
                                    color = if (customCmd.isNotBlank()) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdbBridgeDaemonTab(viewModel: DevToolboxViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val daemonStatus by viewModel.daemonStatus.collectAsStateWithLifecycle()
    val daemonToken by viewModel.daemonToken.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isDaemonRefreshing.collectAsStateWithLifecycle()
    val report by viewModel.daemonVerificationReport.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifyingDaemon.collectAsStateWithLifecycle()
    val screenshotBitmap by viewModel.daemonScreenshot.collectAsStateWithLifecycle()
    val execCmd by viewModel.daemonExecCommand.collectAsStateWithLifecycle()
    val execOutput by viewModel.daemonExecOutput.collectAsStateWithLifecycle()
    val isExecutingCmd by viewModel.isExecutingDaemonCmd.collectAsStateWithLifecycle()

    var selectedSourceCodeTab by remember { mutableIntStateOf(0) } // 0: DaemonServer.java, 1: build_daemon.sh, 2: start_daemon.sh, 3: adbx, 4: verify_daemon.py
    var showExpectedLogDialog by remember { mutableStateOf(false) }

    val presetDaemonCommands = listOf(
        "input tap 500 1000",
        "screencap -p /sdcard/screen.png",
        "getprop ro.build.version.release",
        "id",
        "pm list packages -3",
        "input text 'Hello%sWorld'",
        "am start -a android.intent.action.VIEW -d 'https://google.com'"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daemon_server_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overview & Architectural Value Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "AdbBridgeDaemon (Zero-Loss Server)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tiến trình ngầm app_process (UID 2000) • REST API 127.0.0.1:8765",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "💡 Điểm đột phá: Sau khi kích hoạt 1 lần bằng lệnh ADB, bạn có thể TẮT HOÀN TOÀN Chế độ nhà phát triển và ngắt Wi-Fi. Termux / PRoot / Linux vẫn điều khiển mọi thao tác cảm ứng, chụp màn hình và gỡ app siêu tốc (<5ms).",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2. Real-time Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (daemonStatus.isOnline) Color(0xFF1E3A2B) else Color(0xFF2C2222)
                ),
                border = BorderStroke(
                    1.dp,
                    if (daemonStatus.isOnline) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFE57373).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (daemonStatus.isOnline) Color(0xFF4CAF50) else Color(0xFFE53935),
                                modifier = Modifier.size(12.dp)
                            ) {}
                            Text(
                                text = if (daemonStatus.isOnline) "ONLINE (Đang Hoạt Động)" else "OFFLINE (Chưa Chạy)",
                                fontWeight = FontWeight.Bold,
                                color = if (daemonStatus.isOnline) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        IconButton(
                            onClick = { viewModel.checkDaemonStatus() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("UID TIẾN TRÌNH", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (daemonStatus.isOnline) "${daemonStatus.uid} (${daemonStatus.user})" else "--",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("THỜI GIAN CHẠY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (daemonStatus.isOnline) "${daemonStatus.uptimeSeconds}s (${daemonStatus.uptimeSeconds / 60}m)" else "--",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Column {
                            Text("RAM TRỐNG", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (daemonStatus.isOnline) "${daemonStatus.freeMemoryMb} MB / ${daemonStatus.totalMemoryMb} MB" else "--",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Token Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("MÃ XÁC THỰC BẢO MẬT (AUTH TOKEN):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = daemonToken.ifBlank { "Chưa có token (Server offline hoặc chưa cấp)" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (daemonToken.isNotBlank()) Color(0xFF64B5F6) else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            if (daemonToken.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(daemonToken))
                                        Toast.makeText(context, "Đã sao chép Bearer Token!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Token", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Management Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.startDaemonProcess() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Khởi Động", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.stopDaemonProcess() },
                            enabled = daemonStatus.isOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dừng Server", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. One-Touch ADB Launch & Quick Setup Guide
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Lệnh Kích Hoạt 1-Chạm Qua ADB PC / Termux",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    val adbLaunchCmd = AdbBridgeDaemonManager.getLaunchAdbCommand()
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = adbLaunchCmd,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(adbLaunchCmd))
                                    Toast.makeText(context, "Đã sao chép lệnh ADB!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = "👉 Sau khi chạy lệnh trên 1 lần duy nhất, Daemon sẽ chạy nền độc lập với UID 2000. Bạn có thể rút cáp và tắt Chế độ nhà phát triển.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Interactive Live REST Console & Screenshot Trigger
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Text(
                                text = "Bàn Điều Khiển & Thực Thi Trực Tiếp (adbx)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Button(
                            onClick = { viewModel.captureDaemonScreenshot() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chụp màn hình", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Preset chips
                    Text("Lệnh mẫu thao tác nhanh:", color = Color(0xFF9E9E9E), fontSize = 11.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetDaemonCommands) { cmd ->
                            SuggestionChip(
                                onClick = { viewModel.setDaemonExecCommand(cmd) },
                                label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF2E2E2E)),
                                border = BorderStroke(1.dp, Color(0xFF424242))
                            )
                        }
                    }

                    // Command input row - balanced with 54dp height
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = execCmd,
                            onValueChange = { viewModel.setDaemonExecCommand(it) },
                            placeholder = { Text("adbx input tap 500 1000", color = Color.Gray, fontSize = 12.sp) },
                            leadingIcon = {
                                Text("⚡", fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                            },
                            trailingIcon = {
                                if (execCmd.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setDaemonExecCommand("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("daemon_cmd_input"),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F0F0F),
                                unfocusedContainerColor = Color(0xFF0F0F0F),
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF333333),
                                cursorColor = Color(0xFF4CAF50)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.executeDaemonCommand() })
                        )

                        Button(
                            onClick = { viewModel.executeDaemonCommand() },
                            enabled = !isExecutingCmd && execCmd.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("send_daemon_cmd_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                disabledContainerColor = Color(0xFF263238)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            if (isExecutingCmd) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Chạy", tint = if (execCmd.isNotBlank()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chạy", color = if (execCmd.isNotBlank()) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Output Box
                    if (execOutput.isNotBlank()) {
                        Surface(
                            color = Color(0xFF0F0F0F),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = execOutput,
                                    color = Color(0xFF80D8FF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Screenshot Preview Box (if captured)
                    if (screenshotBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📸 Ảnh Màn Hình Vừa Chụp (/screenshot):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    IconButton(
                                        onClick = { viewModel.clearDaemonScreenshot() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Image(
                                    bitmap = screenshotBitmap!!.asImageBitmap(),
                                    contentDescription = "Daemon Screenshot Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Automated Verification Suite & Benchmark
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "Bộ Kiểm Thử & Đo Benchmark Tự Động",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "6 bài kiểm chứng: UID, Shell, getprop, Screenshot, Security, Latency",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.runDaemonVerification() },
                            enabled = !isVerifying,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("run_verification_btn")
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đang kiểm thử...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chạy 6 Bài Test", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { showExpectedLogDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xem Log Mẫu", fontSize = 11.sp)
                        }
                    }

                    // Verification Results Render
                    if (report != null) {
                        Surface(
                            color = if (report!!.passedTests == report!!.totalTests) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFB71C1C).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (report!!.passedTests == report!!.totalTests) Color(0xFF4CAF50) else Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "TỔNG KẾT: ${report!!.passedTests}/${report!!.totalTests} BÀI ĐẠT (${if (report!!.passedTests == report!!.totalTests) "HOÀN HẢO" else "CÓ LỖI"})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (report!!.passedTests == report!!.totalTests) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = "Độ trễ TB: ${String.format("%.2f", report!!.averageLatencyMs)}ms",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                report!!.testItems.forEach { test ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (test.isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (test.isPassed) Color(0xFF4CAF50) else Color(0xFFE53935),
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Test ${test.testNumber}: ${test.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "${test.latencyMs}ms",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = test.details,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Source Code Exporter & Inspector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📦 Toàn Bộ Mã Nguồn 5 Thành Phần Hệ Thống",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val sourceTabs = listOf("DaemonServer.java", "build_daemon.sh", "start_daemon.sh", "adbx", "verify_daemon.py")
                    ScrollableTabRow(
                        selectedTabIndex = selectedSourceCodeTab,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        sourceTabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = selectedSourceCodeTab == idx,
                                onClick = { selectedSourceCodeTab = idx },
                                text = { Text(title, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            )
                        }
                    }

                    val currentCode = when (selectedSourceCodeTab) {
                        0 -> AdbBridgeDaemonManager.DAEMON_SERVER_JAVA_CODE
                        1 -> AdbBridgeDaemonManager.BUILD_DAEMON_SH_CODE
                        2 -> AdbBridgeDaemonManager.START_DAEMON_SH_CODE
                        3 -> AdbBridgeDaemonManager.ADBX_CLI_CODE
                        else -> AdbBridgeDaemonManager.VERIFY_DAEMON_PY_CODE
                    }

                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sourceTabs[selectedSourceCodeTab],
                                    color = Color(0xFF81C784),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentCode))
                                        Toast.makeText(context, "Đã sao chép ${sourceTabs[selectedSourceCodeTab]}!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sao chép file", fontSize = 10.sp)
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 6.dp))

                            SelectionContainer {
                                Text(
                                    text = currentCode,
                                    color = Color(0xFFECEFF1),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier
                                        .heightIn(max = 220.dp)
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Expected Output Dialog
    if (showExpectedLogDialog) {
        val expectedLog = """============================================================
      ADB BRIDGE DAEMON AUTOMATED VERIFICATION SUITE
============================================================

[TEST 1] Kiểm Tra Trạng Thái & Đặc Quyền UID
------------------------------------------------------------
Status: online | UID: 2000 | Uptime: 14s
>>> KẾT QUẢ: PASS (Đặc quyền Shell UID 2000 xác nhận chính xác)

[TEST 2] Thực Thi Lệnh Cơ Bản (id)
------------------------------------------------------------
Output: uid=2000(shell) gid=2000(shell) groups=2000(shell),1004(input),1007(log),1011(adb)
>>> KẾT QUẢ: PASS (Lệnh shell thực thi thành công)

[TEST 3] Đọc Thuộc Tính Hệ Thống Android (getprop)
------------------------------------------------------------
Android Version: 14
>>> KẾT QUẢ: PASS (Android 14)

[TEST 4] Chụp Ảnh Màn Hình & Stream Binary (screencap)
------------------------------------------------------------
Received: 2184.45 KB | Is Valid PNG Header: True
>>> KẾT QUẢ: PASS (Stream ảnh PNG hoàn chỉnh)

[TEST 5] Kiểm Tra Bảo Mật Token (Unauthorized Test)
------------------------------------------------------------
HTTP Status Code: 401 Unauthorized (Chặn request trái phép thành công)
>>> KẾT QUẢ: PASS

[TEST 6] Đo Độ Trễ (Latency Benchmark - 10 Iterations)
------------------------------------------------------------
Min: 3.42ms | Max: 6.85ms | Avg: 4.61ms
>>> KẾT QUẢ: PASS (Độ trễ trung bình siêu tốc: 4.61ms < 15ms)

============================================================
TỔNG KẾT KIỂM THỬ: 6/6 BÀI KIỂM TRA ĐẠT HOÀN HẢO
============================================================"""

        AlertDialog(
            onDismissRequest = { showExpectedLogDialog = false },
            title = { Text("📋 Bảng Output Mẫu Đối Chiếu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kết quả mẫu khi chạy file verify_daemon.py thành công 100%:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = expectedLog,
                                color = Color(0xFF81C784),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .heightIn(max = 300.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(expectedLog))
                        Toast.makeText(context, "Đã sao chép log mẫu!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sao Chép Log Mẫu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpectedLogDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}
