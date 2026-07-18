import { createHashRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { ProtectedRoute, AdminRoute } from '@/components/auth/ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import MyWorkLogsPage from '@/pages/MyWorkLogsPage'
import PendingPage from '@/pages/PendingPage'
import CompletedPage from '@/pages/CompletedPage'
import ArchivesPage from '@/pages/ArchivesPage'
import NotificationsPage from '@/pages/NotificationsPage'
import UsersPage from '@/pages/UsersPage'
import ActivitiesPage from '@/pages/ActivitiesPage'
import NotFoundPage from '@/pages/NotFoundPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 2 * 60 * 1000,
      gcTime: 30 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
    },
    mutations: { retry: 0 },
  },
})

const router = createHashRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'my-work-logs', element: <MyWorkLogsPage /> },
          { path: 'pending', element: <PendingPage /> },
          { path: 'completed', element: <CompletedPage /> },
          { path: 'archives', element: <ArchivesPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          {
            element: <AdminRoute />,
            children: [
              { path: 'users', element: <UsersPage /> },
              { path: 'activities', element: <ActivitiesPage /> },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <Toaster position="top-right" richColors closeButton />
    </QueryClientProvider>
  )
}
