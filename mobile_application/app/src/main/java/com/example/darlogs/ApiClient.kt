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

    fun initialize() {
        if (CookieHandler.getDefault() == null) {
            val manager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(manager)
        }
    }

    fun clearCookies() {
        (CookieHandler.getDefault() as? CookieManager)?.cookieStore?.removeAll()
    }

    fun getJson(urlString: String): ApiResponse {
        return request(resolveApiUrl(urlString), "GET", null)
    }

    fun postJson(urlString: String, payload: String): ApiResponse {
        return request(resolveApiUrl(urlString), "POST", payload)
    }

    fun putJson(urlString: String, payload: String): ApiResponse {
        return request(resolveApiUrl(urlString), "PUT", payload)
    }

    fun deleteJson(urlString: String): ApiResponse {
        return request(resolveApiUrl(urlString), "DELETE", null)
    }

    fun patchJson(urlString: String, payload: String): ApiResponse {
        return request(resolveApiUrl(urlString), "PATCH", payload)
    }

    private fun resolveApiUrl(urlString: String): String {
        Log.d(TAG, "Resolving URL: $urlString")
        if (!isEmulator()) {
            Log.d(TAG, "Not an emulator, using raw URL")
            return urlString
        }

        val resolved = urlString
            .replace("192.168.1.6", "10.0.2.2")
            .replace("127.0.0.1", "10.0.2.2")
            .replace("localhost", "10.0.2.2")

        if (resolved != urlString) {
            Log.d(TAG, "Emulator detected: rewriting API URL to $resolved")
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
            || Build.MODEL.contains("sdk")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.MANUFACTURER.contains("Google") && Build.BRAND.startsWith("google")
            || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("vbox86p")
            || Build.HOST.contains("android")
    }

    private fun request(urlString: String, method: String, payload: String?): ApiResponse {
        return try {
            // Log the exact URL and method for debugging network issues
            Log.d(TAG, "Requesting URL: $urlString Method: $method")
            if (payload != null) {
                val preview = if (payload.length > 200) payload.substring(0, 200) + "...[truncated]" else payload
                Log.d(TAG, "Payload preview: $preview")
            }
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                // Critical: Use the exact same User-Agent as the WebView bypass
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                
                // If we have cookies from the handshake, use them
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

            if (payload != null) {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")
            
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = responseStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            
            if (responseCode !in 200..299) {
                Log.e(TAG, "Error Response Body: $body")
            }

            val json = try {
                if (connection.contentType?.contains("application/json") == true || body.trim().startsWith("{")) {
                    JSONObject(body)
                } else {
                    null
                }
            } catch (ex: JSONException) {
                Log.e(TAG, "Failed to parse JSON: ${ex.message}")
                null
            }
            val success = responseCode in 200..299 && json != null
            if (json == null && responseCode in 200..299) {
                Log.e(TAG, "Success code but invalid JSON. Body: $body")
            }
            connection.disconnect()
            ApiResponse(success, responseCode, body, json)
        } catch (ex: IOException) {
            Log.e(TAG, "Network request failed - IOException: ${ex.javaClass.simpleName}")
            Log.e(TAG, "Message: ${ex.message}")
            ApiResponse(false, -1, "Network Error: ${ex.javaClass.simpleName}", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Request failed - Exception: ${ex.javaClass.simpleName}", ex)
            ApiResponse(false, -1, "Error: ${ex.message}", null)
        }
    }
}

data class ApiResponse(
    val success: Boolean,
    val responseCode: Int,
    val body: String,
    val json: JSONObject?
)
