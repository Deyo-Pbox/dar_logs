import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as api from '@/api/notifications'

export function useNotifications() {
  return useQuery({
    queryKey: ['notifications'],
    queryFn: api.fetchNotifications,
    staleTime: 30 * 1000,
  })
}

export function useNotificationCount() {
  return useQuery({
    queryKey: ['notifications', 'count'],
    queryFn: api.fetchNotificationCount,
    staleTime: 60 * 1000,
    refetchInterval: 5 * 60 * 1000,
  })
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
