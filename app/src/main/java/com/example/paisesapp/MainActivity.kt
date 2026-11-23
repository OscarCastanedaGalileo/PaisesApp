package com.example.paisesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCargar: Button = findViewById(R.id.btnCargar)
        btnCargar.setOnClickListener {
            val intent = Intent(this, PaisesActivity::class.java)
            startActivity(intent)
        }
    }
}