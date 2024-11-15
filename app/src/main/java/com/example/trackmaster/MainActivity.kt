package com.example.trackmaster

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

// 역 데이터 클래스
data class StationData(val 출발역: String, val 도착역: String, val 시간: Int, val 거리: Int, val 비용: Int)

// 경로 정보 클래스
data class Path(val station: String, val totalCost: Int, val totalTransfers: Int, val route: List<String>, val distances: List<Int>, val costs: List<Int>, val times: List<Int>)

// 새로운 Edge 데이터 클래스 정의
data class Edge(val destination: String, val time: Int, val distance: Int, val cost: Int)

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startStationInput = findViewById<EditText>(R.id.startStationInput)
        val endStationInput = findViewById<EditText>(R.id.endStationInput)
        val calculateButton = findViewById<Button>(R.id.calculateButton)

        val stationList = readCSV(this)
        val graph = buildGraph(stationList)

        calculateButton.setOnClickListener {
            val startStation = startStationInput.text.toString()
            val endStation = endStationInput.text.toString()

            if (startStation.isBlank() || endStation.isBlank()) {
                Toast.makeText(this, "출발역과 도착역을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minTransferResult = findLowestCostPathWithTransfers(graph, startStation, endStation)
            val minCostResult = findLowestCostPath(graph, startStation, endStation)
            val minTimeResult = findLowestTimePath(graph, startStation, endStation)

            if (minTransferResult != null && minCostResult != null && minTimeResult != null) {
                val resultText = """
                    최소 환승 경로
                    경로: ${minTransferResult.route.joinToString(" -> ")}
                    비용: ${minTransferResult.costs.sum()} 원
                    거리: ${minTransferResult.distances.sum()} m
                    소요 시간: ${minTransferResult.times.sum()} 초
                    
                    혼잡도: 
                    ${calculateCongestion(minTransferResult)}

                    최소 비용 경로
                    경로: ${minCostResult.route.joinToString(" -> ")}
                    비용: ${minCostResult.totalCost} 원
                    거리: ${minCostResult.distances.sum()} m
                    소요 시간: ${minCostResult.times.sum()} 초
                    
                    혼잡도: 
                    ${calculateCongestion(minCostResult)}

                    최소 시간 경로
                    경로: ${minTimeResult.route.joinToString(" -> ")}
                    비용: ${minTimeResult.totalCost} 원
                    거리: ${minTimeResult.distances.sum()} m
                    소요 시간: ${minTimeResult.times.sum()} 초
                    
                    혼잡도: 
                    ${calculateCongestion(minTimeResult)}
                """.trimIndent()

                // ResultActivity로 이동하며 결과를 전달
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("resultText", resultText)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateCongestion(path: Path): String {
        val lineCongestions = mutableMapOf<String, MutableList<Int>>()

        path.route.forEach { station ->
            val line = station.substring(0, 1) // 역 이름의 첫 글자가 호선 번호라고 가정
            val congestion = Random().nextInt(100) + 1 // 1부터 100 사이의 랜덤 혼잡도
            lineCongestions.computeIfAbsent(line) { mutableListOf() }.add(congestion)
        }

        // 호선별 평균 혼잡도 계산 후 줄바꿈으로 나열
        return lineCongestions.entries.joinToString { "${it.key}호선 평균 혼잡도: ${it.value.sum() / it.value.size}%" }
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
        priorityQueue.add(Triple(start, 0, 0))

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

        return null
    }

    private fun findLowestCostPath(graph: Map<String, MutableList<Edge>>, start: String, end: String): Path? {
        val costs = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val routeDistances = mutableMapOf<String, List<Int>>()
        val routeCosts = mutableMapOf<String, List<Int>>()
        val routeTimes = mutableMapOf<String, List<Int>>()

        costs[start] = 0
        routeDistances[start] = listOf()
        routeCosts[start] = listOf()
        routeTimes[start] = listOf()

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy { it.second })
        priorityQueue.add(Triple(start, 0, 0))

        while (priorityQueue.isNotEmpty()) {
            val (currentStation, currentCost, _) = priorityQueue.poll()

            if (currentStation == end) {
                val route = generateRoute(previousNodes, end)
                return Path(
                    station = currentStation,
                    totalCost = routeCosts[end]!!.sum(),
                    totalTransfers = 0,
                    route = route,
                    distances = routeDistances[end] ?: listOf(),
                    costs = routeCosts[end] ?: listOf(),
                    times = routeTimes[end] ?: listOf()
                )
            }

            for (edge in graph[currentStation] ?: emptyList()) {
                val newCost = currentCost + edge.cost

                if (newCost < costs.getValue(edge.destination)) {
                    costs[edge.destination] = newCost
                    previousNodes[edge.destination] = currentStation
                    routeDistances[edge.destination] = routeDistances[currentStation]!! + edge.distance
                    routeCosts[edge.destination] = routeCosts[currentStation]!! + edge.cost
                    routeTimes[edge.destination] = routeTimes[currentStation]!! + edge.time
                    priorityQueue.add(Triple(edge.destination, newCost, 0))
                }
            }
        }

        return null
    }

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

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy { it.second })
        priorityQueue.add(Triple(start, 0, 0))

        while (priorityQueue.isNotEmpty()) {
            val (currentStation, currentTime, _) = priorityQueue.poll()

            if (currentStation == end) {
                val route = generateRoute(previousNodes, end)
                return Path(
                    station = currentStation,
                    totalCost = routeCosts[end]!!.sum(),
                    totalTransfers = 0,
                    route = route,
                    distances = routeDistances[end] ?: listOf(),
                    costs = routeCosts[end] ?: listOf(),
                    times = routeTimes[end] ?: listOf()
                )
            }

            for (edge in graph[currentStation] ?: emptyList()) {
                val newTime = currentTime + edge.time

                if (newTime < times.getValue(edge.destination)) {
                    times[edge.destination] = newTime
                    previousNodes[edge.destination] = currentStation
                    routeDistances[edge.destination] = routeDistances[currentStation]!! + edge.distance
                    routeCosts[edge.destination] = routeCosts[currentStation]!! + edge.cost
                    routeTimes[edge.destination] = routeTimes[currentStation]!! + edge.time
                    priorityQueue.add(Triple(edge.destination, newTime, 0))
                }
            }
        }

        return null
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
