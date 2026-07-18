package com.example.darlogs.data

import com.example.darlogs.BuildConfig

object ApiConfig {
    private val baseUrl: String get() = BuildConfig.API_BASE_URL
    private val api: String get() = "${baseUrl}api/v1/"

    val login: String get() = "${api}auth/login"
    val register: String get() = "${api}auth/register"

    val activities: String get() = "${api}activities"
    val activitiesArchived: String get() = "${api}activities/archived"
    fun activity(id: Int) = "${api}activities/$id"
    fun activityRoute(id: Int) = "${api}activities/$id/route"
    fun activityRestore(id: Int) = "${api}activities/$id/restore"
    fun activityPermanentDelete(id: Int) = "${api}activities/$id/permanent"

    val audit: String get() = "${api}audit"

    val users: String get() = "${api}users"
    val usersApproved: String get() = "${api}users/approved"
    fun user(id: Int) = "${api}users/$id"

    val notifications: String get() = "${api}notifications"
    val notificationsCount: String get() = "${api}notifications/count"
    fun notificationRead(id: Int) = "${api}notifications/$id/read"
    val notificationsReadAll: String get() = "${api}notifications/read-all"

    val dashboardStats: String get() = "${api}dashboard/stats"
    val dashboardPendingCount: String get() = "${api}dashboard/pending-count"

    val municipalities: String get() = "${api}references/municipalities"
    val routeUsers: String get() = "${api}references/users"

    val health: String get() = "${api}health"
}
