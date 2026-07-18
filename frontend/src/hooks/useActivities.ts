import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as api from '@/api/activities'
import type { ActivityFilters } from '@/api/activities'
import { useDebounce } from './useDebounce'

export function useActivities(filters: ActivityFilters) {
  const debouncedSearch = useDebounce(filters.search, 300)
  const queryFilters = { ...filters, search: debouncedSearch || undefined }

  return useQuery({
    queryKey: ['activities', queryFilters],
    queryFn: () => api.fetchActivities(queryFilters),
    staleTime: 30 * 1000,
    placeholderData: (prev) => prev,
  })
}

export function useArchivedActivities(filters: ActivityFilters) {
  const debouncedSearch = useDebounce(filters.search, 300)
  const queryFilters = { ...filters, search: debouncedSearch || undefined }

  return useQuery({
    queryKey: ['activities', 'archived', queryFilters],
    queryFn: () => api.fetchArchivedActivities(queryFilters),
    staleTime: 30 * 1000,
  })
}

export function useActivity(id: number | null) {
  return useQuery({
    queryKey: ['activities', id],
    queryFn: () => api.fetchActivity(id!),
    enabled: !!id,
    staleTime: 60 * 1000,
  })
}

export function useCreateActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.createActivity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useUpdateActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Parameters<typeof api.updateActivity>[1] }) =>
      api.updateActivity(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
  })
}

export function useArchiveActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.archiveActivity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useRestoreActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.restoreActivity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useDeleteActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.deleteActivity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useRouteActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, targetUserId }: { id: number; targetUserId: number }) =>
      api.routeActivity(id, targetUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
