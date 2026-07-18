package com.example.darlogs

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("username", "Guest")
        startActivity(intent)
        finish()
    }
}
