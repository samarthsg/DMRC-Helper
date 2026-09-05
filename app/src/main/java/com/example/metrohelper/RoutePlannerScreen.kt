package com.example.metrohelper

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.annotation.SuppressLint
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable

/* -----------------------------------------------------------
   Station AutoComplete Dropdown
   - Fast filtering
   - Clear button support
   - Lightweight (no coroutine)
------------------------------------------------------------ */
@Composable
fun StationAutoComplete(
    label: String,
    stations: List<String>,
    value: String,
    onValueChange: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filtered = remember(value) {
        stations.filter {
            it.contains(value, ignoreCase = true)
        }.take(6)
    }

    Column {

        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },

            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),

            keyboardActions = KeyboardActions(
                onDone = {
                    val selectedStation = filtered.firstOrNull()

                    if (selectedStation != null) {
                        onValueChange(selectedStation)
                        expanded = false
                        focusManager.clearFocus()
                    }
                }
            ),

            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        expanded = false
                    }
                },

            singleLine = true
        )

        if (expanded && filtered.isNotEmpty()) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {

                LazyColumn {

                    items(filtered) { station ->

                        DropdownMenuItem(
                            text = { Text(station) },
                            onClick = {
                                onValueChange(station)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RoutePlannerScreen() {

    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var showWebView by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }

    /* -----------------------------------------------------------
       Converts station name to website slug format
       Example:
       "New Delhi" -> "new-delhi-delhi-metro-station"
    ------------------------------------------------------------ */
    fun buildSlug(name: String): String {

        val fixedName = when (name.trim().lowercase()) {
            "noida city centre" -> "noida-city-center"
            else -> name
                .trim()
                .lowercase()
                .replace("&", "and")
                .replace(".", "")
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "-")
                .replace("--", "-")
        }

        return "$fixedName-delhi-metro-station"
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val webView = remember {
        WebView(context).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = object : WebViewClient() {

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {

                    val url = request?.url.toString()

                    // block ad domains
                    if (url.contains("doubleclick") ||
                        url.contains("googlesyndication") ||
                        url.contains("googleads") ||
                        url.contains("adservice") ||
                        url.contains("adserver") ||
                        url.contains("ads.")
                    ) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            null
                        )
                    }

                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {

                    // remove banner ads using JS
                    view?.evaluateJavascript(
                        """
                    javascript:(function() {
                        var ads = document.querySelectorAll(
                            '[id*="ad"], .ads, iframe, .banner, .advert'
                        );
                        ads.forEach(function(ad){
                            ad.style.display="none";
                        });
                    })()
                    """.trimIndent(),
                        null
                    )
                }
            }
        }
    }
    /* -----------------------------------------------------------
       Load metro station names from JSON (background)
    ------------------------------------------------------------ */
    val stations by produceState(initialValue = emptyList<String>()) {
        value = loadMetroStations(context).map { it.name }
    }

    BackHandler(enabled = showWebView) {
        webView.stopLoading()
        webView.clearHistory()
        webView.clearCache(true)
        showWebView = false
        url = ""
    }

    if (!showWebView) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
                .padding(16.dp)
        ) {

            Text(
                text = "Plan Your Route",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            StationAutoComplete(
                label = "From Station",
                stations = stations,
                value = from,
                onValueChange = { from = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            StationAutoComplete(
                label = "To Station",
                stations = stations,
                value = to,
                onValueChange = { to = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {

                    // Route tabhi search hoga jab dono stations properly select kiye gaye hon
                    if (from !in stations || to !in stations) {
                        return@Button
                    }

                    // Same station ko From aur To dono mein select nahi kar sakte
                    if (from == to) {
                        return@Button
                    }

                    val fromSlug = buildSlug(from)
                    val toSlug = buildSlug(to)

                    url =
                        "https://delhimetrorail.info/${fromSlug}-to-${toSlug}"

                    showWebView = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search Route")
            }
        }

    } else {

        Box(Modifier.fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView },
                update = {
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    webView.stopLoading()
                    webView.clearHistory()
                    webView.clearCache(true)
                    showWebView = false
                    url = ""
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
}
