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

class CompletedRecordsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_record_list, container, false)
        root.findViewById<TextView>(R.id.sectionTitle).text = getString(R.string.nav_completed)
        root.findViewById<RecyclerView>(R.id.recordRecyclerView).layoutManager = LinearLayoutManager(requireContext())
        val adapter = RecordAdapter()
        root.findViewById<RecyclerView>(R.id.recordRecyclerView).adapter = adapter

        val emptyState = root.findViewById<TextView>(R.id.emptyStateText)
        val statusView = root.findViewById<TextView>(R.id.sectionStatus)

        statusView.text = getString(R.string.loading_message)
        emptyState.visibility = View.GONE

        Thread {
            val apiUrl = "${com.example.darlogs.data.ApiConfig.activities}?work_status=finished"
            val response = ApiClient.getJson(apiUrl)
            activity?.runOnUiThread {
                if (!response.success || response.json == null) {
                    statusView.text = getString(R.string.error_network)
                    return@runOnUiThread
                }
                val success = response.json.optBoolean("success", false)
                if (!success) {
                    statusView.text = response.json.optString("message", getString(R.string.error_network))
                    return@runOnUiThread
                }
                val data = response.json.optJSONArray("data") ?: JSONArray()
                if (data.length() == 0) {
                    statusView.text = ""
                    emptyState.visibility = View.VISIBLE
                    return@runOnUiThread
                }
                statusView.text = ""
                emptyState.visibility = View.GONE
                adapter.updateRecords(data)
            }
        }.start()

        return root
    }
}
