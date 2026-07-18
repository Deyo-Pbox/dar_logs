import { useEffect } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

export function ProtectedRoute() {
  const token = useAuthStore((s) => s.token)
  const isExpired = useAuthStore((s) => s.isExpired)
  const logout = useAuthStore((s) => s.logout)

  const isUnauthorized = !token || isExpired()

  useEffect(() => {
    if (isUnauthorized) {
      logout()
    }
  }, [isUnauthorized, logout])

  if (isUnauthorized) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

export function AdminRoute() {
  const { isAdmin } = useAuthStore()

  if (!isAdmin()) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
