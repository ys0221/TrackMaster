package com.example.trackmaster

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startStationInput = findViewById<EditText>(R.id.startStationInput)
        val endStationInput = findViewById<EditText>(R.id.endStationInput)
        val searchButton = findViewById<Button>(R.id.searchButton)

        val stationList = readCSV(this)
        val graph = buildGraph(stationList)

        // 두 경로 계산 후 데이터를 Intent에 추가하는 부분
        searchButton.setOnClickListener {
            val startStation = startStationInput.text.toString()
            val endStation = endStationInput.text.toString()

            if (startStation.isNotBlank() && endStation.isNotBlank()) {
                val minTransferResult = findLowestCostPathWithTransfers(graph, startStation, endStation)
                val minCostResult = findLowestCostPath(graph, startStation, endStation)

                if (minTransferResult != null && minCostResult != null) {
                    val intent = Intent(this, ResultActivity::class.java)

                    // 최소 환승 경로 데이터 추가
                    intent.putStringArrayListExtra("transferRoute", ArrayList(minTransferResult.route))
                    intent.putIntegerArrayListExtra("transferDistance", ArrayList(minTransferResult.distances))
                    intent.putIntegerArrayListExtra("transferCosts", ArrayList(minTransferResult.costs))
                    intent.putIntegerArrayListExtra("transferTimes", ArrayList(minTransferResult.times))
                    intent.putExtra("transferTransfers", minTransferResult.totalTransfers)

                    // 최소 비용 경로 데이터 추가
                    intent.putStringArrayListExtra("costRoute", ArrayList(minCostResult.route))
                    intent.putIntegerArrayListExtra("costDistance", ArrayList(minCostResult.distances))
                    intent.putIntegerArrayListExtra("costs", ArrayList(minCostResult.costs))
                    intent.putIntegerArrayListExtra("times", ArrayList(minCostResult.times))
                    intent.putExtra("totalCost", minCostResult.totalCost)
                    intent.putExtra("costTransfers", minCostResult.totalTransfers)

                    startActivity(intent)
                } else {
                    Toast.makeText(this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "출발역과 도착역을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun readCSV(context: Context): List<StationData> {
        val stationList = mutableListOf<StationData>()

        try {
            context.assets.open("stations.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // 헤더 건너뜀

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

    // 최소 환승 경로 함수
    private fun findLowestCostPathWithTransfers(graph: Map<String, MutableList<Edge>>, start: String, end: String): Path? {
        val costs = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val transfers = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val distances = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val routeDistances = mutableMapOf<String, List<Int>>()
        val routeCosts = mutableMapOf<String, List<Int>>()
        val routeTimes = mutableMapOf<String, List<Int>>()

        costs[start] = 0
        transfers[start] = 0
        distances[start] = 0
        routeDistances[start] = listOf()
        routeCosts[start] = listOf()
        routeTimes[start] = listOf()

        val priorityQueue = PriorityQueue<Triple<String, Int, Int>>(compareBy<Triple<String, Int, Int>> { it.third }.thenBy { distances.getValue(it.first) })
        priorityQueue.add(Triple(start, 0, 0))  // (station, cost, transfers)

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
                val newDistance = distances.getValue(currentStation) + edge.distance

                // Update based on transfer count, and distance if transfer counts are equal
                if (newTransfers < transfers.getValue(edge.destination) ||
                    (newTransfers == transfers.getValue(edge.destination) && newDistance < distances.getValue(edge.destination))
                ) {
                    costs[edge.destination] = newCost
                    transfers[edge.destination] = newTransfers
                    distances[edge.destination] = newDistance
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


    // 최소 비용 경로 함수
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
        priorityQueue.add(Triple(start, 0, 0))  // (역 이름, 비용, 환승 횟수)

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

                // 최소 비용 기준으로 업데이트, 비용이 같으면 환승 횟수는 비교하지 않음
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
