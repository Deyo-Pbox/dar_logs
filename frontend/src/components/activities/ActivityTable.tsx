import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import type { ActivityLog } from '@/api/types'
import { useAuthStore } from '@/stores/authStore'
import { format } from 'date-fns'
import { MoreHorizontal, CheckCircle, Archive, Pencil, Trash2, UserPlus } from 'lucide-react'

interface Props {
  records: ActivityLog[]
  loading?: boolean
  onEdit?: (record: ActivityLog) => void
  onRoute?: (record: ActivityLog) => void
  onDelete?: (record: ActivityLog) => void
  onArchive?: (record: ActivityLog) => void
  onToggleStatus?: (record: ActivityLog) => void
  showActions?: boolean
  compact?: boolean
}

export function ActivityTable({ records, loading, onEdit, onRoute, onDelete, onArchive, onToggleStatus, showActions = true, compact }: Props) {
  const { user } = useAuthStore()

  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
      </div>
    )
  }

  if (records.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <div className="rounded-full bg-muted p-3">
          <Archive className="h-6 w-6 text-muted-foreground" />
        </div>
        <p className="mt-4 text-sm font-medium">No records found</p>
        <p className="text-xs text-muted-foreground">Try adjusting your filters</p>
      </div>
    )
  }

  const columns = compact
    ? ['Municipality', 'Claimant', 'Title No.', 'Status', 'Date']
    : ['Municipality', 'Claimant', 'Title No.', 'Lot No.', 'Survey No.', 'Area (ha)', 'Status', 'Routed To', 'Date']

  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            {columns.map((c) => (
              <th key={c} className="px-3 py-2 text-left font-medium text-muted-foreground">{c}</th>
            ))}
            {showActions && <th className="px-3 py-2 text-right font-medium text-muted-foreground w-10">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {records.map((r) => (
            <tr key={r.id} className="border-b hover:bg-muted/30 transition-colors">
              <td className="px-3 py-2 font-medium">{r.municipality}</td>
              <td className="px-3 py-2">{r.lo_claimant || '—'}</td>
              <td className="px-3 py-2 font-mono text-xs">{r.title_no || '—'}</td>
              {!compact && <td className="px-3 py-2">{r.lot_no || '—'}</td>}
              {!compact && <td className="px-3 py-2">{r.survey_no || '—'}</td>}
              {!compact && <td className="px-3 py-2">{r.area_has ?? '—'}</td>}
              <td className="px-3 py-2">
                <Badge variant={r.work_status === 'finished' ? 'success' : 'warning'}>
                  {r.work_status === 'finished' ? 'Done' : 'Pending'}
                </Badge>
              </td>
              <td className="px-3 py-2 text-muted-foreground">{r.route_to_username || r.route_to || '—'}</td>
              <td className="px-3 py-2 text-muted-foreground text-xs">
                {format(new Date(r.updated_at), 'MMM d, yyyy')}
              </td>
              {showActions && (
                <td className="px-3 py-2 text-right">
                  {(user?.role === 'admin' || r.created_by === user?.id) && (
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        {onEdit && (
                          <DropdownMenuItem onClick={() => onEdit(r)}>
                            <Pencil className="h-4 w-4 mr-2" /> Edit
                          </DropdownMenuItem>
                        )}
                        {onRoute && (
                          <DropdownMenuItem onClick={() => onRoute(r)}>
                            <UserPlus className="h-4 w-4 mr-2" /> Route
                          </DropdownMenuItem>
                        )}
                        {onToggleStatus && r.work_status === 'not_finished' && (
                          <DropdownMenuItem onClick={() => onToggleStatus(r)}>
                            <CheckCircle className="h-4 w-4 mr-2" /> Mark Done
                          </DropdownMenuItem>
                        )}
                        {onArchive && (
                          <DropdownMenuItem onClick={() => onArchive(r)} className="text-destructive">
                            <Archive className="h-4 w-4 mr-2" /> Archive
                          </DropdownMenuItem>
                        )}
                        {onDelete && user?.role === 'admin' && (
                          <DropdownMenuItem onClick={() => onDelete(r)} className="text-destructive">
                            <Trash2 className="h-4 w-4 mr-2" /> Delete
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function ActivityPagination({ page, total, perPage, onPageChange }: {
  page: number
  total: number
  perPage: number
  onPageChange: (p: number) => void
}) {
  const totalPages = Math.max(1, Math.ceil(total / perPage))

  return (
    <div className="flex items-center justify-between pt-4">
      <p className="text-sm text-muted-foreground">
        Page {page} of {totalPages} ({total} records)
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <Button variant="outline" size="sm" disabled={page >= totalPages} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  )
}
