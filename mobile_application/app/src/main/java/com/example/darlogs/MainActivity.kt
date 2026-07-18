package com.example.darlogs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize()
        ApiClient.clearCookies()

        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("username", "Guest")
        startActivity(intent)
        finish()
    }

    private fun performLogin(username: String, password: String): LoginResult {
        return try {
            val apiUrl = getString(R.string.login_api_url)
            val requestBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }.toString()

            val response = ApiClient.postJson(apiUrl, requestBody)
            if (!response.success || response.json == null) {
                return LoginResult(false, response.body, null)
            }

            val success = response.json.optBoolean("success", false)
            val message = response.json.optString("message", getString(R.string.error_login_failed))
            val user = response.json.optJSONObject("user")
            val responseUsername = user?.optString("username")

            LoginResult(success, message, responseUsername)
        } catch (ex: IOException) {
            Log.e("DARLogsLogin", "Network error", ex)
            LoginResult(false, "Network error: ${ex.message}", null)
        } catch (ex: Exception) {
            Log.e("DARLogsLogin", "Login error", ex)
            LoginResult(false, "Login error: ${ex.message}", null)
        }
    }

    private data class LoginResult(val success: Boolean, val message: String, val username: String?)
}
