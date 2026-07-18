package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class DashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val greetingView = root.findViewById<TextView>(R.id.dashboardGreeting)
        val subtitleView = root.findViewById<TextView>(R.id.dashboardSubtitle)
        val statusView = root.findViewById<TextView>(R.id.dashboardStatus)
        val overviewSummary = root.findViewById<TextView>(R.id.overviewSummary)
        val recentRecordsStatus = root.findViewById<TextView>(R.id.recentRecordsStatus)
        val recentRecordsRecyclerView = root.findViewById<RecyclerView>(R.id.recentRecordsRecyclerView)
        val activeRecords = root.findViewById<TextView>(R.id.activeRecordsValue)
        val archivedRecords = root.findViewById<TextView>(R.id.archivedRecordsValue)
        val totalUsers = root.findViewById<TextView>(R.id.totalUsersValue)
        val recentEdits = root.findViewById<TextView>(R.id.recentEditsValue)
        val activeUsers = root.findViewById<TextView>(R.id.activeUsersValue)

        val recentRecordsAdapter = RecordAdapter()
        recentRecordsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recentRecordsRecyclerView.adapter = recentRecordsAdapter

        val username = activity?.intent?.getStringExtra("username") ?: "User"
        greetingView.text = "Welcome back, $username"
        subtitleView.text = "Track active records, review recent updates, and keep the workflow moving."
        statusView.text = getString(R.string.loading_message)

        Thread {
            val statsResponse = ApiClient.getJson(com.example.darlogs.data.ApiConfig.dashboardStats)
            val recordsResponse = ApiClient.getJson(com.example.darlogs.data.ApiConfig.activities)

            activity?.runOnUiThread {
                if (!statsResponse.success || statsResponse.json == null) {
                    statusView.text = getString(R.string.error_network)
                    return@runOnUiThread
                }
                val success = statsResponse.json.optBoolean("success", false)
                if (!success) {
                    statusView.text = statsResponse.json.optString("message", getString(R.string.error_network))
                    return@runOnUiThread
                }
                val stats = statsResponse.json.optJSONObject("stats") ?: JSONObject()
                val activeCount = stats.optInt("total_records")
                val archivedCount = stats.optInt("archived_records")
                val userCount = stats.optInt("total_users")
                val recentCount = stats.optInt("recent_edits")
                val activeUserCount = stats.optInt("active_users")

                activeRecords.text = activeCount.toString()
                archivedRecords.text = archivedCount.toString()
                totalUsers.text = userCount.toString()
                recentEdits.text = recentCount.toString()
                activeUsers.text = activeUserCount.toString()
                overviewSummary.text = "You currently have $activeCount active records, $archivedCount archived records, and $recentCount recent updates in the last 24 hours."

                if (recordsResponse.success && recordsResponse.json != null) {
                    val data = recordsResponse.json.optJSONArray("data") ?: JSONArray()
                    if (data.length() > 0) {
                        val limited = JSONArray()
                        val limit = minOf(5, data.length())
                        for (i in 0 until limit) {
                            data.optJSONObject(i)?.let { limited.put(it) }
                        }
                        recentRecordsAdapter.updateRecords(limited)
                        recentRecordsStatus.text = "Showing the latest ${limited.length()} records"
                    } else {
                        recentRecordsStatus.text = "No active records are available right now."
                        recentRecordsAdapter.updateRecords(JSONArray())
                    }
                } else {
                    recentRecordsStatus.text = getString(R.string.error_network)
                }

                statusView.text = "Dashboard is ready"
            }
        }.start()

        return root
    }
}
