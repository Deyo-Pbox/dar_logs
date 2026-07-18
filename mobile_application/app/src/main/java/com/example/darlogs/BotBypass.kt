package com.example.darlogs

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.*
import java.net.CookieHandler
import java.net.HttpCookie
import java.net.URI

object BotBypass {
    private const val TAG = "BotBypass"

    suspend fun solveChallenge(context: Context, baseUrl: String): Boolean = withContext(Dispatchers.Main) {
        Log.i(TAG, "🔐 Solving InfinityFree bot challenge...")
        val deferred = CompletableDeferred<Boolean>()

        val webView = WebView(context.applicationContext).apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            val wvCookieManager = CookieManager.getInstance()
            wvCookieManager.setAcceptCookie(true)
            wvCookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "   Page finished: ${url?.take(100)}")
                    if (url != null && !url.contains("aes.js") && url != "$baseUrl/") {
                        Log.i(TAG, "✅ Challenge solved, landed: ${url.take(80)}")
                        deferred.complete(true)
                    }
                }
            }
        }

        try {
            withTimeoutOrNull(10000) {
                webView.loadUrl(baseUrl)
                deferred.await()
            } ?: run {
                Log.w(TAG, "⚠️ Challenge timed out")
                deferred.complete(false)
            }
        } finally {
            syncWebViewCookiesToJava(baseUrl)
            webView.destroy()
        }

        deferred.await()
    }

    private fun syncWebViewCookiesToJava(url: String) {
        try {
            val wvCookies = CookieManager.getInstance().getCookie(url) ?: ""
            if (wvCookies.isEmpty()) {
                Log.w(TAG, "❌ No WebView cookies to sync")
                return
            }

            val javaHandler = CookieHandler.getDefault()
            val uri = URI(url)

            val cookies = wvCookies.split(";")
                .mapNotNull { part ->
                    val pair = part.trim().split("=", limit = 2)
                    if (pair.size == 2) {
                        HttpCookie(pair[0], pair[1]).also {
                            it.domain = uri.host
                            it.path = "/"
                            it.version = 0
                        }
                    } else null
                }

            // Add to Java CookieManager via the CookieStore
            val jar = (javaHandler as? java.net.CookieManager)?.cookieStore
            cookies.forEach { cookie ->
                jar?.add(uri, cookie)
                Log.d(TAG, "   🍪 Synced: ${cookie.name}=${cookie.value.take(20)}...")
            }
            Log.i(TAG, "🎫 Synced ${cookies.size} cookies to Java CookieManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync cookies", e)
        }
    }
}
