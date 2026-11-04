package com.vanta.launcher.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanta.launcher.data.ControlButton
import com.vanta.launcher.data.ControlJsonCodec
import com.vanta.launcher.data.ControlLayout
import com.vanta.launcher.data.ControlLayoutStore
import com.vanta.launcher.data.LauncherDatabase
import com.vanta.launcher.data.LocalAccount
import com.vanta.launcher.data.SkinStore
import com.vanta.launcher.ui.theme.VantaAccent
import com.vanta.launcher.ui.theme.VantaAccentDark
import com.vanta.launcher.ui.theme.VantaBackground
import com.vanta.launcher.ui.theme.VantaBorder
import com.vanta.launcher.ui.theme.VantaDanger
import com.vanta.launcher.ui.theme.VantaMuted
import com.vanta.launcher.ui.theme.VantaPanel
import com.vanta.launcher.ui.theme.VantaPanelSoft
import com.vanta.launcher.ui.theme.VantaSurface
import com.vanta.launcher.ui.theme.VantaText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private enum class AppTab(val title: String) {
    HOME("Home"), ACCOUNTS("Accounts"), CONTROLS("Controls"), SETTINGS("Settings")
}

@Composable
fun VantaApp(
    context: Context,
    database: LauncherDatabase,
    controls: ControlLayoutStore,
    bootstrap: () -> List<String>
) {
    val prefs = remember { context.getSharedPreferences("vanta-first-run", Context.MODE_PRIVATE) }
    var initialized by remember { mutableStateOf(prefs.getBoolean("initialized", false)) }
    if (!initialized) {
        BootScreen(
            bootstrap = bootstrap,
            onComplete = {
                prefs.edit().putBoolean("initialized", true).apply()
                initialized = true
            }
        )
    } else {
        LauncherShell(context, database, controls)
    }
}

@Composable
private fun BootScreen(bootstrap: () -> List<String>, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var started by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checks by remember { mutableStateOf(listOf<String>()) }

    Box(
        Modifier.fillMaxSize().background(VantaBackground).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 38.dp)) {
                Text("VANTA", fontSize = 42.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = VantaText)
                Text("LAUNCHER", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp, color = VantaAccent)
                Spacer(Modifier.height(22.dp))
                Text("BEYOND THE BLOCKS.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = VantaText)
                Text("A native Android home for Minecraft Java.", fontSize = 13.sp, color = VantaMuted)
                Spacer(Modifier.height(32.dp))
                when {
                    !started -> AccentButton("RUN CODE />", Modifier.width(250.dp)) { started = true
                        running = true
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) { bootstrap() }
                                checks = result
                                finished = true
                            } catch (e: Exception) {
                                error = e.message ?: "Initialization failed."
                            } finally {
                                running = false
                            }
                        }
                    }
                    running -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = VantaAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("INITIALIZING LOCAL ENVIRONMENT", color = VantaAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    finished -> AccentButton("NEXT", Modifier.width(250.dp), onClick = onComplete)
                    else -> AccentButton("RETRY", Modifier.width(250.dp)) {
                        error = null
                        started = false
                    }
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = VantaDanger, fontSize = 12.sp)
                }
            }
            Panel(Modifier.weight(1f).fillMaxHeight(0.82f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (finished) VantaAccent else VantaMuted))
                    Spacer(Modifier.width(10.dp))
                    Text("SYSTEM INITIALIZATION", fontSize = 13.sp, color = VantaText, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Spacer(Modifier.height(18.dp))
                if (!started) {
                    Text("Awaiting startup command...", color = VantaMuted, fontSize = 13.sp)
                } else {
                    checks.forEach { line ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = VantaAccent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(line, color = VantaMuted, fontSize = 12.sp)
                        }
                    }
                    if (finished) {
                        Spacer(Modifier.height(16.dp))
                        Text("FINISHED", color = VantaAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Local initialization completed. Minecraft engine integration is not part of this foundation build.", color = VantaMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        Text("VANTA // ANDROID", Modifier.align(Alignment.BottomStart), color = VantaMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text("FOUNDATION 0.1.0", Modifier.align(Alignment.BottomEnd), color = VantaMuted, fontSize = 10.sp, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun LauncherShell(context: Context, database: LauncherDatabase, controls: ControlLayoutStore) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var refreshKey by remember { mutableStateOf(0) }
    var showEngineInfo by remember { mutableStateOf(false) }
    val account = remember(refreshKey) { database.activeAccount() }

    Column(Modifier.fillMaxSize().background(VantaBackground)) {
        Row(
            Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("VANTA", color = VantaText, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp)
            Spacer(Modifier.width(9.dp))
            Text("LAUNCHER", color = VantaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
            Spacer(Modifier.weight(1f))
            TopAction(Icons.Default.AccountCircle, "Accounts") { tab = AppTab.ACCOUNTS }
            TopAction(Icons.Default.Gamepad, "Controls") { tab = AppTab.CONTROLS }
            TopAction(Icons.Default.Settings, "Settings") { tab = AppTab.SETTINGS }
        }
        Divider(color = VantaBorder.copy(alpha = 0.6f), thickness = 1.dp)
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(184.dp).fillMaxHeight().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("WORKSPACE", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 7.dp))
                NavItem("Home", Icons.Default.Home, tab == AppTab.HOME) { tab = AppTab.HOME }
                NavItem("Accounts", Icons.Default.Person, tab == AppTab.ACCOUNTS) { tab = AppTab.ACCOUNTS }
                NavItem("Controls", Icons.Default.Tune, tab == AppTab.CONTROLS) { tab = AppTab.CONTROLS }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VantaSurface).padding(12.dp)) {
                    Text("ENGINE STATUS", color = VantaMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(VantaDanger))
                        Spacer(Modifier.width(7.dp))
                        Text("Not connected", color = VantaText, fontSize = 11.sp)
                    }
                }
                NavItem("Settings", Icons.Default.Settings, tab == AppTab.SETTINGS) { tab = AppTab.SETTINGS }
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(start = 5.dp, end = 22.dp, top = 16.dp, bottom = 18.dp)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 18 }) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 18 })
                    },
                    label = "vanta-tab-transition"
                ) { target ->
                    when (target) {
                        AppTab.HOME -> HomeScreen(account, database, onAccounts = { tab = AppTab.ACCOUNTS }, onLaunch = { showEngineInfo = true })
                        AppTab.ACCOUNTS -> AccountsScreen(context, database, refreshKey) { refreshKey++ }
                        AppTab.CONTROLS -> ControlsScreen(context, controls)
                        AppTab.SETTINGS -> SettingsScreen(context)
                    }
                }
            }
        }
    }

    if (showEngineInfo) {
        AlertDialog(
            onDismissRequest = { showEngineInfo = false },
            containerColor = VantaPanel,
            title = { Text("Minecraft engine not connected", color = VantaText) },
            text = {
                Text(
                    "This build contains the VANTA interface, local account database, skin storage, and JSON control editor. It does not yet contain the native Minecraft Java runtime, renderer, or input bridge, so it cannot launch a game session. The engine integration is deliberately not simulated.",
                    color = VantaMuted
                )
            },
            confirmButton = { TextButton(onClick = { showEngineInfo = false }) { Text("UNDERSTOOD", color = VantaAccent) } }
        )
    }
}

@Composable
private fun HomeScreen(
    account: LocalAccount?,
    database: LauncherDatabase,
    onAccounts: () -> Unit,
    onLaunch: () -> Unit
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Panel(Modifier.width(310.dp).fillMaxHeight()) {
            Text("PLAYER PROFILE", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PixelAvatar(account, Modifier.size(112.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(account?.username ?: "No account", color = VantaText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(if (account == null) "No local profile selected" else "Local profile", color = if (account == null) VantaMuted else VantaAccent, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.weight(1f))
            AccentButton(if (account == null) "ADD ACCOUNT" else "MANAGE ACCOUNT", Modifier.fillMaxWidth(), onAccounts)
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Panel(Modifier.fillMaxWidth().weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SELECTED INSTANCE", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No installation selected", color = VantaText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Installations and Minecraft versions will appear here after the engine is integrated.", color = VantaMuted, fontSize = 12.sp)
                    }
                    Box(Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)).background(VantaPanelSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Gamepad, null, tint = VantaMuted, modifier = Modifier.size(38.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Divider(color = VantaBorder)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Metric("GAME VERSION", "—")
                    Metric("MODS", "—")
                    Metric("JAVA RUNTIME", "Not installed")
                    Metric("MEMORY", "Auto")
                }
            }
            Panel(Modifier.fillMaxWidth().height(156.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("READY WHEN THE ENGINE IS", color = VantaText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("VANTA's launcher foundation is initialized. Minecraft launch is gated until a real engine core is connected.", color = VantaMuted, fontSize = 11.sp)
                    }
                    AccentButton("LAUNCH", Modifier.width(170.dp), onLaunch)
                }
            }
        }
    }
}

@Composable
private fun AccountsScreen(context: Context, database: LauncherDatabase, refreshKey: Int, onChanged: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginName by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val accounts = remember(refreshKey) { database.listAccounts() }
    val active = remember(refreshKey) { database.activeAccount() }

    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val selected = database.activeAccount()
            if (selected == null) {
                message = "Create or select a local account first."
                error = true
            } else {
                try {
                    val savedPath = SkinStore.importSkin(context, selected.id, uri)
                    database.updateSkin(selected.id, savedPath)
                    message = "Skin imported for ${selected.username}."
                    error = false
                    onChanged()
                } catch (e: Exception) {
                    message = e.message ?: "Skin import failed."
                    error = true
                }
            }
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(0.92f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeading("Accounts", "Local profiles stored on this device. No Firebase or cloud account database.")
            Panel(Modifier.fillMaxWidth().weight(1f)) {
                Text("CREATE LOCAL ACCOUNT", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(16) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                    placeholder = { Text("Your Minecraft name") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(13.dp))
                AccentButton("CREATE", Modifier.fillMaxWidth()) {
                    try {
                        database.createAccount(username, password)
                        username = ""
                        password = ""
                        message = "Local profile created. Steve is now the default skin."
                        error = false
                        onChanged()
                    } catch (e: Exception) {
                        message = e.message ?: "Could not create the profile."
                        error = true
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Passwords are stored as salted PBKDF2 hashes in the app's private SQLite database.", color = VantaMuted, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                Divider(color = VantaBorder)
                Spacer(Modifier.height(12.dp))
                Text("SIGN IN TO AN EXISTING LOCAL PROFILE", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(loginName, { loginName = it.take(16) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Name") }, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(loginPassword, { loginPassword = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val result = database.authenticate(loginName, loginPassword)
                    if (result != null) {
                        message = "Signed in as ${result.username}."
                        error = false
                        loginPassword = ""
                        onChanged()
                    } else {
                        message = "Incorrect name or password."
                        error = true
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SIGN IN")
                }
                message?.let {
                    Spacer(Modifier.height(9.dp))
                    Text(it, color = if (error) VantaDanger else VantaAccent, fontSize = 11.sp)
                }
            }
        }
        Column(Modifier.weight(1.08f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.fillMaxWidth().weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PROFILES", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${accounts.size} saved", color = VantaMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(12.dp))
                if (accounts.isEmpty()) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(VantaPanel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelAvatar(null, Modifier.size(60.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("No account", color = VantaText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Create a local profile to get started.", color = VantaMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    accounts.forEach { item ->
                        AccountRow(
                            account = item,
                            active = item.id == active?.id,
                            onSelect = {
                                database.activate(item.id)
                                message = "Active profile: ${item.username}"
                                error = false
                                onChanged()
                            },
                            onDelete = {
                                database.deleteAccount(item.id)
                                message = "Profile removed from this device."
                                error = false
                                onChanged()
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Divider(color = VantaBorder)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SKIN MANAGER", color = VantaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Import a Java skin PNG for the active profile.", color = VantaMuted, fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { skinPicker.launch(arrayOf("image/png")) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("IMPORT PNG")
                    }
                }
                if (active != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current skin: ${if (active.skinPath == "builtin:steve") "Steve (default)" else "Custom PNG"}", color = VantaAccent, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        if (active.skinPath != "builtin:steve") {
                            TextButton(onClick = {
                                database.updateSkin(active.id, "builtin:steve")
                                message = "Default Steve skin restored."
                                error = false
                                onChanged()
                            }) { Text("RESET TO STEVE", color = VantaAccent, fontSize = 9.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: LocalAccount, active: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(if (active) VantaAccentDark.copy(alpha = 0.65f) else VantaPanel)
            .border(1.dp, if (active) VantaAccent.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelAvatar(account, Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(account.username, color = VantaText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(if (active) "ACTIVE · LOCAL PROFILE" else "LOCAL PROFILE", color = if (active) VantaAccent else VantaMuted, fontSize = 9.sp, letterSpacing = 0.8.sp)
        }
        TextButton(onClick = onSelect, enabled = !active) { Text(if (active) "SELECTED" else "USE", color = VantaAccent, fontSize = 10.sp) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete local profile", tint = VantaMuted, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun ControlsScreen(context: Context, store: ControlLayoutStore) {
    var layout by remember { mutableStateOf(runCatching { store.load() }.getOrElse { store.defaultLayout() }) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ControlButton?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeError by remember { mutableStateOf(false) }

    fun persist(updated: ControlLayout) {
        try {
            store.save(updated)
            layout = updated
            notice = "Layout saved locally."
            noticeError = false
        } catch (e: Exception) {
            notice = e.message ?: "Could not save this layout."
            noticeError = true
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val imported = context.contentResolver.openInputStream(uri)?.use { store.importFrom(it) }
                    ?: throw IllegalArgumentException("Could not open the selected JSON file.")
                layout = imported
                selectedId = null
                notice = "Imported '${imported.name}' successfully."
                noticeError = false
            } catch (e: Exception) {
                notice = e.message ?: "Import failed."
                noticeError = true
            }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { store.exportTo(it, layout) }
                    ?: throw IllegalArgumentException("Could not write to the selected destination.")
                notice = "Exported '${layout.name}.json'."
                noticeError = false
            } catch (e: Exception) {
                notice = e.message ?: "Export failed."
                noticeError = true
            }
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1.12f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            ScreenHeading("Touch controls", "Create a VANTA layout, position controls, and exchange layouts as JSON.")
            Panel(Modifier.fillMaxWidth().weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(layout.name, color = VantaText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${layout.buttons.size} controls · ${layout.author}", color = VantaMuted, fontSize = 10.sp)
                    }
                    SmallIconAction(Icons.Default.Add, "Add new button") {
                        if (layout.buttons.size >= ControlJsonCodec.MAX_BUTTONS) {
                            notice = "A layout can contain at most 64 controls."
                            noticeError = true
                        } else {
                            val newButton = ControlButton(UUID.randomUUID().toString(), "NEW", "KEY_N", 0.42f, 0.42f, 52f)
                            persist(layout.copy(buttons = layout.buttons + newButton))
                            selectedId = newButton.id
                            editing = newButton
                        }
                    }
                    SmallIconAction(Icons.Default.Upload, "Import JSON") { importPicker.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    SmallIconAction(Icons.Default.Download, "Export JSON") { exportPicker.launch("${layout.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json") }
                    SmallIconAction(Icons.Default.Refresh, "Reset layout") { persist(store.defaultLayout()); selectedId = null }
                }
                Spacer(Modifier.height(12.dp))
                Text("PREVIEW CANVAS", color = VantaMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF111317)).border(1.dp, VantaBorder, RoundedCornerShape(18.dp))
                ) {
                    val canvasWidth = maxWidth
                    val canvasHeight = maxHeight
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("MINECRAFT TOUCH LAYOUT", color = VantaBorder, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                    layout.buttons.forEach { button ->
                        Surface(
                            modifier = Modifier.offset(x = canvasWidth * button.x, y = canvasHeight * button.y)
                                .size(button.size.dp)
                                .clickable { selectedId = button.id; editing = button },
                            shape = RoundedCornerShape(13.dp),
                            color = if (selectedId == button.id) VantaAccentDark else VantaPanelSoft.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedId == button.id) VantaAccent else VantaBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(button.label, color = if (selectedId == button.id) VantaAccent else VantaText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Tap a control to edit its label, key action, size, and normalized position.", color = VantaMuted, fontSize = 10.sp)
                notice?.let { Text(it, color = if (noticeError) VantaDanger else VantaAccent, fontSize = 10.sp) }
            }
        }
        Panel(Modifier.width(260.dp).fillMaxHeight()) {
            Text("CONTROL LIST", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                layout.buttons.forEachIndexed { index, button ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (selectedId == button.id) VantaAccentDark else VantaPanel)
                            .clickable { selectedId = button.id; editing = button }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(button.label, color = VantaText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(button.action, color = VantaMuted, fontSize = 9.sp)
                        }
                        IconButton(onClick = {
                            if (index > 0) {
                                val items = layout.buttons.toMutableList()
                                val current = items.removeAt(index)
                                items.add(index - 1, current)
                                persist(layout.copy(buttons = items))
                            }
                        }, enabled = index > 0, modifier = Modifier.size(25.dp)) {
                            Icon(Icons.Default.ArrowUpward, null, tint = VantaMuted, modifier = Modifier.size(13.dp))
                        }
                        IconButton(onClick = {
                            if (index < layout.buttons.lastIndex) {
                                val items = layout.buttons.toMutableList()
                                val current = items.removeAt(index)
                                items.add(index + 1, current)
                                persist(layout.copy(buttons = items))
                            }
                        }, enabled = index < layout.buttons.lastIndex, modifier = Modifier.size(25.dp)) {
                            Icon(Icons.Default.ArrowDownward, null, tint = VantaMuted, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
            Divider(color = VantaBorder)
            Spacer(Modifier.height(9.dp))
            Text("JSON FORMAT", color = VantaMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("vanta.control-layout / v1", color = VantaText, fontSize = 11.sp)
            Text("This VANTA JSON schema is not yet an Amethyst/Zalith engine control file.", color = VantaMuted, fontSize = 9.sp)
        }
    }

    editing?.let { selected ->
        ButtonEditorDialog(
            initial = selected,
            onDismiss = { editing = null },
            onDelete = {
                if (layout.buttons.size <= 1) {
                    notice = "A control layout must keep at least one button."
                    noticeError = true
                } else {
                    persist(layout.copy(buttons = layout.buttons.filterNot { it.id == selected.id }))
                    selectedId = null
                    editing = null
                }
            },
            onSave = { updated ->
                persist(layout.copy(buttons = layout.buttons.map { if (it.id == updated.id) updated else it }))
                editing = null
            }
        )
    }
}

@Composable
private fun ButtonEditorDialog(
    initial: ControlButton,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ControlButton) -> Unit
) {
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var action by remember(initial.id) { mutableStateOf(initial.action) }
    var x by remember(initial.id) { mutableStateOf(initial.x) }
    var y by remember(initial.id) { mutableStateOf(initial.y) }
    var size by remember(initial.id) { mutableStateOf(initial.size) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VantaPanel,
        title = { Text("Edit control", color = VantaText) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(label, { label = it.take(12) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Button label") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(action, { action = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Key action (e.g. SPACE, KEY_W)") })
                Spacer(Modifier.height(12.dp))
                Text("Horizontal position · ${(x * 100).toInt()}%", color = VantaMuted, fontSize = 11.sp)
                Slider(x, { x = it }, valueRange = 0f..0.92f)
                Text("Vertical position · ${(y * 100).toInt()}%", color = VantaMuted, fontSize = 11.sp)
                Slider(y, { y = it }, valueRange = 0f..0.84f)
                Text("Size · ${size.toInt()} dp", color = VantaMuted, fontSize = 11.sp)
                Slider(size, { size = it }, valueRange = 32f..96f)
                error?.let { Text(it, color = VantaDanger, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = initial.copy(label = label.trim(), action = action, x = x, y = y, size = size)
                try {
                    ControlJsonCodec.validate(ControlLayout("Edit", "VANTA", 1, listOf(updated)))
                    onSave(updated)
                } catch (e: Exception) { error = e.message }
            }) { Text("SAVE", color = VantaAccent) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("DELETE", color = VantaDanger) }
                TextButton(onClick = onDismiss) { Text("CANCEL", color = VantaMuted) }
            }
        }
    )
}

@Composable
private fun SettingsScreen(context: Context) {
    val info = remember {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memory = android.app.ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        listOf(
            "Android" to android.os.Build.VERSION.RELEASE,
            "Device ABI" to (android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
            "Available RAM" to "${memory.availMem / (1024 * 1024)} MB",
            "App storage" to context.filesDir.absolutePath
        )
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeading("Settings", "Device diagnostics and local launcher configuration.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Text("APPLICATION", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(12.dp))
                InfoRow("Application", "VANTA Launcher")
                InfoRow("Version", "0.1.0 · Foundation")
                InfoRow("Storage", "Private app storage")
                InfoRow("Account database", "SQLite · Local only")
                InfoRow("Password security", "PBKDF2-HMAC-SHA256")
                InfoRow("Firebase", "Not used")
                Spacer(Modifier.weight(1f))
                Text("Profiles, skins, and control layouts remain on this device unless you export them.", color = VantaMuted, fontSize = 10.sp)
            }
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Text("DEVICE DIAGNOSTICS", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(12.dp))
                info.forEach { InfoRow(it.first, it.second) }
                Spacer(Modifier.height(14.dp))
                Divider(color = VantaBorder)
                Spacer(Modifier.height(14.dp))
                Text("MINECRAFT ENGINE", color = VantaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = VantaDanger, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Not integrated in this build", color = VantaDanger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("The Android Java runtime, renderer libraries, Minecraft process lifecycle, and in-game input bridge are not included yet.", color = VantaMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AngelAuraMC/Amethyst-Android"))
                    context.startActivity(intent)
                }, shape = RoundedCornerShape(13.dp)) {
                    Text("VIEW ENGINE REFERENCE", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PixelAvatar(account: LocalAccount?, modifier: Modifier = Modifier) {
    val customPath = account?.skinPath?.takeIf { it != "builtin:steve" }
    val bitmap = remember(customPath) { customPath?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Imported skin texture", modifier = modifier.clip(RoundedCornerShape(15.dp)))
    } else {
        Canvas(modifier) {
            val cell = size.width / 16f
            fun pixel(x: Int, y: Int, w: Int, h: Int, color: Color) {
                drawRect(color, Offset(x * cell, y * cell), Size(w * cell, h * cell), style = Fill)
            }
            if (account == null) {
                val dark = Color(0xFF575B63)
                val mid = Color(0xFF858A92)
                val light = Color(0xFFB4B8BF)
                pixel(5, 1, 6, 5, dark)
                pixel(4, 3, 1, 4, mid)
                pixel(11, 3, 1, 4, mid)
                pixel(5, 5, 6, 2, light)
                pixel(6, 7, 4, 1, mid)
                pixel(4, 8, 8, 5, dark)
                pixel(5, 13, 3, 3, mid)
                pixel(8, 13, 3, 3, mid)
                pixel(2, 8, 2, 5, dark)
                pixel(12, 8, 2, 5, dark)
            } else {
                val hair = Color(0xFF493327)
                val skin = Color(0xFFD7A77F)
                val skinShade = Color(0xFFB98460)
                val blue = Color(0xFF2875B9)
                val blueShade = Color(0xFF1D5689)
                val pants = Color(0xFF344B78)
                val shoes = Color(0xFF454545)
                val white = Color(0xFFE5E7EB)
                pixel(5, 1, 6, 5, hair)
                pixel(4, 2, 1, 5, hair)
                pixel(11, 2, 1, 5, hair)
                pixel(6, 4, 1, 1, white)
                pixel(9, 4, 1, 1, white)
                pixel(6, 5, 1, 1, Color(0xFF26364B))
                pixel(9, 5, 1, 1, Color(0xFF26364B))
                pixel(7, 6, 2, 1, skinShade)
                pixel(5, 7, 6, 1, skin)
                pixel(5, 8, 6, 4, blue)
                pixel(4, 8, 1, 4, skin)
                pixel(11, 8, 1, 4, skin)
                pixel(5, 12, 3, 3, pants)
                pixel(8, 12, 3, 3, pants)
                pixel(5, 15, 3, 1, shoes)
                pixel(8, 15, 3, 1, shoes)
                pixel(5, 8, 1, 4, blueShade)
                pixel(10, 8, 1, 4, blueShade)
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(25.dp)).background(VantaPanel).padding(17.dp),
        content = content
    )
}

@Composable
private fun ScreenHeading(title: String, subtitle: String) {
    Column {
        Text(title, color = VantaText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = VantaMuted, fontSize = 11.sp)
    }
}

@Composable
private fun Metric(title: String, value: String) {
    Column {
        Text(title, color = VantaMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, color = VantaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = VantaMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = VantaText, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Divider(color = VantaBorder.copy(alpha = 0.5f))
}

@Composable
private fun AccentButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(47.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VantaAccent, contentColor = Color(0xFF0B2816))
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun NavItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(13.dp))
            .background(if (selected) VantaAccentDark else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) VantaAccent else VantaMuted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(11.dp))
        Text(title, color = if (selected) VantaText else VantaMuted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun TopAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, description, tint = VantaMuted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SmallIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(37.dp)) {
        Icon(icon, description, tint = VantaMuted, modifier = Modifier.size(18.dp))
    }
}
