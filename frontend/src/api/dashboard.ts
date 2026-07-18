import apiClient from './client'
import type { StatsResponse, User } from './types'

export async function fetchDashboardStats(): Promise<StatsResponse> {
  const { data } = await apiClient.get('/dashboard/stats')
  return data
}

export async function fetchPendingCount(): Promise<{ success: boolean; count: number }> {
  const { data } = await apiClient.get('/dashboard/pending-count')
  return data
}

export async function fetchMunicipalities() {
  const { data } = await apiClient.get('/references/municipalities')
  return data
}

export async function fetchRouteUsers(): Promise<{ success: boolean; data: User[] }> {
  const { data } = await apiClient.get('/references/users')
  return data
}
