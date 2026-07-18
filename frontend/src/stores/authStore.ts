import type { User } from '@/api/types'
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { decodeJwt } from '@/lib/jwt'

interface AuthState {
  token: string | null
  user: User | null
  setAuth: (token: string, user: User) => void
  logout: () => void
  isAdmin: () => boolean
  isExpired: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null }),
      isAdmin: () => get().user?.role === 'admin',
      isExpired: () => {
        const token = get().token
        if (!token) return true
        try {
          const payload = decodeJwt(token)
          return payload.exp * 1000 < Date.now()
        } catch {
          return true
        }
      },
    }),
    { name: 'dar_auth' }
  )
)
