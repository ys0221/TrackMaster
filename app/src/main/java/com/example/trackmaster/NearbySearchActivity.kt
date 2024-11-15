package com.example.trackmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NearbySearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_search)

        val stationNumberInput = findViewById<EditText>(R.id.stationNumberInput)
        val searchNearbyButton = findViewById<Button>(R.id.searchNearbyButton)

        searchNearbyButton.setOnClickListener {
            val stationNumber = stationNumberInput.text.toString()
            if (stationNumber.isNotBlank()) {
                val intent = Intent(this, NearbyResultActivity::class.java)
                intent.putExtra("stationNumber", stationNumber)
                startActivity(intent)
            } else {
                Toast.makeText(this, "역 번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
