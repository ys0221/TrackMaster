package com.example.trackmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val resultTextView = findViewById<TextView>(R.id.resultText)
        val resultText = intent.getStringExtra("resultText")
        val nearbyButton = findViewById<Button>(R.id.nearbyButton)
        nearbyButton.setOnClickListener {
            val intent = Intent(this, NearbySearchActivity::class.java)
            startActivity(intent)
        }

        resultTextView.text = resultText ?: "결과가 없습니다."
    }
}
