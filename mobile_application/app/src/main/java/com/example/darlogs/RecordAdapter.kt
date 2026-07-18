package com.example.darlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class RecordAdapter : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {
    private val records = mutableListOf<JSONObject>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(data: JSONArray) {
        records.clear()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { records.add(it) }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText = itemView.findViewById<TextView>(R.id.itemTitle)
        private val subtitleText = itemView.findViewById<TextView>(R.id.itemSubtitle)
        private val detailText = itemView.findViewById<TextView>(R.id.itemDetail)
        private val extraText = itemView.findViewById<TextView>(R.id.itemExtra)

        fun bind(record: JSONObject) {
            val municipality = record.optString("municipality")
            val claimant = record.optString("lo_claimant")
            val titleNo = record.optString("title_no")
            val routeTo = record.optString("route_to")
            val status = record.optString("work_status")
            val updatedAt = record.optString("updated_at")

            titleText.text = "$municipality — $claimant"
            subtitleText.text = formatLabel(titleNo, "Title:")
            detailText.text = "Route: ${if (routeTo.isBlank()) "—" else routeTo} | Status: ${if (status.isBlank()) "—" else status}"
            extraText.text = "Updated: ${if (updatedAt.isBlank()) "—" else updatedAt}"
        }

        private fun formatLabel(value: String, prefix: String): String {
            return if (value.isBlank()) "$prefix —" else "$prefix $value"
        }
    }
}
