import apiClient from './client'
import type { User, ApiResponse } from './types'

export async function fetchUsers(): Promise<{ success: boolean; data: User[] }> {
  const { data } = await apiClient.get('/users')
  return data
}

export async function fetchApprovedUsers(): Promise<{ success: boolean; data: User[] }> {
  const { data } = await apiClient.get('/users/approved')
  return data
}

export async function fetchUser(id: number): Promise<{ success: boolean } & User> {
  const { data } = await apiClient.get(`/users/${id}`)
  return data
}

export async function createUser(payload: { username: string; password: string; role: 'admin' | 'user' }): Promise<ApiResponse & { id?: number; user?: User }> {
  const { data } = await apiClient.post('/users', payload)
  return data
}

export async function updateUser(id: number, payload: Partial<{ username: string; role: 'admin' | 'user'; approved: boolean }>): Promise<ApiResponse> {
  const { data } = await apiClient.put(`/users/${id}`, payload)
  return data
}

export async function deleteUser(id: number): Promise<ApiResponse> {
  const { data } = await apiClient.delete(`/users/${id}`)
  return data
}
