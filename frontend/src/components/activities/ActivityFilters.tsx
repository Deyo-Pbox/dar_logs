import { useState } from 'react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Search, X } from 'lucide-react'
import type { ActivityFilters as Filters } from '@/api/activities'
import { useMunicipalities } from '@/hooks/useDashboard'

interface Props {
  filters: Filters
  onChange: (f: Filters) => void
  showStatus?: boolean
  showScope?: boolean
}

export function ActivityFilters({ filters, onChange, showStatus = true }: Props) {
  const { data: muniData } = useMunicipalities()
  const municipalities: string[] = muniData?.municipalities ?? muniData?.data ?? []
  const [search, setSearch] = useState(filters.search ?? '')

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search records..."
          className="pl-8"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            onChange({ ...filters, search: e.target.value, page: 1 })
          }}
        />
        {search && (
          <button
            className="absolute right-2.5 top-2.5"
            onClick={() => { setSearch(''); onChange({ ...filters, search: '', page: 1 }) }}
          >
            <X className="h-4 w-4 text-muted-foreground" />
          </button>
        )}
      </div>

      {showStatus && (
        <div className="flex gap-2">
          <Button
            variant={!filters.work_status ? 'default' : 'outline'}
            size="sm"
            onClick={() => onChange({ ...filters, work_status: '', page: 1 })}
          >
            All
          </Button>
          <Button
            variant={filters.work_status === 'not_finished' ? 'default' : 'outline'}
            size="sm"
            onClick={() => onChange({ ...filters, work_status: 'not_finished', page: 1 })}
          >
            Pending
          </Button>
          <Button
            variant={filters.work_status === 'finished' ? 'default' : 'outline'}
            size="sm"
            onClick={() => onChange({ ...filters, work_status: 'finished', page: 1 })}
          >
            Done
          </Button>
        </div>
      )}

      {municipalities.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {filters.municipality && (
            <Badge variant="secondary" className="cursor-pointer" onClick={() => onChange({ ...filters, municipality: '', page: 1 })}>
              {filters.municipality} <X className="ml-1 h-3 w-3" />
            </Badge>
          )}
          {!filters.municipality && municipalities.slice(0, 8).map((m) => (
            <Badge
              key={m}
              variant="outline"
              className="cursor-pointer hover:bg-primary/10"
              onClick={() => onChange({ ...filters, municipality: m, page: 1 })}
            >
              {m}
            </Badge>
          ))}
        </div>
      )}
    </div>
  )
}
