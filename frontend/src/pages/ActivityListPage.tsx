import { useState } from 'react'
import { useActivities, useCreateActivity, useUpdateActivity, useArchiveActivity, useRouteActivity } from '@/hooks/useActivities'
import { useRouteUsers } from '@/hooks/useDashboard'
import { ActivityTable, ActivityPagination } from '@/components/activities/ActivityTable'
import { ActivityFilters } from '@/components/activities/ActivityFilters'
import { ActivityForm } from '@/components/activities/ActivityForm'
import { RouteDialog } from '@/components/activities/RouteDialog'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'
import { Plus } from 'lucide-react'
import type { ActivityLog, ActivityPayload } from '@/api/types'
import type { ActivityFilters as Filters } from '@/api/activities'

interface Props {
  title: string
  description: string
  defaultFilters: Partial<Filters>
  showStatus?: boolean
}

export function ActivityListPage({ title, description, defaultFilters, showStatus = true }: Props) {
  const [filters, setFilters] = useState<Filters>({ page: 1, per_page: 20, ...defaultFilters })
  const [formOpen, setFormOpen] = useState(false)
  const [editRecord, setEditRecord] = useState<ActivityLog | null>(null)
  const [routeRecord, setRouteRecord] = useState<ActivityLog | null>(null)

  const { data, isPending } = useActivities(filters)
  const { data: userData } = useRouteUsers()

  const create = useCreateActivity()
  const update = useUpdateActivity()
  const archive = useArchiveActivity()
  const route = useRouteActivity()

  const records = data?.records ?? []
  const total = data?.total ?? 0
  const page = data?.page ?? 1
  const perPage = data?.per_page ?? 20
  const users = userData?.data ?? []

  const handleSubmit = (payload: ActivityPayload) => {
    if (editRecord) {
      update.mutate(
        { id: editRecord.id, payload },
        {
          onSuccess: () => { toast.success('Record updated'); setFormOpen(false); setEditRecord(null) },
          onError: () => toast.error('Failed to update record'),
        }
      )
    } else {
      create.mutate(payload, {
        onSuccess: () => { toast.success('Record created'); setFormOpen(false) },
        onError: () => toast.error('Failed to create record'),
      })
    }
  }

  const handleArchive = (record: ActivityLog) => {
    archive.mutate(record.id, {
      onSuccess: () => toast.success('Record archived'),
      onError: (e: Error) => toast.error(e.message || 'Failed to archive'),
    })
  }

  const handleToggleStatus = (record: ActivityLog) => {
    update.mutate(
      { id: record.id, payload: { work_status: record.work_status === 'not_finished' ? 'finished' : 'not_finished' } },
      { onSuccess: () => toast.success('Status updated'), onError: () => toast.error('Failed to update') }
    )
  }

  const handleRoute = (recordId: number, targetUserId: number) => {
    route.mutate(
      { id: recordId, targetUserId },
      { onSuccess: () => toast.success('Record routed'), onError: () => toast.error('Failed to route') }
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{title}</h1>
          <p className="text-muted-foreground">{description}</p>
        </div>
        <Button onClick={() => { setEditRecord(null); setFormOpen(true) }}>
          <Plus className="h-4 w-4 mr-2" /> New Record
        </Button>
      </div>

      <ActivityFilters
        filters={filters}
        onChange={setFilters}
        showStatus={showStatus}
      />

      <ActivityTable
        records={records}
        loading={isPending}
        onEdit={(r) => { setEditRecord(r); setFormOpen(true) }}
        onRoute={(r) => setRouteRecord(r)}
        onArchive={handleArchive}
        onToggleStatus={handleToggleStatus}
      />

      <ActivityPagination page={page} total={total} perPage={perPage} onPageChange={(p) => setFilters({ ...filters, page: p })} />

      <ActivityForm
        open={formOpen}
        onOpenChange={setFormOpen}
        onSubmit={handleSubmit}
        record={editRecord}
        loading={create.isPending || update.isPending}
      />

      <RouteDialog
        open={!!routeRecord}
        onOpenChange={(v) => { if (!v) setRouteRecord(null) }}
        record={routeRecord}
        users={users}
        onRoute={handleRoute}
        loading={route.isPending}
      />
    </div>
  )
}
