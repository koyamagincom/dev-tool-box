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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var selectedSubMode by remember { mutableIntStateOf(0) } // 0: AI Agent, 1: Touch Macro

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
                text = { Text("🤖 AI Assistant") },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Agent") },
                modifier = Modifier.testTag("subtab_ai_agent")
            )
            Tab(
                selected = selectedSubMode == 1,
                onClick = { selectedSubMode = 1 },
                text = { Text("⚡ Touch Macro") },
                icon = { Icon(Icons.Default.TouchApp, contentDescription = "Macro") },
                modifier = Modifier.testTag("subtab_macro")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubMode) {
                0 -> AiAutomationTab(viewModel)
                1 -> MacroAutoClickerTab(viewModel)
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
        "screencap -p /sdcard/screen.png"
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
                    containerColor = MaterialTheme.colorScheme.surface
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
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Kết Nối Wireless ADB & Shell",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Badge(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isConnected) "Đã kết nối ADB" else "Chưa nối ADB",
                                color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { viewModel.setWirelessAdbIp(it) },
                            label = { Text("Địa chỉ IP") },
                            modifier = Modifier.weight(2f).testTag("adb_ip_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.setWirelessAdbPort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f).testTag("adb_port_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { viewModel.setWirelessAdbPairingCode(it) },
                        label = { Text("Mã Ghép Nối (Pairing Code - Tùy chọn)") },
                        modifier = Modifier.fillMaxWidth().testTag("adb_code_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.connectWirelessAdb() },
                            modifier = Modifier.weight(1f).testTag("connect_adb_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Power, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kết Nối ADB")
                        }

                        Button(
                            onClick = { viewModel.shareAdbToAgy() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f).testTag("share_adb_agy_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share ADB to AGY (5555)")
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
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
                        Icon(
                            imageVector = Icons.Default.IntegrationInstructions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "🤖 Automation Script Orchestration API (AGY / Termux)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = "AGY Assistant & Termux có thể gửi lệnh điều phối trực tiếp qua broadcast:",
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
                        am broadcast -a com.aistudio.devtoolbox.ACTION_AUTO --es action screenshot --es path "/sdcard/DCIM/AGY/out.png"
                    """.trimIndent()

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = codeExample,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ADB Terminal Console",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    if (logs.isNotEmpty()) {
                                        val fullLog = logs.joinToString("\n")
                                        clipboardManager.setText(AnnotatedString(fullLog))
                                        Toast.makeText(context, "📋 Đã sao chép toàn bộ log terminal!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Chưa có log nào để sao chép", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", tint = Color(0xFF00BCD4))
                            }
                            IconButton(onClick = { viewModel.clearShellLogs() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.LightGray)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF121212), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
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
                                            line.startsWith("$") -> Color(0xFF4CAF50)
                                            line.startsWith("➜") -> Color(0xFF00BCD4)
                                            line.contains("❌") || line.contains("Error") -> Color(0xFFFF5252)
                                            else -> Color.LightGray
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Lệnh mẫu chọn nhanh:",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presetCommands) { cmd ->
                            SuggestionChip(
                                onClick = { viewModel.setCustomShellCommand(cmd) },
                                label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF2D2D2D))
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customCmd,
                            onValueChange = { viewModel.setCustomShellCommand(it) },
                            placeholder = { Text("Nhập lệnh shell...", color = Color.Gray) },
                            modifier = Modifier.weight(1f).testTag("shell_command_input"),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.executeShellCommand() })
                        )

                        IconButton(
                            onClick = { viewModel.executeShellCommand() },
                            enabled = !isExecuting && customCmd.isNotBlank(),
                            modifier = Modifier.testTag("send_shell_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}
