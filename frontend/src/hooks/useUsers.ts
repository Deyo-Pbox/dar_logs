import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as api from '@/api/users'

export function useUsers() {
  return useQuery({
    queryKey: ['users'],
    queryFn: api.fetchUsers,
    staleTime: 2 * 60 * 1000,
  })
}

export function useUser(id: number | null) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => api.fetchUser(id!),
    enabled: !!id,
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Parameters<typeof api.updateUser>[1] }) =>
      api.updateUser(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: api.deleteUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
