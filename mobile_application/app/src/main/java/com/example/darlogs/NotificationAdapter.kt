package com.example.darlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private val notifications = mutableListOf<JSONObject>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(data: JSONArray) {
        notifications.clear()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { notifications.add(it) }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText = itemView.findViewById<TextView>(R.id.notificationTitle)
        private val subtitleText = itemView.findViewById<TextView>(R.id.notificationSubtitle)
        private val timestampText = itemView.findViewById<TextView>(R.id.notificationTimestamp)

        fun bind(notification: JSONObject) {
            val message = notification.optString("message")
            val sender = notification.optString("sender_name")
            val createdAt = notification.optString("created_at")
            val status = if (notification.optInt("is_read", 0) == 0) "Unread" else "Read"

            titleText.text = message
            subtitleText.text = "From: $sender | $status"
            timestampText.text = createdAt
        }
    }
}
