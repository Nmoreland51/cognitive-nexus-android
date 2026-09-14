package com.nmoreland.cognitivenexus.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nmoreland.cognitivenexus.data.readableError
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

@Composable internal fun Field(label: String, value: String, change: (String) -> Unit, multiline: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, singleLine = !multiline,
        minLines = if (multiline) 3 else 1, maxLines = if (multiline) 12 else 1, modifier = Modifier.fillMaxWidth())
}
@Composable internal fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f)); Switch(checked = value, onCheckedChange = change)
    }
}
@Composable internal fun Picker(label: String, value: String, choices: List<String>, change: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${value.ifBlank { "Automatic" }}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 340.dp)) {
            choices.distinct().forEach { item -> DropdownMenuItem(text = { Text(item.ifBlank { "Automatic" }) }, onClick = { change(item); open = false }) }
        }
    }
}

@Composable internal fun ChatScreen(state: NexusState, vm: NexusViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }
    val op = state.operations["chat"]
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.selectSession() }, enabled = state.connected && op?.busy != true) { Text("New chat") }
            val context = LocalContext.current
            TextButton(onClick = {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.messages.joinToString("\n\n") { "${it.role}: ${it.content}" })
                }, "Export conversation"))
            }, enabled = state.messages.isNotEmpty()) { Text("Export") }
        }
        if (state.sessions.isNotEmpty()) {
            var show by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { show = true }, enabled = op?.busy != true) { Text("Saved conversations (${state.sessions.size})") }
                DropdownMenu(expanded = show, onDismissRequest = { show = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                    state.sessions.forEach { session -> DropdownMenuItem(text = { Text(session.title ?: "Conversation") }, onClick = { vm.selectSession(session.id); show = false }) }
                }
            }
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.messages.isEmpty()) item {
                Text("Start a real conversation", style = MaterialTheme.typography.headlineSmall)
                Text("Choose your backend and model in Settings. Replies come from the original Cognitive Nexus engine; unavailable models produce errors, not demo replies.")
            }
            items(state.messages, key = { it.id }) { message ->
                val user = message.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (user) Coral.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(.94f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (user) "You" else "Cognitive Nexus", style = MaterialTheme.typography.labelSmall)
                            androidx.compose.foundation.text.selection.SelectionContainer { Text(message.content) }
                        }
                    }
                }
            }
        }
        Operation("chat", state, vm, showResult = false)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Ask Cognitive Nexus") }, maxLines = 5, modifier = Modifier.weight(1f))
            Button(onClick = { vm.run("chat", "chat", draft.trim()) }, enabled = state.connected && op?.busy != true && draft.isNotBlank()) { Text("Send") }
        }
        // Preserve the draft on failures; clear only after the saved reply is received.
        LaunchedEffect(op?.status) { if (op?.status == "Completed: chat") draft = "" }
    }
}

@Composable internal fun SettingsScreen(state: NexusState, vm: NexusViewModel) {
    var url by rememberSaveable(state.backendUrl) { mutableStateOf(state.backendUrl) }
    var token by remember { mutableStateOf("") }
    val options = state.options
    Page("Settings") {
        Text("Connect to the Mobile API v2 adapter—not Streamlit port 8501 or the old demo server.")
        Field("Backend URL", url, { url = it })
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Backend access token") },
            supportingText = { Text("Blank keeps the saved token only for the same URL.") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.saveConnection(url, token); token = "" }) { Text("Save and connect") }
        if (state.settingsMessage.isNotEmpty()) Text(state.settingsMessage)
        Text("10.0.2.2:8000 works only on an Android emulator. A physical phone needs your computer's reachable LAN IP or HTTPS address. Keep the backend running and use the same Wi-Fi/VPN. HTTP debug traffic is not encrypted; use HTTPS in production.")
        HorizontalDivider()
        Text("Models and chat behavior", style = MaterialTheme.typography.titleLarge)
        val providers = (state.operations["overview"]?.result?.get("providers") as? JsonArray).orEmpty()
        val providerNames = providers.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.content }
        Picker("Provider", options.provider, listOf("auto") + providerNames) { vm.updateOptions(options.copy(provider = it, model = "")) }
        val models = providers.filter { options.provider == "auto" || it.jsonObject["name"]?.jsonPrimitive?.content == options.provider }
            .flatMap { (it.jsonObject["models"] as? JsonArray).orEmpty().map { m -> m.jsonPrimitive.content } }
        Picker("Model", options.model, listOf("") + models) { vm.updateOptions(options.copy(model = it)) }
        Text("Model lists are detected/configured, not an inference guarantee. Provider API keys stay on the server. Selecting a cloud provider may incur its usage charges.")
        Toggle("Auto Precision", options.auto_precision_mode) { vm.updateOptions(options.copy(auto_precision_mode = it)) }
        Toggle("Use adaptive memory", options.use_memory) { vm.updateOptions(options.copy(use_memory = it)) }
        Toggle("Use knowledge in chat", options.use_knowledge_for_chat) { vm.updateOptions(options.copy(use_knowledge_for_chat = it)) }
        Toggle("Allow web research in chat", options.use_web_for_chat) { vm.updateOptions(options.copy(use_web_for_chat = it)) }
        HorizontalDivider()
        Text("Original engine persona", style = MaterialTheme.typography.titleLarge)
        Text("These changes affect the shared backend persona, including the desktop app.")
        Button(onClick = { vm.run("profile", "profile") }, enabled = state.connected && state.operations["profile"]?.busy != true) { Text("Load persona") }
        val profile = state.operations["profile"]?.result
        if (profile != null) {
            var fields by remember(profile) { mutableStateOf(profile.mapValues { it.value.jsonPrimitive.content }) }
            fields.forEach { (key, value) -> Field(label(key), value, { fields = fields + (key to it) }, multiline = key !in listOf("user_name", "assistant_name")) }
            Button(onClick = { vm.run("profile", "save_profile", JsonObject(fields.mapValues { JsonPrimitive(it.value) }).toString()) }, enabled = state.operations["profile"]?.busy != true) { Text("Save shared persona") }
        }
        Operation("profile", state, vm, showResult = false)
    }
}

@Composable internal fun ResearchScreen(route: String, state: NexusState, vm: NexusViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val opts = state.options
    Page(routes.getValue(route)) {
        Field("Research question", query, { query = it }, multiline = true)
        Picker("Depth", opts.depth, listOf("Quick", "Standard", "Deep")) { vm.updateOptions(opts.copy(depth = it)) }
        Picker("Maximum sources", opts.max_results.toString(), listOf("3", "5", "10", "25")) { vm.updateOptions(opts.copy(max_results = it.toInt())) }
        Toggle("Follow links", opts.follow_links) { vm.updateOptions(opts.copy(follow_links = it)) }
        Toggle("Use LLM summary", opts.summarize) { vm.updateOptions(opts.copy(summarize = it)) }
        Toggle("Save research to knowledge memory", opts.save_to_memory) { vm.updateOptions(opts.copy(save_to_memory = it)) }
        Text("Research contacts external websites. Without LLM summary, results are source extracts and engine heuristics—not an AI-authored answer.")
        Button(onClick = { vm.run(route, route, query) }, enabled = state.connected && query.isNotBlank() && state.operations[route]?.busy != true) { Text("Run research") }
        if (route == "web") OutlinedButton(onClick = { vm.run(route, "bloodhound", query) }, enabled = state.connected && query.isNotBlank() && state.operations[route]?.busy != true) { Text("Run Bloodhound (requires LLM)") }
        Operation(route, state, vm)
    }
}

@Composable internal fun KnowledgeScreen(state: NexusState, vm: NexusViewModel) {
    var url by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> selected = uri?.toString() }
    val enabled = state.connected && state.operations["knowledge"]?.busy != true && !reading
    Page("Files / Knowledge") {
        Field("Public website URL", url, { url = it })
        Button(onClick = { vm.run("knowledge", "ingest_url", url) }, enabled = enabled && url.isNotBlank()) { Text("Ingest website") }
        HorizontalDivider()
        Text("Upload UTF-8 text, Markdown, JSON or CSV (up to 1 MB). Files are sent to your backend's knowledge store.")
        OutlinedButton(onClick = { picker.launch(arrayOf("text/*", "application/json", "application/octet-stream")) }, enabled = !reading) { Text(if (selected == null) "Choose a file" else "Change selected file") }
        if (selected != null) Text("File selected. Tap Upload to send it to the backend.")
        Button(onClick = {
            scope.launch {
                reading = true
                try {
                    val (name, text) = withContext(Dispatchers.IO) {
                        val uri = Uri.parse(selected!!)
                        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                            if (c.moveToFirst()) c.getString(0) else null
                        } ?: "upload.txt"
                        require(name.substringAfterLast('.', "").lowercase() in listOf("txt", "md", "json", "csv")) { "Choose .txt, .md, .json or .csv." }
                        val output = ByteArrayOutputStream()
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = stream.read(buffer); if (n < 0) break
                                require(output.size() + n <= 1_000_000) { "File exceeds 1 MB." }; output.write(buffer, 0, n)
                            }
                        } ?: error("The selected file cannot be opened. Select it again.")
                        name to Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString()
                    }
                    vm.run("knowledge", "ingest", text, name)
                } catch (e: Exception) { if (e is CancellationException) throw e; vm.inputError("knowledge", readableError(e)) }
                finally { reading = false }
            }
        }, enabled = enabled && selected != null) { Text(if (reading) "Reading file…" else "Upload selected file") }
        HorizontalDivider()
        Text("Markdown knowledge note", style = MaterialTheme.typography.titleLarge)
        Field("Title", title, { title = it }); Field("Tags", tags, { tags = it }); Field("Note", body, { body = it }, true)
        Toggle("Add note to retrieval store", state.options.save_to_memory) { vm.updateOptions(state.options.copy(save_to_memory = it)) }
        Button(onClick = { vm.run("knowledge", "note", body, title, tags) }, enabled = enabled && title.isNotBlank() && body.isNotBlank()) { Text("Save note") }
        OutlinedButton(onClick = { vm.run("knowledge", "notes") }, enabled = enabled) { Text("List saved notes") }
        HorizontalDivider()
        Field("Ask local knowledge", query, { query = it }, true)
        Toggle("Use LLM synthesis for knowledge", state.options.knowledge_use_ai) { vm.updateOptions(state.options.copy(knowledge_use_ai = it)) }
        Button(onClick = { vm.run("knowledge", "knowledge", query) }, enabled = enabled && query.isNotBlank()) { Text("Query knowledge") }
        Operation("knowledge", state, vm)
    }
}

@Composable internal fun MemoryScreen(state: NexusState, vm: NexusViewModel) {
    var fact by rememberSaveable { mutableStateOf("") }
    var confirmForget by remember { mutableStateOf(false) }
    val enabled = state.connected && state.operations["memory"]?.busy != true
    Page("Memory") {
        Text("These are the original engine's shared saved facts and adaptive memory status—not a separate phone-only memory.")
        Button(onClick = { vm.run("memory", "memory") }, enabled = enabled) { Text("Load memory") }
        Field("Fact to remember / text to match for forgetting", fact, { fact = it }, true)
        Button(onClick = { vm.run("memory", "remember", fact) }, enabled = enabled && fact.isNotBlank()) { Text("Remember this fact") }
        OutlinedButton(onClick = { confirmForget = true }, enabled = enabled && fact.isNotBlank()) { Text("Forget matching facts…") }
        Operation("memory", state, vm)
    }
    if (confirmForget) AlertDialog(onDismissRequest = { confirmForget = false }, title = { Text("Forget shared facts?") },
        text = { Text("The original engine will remove facts matching: $fact. This affects desktop and mobile memory.") },
        confirmButton = { TextButton(onClick = { confirmForget = false; vm.run("memory", "forget", fact) }) { Text("Forget") } },
        dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Cancel") } })
}

@Composable internal fun ImagesScreen(state: NexusState, vm: NexusViewModel) {
    var prompt by rememberSaveable { mutableStateOf("") }
    var workflow by rememberSaveable { mutableStateOf("") }
    val opts = state.options
    val enabled = state.connected && state.operations["images"]?.busy != true
    Page("Image Generation") {
        Text("Uses Automatic1111, local Diffusers, or a saved ComfyUI workflow on the server. Missing image providers produce a clear error—never a placeholder image.")
        Field("Prompt", prompt, { prompt = it }, true)
        Field("Negative prompt", opts.negative_prompt, { vm.updateOptions(opts.copy(negative_prompt = it)) })
        Picker("Image provider", opts.image_provider, listOf("auto", "automatic1111", "diffusers_local")) { vm.updateOptions(opts.copy(image_provider = it)) }
        val styles = (state.operations["overview"]?.result?.get("image_styles") as? JsonArray)?.map { it.jsonPrimitive.content } ?: listOf("realistic", "cinematic", "anime", "digital_art", "none")
        Picker("Style", opts.style, styles) { vm.updateOptions(opts.copy(style = it)) }
        Picker("Width", opts.width.toString(), listOf("256", "512", "768", "1024")) { vm.updateOptions(opts.copy(width = it.toInt())) }
        Picker("Height", opts.height.toString(), listOf("256", "512", "768", "1024")) { vm.updateOptions(opts.copy(height = it.toInt())) }
        Picker("Steps", opts.steps.toString(), listOf("10", "20", "25", "30", "50")) { vm.updateOptions(opts.copy(steps = it.toInt())) }
        Picker("Number of images", opts.num_images.toString(), listOf("1", "2", "3", "4")) { vm.updateOptions(opts.copy(num_images = it.toInt())) }
        Button(onClick = { vm.run("images", "images", prompt) }, enabled = enabled && prompt.isNotBlank()) { Text("Generate images") }
        HorizontalDivider()
        Text("ComfyUI workflows", style = MaterialTheme.typography.titleLarge)
        Text("Save a trusted API-format workflow on the backend first. This app does not upload or execute arbitrary server code.")
        OutlinedButton(onClick = { vm.run("workflows", "workflows") }, enabled = state.connected && state.operations["workflows"]?.busy != true) { Text("Load saved workflows") }
        val workflows = (state.operations["workflows"]?.result?.get("workflows") as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
        Picker("Workflow", workflow, workflows) { workflow = it }
        Button(onClick = { vm.run("images", "comfyui", prompt, workflow) }, enabled = enabled && prompt.isNotBlank() && workflow.isNotBlank()) { Text("Run saved workflow") }
        Operation("workflows", state, vm, showResult = false)
        Operation("images", state, vm, showResult = false)
        state.operations["images"]?.result?.let { result -> GalleryItems(result, vm) }
    }
}

@Composable internal fun GalleryScreen(state: NexusState, vm: NexusViewModel) {
    Page("Gallery") {
        Button(onClick = { vm.run("gallery", "gallery") }, enabled = state.connected && state.operations["gallery"]?.busy != true) { Text("Load saved images") }
        Operation("gallery", state, vm, showResult = false)
        state.operations["gallery"]?.result?.let { GalleryItems(it, vm) }
    }
}

@Composable private fun GalleryItems(result: JsonObject, vm: NexusViewModel) {
    val images = (result["images"] as? JsonArray).orEmpty()
    var selected by remember { mutableStateOf<String?>(null) }
    if (images.isEmpty()) Text("No saved images were returned by this backend.")
    images.forEach { element ->
        val item = element.jsonObject
        val id = item["id"]?.jsonPrimitive?.content ?: return@forEach
        TextButton(onClick = { selected = if (selected == id) null else id }) { Text(item["name"]?.jsonPrimitive?.content ?: "Image") }
        if (selected == id) {
            var bitmap by remember(id) { mutableStateOf<android.graphics.Bitmap?>(null) }
            var error by remember(id) { mutableStateOf<String?>(null) }
            var saved by remember(id) { mutableStateOf("") }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
                if (uri != null) scope.launch {
                    saved = "Saving…"
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { vm.repository.saveImage(id, it) }
                                ?: throw IllegalStateException("Could not open the selected destination.")
                        }
                        saved = "Image saved to your selected location."
                    } catch (e: Exception) { if (e is CancellationException) throw e; saved = readableError(e) }
                }
            }
            LaunchedEffect(id) {
                try { bitmap = vm.repository.image(id) } catch (e: Exception) { if (e is CancellationException) throw e; error = readableError(e) }
            }
            if (bitmap == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            bitmap?.let { Image(it.asImageBitmap(), item["prompt"]?.jsonPrimitive?.content ?: "Generated image", Modifier.fillMaxWidth().heightIn(max = 500.dp)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(item["prompt"]?.jsonPrimitive?.content.orEmpty())
            Text("Provider: ${item["provider"]?.jsonPrimitive?.content.orEmpty()}")
            OutlinedButton(onClick = { save.launch(item["name"]?.jsonPrimitive?.content ?: "cognitive-nexus.png") }, enabled = saved != "Saving…") { Text("Save image to phone") }
            if (saved.isNotBlank()) Text(saved)
        }
    }
}
