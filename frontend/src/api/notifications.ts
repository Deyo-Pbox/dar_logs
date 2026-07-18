import apiClient from './client'
import type { Notification, ApiResponse } from './types'

export async function fetchNotifications(): Promise<{ success: boolean; data: Notification[] }> {
  const { data } = await apiClient.get('/notifications')
  return data
}

export async function fetchNotificationCount(): Promise<{ success: boolean; count: number }> {
  const { data } = await apiClient.get('/notifications/count')
  return data
}

export async function markNotificationRead(id: number): Promise<ApiResponse> {
  const { data } = await apiClient.patch(`/notifications/${id}/read`)
  return data
}

export async function markAllNotificationsRead(): Promise<ApiResponse & { updated?: number }> {
  const { data } = await apiClient.post('/notifications/read-all')
  return data
}
