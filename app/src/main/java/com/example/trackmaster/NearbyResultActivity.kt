package com.example.trackmaster

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class NearbyResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_result)

        val stationNumber = intent.getStringExtra("stationNumber")
        val stationInfoTextView = findViewById<TextView>(R.id.stationInfoTextView)
        val amenitiesListView = findViewById<ListView>(R.id.amenitiesListView)

        // CSV 파일에서 역 번호에 해당하는 편의시설 목록을 불러옴
        val amenitiesList = loadAmenitiesForStation(stationNumber)

        // 역 번호가 존재하지 않는 경우 처리
        if (amenitiesList.isEmpty()) {
            stationInfoTextView.text = "역 번호를 찾을 수 없습니다."
        } else {
            stationInfoTextView.text = "역 번호 ${stationNumber}의 주변 편의시설 목록"
        }

        // ArrayAdapter를 사용해 ListView에 데이터 설정
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, amenitiesList)
        amenitiesListView.adapter = adapter
    }

    private fun loadAmenitiesForStation(stationNumber: String?): List<String> {
        val amenitiesList = mutableListOf<String>()
        if (stationNumber == null) return amenitiesList // stationNumber가 null인 경우 빈 리스트 반환

        try {
            assets.open("amenities.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // 헤더 건너뛰기
                    reader.forEachLine { line ->
                        val tokens = line.split(",")
                        if (tokens[0] == stationNumber && tokens.size >= 7) {
                            // CSV 파일에서 편의시설 이름과 거리를 읽어들임
                            val facility1 = tokens[1]
                            val facility2 = tokens[2]
                            val facility3 = tokens[3]
                            val distance1 = tokens[4].toIntOrNull() ?: Int.MAX_VALUE // 거리 값을 정수로 변환 (변환 실패 시 최대값 설정)
                            val distance2 = tokens[5].toIntOrNull() ?: Int.MAX_VALUE
                            val distance3 = tokens[6].toIntOrNull() ?: Int.MAX_VALUE

                            // 포맷에 맞게 리스트에 추가
                            amenitiesList.add("$facility1\n거리: ${distance1}m")
                            amenitiesList.add("$facility2\n거리: ${distance2}m")
                            amenitiesList.add("$facility3\n거리: ${distance3}m")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return amenitiesList
    }
}
