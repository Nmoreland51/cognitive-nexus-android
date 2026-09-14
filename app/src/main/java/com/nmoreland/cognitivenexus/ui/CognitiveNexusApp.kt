package com.nmoreland.cognitivenexus.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.nmoreland.cognitivenexus.BuildConfig
import com.nmoreland.cognitivenexus.data.BackendSettingsRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

internal val Coral = Color(0xFFFF4B4B)
internal val Ink = Color(0xFF262E3D)
internal val Mist = Color(0xFFF5F6FA)
private val Night = Color(0xFF101318)
private val NightSurface = Color(0xFF1A1F29)
private val NightVariant = Color(0xFF252C38)
internal val routes = linkedMapOf("overview" to "Home / Overview", "chat" to "Chat",
    "research" to "Reality-First Research", "web" to "Web Research", "knowledge" to "Files / Knowledge",
    "memory" to "Memory", "images" to "Image Generation", "gallery" to "Gallery",
    "diagnostics" to "Diagnostics", "settings" to "Settings", "tools" to "Tools / Utilities")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CognitiveNexusApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { BackendSettingsRepository(context) }
    val vm: NexusViewModel = viewModel(factory = NexusViewModel.factory(settings))
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route ?: "overview"
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var appearanceMenuOpen by rememberSaveable { mutableStateOf(false) }
    val navigate: (String) -> Unit = { route ->
        nav.navigate(route) { launchSingleTop = true; popUpTo(nav.graph.startDestinationId) { saveState = true }; restoreState = true }
        scope.launch { drawer.close() }
    }
    val colors = if (state.darkTheme) darkColorScheme(
        primary = Coral, background = Night, surface = NightSurface, surfaceVariant = NightVariant,
        onSurface = Color(0xFFE4E8F0), onBackground = Color(0xFFE4E8F0), onSurfaceVariant = Color(0xFFC5CBD7),
    ) else lightColorScheme(primary = Coral, background = Color.White, surface = Color.White,
        onSurface = Ink, onBackground = Ink, surfaceVariant = Mist)
    MaterialTheme(colorScheme = colors) {
        ModalNavigationDrawer(drawerState = drawer, drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Cognitive Nexus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Native client · Mobile API v2", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(state.connectionStatus, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    routes.forEach { (route, label) -> NavigationDrawerItem(label = { Text(label) }, selected = route == current, onClick = { navigate(route) }) }
                }
            }
        }) {
            Scaffold(topBar = {
                TopAppBar(title = { Text("Cognitive Nexus", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Open navigation") } },
                    actions = {
                        IconButton(onClick = vm::refreshConnection) { Icon(Icons.Default.Refresh, "Check backend") }
                        Box {
                            IconButton(onClick = { appearanceMenuOpen = true }) { Icon(Icons.Default.Menu, "Appearance menu") }
                            DropdownMenu(expanded = appearanceMenuOpen, onDismissRequest = { appearanceMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (state.darkTheme) "Use light mode" else "Use dark mode") },
                                    onClick = { vm.setDarkTheme(!state.darkTheme); appearanceMenuOpen = false },
                                )
                            }
                        }
                    })
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
                    ScrollableTabRow(selectedTabIndex = routes.keys.indexOf(current).coerceAtLeast(0), edgePadding = 12.dp) {
                        routes.forEach { (route, title) -> Tab(selected = current == route, onClick = { navigate(route) }, text = { Text(title) }) }
                    }
                    if (!state.connected) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(state.connectionStatus, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { navigate("settings") }) { Text("Configure backend connection") }
                            }
                        }
                    }
                    NavHost(navController = nav, startDestination = "overview", modifier = Modifier.weight(1f)) {
                        composable("overview") { Overview(state, vm, navigate) }
                        composable("chat") { ChatScreen(state, vm) }
                        composable("settings") { SettingsScreen(state, vm) }
                        composable("research") { ResearchScreen("research", state, vm) }
                        composable("web") { ResearchScreen("web", state, vm) }
                        composable("knowledge") { KnowledgeScreen(state, vm) }
                        composable("memory") { MemoryScreen(state, vm) }
                        composable("images") { ImagesScreen(state, vm) }
                        composable("gallery") { GalleryScreen(state, vm) }
                        composable("diagnostics") { ReadScreen("diagnostics", state, vm, "Refresh diagnostics") }
                        composable("tools") { ReadScreen("tools", state, vm, "Load tools inventory") }
                    }
                }
            }
        }
    }
}

@Composable internal fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun Overview(state: NexusState, vm: NexusViewModel, navigate: (String) -> Unit) {
    Page("Cognitive Nexus") {
        Text("One workspace for conversation, research, knowledge, memory and images.")
        Text("Native + real backend · ${BuildConfig.VERSION_NAME}\nBuild ${BuildConfig.SOURCE_REVISION}", color = Coral)
        val data = state.operations["overview"]?.result
        val counts = data?.get("counts") as? JsonObject
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.connectionStatus, fontWeight = FontWeight.Bold)
                Text("LLMs and tools run on your backend. No phone-side model or invented offline replies.")
                counts?.forEach { (name, number) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label(name)); Text(number.toString()) } }
                if (counts == null) Text("Counts unavailable until the backend returns them.")
            }
        }
        Text("Quick actions", style = MaterialTheme.typography.titleLarge)
        listOf("chat", "research", "knowledge", "images").forEach { route ->
            OutlinedButton(onClick = { navigate(route) }, modifier = Modifier.fillMaxWidth()) { Text(routes.getValue(route)) }
        }
        Button(onClick = { vm.run("overview", "overview") }, enabled = state.connected && state.operations["overview"]?.busy != true) { Text("Refresh live status") }
        Operation("overview", state, vm)
    }
}

@Composable internal fun ReadScreen(route: String, state: NexusState, vm: NexusViewModel, button: String) {
    Page(routes.getValue(route)) {
        Text(if (route == "diagnostics") "Live detected providers and last inference details. Availability is not proof a model can complete a response. Secrets and raw server logs are not exposed."
            else "The original app lists available tools. This screen does the same; it does not expose remote shell execution.")
        Button(onClick = { vm.run(route, route) }, enabled = state.connected && state.operations[route]?.busy != true) { Text(button) }
        Operation(route, state, vm)
    }
}

@Composable internal fun Operation(route: String, state: NexusState, vm: NexusViewModel, showResult: Boolean = true) {
    val op = state.operations[route] ?: return
    if (op.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (op.status.isNotEmpty()) Text(op.status, style = MaterialTheme.typography.labelMedium)
    op.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (!op.busy && op.pendingId != null) TextButton(onClick = { vm.resume(route, op.pendingId) }) { Text("Check saved task (does not run it again)") }
    if (showResult) op.result?.let { result ->
        val context = LocalContext.current
        TextButton(onClick = {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, humanText(result)) }, "Share result"))
        }) { Text("Share / export result") }
        ResultTree(result)
    }
}

internal fun label(key: String) = key.replace('_', ' ').replaceFirstChar { it.titlecase() }
internal fun humanText(value: JsonElement): String = when (value) {
    is JsonObject -> value.entries.joinToString("\n\n") { "${label(it.key)}\n${humanText(it.value)}" }
    is JsonArray -> value.joinToString("\n\n", transform = ::humanText)
    is JsonPrimitive -> value.content
}

@Composable internal fun ResultTree(value: JsonElement, depth: Int = 0) {
    when (value) {
        is JsonObject -> value.forEach { (key, child) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label(key), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (depth >= 1 && (child is JsonObject || child is JsonArray)) {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide details" else "Show details") }
                    if (expanded) ResultTree(child, depth + 1)
                } else ResultTree(child, depth + 1)
            }
        }
        is JsonArray -> {
            var count by remember { mutableIntStateOf(10) }
            if (value.isEmpty()) Text("None", style = MaterialTheme.typography.bodySmall)
            value.take(count).forEach { child ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.padding(12.dp)) { ResultTree(child, depth + 1) }
                }
            }
            if (value.size > count) TextButton(onClick = { count += 10 }) { Text("Show more (${value.size - count} remaining)") }
        }
        is JsonPrimitive -> {
            val text = if (value is JsonNull) "Not available" else value.content
            val uri = androidx.compose.ui.platform.LocalUriHandler.current
            if (text.startsWith("https://") || text.startsWith("http://")) {
                TextButton(onClick = { runCatching { uri.openUri(text) } }) { Text(text) }
            } else androidx.compose.foundation.text.selection.SelectionContainer { Text(text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
