package com.example.trackmaster

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

data class StationData(val 출발역: String, val 도착역: String, val 시간: Int, val 거리: Int, val 비용: Int)
data class Path(
    val station: String,
    val totalCost: Int,
    val totalTransfers: Int,
    val route: List<String>,
    val distances: List<Int>,
    val costs: List<Int>,
    val times: List<Int>
)
data class Edge(val destination: String, val time: Int, val distance: Int, val cost: Int)

class MainActivity : AppCompatActivity() {

    private lateinit var resultLayout: LinearLayout

    private val lineColors = mapOf(
        1 to "#FF5733", // 1호선: 빨간색
        2 to "#33FF57", // 2호선: 초록색
        3 to "#3357FF", // 3호선: 파란색
        4 to "#FFC300", // 4호선: 노란색
        5 to "#900C3F", // 5호선: 자주색
        6 to "#581845", // 6호선: 보라색
        7 to "#DAF7A6", // 7호선: 연두색
        8 to "#C70039", // 8호선: 진한 빨간색
        9 to "#1C2833"  // 9호선: 진한 파란색
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View 연결
        val startStationInput = findViewById<EditText>(R.id.startStationInput)
        val endStationInput = findViewById<EditText>(R.id.endStationInput)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        resultLayout = findViewById(R.id.resultLayout)

        val minTransferButton = findViewById<Button>(R.id.minTransferButton)
        val minTimeButton = findViewById<Button>(R.id.minTimeButton)
        val minCostButton = findViewById<Button>(R.id.minCostButton)

        val defaultColor = Color.parseColor("#CCCCCC")
        val clickedColor = Color.YELLOW

        val stationList = readCSV(this)
        val graph = buildGraph(stationList)

        // 경로 검색 버튼 클릭
        calculateButton.setOnClickListener {
            val startStation = startStationInput.text.toString()
            val endStation = endStationInput.text.toString()

            if (startStation.isBlank() || endStation.isBlank()) {
                displayErrorMessage("출발역과 도착역을 입력하세요.")
                return@setOnClickListener
            }

            resetButtonColors(minTransferButton, minTimeButton, minCostButton)
            clearResults()

            // 경로 계산
            val minTransferResult = findLowestCostPathWithTransfers(graph, startStation, endStation)
            val minCostResult = findLowestCostPath(graph, startStation, endStation)
            val minTimeResult = findLowestTimePath(graph, startStation, endStation)

            // 결과 동적 출력
            if (minTransferResult != null) {
                addRouteToLayout("최소 환승 경로", minTransferResult.route)
                //displayDetailedResult("최소 환승 경로", minTransferResult)
            }
            if (minCostResult != null) {
                addRouteToLayout("최소 비용 경로", minCostResult.route)
            }
            if (minTimeResult != null) {
                addRouteToLayout("최소 시간 경로", minTimeResult.route)
            }
        }

        // 최소 환승 버튼
        minTransferButton.setOnClickListener {
            resetButtonColors(minTransferButton, minTimeButton, minCostButton)
            minTransferButton.backgroundTintList = ColorStateList.valueOf(clickedColor)
            handleSpecificCondition(graph, startStationInput, endStationInput, "최소 환승 경로") { g, s, e ->
                findLowestCostPathWithTransfers(g, s, e)
            }
        }

        // 최소 시간 버튼
        minTimeButton.setOnClickListener {
            resetButtonColors(minTransferButton, minTimeButton, minCostButton)
            minTimeButton.backgroundTintList = ColorStateList.valueOf(clickedColor)
            handleSpecificCondition(graph, startStationInput, endStationInput, "최소 시간 경로") { g, s, e ->
                findLowestTimePath(g, s, e)
            }
        }

        // 최소 비용 버튼
        minCostButton.setOnClickListener {
            resetButtonColors(minTransferButton, minTimeButton, minCostButton)
            minCostButton.backgroundTintList = ColorStateList.valueOf(clickedColor)
            handleSpecificCondition(graph, startStationInput, endStationInput, "최소 비용 경로") { g, s, e ->
                findLowestCostPath(g, s, e)
            }
        }
    }

    // 경로를 UI에 추가
    private fun addRouteToLayout(label: String, route: List<String>) {
        val lineNumberSet = route.map { getLineNumber(it) }.distinct()

        // 호선 TextView
        val lineTextView = TextView(this)
        lineTextView.text = "$label: ${lineNumberSet.joinToString(" -> ") { "${it}호선" }}"
        lineTextView.setTextColor(Color.BLACK)
        lineTextView.textSize = 16f
        resultLayout.addView(lineTextView)

        // 경로 TextView
        val routeTextView = TextView(this)
        routeTextView.text = formatRouteWithColors(route)
        routeTextView.visibility = TextView.GONE // 초기에는 숨김
        resultLayout.addView(routeTextView)

        // 클릭 이벤트로 경로 토글
        lineTextView.setOnClickListener {
            routeTextView.visibility =
                if (routeTextView.visibility == TextView.GONE) TextView.VISIBLE else TextView.GONE
        }
    }
    // 특정 조건에 대한 결과 출력
    private fun handleSpecificCondition(
        graph: Map<String, MutableList<Edge>>,
        startStationInput: EditText,
        endStationInput: EditText,
        label: String,
        pathFindingFunction: (Map<String, MutableList<Edge>>, String, String) -> Path?
    ) {
        val startStation = startStationInput.text.toString()
        val endStation = endStationInput.text.toString()

        if (startStation.isBlank() || endStation.isBlank()) {
            displayErrorMessage("출발역과 도착역을 입력하세요.")
            return
        }

        clearResults()
        val result = pathFindingFunction(graph, startStation, endStation)

        if (result != null) {
            displayDetailedResult(label, result)
        } else {
            displayErrorMessage("$label 찾을 수 없습니다.")
        }
    }

    // 결과와 세부 정보 출력
    private fun displayDetailedResult(label: String, result: Path) {
        val textView = TextView(this)
        textView.text = """
            $label:
            경로: ${formatRouteWithColors(result.route)}
            비용: ${result.costs.sum()} 원
            거리: ${result.distances.sum()} m
            소요 시간: ${result.times.sum()} 초
        """.trimIndent()
        textView.setTextColor(Color.BLACK)
        textView.textSize = 16f
        resultLayout.addView(textView)
    }

    // 경로를 색상별로 포맷
    private fun formatRouteWithColors(route: List<String>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for ((index, station) in route.withIndex()) {
            val lineNumber = getLineNumber(station)
            val color = lineColors[lineNumber] ?: "#000000"
            val start = builder.length
            builder.append(station)
            val end = builder.length
            builder.setSpan(
                ForegroundColorSpan(Color.parseColor(color)),
                start,
                end,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (index < route.size - 1) builder.append(" -> ")
        }
        return builder
    }

    // 에러 메시지 출력
    private fun displayErrorMessage(message: String) {
        clearResults()
        val errorTextView = TextView(this)
        errorTextView.text = message
        errorTextView.setTextColor(Color.RED)
        errorTextView.textSize = 16f
        resultLayout.addView(errorTextView)
    }

    // 결과 초기화
    private fun clearResults() {
        resultLayout.removeAllViews()
    }

    // 버튼 색상 초기화
    private fun resetButtonColors(vararg buttons: Button) {
        for (button in buttons) {
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
        }
    }

    private fun getLineNumber(stationCode: String): Int {
        return stationCode.substring(0, 1).toInt()
    }

    private fun readCSV(context: Context): List<StationData> {
        val stationList = mutableListOf<StationData>()
        try {
            context.assets.open("stations.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine()
                    reader.forEachLine { line ->
                        val tokens = line.split(",")
                        if (tokens.size == 5) {
                            val station = StationData(
                                출발역 = tokens[0],
                                도착역 = tokens[1],
                                시간 = tokens[2].toInt(),
                                거리 = tokens[3].toInt(),
                                비용 = tokens[4].toInt()
                            )
                            stationList.add(station)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stationList
    }

    private fun buildGraph(stationList: List<StationData>): Map<String, MutableList<Edge>> {
        val graph = mutableMapOf<String, MutableList<Edge>>()
        for (station in stationList) {
            graph.computeIfAbsent(station.출발역) { mutableListOf() }
                .add(Edge(station.도착역, station.시간, station.거리, station.비용))
            graph.computeIfAbsent(station.도착역) { mutableListOf() }
                .add(Edge(station.출발역, station.시간, station.거리, station.비용))
        }
        return graph
    }

    // 최소 환승 경로 찾는 함수
    private fun findLowestCostPathWithTransfers(graph: Map<String, MutableList<Edge>>, start: String, end: String): Path? {
        val costs = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val transfers = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val routeDistances = mutableMapOf<String, List<Int>>()
        val routeCosts = mutableMapOf<String, List<Int>>()
        val routeTimes = mutableMapOf<String, List<Int>>()

        costs[start] = 0
        transfers[start] = 0
        routeDistances[start] = listOf()
        routeCosts[start] = listOf()
        routeTimes[start] = listOf()

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy<Triple<String, Int, Int>> { it.third }.thenBy { costs.getValue(it.first) })
        priorityQueue.add(Triple(start, 0, 0))  // (station, cost, transfersg)

        while (priorityQueue.isNotEmpty()) {
            val (currentStation, currentCost, currentTransfers) = priorityQueue.poll()

            if (currentStation == end) {
                val route = generateRoute(previousNodes, end)
                return Path(
                    station = currentStation,
                    totalCost = routeCosts[end]!!.sum(),
                    totalTransfers = transfers[end]!!,
                    route = route,
                    distances = routeDistances[end] ?: listOf(),
                    costs = routeCosts[end] ?: listOf(),
                    times = routeTimes[end] ?: listOf()
                )
            }

            for (edge in graph[currentStation] ?: emptyList()) {
                val newCost = currentCost + edge.cost
                val newTransfers = if (currentStation.first() != edge.destination.first()) currentTransfers + 1 else currentTransfers

                if (newTransfers < transfers.getValue(edge.destination) ||
                    (newTransfers == transfers.getValue(edge.destination) && newCost < costs.getValue(edge.destination))
                ) {
                    costs[edge.destination] = newCost
                    transfers[edge.destination] = newTransfers
                    previousNodes[edge.destination] = currentStation
                    routeDistances[edge.destination] = routeDistances[currentStation]!! + edge.distance
                    routeCosts[edge.destination] = routeCosts[currentStation]!! + edge.cost
                    routeTimes[edge.destination] = routeTimes[currentStation]!! + edge.time
                    priorityQueue.add(Triple(edge.destination, newCost, newTransfers))
                }
            }
        }

        return null // If no path is found
    }

    // 최소 비용 경로 찾는 함수
    private fun findLowestCostPath(graph: Map<String, MutableList<Edge>>, start: String, end: String): Path? {
        val costs = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val transfers = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val routeDistances = mutableMapOf<String, List<Int>>()
        val routeCosts = mutableMapOf<String, List<Int>>()
        val routeTimes = mutableMapOf<String, List<Int>>()

        costs[start] = 0
        transfers[start] = 0
        routeDistances[start] = listOf()
        routeCosts[start] = listOf()
        routeTimes[start] = listOf()

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy { it.second }) // 비용 기준 우선 정렬
        priorityQueue.add(Triple(start, 0, 0))  // (station, cost, transfers)

        while (priorityQueue.isNotEmpty()) {
            val (currentStation, currentCost, currentTransfers) = priorityQueue.poll()

            if (currentStation == end) {
                val route = generateRoute(previousNodes, end)
                return Path(
                    station = currentStation,
                    totalCost = routeCosts[end]!!.sum(),
                    totalTransfers = currentTransfers,
                    route = route,
                    distances = routeDistances[end] ?: listOf(),
                    costs = routeCosts[end] ?: listOf(),
                    times = routeTimes[end] ?: listOf()
                )
            }

            for (edge in graph[currentStation] ?: emptyList()) {
                val newCost = currentCost + edge.cost
                val newTransfers = if (currentStation.first() != edge.destination.first()) currentTransfers + 1 else currentTransfers

                if (newCost < costs.getValue(edge.destination)) {
                    costs[edge.destination] = newCost
                    transfers[edge.destination] = newTransfers
                    previousNodes[edge.destination] = currentStation
                    routeDistances[edge.destination] = routeDistances[currentStation]!! + edge.distance
                    routeCosts[edge.destination] = routeCosts[currentStation]!! + edge.cost
                    routeTimes[edge.destination] = routeTimes[currentStation]!! + edge.time
                    priorityQueue.add(Triple(edge.destination, newCost, newTransfers))
                }
            }
        }

        return null // If no path is found
    }

    // 최소 시간 경로 찾는 함수
    private fun findLowestTimePath(graph: Map<String, MutableList<Edge>>, start: String, end: String): Path? {
        val times = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val routeDistances = mutableMapOf<String, List<Int>>()
        val routeCosts = mutableMapOf<String, List<Int>>()
        val routeTimes = mutableMapOf<String, List<Int>>()

        times[start] = 0
        routeDistances[start] = listOf()
        routeCosts[start] = listOf()
        routeTimes[start] = listOf()

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy { it.second }) // 시간 기준 우선 정렬
        priorityQueue.add(Triple(start, 0, 0))  // (역 이름, 시간, 환승 횟수)

        while (priorityQueue.isNotEmpty()) {
            val (currentStation, currentTime, currentTransfers) = priorityQueue.poll()

            if (currentStation == end) {
                val route = generateRoute(previousNodes, end)
                return Path(
                    station = currentStation,
                    totalCost = routeCosts[end]!!.sum(),
                    totalTransfers = currentTransfers,
                    route = route,
                    distances = routeDistances[end] ?: listOf(),
                    costs = routeCosts[end] ?: listOf(),
                    times = routeTimes[end] ?: listOf()
                )
            }

            for (edge in graph[currentStation] ?: emptyList()) {
                val newTime = currentTime + edge.time
                val newTransfers = if (currentStation.first() != edge.destination.first()) currentTransfers + 1 else currentTransfers

                if (newTime < times.getValue(edge.destination)) {
                    times[edge.destination] = newTime
                    previousNodes[edge.destination] = currentStation
                    routeDistances[edge.destination] = routeDistances[currentStation]!! + edge.distance
                    routeCosts[edge.destination] = routeCosts[currentStation]!! + edge.cost
                    routeTimes[edge.destination] = routeTimes[currentStation]!! + edge.time
                    priorityQueue.add(Triple(edge.destination, newTime, newTransfers))
                }
            }
        }

        return null // 경로를 찾지 못한 경우
    }


    private fun generateRoute(previousNodes: Map<String, String?>, end: String): List<String> {
        val route = mutableListOf<String>()
        var currentNode: String? = end
        while (currentNode != null) {
            route.add(currentNode)
            currentNode = previousNodes[currentNode]
        }
        return route.reversed()
    }
}