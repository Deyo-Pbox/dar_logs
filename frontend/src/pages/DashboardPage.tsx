import { useDashboardStats } from '@/hooks/useDashboard'
import { useActivities } from '@/hooks/useActivities'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { FileText, Archive, Users, Activity } from 'lucide-react'
import { format } from 'date-fns'

export default function DashboardPage() {
  const { data: statsData, isPending: statsLoading } = useDashboardStats()
  const { data: recentData, isPending: recentLoading } = useActivities({
    per_page: 5,
    scope: 'all',
  })

  const stats = statsData?.stats
  const recentRecords = recentData?.records ?? []

  const cards = [
    { label: 'Total Records', value: stats?.total_records, icon: FileText, color: 'text-blue-600 bg-blue-50' },
    { label: 'Archived', value: stats?.archived_records, icon: Archive, color: 'text-amber-600 bg-amber-50' },
    { label: 'Users', value: stats?.total_users, icon: Users, color: 'text-green-600 bg-green-50' },
    { label: 'Active Users', value: stats?.active_users, icon: Activity, color: 'text-purple-600 bg-purple-50' },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">Overview of system activity and statistics</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map(({ label, value, icon: Icon, color }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{label}</CardTitle>
              <div className={`rounded-lg p-2 ${color}`}>
                <Icon className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              {statsLoading ? (
                <Skeleton className="h-8 w-16" />
              ) : (
                <div className="text-2xl font-bold">{value ?? 0}</div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
        </CardHeader>
        <CardContent>
          {recentLoading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
            </div>
          ) : recentRecords.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-8">No records found</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="pb-2 font-medium">Municipality</th>
                    <th className="pb-2 font-medium">Claimant</th>
                    <th className="pb-2 font-medium">Title No.</th>
                    <th className="pb-2 font-medium">Status</th>
                    <th className="pb-2 font-medium hidden sm:table-cell">Date</th>
                  </tr>
                </thead>
                <tbody>
                  {recentRecords.map((r) => (
                    <tr key={r.id} className="border-b last:border-0">
                      <td className="py-2">{r.municipality}</td>
                      <td className="py-2">{r.lo_claimant || '—'}</td>
                      <td className="py-2 font-mono text-xs">{r.title_no || '—'}</td>
                      <td className="py-2">
                        <Badge variant={r.work_status === 'finished' ? 'success' : 'warning'}>
                          {r.work_status === 'finished' ? 'Done' : 'Pending'}
                        </Badge>
                      </td>
                      <td className="py-2 text-muted-foreground hidden sm:table-cell">
                        {format(new Date(r.updated_at), 'MMM d, yyyy')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
