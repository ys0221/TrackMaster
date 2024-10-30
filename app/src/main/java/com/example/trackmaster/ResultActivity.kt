package com.example.trackmaster

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 최소 비용 경로 데이터 받기
        val costRoute = intent.getStringArrayListExtra("costRoute")
        val costDistance = intent.getIntegerArrayListExtra("costDistance")
        val costs = intent.getIntegerArrayListExtra("costs")
        val times = intent.getIntegerArrayListExtra("times")
        val costTransfers = intent.getIntExtra("costTransfers", -1)

        // 최소 환승 경로 데이터 받기
        val transferRoute = intent.getStringArrayListExtra("transferRoute")
        val transferDistance = intent.getIntegerArrayListExtra("transferDistance")
        val transferCosts = intent.getIntegerArrayListExtra("transferCosts")
        val transferTimes = intent.getIntegerArrayListExtra("transferTimes")
        val transferTransfers = intent.getIntExtra("transferTransfers", -1)

        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        // 최소 비용 경로 총 비용 계산
        val totalCost = costs?.sum() ?: -1

        // 최소 환승 경로 총 비용 계산
        val transferTotalCost = transferCosts?.sum() ?: -1

        // 최소 비용 경로 표시
        val costRouteString = buildString {
            append("최소 비용 경로:\n")
            costRoute?.forEachIndexed { index, station ->
                append(station)
                if (index < costDistance?.size ?: 0) {
                    append(" (${costDistance?.get(index)}m, ${costs?.get(index)}원, ${times?.get(index)}초)")
                    if (index < costRoute.size - 1) {
                        append(" -> ")
                    }
                }
            }
            append("\n총 비용: $totalCost 원\n총 환승 횟수: $costTransfers 회\n")
        }

        // 최소 환승 경로 표시
        val transferRouteString = buildString {
            append("최소 환승 경로:\n")
            transferRoute?.forEachIndexed { index, station ->
                append(station)
                if (index < transferDistance?.size ?: 0) {
                    append(" (${transferDistance?.get(index)}m, ${transferCosts?.get(index)}원, ${transferTimes?.get(index)}초)")
                    if (index < transferRoute.size - 1) {
                        append(" -> ")
                    }
                }
            }
            append("\n총 비용: $transferTotalCost 원\n총 환승 횟수: $transferTransfers 회\n")
        }

        // 결과를 TextView에 설정
        resultTextView.text = "$costRouteString\n$transferRouteString"
    }
}
