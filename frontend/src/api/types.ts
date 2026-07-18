export interface User {
  id: number
  username: string
  role: 'admin' | 'user'
  approved: boolean
  last_activity: string | null
  created_at: string
  updated_at?: string
}

export interface ActivityLog {
  id: number
  municipality: string
  lo_claimant: string | null
  title_no: string | null
  odts_no: string | null
  lot_no: string | null
  survey_no: string | null
  area_has: number | null
  location: string | null
  transmitted_documents: string | null
  route_to: string | null
  route_to_user_id: number | null
  routed_from_user_id: number | null
  routed_at: string | null
  received_by_control_no: string | null
  remarks_action_taken: string | null
  work_status: 'not_finished' | 'finished'
  archived_at: string | null
  archived_by: number | null
  created_by: number
  updated_by: number | null
  created_at: string
  updated_at: string
  route_to_username?: string | null
  routed_from_username?: string | null
  created_by_username?: string | null
}

export interface ActivityPayload {
  municipality: string
  lo_claimant?: string
  title_no?: string
  odts_no?: string
  lot_no?: string
  survey_no?: string
  area_has?: number
  location?: string
  transmitted_documents?: string
  route_to_user_id?: number | null
  received_by_control_no?: string
  remarks_action_taken?: string
  work_status?: 'not_finished' | 'finished'
}

export interface DashboardStats {
  total_records: number
  archived_records: number
  total_users: number
  recent_edits: number
  active_users: number
}

export interface Notification {
  id: number
  user_id: number
  type: string
  record_id: number | null
  sender_id: number | null
  message: string
  is_read: boolean
  created_at: string
}

export interface AuditLog {
  id: number
  user_id: number | null
  username: string
  action: string
  table_name: string
  record_id: number | null
  details: string | null
  created_at: string
}

export interface PaginatedResponse<T> {
  success: boolean
  records?: T[]
  total?: number
  page?: number
  per_page?: number
  data?: T[]
  message?: string
}

export interface ApiResponse {
  success: boolean
  message?: string
}

export interface LoginResponse {
  success: boolean
  token: string
  user: User
}

export interface RegisterResponse {
  success: boolean
  message?: string
}

export interface StatsResponse {
  success: boolean
  stats: DashboardStats
  user: User
}
