package com.nmoreland.cognitivenexus

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nmoreland.cognitivenexus.ui.CognitiveNexusApp

private const val LIVE_ORIGIN = "https://nmoreland51.github.io"
private const val LIVE_PATH = "/cognitive-nexus-android/"
private const val LIVE_URL = "$LIVE_ORIGIN$LIVE_PATH"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiveWorkspace() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveWorkspace() {
    val context = LocalContext.current
    var nativeMode by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = !nativeMode && canGoBack) {
        webView?.goBack()
    }

    if (nativeMode) {
        Column(Modifier.fillMaxSize()) {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cognitive Nexus · Native", modifier = Modifier.padding(10.dp))
                    TextButton(onClick = { nativeMode = false }) { Text("Live mode") }
                }
            }
            Box(Modifier.weight(1f)) { CognitiveNexusApp() }
        }
    } else {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Scaffold(topBar = {
                TopAppBar(
                    title = { Text("Cognitive Nexus · Live") },
                    actions = {
                        TextButton(onClick = { webView?.reload() }) { Text("Refresh") }
                        TextButton(onClick = { nativeMode = true }) { Text("Native") }
                    }
                )
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    if (failed) {
                        Surface(tonalElevation = 2.dp) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("GitHub Pages is unavailable or not yet enabled.")
                                Text("The native application is still accessible.", style = MaterialTheme.typography.bodySmall)
                                Row {
                                    TextButton(onClick = { failed = false; webView?.reload() }) { Text("Retry live") }
                                    TextButton(onClick = { nativeMode = true }) { Text("Open native") }
                                }
                            }
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.setSupportMultipleWindows(false)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val uri = request.url
                                        val trusted = uri.scheme == "https" &&
                                            uri.host == "nmoreland51.github.io" &&
                                            (uri.path == LIVE_PATH.removeSuffix("/") || uri.path?.startsWith(LIVE_PATH) == true)
                                        if (trusted) return false
                                        if (request.isForMainFrame && uri.scheme in listOf("https", "http")) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                        failed = false
                                    }

                                    override fun onPageFinished(view: WebView, url: String?) {
                                        canGoBack = view.canGoBack()
                                    }

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError
                                    ) {
                                        if (request.isForMainFrame) failed = true
                                    }
                                }
                                loadUrl(LIVE_URL)
                                webView = this
                            }
                        },
                        update = { webView = it },
                        onRelease = { view ->
                            webView = null
                            view.stopLoading()
                            view.destroy()
                        }
                    )
                }
            }
        }
    }
}
