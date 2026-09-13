package com.nmoreland.cognitivenexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nmoreland.cognitivenexus.data.BackendSettingsRepository
import androidx.compose.ui.platform.LocalContext

@Composable
fun CognitiveNexusApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { BackendSettingsRepository(context) }
    val navController = rememberNavController()
    MaterialTheme {
        NavHost(navController = navController, startDestination = "chat") {
            composable("chat") {
                val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(settings))
                ChatScreen(
                    state = chatViewModel.state.collectAsStateWithLifecycle().value,
                    onDraftChange = chatViewModel::updateDraft,
                    onSend = chatViewModel::send,
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settings))
                SettingsScreen(
                    state = settingsViewModel.state.collectAsStateWithLifecycle().value,
                    onSave = settingsViewModel::save,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cognitive Nexus") },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty()) WelcomeState(state.backendUrl)
            else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { items(state.messages, key = { it.id }) { MessageBubble(it) } }
            }
            if (state.isSending) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp)); Text("Waiting for the backend…", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.error?.let { ErrorState(it) }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.draft,
                    onValueChange = onDraftChange,
                    enabled = !state.isSending,
                    label = { Text("Ask Cognitive Nexus") },
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.isSending) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun WelcomeState(backendUrl: String) {
    Box(Modifier.fillMaxWidth().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to Cognitive Nexus", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Start a conversation when your AI backend is running and reachable.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("Backend: $backendUrl", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(18.dp)) {
            Text(message.text, Modifier.padding(14.dp), color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) { Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(state: SettingsUiState, onSave: (String) -> Unit, onBack: () -> Unit) {
    var editUrl by remember { mutableStateOf("") }
    LaunchedEffect(state.backendUrl) { if (editUrl.isEmpty() || state.saved) editUrl = state.backendUrl }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Backend connection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { onSave(editUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
            state.error?.let { ErrorState(it) }
            if (state.saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)
            Text("Default emulator URL: http://10.0.2.2:8000/", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("This address works only from an Android emulator. It maps the emulator to the computer running the backend.")
            Text("A physical phone needs a reachable computer LAN IP (for development) or an HTTPS URL. Do not use localhost on a phone.")
            Text("Production must use HTTPS. Release builds do not allow cleartext HTTP connections.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
