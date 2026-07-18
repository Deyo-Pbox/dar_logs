import { useState } from 'react'
import { useArchivedActivities, useRestoreActivity, useDeleteActivity } from '@/hooks/useActivities'
import { ActivityPagination } from '@/components/activities/ActivityTable'
import { ActivityFilters } from '@/components/activities/ActivityFilters'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'
import { Undo2, Trash2 } from 'lucide-react'
import type { ActivityFilters as Filters } from '@/api/activities'

export default function ArchivesPage() {
  const [filters, setFilters] = useState<Filters>({ page: 1, per_page: 20 })
  const { data, isPending } = useArchivedActivities(filters)
  const restoreMutation = useRestoreActivity()
  const deleteMutation = useDeleteActivity()

  const records = data?.records ?? []
  const total = data?.total ?? 0
  const page = data?.page ?? 1
  const perPage = data?.per_page ?? 20

  const getOnRestore = (id: number) => () => restoreMutation.mutate(id, { onSuccess: () => toast.success('Record restored'), onError: () => toast.error('Failed') })
  const getOnDelete = (id: number) => () => deleteMutation.mutate(id, { onSuccess: () => toast.success('Record deleted'), onError: () => toast.error('Failed') })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Archives</h1>
        <p className="text-muted-foreground">Archived records — restore or permanently delete</p>
      </div>
      <ActivityFilters filters={filters} onChange={setFilters} showStatus={false} />
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-muted/50">
              <th className="px-3 py-2 text-left font-medium text-muted-foreground">Municipality</th>
              <th className="px-3 py-2 text-left font-medium text-muted-foreground">Claimant</th>
              <th className="px-3 py-2 text-left font-medium text-muted-foreground">Title No.</th>
              <th className="px-3 py-2 text-left font-medium text-muted-foreground">Archived Date</th>
              <th className="px-3 py-2 text-right font-medium text-muted-foreground w-24">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isPending ? (
              <tr><td colSpan={5} className="py-12 text-center text-muted-foreground">Loading...</td></tr>
            ) : records.length === 0 ? (
              <tr><td colSpan={5} className="py-12 text-center text-muted-foreground">No archived records</td></tr>
            ) : records.map((r) => (
              <tr key={r.id} className="border-b hover:bg-muted/30">
                <td className="px-3 py-2 font-medium">{r.municipality}</td>
                <td className="px-3 py-2">{r.lo_claimant || '—'}</td>
                <td className="px-3 py-2 font-mono text-xs">{r.title_no || '—'}</td>
                <td className="px-3 py-2 text-muted-foreground text-xs">{r.archived_at ? new Date(r.archived_at).toLocaleDateString() : '—'}</td>
                <td className="px-3 py-2 text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" className="h-7 w-7" onClick={getOnRestore(r.id)} disabled={restoreMutation.isPending}>
                      <Undo2 className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={getOnDelete(r.id)} disabled={deleteMutation.isPending}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ActivityPagination page={page} total={total} perPage={perPage} onPageChange={(p) => setFilters({ ...filters, page: p })} />
    </div>
  )
}
