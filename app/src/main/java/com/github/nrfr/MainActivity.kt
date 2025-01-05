package com.github.nrfr

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.nrfr.ui.theme.NrfrTheme
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var isShizukuReady by mutableStateOf(false)
    private var showAbout by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Hidden API 访问
        HiddenApiBypass.addHiddenApiExemptions("L")
        HiddenApiBypass.addHiddenApiExemptions("I")

        // 检查 Shizuku 状态
        checkShizukuStatus()

        // 添加 Shizuku 权限监听器
        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            isShizukuReady = grantResult == PackageManager.PERMISSION_GRANTED
            if (!isShizukuReady) {
                Toast.makeText(this, "需要 Shizuku 权限才能运行", Toast.LENGTH_LONG).show()
            }
        }

        // 添加 Shizuku 绑定监听器
        Shizuku.addBinderReceivedListener {
            checkShizukuStatus()
        }

        setContent {
            NrfrTheme {
                if (showAbout) {
                    AboutScreen(onBack = { showAbout = false })
                } else if (isShizukuReady) {
                    MainScreen(onShowAbout = { showAbout = true })
                } else {
                    ShizukuNotReadyScreen()
                }
            }
        }
    }

    private fun checkShizukuStatus() {
        isShizukuReady = if (Shizuku.getBinder() == null) {
            Toast.makeText(this, "请先安装并启用 Shizuku", Toast.LENGTH_LONG).show()
            false
        } else {
            val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Shizuku.requestPermission(0)
            }
            hasPermission
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener { _, _ -> }
        Shizuku.removeBinderReceivedListener { }
    }
}

@Composable
fun ShizukuNotReadyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要 Shizuku 权限",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请安装并启用 Shizuku，然后重启应用",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            modifier = Modifier.size(48.dp),
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("关于")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 应用信息
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "功能介绍",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "• 修改 SIM 卡的国家码配置，可用于解除部分应用的地区限制\n" +
                                    "• 帮助使用海外 SIM 卡时获得更好的本地化体验\n" +
                                    "• 解决部分应用识别 SIM 卡地区错误的问题\n" +
                                    "• 无需 Root 权限，无需修改系统文件，安全且可随时还原\n" +
                                    "• 支持 Android 12 及以上系统版本\n" +
                                    "• 支持双卡设备，可分别配置不同国家码",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 作者信息
                Text(
                    "作者信息",
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("作者: Antkites")
                        Text(
                            "GitHub: Ackites",
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ackites"))
                                )
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "X (Twitter): @actkites",
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://x.com/intent/follow?screen_name=actkites")
                                    )
                                )
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 开源信息
                Text(
                    "开源信息",
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "本项目已在 GitHub 开源",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "访问项目主页",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/Ackites/Nrfr")
                                        )
                                    )
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 版权信息
                Text(
                    "© 2024 Antkites. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onShowAbout: () -> Unit) {
    val context = LocalContext.current
    var selectedSimCard by remember { mutableStateOf<SimCardInfo?>(null) }
    var selectedCountryCode by remember { mutableStateOf("") }
    var customCountryCode by remember { mutableStateOf("") }
    var isCustomCountryCode by remember { mutableStateOf(false) }
    var selectedCarrier by remember { mutableStateOf<CarrierConfigManager.PresetCarriers.CarrierPreset?>(null) }
    var customCarrierName by remember { mutableStateOf("") }
    var isSimCardMenuExpanded by remember { mutableStateOf(false) }
    var isCountryCodeMenuExpanded by remember { mutableStateOf(false) }
    var isCarrierMenuExpanded by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // 获取实际的 SIM 卡信息
    val simCards = remember(context, refreshTrigger) { CarrierConfigManager.getSimCards(context) }

    // 当 simCards 更新时，更新选中的 SIM 卡信息
    LaunchedEffect(simCards, selectedSimCard) {
        if (selectedSimCard != null) {
            selectedSimCard = simCards.find { it.slot == selectedSimCard?.slot }
        }
    }

    // 国家码列表
    data class CountryInfo(val code: String, val name: String)

    val countryCodes = listOf(
        CountryInfo("CN", "中国"),
        CountryInfo("HK", "中国香港"),
        CountryInfo("MO", "中国澳门"),
        CountryInfo("TW", "中国台湾"),
        CountryInfo("JP", "日本"),
        CountryInfo("KR", "韩国"),
        CountryInfo("US", "美国"),
        CountryInfo("GB", "英国"),
        CountryInfo("DE", "德国"),
        CountryInfo("FR", "法国"),
        CountryInfo("IT", "意大利"),
        CountryInfo("ES", "西班牙"),
        CountryInfo("PT", "葡萄牙"),
        CountryInfo("RU", "俄罗斯"),
        CountryInfo("IN", "印度"),
        CountryInfo("AU", "澳大利亚"),
        CountryInfo("NZ", "新西兰"),
        CountryInfo("SG", "新加坡"),
        CountryInfo("MY", "马来西亚"),
        CountryInfo("TH", "泰国"),
        CountryInfo("VN", "越南"),
        CountryInfo("ID", "印度尼西亚"),
        CountryInfo("PH", "菲律宾"),
        CountryInfo("CA", "加拿大"),
        CountryInfo("MX", "墨西哥"),
        CountryInfo("BR", "巴西"),
        CountryInfo("AR", "阿根廷"),
        CountryInfo("ZA", "南非")
    ).sortedBy { it.name }  // 按国家名称排序

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            modifier = Modifier.size(48.dp),
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nrfr")
                    }
                },
                actions = {
                    IconButton(onClick = onShowAbout) {
                        Icon(Icons.Default.Info, contentDescription = "关于")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SIM卡选择
            ExposedDropdownMenuBox(
                expanded = isSimCardMenuExpanded,
                onExpandedChange = { isSimCardMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSimCard?.let { "SIM ${it.slot} (${it.carrierName})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择SIM卡") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSimCardMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isSimCardMenuExpanded,
                    onDismissRequest = { isSimCardMenuExpanded = false }
                ) {
                    simCards.forEach { simCard ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("SIM ${simCard.slot} (${simCard.carrierName})")
                                    if (simCard.currentConfig.isEmpty()) {
                                        Text(
                                            "无覆盖配置",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        simCard.currentConfig.forEach { (key, value) ->
                                            Text(
                                                "$key: $value",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                selectedSimCard = simCard
                                isSimCardMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 显示当前选中的 SIM 卡的配置信息
            selectedSimCard?.let { simCard ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "当前配置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (simCard.currentConfig.isEmpty()) {
                            Text(
                                "无覆盖配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            simCard.currentConfig.forEach { (key, value) ->
                                Text(
                                    "$key: $value",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // 国家码选择
            ExposedDropdownMenuBox(
                expanded = isCountryCodeMenuExpanded,
                onExpandedChange = { isCountryCodeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = when {
                        isCustomCountryCode -> "自定义"
                        selectedCountryCode.isEmpty() -> ""
                        else -> countryCodes.find { it.code == selectedCountryCode }?.let { "${it.name} (${it.code})" }
                            ?: selectedCountryCode
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择国家码") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryCodeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCountryCodeMenuExpanded,
                    onDismissRequest = { isCountryCodeMenuExpanded = false }
                ) {
                    // 预设国家码列表
                    countryCodes.forEach { countryInfo ->
                        DropdownMenuItem(
                            text = { Text("${countryInfo.name} (${countryInfo.code})") },
                            onClick = {
                                selectedCountryCode = countryInfo.code
                                isCustomCountryCode = false
                                isCountryCodeMenuExpanded = false
                            }
                        )
                    }
                    // 自定义选项
                    DropdownMenuItem(
                        text = { Text("自定义") },
                        onClick = {
                            isCustomCountryCode = true
                            selectedCountryCode = customCountryCode
                            isCountryCodeMenuExpanded = false
                        }
                    )
                }
            }

            // 自定义国家码输入框
            if (isCustomCountryCode) {
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = customCountryCode,
                    onValueChange = {
                        if (it.length <= 2 && it.all { char -> char.isLetter() }) {
                            customCountryCode = it.uppercase()
                            selectedCountryCode = it.uppercase()
                        }
                    },
                    label = { Text("自定义国家码 (2位字母)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 运营商选择
            ExposedDropdownMenuBox(
                expanded = isCarrierMenuExpanded,
                onExpandedChange = { isCarrierMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCarrier?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择运营商") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCarrierMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCarrierMenuExpanded,
                    onDismissRequest = { isCarrierMenuExpanded = false }
                ) {
                    // 分组显示运营商
                    // 中国大陆
                    Text(
                        "中国大陆",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "CN" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 中国香港
                    Text(
                        "中国香港",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "HK" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 中国澳门
                    Text(
                        "中国澳门",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "MO" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 中国台湾
                    Text(
                        "中国台湾",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "TW" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 日本
                    Text(
                        "日本",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "JP" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 韩国
                    Text(
                        "韩国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "KR" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 新加坡
                    Text(
                        "新加坡",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "SG" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 马来西亚
                    Text(
                        "马来西亚",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "MY" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 泰国
                    Text(
                        "泰国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "TH" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 越南
                    Text(
                        "越南",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "VN" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 印度尼西亚
                    Text(
                        "印度尼西亚",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "ID" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 菲律宾
                    Text(
                        "菲律宾",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "PH" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 印度
                    Text(
                        "印度",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "IN" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 澳大利亚
                    Text(
                        "澳大利亚",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "AU" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 美国
                    Text(
                        "美国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "US" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 加拿大
                    Text(
                        "加拿大",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "CA" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 英国
                    Text(
                        "英国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "GB" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 德国
                    Text(
                        "德国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "DE" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 法国
                    Text(
                        "法国",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "FR" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 意大利
                    Text(
                        "意大利",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "IT" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 西班牙
                    Text(
                        "西班牙",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "ES" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 俄罗斯
                    Text(
                        "俄罗斯",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "RU" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 巴西
                    Text(
                        "巴西",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter { it.region == "BR" }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // 其他地区
                    Text(
                        "其他地区",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CarrierConfigManager.PresetCarriers.presets
                        .filter {
                            it.region !in listOf(
                                "CN", "HK", "MO", "TW", "JP", "KR",
                                "SG", "MY", "TH", "VN", "ID", "PH", "IN",
                                "GB", "DE", "FR", "IT", "ES", "RU",
                                "US", "CA", "BR", "AU", ""
                            )
                        }
                        .forEach { preset ->
                            DropdownMenuItem(
                                text = { Text("${preset.name} (${preset.region})") },
                                onClick = {
                                    selectedCarrier = preset
                                    customCarrierName = preset.displayName
                                    isCarrierMenuExpanded = false
                                }
                            )
                        }

                    // 自定义选项
                    if (CarrierConfigManager.PresetCarriers.presets.any { it.region.isEmpty() }) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        CarrierConfigManager.PresetCarriers.presets
                            .filter { it.region.isEmpty() }
                            .forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name) },
                                    onClick = {
                                        selectedCarrier = preset
                                        isCarrierMenuExpanded = false
                                    }
                                )
                            }
                    }
                }
            }

            // 自定义运营商名称输入框
            if (selectedCarrier?.name == "自定义") {
                OutlinedTextField(
                    value = customCarrierName,
                    onValueChange = { customCarrierName = it },
                    label = { Text("自定义运营商名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 还原按钮
                OutlinedButton(
                    onClick = {
                        selectedSimCard?.let { simCard ->
                            try {
                                CarrierConfigManager.resetCarrierConfig(simCard.subId)
                                Toast.makeText(context, "设置已还原", Toast.LENGTH_SHORT).show()
                                refreshTrigger += 1
                                selectedCountryCode = ""
                                selectedCarrier = null
                                customCarrierName = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "还原失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedSimCard != null
                ) {
                    Text("还原设置")
                }

                // 保存按钮
                Button(
                    onClick = {
                        selectedSimCard?.let { simCard ->
                            try {
                                val carrierName = if (selectedCarrier?.name == "自定义") {
                                    customCarrierName.takeIf { it.isNotEmpty() }
                                } else {
                                    selectedCarrier?.displayName
                                }
                                val countryCode = if (isCustomCountryCode) {
                                    customCountryCode.takeIf { it.length == 2 }
                                } else {
                                    selectedCountryCode
                                }
                                CarrierConfigManager.setCarrierConfig(
                                    simCard.subId,
                                    countryCode,
                                    carrierName
                                )
                                Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                                refreshTrigger += 1
                            } catch (e: Exception) {
                                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedSimCard != null && (
                            (isCustomCountryCode && customCountryCode.length == 2) ||
                                    (!isCustomCountryCode && selectedCountryCode.isNotEmpty()) ||
                                    (selectedCarrier != null && (selectedCarrier?.name != "自定义" || customCarrierName.isNotEmpty()))
                            )
                ) {
                    Text("保存生效")
                }
            }
        }
    }
}
