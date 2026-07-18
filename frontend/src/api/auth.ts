import apiClient from './client'
import type { LoginResponse, RegisterResponse } from './types'

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post('/auth/login', { username, password })
  return data
}

export async function register(username: string, password: string): Promise<RegisterResponse> {
  const { data } = await apiClient.post('/auth/register', { username, password })
  return data
}
