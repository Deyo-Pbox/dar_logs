import apiClient from './client'
import type { ActivityLog, ActivityPayload, PaginatedResponse, ApiResponse, AuditLog } from './types'

export interface ActivityFilters {
  scope?: 'user' | 'all'
  work_status?: 'not_finished' | 'finished' | ''
  municipality?: string
  search?: string
  page?: number
  per_page?: number
}

export async function fetchActivities(filters: ActivityFilters = {}): Promise<PaginatedResponse<ActivityLog>> {
  const { data } = await apiClient.get('/activities', { params: filters })
  return data
}

export async function fetchArchivedActivities(filters: ActivityFilters = {}): Promise<PaginatedResponse<ActivityLog>> {
  const { data } = await apiClient.get('/activities/archived', { params: filters })
  return data
}

export async function fetchAuditLogs(limit = 100): Promise<{ success: boolean; data: AuditLog[] }> {
  const { data } = await apiClient.get('/audit', { params: { limit } })
  return data
}

export async function fetchActivity(id: number): Promise<{ success: boolean } & ActivityLog> {
  const { data } = await apiClient.get(`/activities/${id}`)
  return data
}

export async function createActivity(payload: ActivityPayload): Promise<ApiResponse & { id?: number }> {
  const { data } = await apiClient.post('/activities', payload)
  return data
}

export async function updateActivity(id: number, payload: Partial<ActivityPayload>): Promise<ApiResponse> {
  const { data } = await apiClient.put(`/activities/${id}`, payload)
  return data
}

export async function archiveActivity(id: number): Promise<ApiResponse> {
  const { data } = await apiClient.delete(`/activities/${id}`)
  return data
}

export async function restoreActivity(id: number): Promise<ApiResponse> {
  const { data } = await apiClient.post(`/activities/${id}/restore`)
  return data
}

export async function deleteActivity(id: number): Promise<ApiResponse> {
  const { data } = await apiClient.delete(`/activities/${id}/permanent`)
  return data
}

export async function routeActivity(id: number, targetUserId: number): Promise<ApiResponse> {
  const { data } = await apiClient.post(`/activities/${id}/route`, { user_id: targetUserId })
  return data
}
