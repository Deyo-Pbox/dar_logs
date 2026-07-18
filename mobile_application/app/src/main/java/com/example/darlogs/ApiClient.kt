package com.example.darlogs

import android.os.Build
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val TAG = "ApiClient"

    @Volatile
    private var initialized = false

    @Volatile
    var authToken: String? = null

    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val manager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(manager)
            initialized = true
            Log.i(TAG, "🍪 Cookie manager initialized")
        }
    }

    fun isAuthenticated(): Boolean = authToken != null

    fun clearAuth() {
        Log.i(TAG, "🔒 Auth token cleared")
        authToken = null
    }

    fun setAuth(token: String) {
        val masked = if (token.length > 10) "${token.substring(0, 6)}...${token.takeLast(4)}" else "***"
        Log.i(TAG, "🔑 Auth token set: $masked")
        authToken = token
    }

    fun getJson(urlString: String): ApiResponse =
        request(resolveApiUrl(urlString), "GET", null)

    fun postJson(urlString: String, payload: String): ApiResponse =
        request(resolveApiUrl(urlString), "POST", payload)

    fun putJson(urlString: String, payload: String): ApiResponse =
        request(resolveApiUrl(urlString), "PUT", payload)

    fun deleteJson(urlString: String): ApiResponse =
        request(resolveApiUrl(urlString), "DELETE", null)

    fun patchJson(urlString: String, payload: String): ApiResponse =
        request(resolveApiUrl(urlString), "PATCH", payload)

    private fun resolveApiUrl(urlString: String): String {
        Log.d(TAG, "📍 Resolving URL: $urlString | Emulator: ${isEmulator()}")
        if (!isEmulator()) return urlString

        val resolved = urlString
            .replace("192.168.1.6", "10.0.2.2")
            .replace("127.0.0.1", "10.0.2.2")
            .replace("localhost", "10.0.2.2")

        if (resolved != urlString) {
            Log.i(TAG, "📱 Emulator rewrite: $urlString → $resolved")
        }
        return resolved
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.FINGERPRINT.lowercase().contains("vbox")
            || Build.FINGERPRINT.lowercase().contains("test-keys")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("sdk_gphone")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.MANUFACTURER.contains("Google") && Build.BRAND.startsWith("google")
            || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("vbox86p")
            || Build.HOST.contains("android")
    }

    private fun request(urlString: String, method: String, payload: String?): ApiResponse {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "⬆️ REQUEST $method $urlString")
        Log.d(TAG, "   Auth: ${if (authToken != null) "✅ present" else "❌ none"}")
        if (payload != null && payload.isNotEmpty()) {
            val preview = if (payload.length > 300) payload.take(300) + "...[truncated]" else payload
            Log.d(TAG, "   Body: $preview")
        }

        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                authToken?.let { setRequestProperty("Authorization", "Bearer $it") }

                // Forward InfinityFree anti-bot cookies
                android.webkit.CookieManager.getInstance().getCookie(urlString)?.let { cookies ->
                    setRequestProperty("Cookie", cookies)
                }

                connectTimeout = 15000
                readTimeout = 15000
                doInput = true
                if (method == "POST" || method == "PUT" || method == "PATCH") {
                    doOutput = true
                }
            }

            if (payload != null && payload.isNotEmpty()) {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }

            val responseCode = connection.responseCode
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "⬇️ RESPONSE $responseCode ${statusLabel(responseCode)} in ${elapsed}ms")

            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = responseStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            val bodyPreview = if (body.length > 500) body.take(500) + "...[${body.length} total]" else body
            if (responseCode !in 200..299) {
                Log.w(TAG, "   ⚠️ Error body: $bodyPreview")
            } else {
                Log.d(TAG, "   Body preview: $bodyPreview")
            }

            val json = try {
                if (body.isNotEmpty() && (body.trim().startsWith("{") || body.trim().startsWith("["))) {
                    JSONObject(body)
                } else {
                    if (body.isNotEmpty()) Log.w(TAG, "   Response is not JSON: ${
                        body.take(100)
                    }")
                    null
                }
            } catch (ex: JSONException) {
                Log.e(TAG, "   ❌ JSON parse failed: ${ex.message}")
                null
            }
            connection.disconnect()

            val success = responseCode in 200..299 && json != null
            if (!success) {
                val reason = when {
                    responseCode !in 200..299 -> "HTTP $responseCode"
                    json == null -> "Invalid JSON"
                    else -> "Unknown"
                }
                Log.w(TAG, "   ❌ Request failed: $reason")
            }

            ApiResponse(success, responseCode, body, json)
        } catch (ex: IOException) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ NETWORK ERROR after ${elapsed}ms: ${ex.javaClass.simpleName} - ${ex.message}", ex)
            ApiResponse(false, -1, ex.message ?: "Network Error", null)
        } catch (ex: Exception) {
            Log.e(TAG, "❌ UNEXPECTED ERROR: ${ex.javaClass.simpleName} - ${ex.message}", ex)
            ApiResponse(false, -1, "Error: ${ex.message}", null)
        }
    }

    private fun statusLabel(code: Int): String = when (code) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        409 -> "Conflict"
        422 -> "Validation Error"
        429 -> "Rate Limited"
        500 -> "Server Error"
        else -> ""
    }
}

data class ApiResponse(
    val success: Boolean,
    val responseCode: Int,
    val body: String,
    val json: JSONObject?
)
