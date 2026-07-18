import { useQuery } from '@tanstack/react-query'
import * as api from '@/api/dashboard'

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: api.fetchDashboardStats,
    staleTime: 5 * 60 * 1000,
  })
}

export function usePendingCount() {
  return useQuery({
    queryKey: ['dashboard', 'pending-count'],
    queryFn: api.fetchPendingCount,
    staleTime: 60 * 1000,
    refetchInterval: 5 * 60 * 1000,
  })
}

export function useMunicipalities() {
  return useQuery({
    queryKey: ['references', 'municipalities'],
    queryFn: api.fetchMunicipalities,
    staleTime: 30 * 60 * 1000,
  })
}

export function useRouteUsers() {
  return useQuery({
    queryKey: ['references', 'route-users'],
    queryFn: api.fetchRouteUsers,
    staleTime: 5 * 60 * 1000,
  })
}
