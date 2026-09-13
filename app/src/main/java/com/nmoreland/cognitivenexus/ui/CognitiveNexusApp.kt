package com.nmoreland.cognitivenexus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nmoreland.cognitivenexus.data.BackendSettingsRepository
import kotlinx.coroutines.launch

private val NexusInk = Color(0xFF262E3D)
private val NexusCoral = Color(0xFFFF4B4B)
private val NexusMist = Color(0xFFF5F6FA)
private val NexusMint = Color(0xFFE1F3EB)
private val NexusMuted = Color(0xFF6F7480)

private data class NexusTab(val route: String, val label: String)

private val nexusTabs = listOf(
    NexusTab("overview", "Home / Overview"), NexusTab("chat", "Chat"),
    NexusTab("research", "Reality-First Research"), NexusTab("web", "Web Research"),
    NexusTab("knowledge", "Files / Knowledge"), NexusTab("memory", "Memory"),
    NexusTab("images", "Image Generation"), NexusTab("gallery", "Gallery"),
    NexusTab("diagnostics", "Diagnostics"), NexusTab("settings", "Settings"),
    NexusTab("tools", "Tools / Utilities"),
)

@Composable
fun CognitiveNexusApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { BackendSettingsRepository(context) }
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(settings))
    val chatState by chatViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val openRoute: (Int) -> Unit = { index ->
        selectedTab = index
        navController.navigate(nexusTabs[index].route)
    }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = NexusCoral, onPrimary = Color.White, background = Color.White, surface = Color.White, surfaceVariant = NexusMist, onSurface = NexusInk, onBackground = NexusInk)) {
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
            NexusDrawer(chatState, { scope.launch { drawerState.close() } }, {
                openRoute(nexusTabs.indexOfFirst { it.route == "settings" })
                scope.launch { drawerState.close() }
            }, chatViewModel::refreshBackendStatus)
        }) {
            NavHost(navController = navController, startDestination = "overview") {
                composable("overview") { NexusOverview(chatState, selectedTab, { scope.launch { drawerState.open() } }, openRoute) }
                composable("chat") { NexusChatScreen(chatState, selectedTab, { scope.launch { drawerState.open() } }, openRoute, chatViewModel::updateDraft, chatViewModel::send) }
                composable("settings") {
                    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settings))
                    NexusSettingsScreen(settingsViewModel.state.collectAsStateWithLifecycle().value, settingsViewModel::save) { openRoute(0) }
                }
                nexusTabs.filter { it.route !in setOf("overview", "chat", "settings") }.forEach { tab ->
                    composable(tab.route) { NexusFeatureScreen(tab, selectedTab, { scope.launch { drawerState.open() } }, openRoute) }
                }
            }
        }
    }
}

@Composable
private fun NexusDrawer(state: ChatUiState, onClose: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = NexusMist) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = onClose) { Icon(Icons.Default.ChevronLeft, "Close control center") } }
            Text("Cognitive Nexus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Surface(color = if (state.backendStatus == "Connected") NexusMint else Color(0xFFFFEEE9), shape = RoundedCornerShape(8.dp)) {
                Text(if (state.backendStatus == "Connected") "Backend available" else state.backendStatus, Modifier.padding(14.dp), color = if (state.backendStatus == "Connected") Color(0xFF287A54) else NexusInk)
            }
            Text("Endpoint: ${state.backendUrl}", style = MaterialTheme.typography.bodySmall, color = NexusMuted)
            DrawerSelect("Throughput profile", "Fast")
            DrawerSelect("Chat model", state.activeModel ?: "Backend required")
            Text("Answer Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Auto Precision Mode", style = MaterialTheme.typography.bodyLarge)
            Text("Current answer mode: Auto Precision ready", color = NexusMuted, style = MaterialTheme.typography.bodySmall)
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DrawerMetric("Knowledge chunks", "0"); DrawerMetric("Images", "6")
            TextButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Refresh app") }
            TextButton(onClick = onSettings) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(6.dp)); Text("Settings") }
        }
    }
}

@Composable private fun DrawerSelect(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = NexusMuted); Surface(color = Color.White, shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth()) { Text(value, Modifier.padding(14.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun DrawerMetric(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = NexusMuted); Text(value, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun NexusOverview(state: ChatUiState, selectedTab: Int, onDrawer: () -> Unit, onTabSelected: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        NexusHeader(onDrawer, selectedTab, onTabSelected)
        Text("Home / Overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))
        Text("A compact control-room view of the working Cognitive Nexus engine.", color = NexusMuted, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
        MetricGrid(listOf("App" to "Online", "Backend" to state.backendStatus, "Active provider" to if (state.backendStatus == "Connected") "Ollama" else "Not connected", "Model" to (state.activeModel ?: "—"), "Demo safety" to "Off"))
        SectionTitle("Control Center")
        Capability("Chat and routing", "Ask questions, route tasks, use local providers, and keep fallback behavior visible.")
        Capability("Reality-first research", "Search sources, extract claims, score trust, flag contradictions, and save reports.")
        Capability("Memory and knowledge", "Use local facts, uploaded knowledge, Markdown notes, and retrieval summaries.")
        SectionTitle("System Snapshot")
        MetricGrid(listOf("Chat messages" to state.messages.size.toString(), "Local facts" to "0", "Knowledge chunks" to "0", "Reports" to "0", "Web sessions" to "0", "Images" to "6"))
        SectionTitle("Quick Actions")
        QuickAction("Open Chat", "Normal work and code help"); QuickAction("Reality-First Research", "Sourced answers with claims and trust scoring"); QuickAction("Files / Knowledge", "Ingest notes or URLs")
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun NexusHeader(onDrawer: () -> Unit, selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onDrawer) { Icon(Icons.Default.MenuOpen, "Open provider control center") }; Spacer(Modifier.weight(1f)); Text("CN", fontWeight = FontWeight.Black, color = NexusCoral) }
    Text("Cognitive Nexus", fontSize = 39.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, color = NexusInk, modifier = Modifier.padding(top = 22.dp))
    Text("Local-first AI research control center for chat, memory, web research, Reality-First reports, diagnostics, and local providers.", color = NexusMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp))
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp, containerColor = Color.White, contentColor = NexusCoral, divider = { HorizontalDivider(color = Color(0xFFE8E9ED)) }) {
        nexusTabs.forEachIndexed { index, tab -> Tab(selected = index == selectedTab, onClick = { onTabSelected(index) }, text = { Text(tab.label, maxLines = 1) }) }
    }
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { metrics.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (label, value) -> Surface(Modifier.weight(1f), color = NexusMist, shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(14.dp)) { Text(label, color = NexusMuted, style = MaterialTheme.typography.labelMedium); Text(value, color = NexusInk, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }
@Composable private fun SectionTitle(title: String) = Text(title, style = MaterialTheme.typography.titleLarge, color = NexusInk, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 25.dp, bottom = 10.dp))
@Composable private fun Capability(title: String, body: String) { Surface(color = NexusMist, shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = NexusMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) } } }
@Composable private fun QuickAction(title: String, detail: String) { Row(Modifier.fillMaxWidth().clickable { }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Surface(color = NexusCoral, shape = RoundedCornerShape(7.dp), modifier = Modifier.size(8.dp)) {}; Spacer(Modifier.width(10.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, color = NexusMuted, style = MaterialTheme.typography.bodySmall) } } }

@Composable
private fun NexusFeatureScreen(tab: NexusTab, selectedTab: Int, onDrawer: () -> Unit, onTabSelected: (Int) -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) { NexusHeader(onDrawer, selectedTab, onTabSelected); Text(tab.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp)); Text("This native Android workspace is ready for the verified Cognitive Nexus backend connection.", color = NexusMuted, modifier = Modifier.padding(top = 8.dp)); Surface(color = NexusMist, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Backend-powered content appears here once the configured service is reachable.", Modifier.padding(18.dp), color = NexusInk) } } }

@Composable
private fun NexusChatScreen(state: ChatUiState, selectedTab: Int, onDrawer: () -> Unit, onTabSelected: (Int) -> Unit, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        NexusHeader(onDrawer, selectedTab, onTabSelected); Text("Chat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        if (state.messages.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Ask Cognitive Nexus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("Real replies appear only after the configured backend responds.", color = NexusMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)) } }
        else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { state.messages.forEach { MessageBubble(it) } }
        state.error?.let { ErrorState(it) }; if (state.isSending) Text("Waiting for the backend…", color = NexusMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = state.draft, onValueChange = onDraftChange, enabled = !state.isSending, label = { Text("Ask Cognitive Nexus") }, modifier = Modifier.weight(1f), maxLines = 4); IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.isSending) { Icon(Icons.AutoMirrored.Filled.Send, "Send") } }
    }
}

@Composable private fun MessageBubble(message: ChatMessage) { Box(Modifier.fillMaxWidth(), contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart) { Surface(color = if (message.isUser) Color(0xFFFFECEC) else NexusMist, shape = RoundedCornerShape(14.dp)) { Text(message.text, Modifier.padding(13.dp), color = NexusInk) } } }
@Composable private fun ErrorState(message: String) { Surface(color = Color(0xFFFFE9E7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text(message, Modifier.padding(12.dp), color = Color(0xFF8C1D18)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusSettingsScreen(state: SettingsUiState, onSave: (String) -> Unit, onBack: () -> Unit) {
    var editUrl by remember { mutableStateOf("") }
    LaunchedEffect(state.backendUrl) { if (editUrl.isEmpty() || state.saved) editUrl = state.backendUrl }
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Backend connection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Button(onClick = { onSave(editUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }; state.error?.let { ErrorState(it) }; if (state.saved) Text("Saved.", color = Color(0xFF287A54)); Text("Default emulator URL: http://10.0.2.2:8000/", fontWeight = FontWeight.SemiBold); Text("This works only on an Android emulator. A physical phone needs a reachable computer LAN IP or HTTPS URL; never use localhost.", color = NexusMuted); Text("Production must use HTTPS. Release builds do not allow cleartext HTTP connections.", color = NexusMuted, style = MaterialTheme.typography.bodySmall) }
    }
}
