import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { ActivityLog, User } from '@/api/types'
import { Loader2, Send } from 'lucide-react'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  record: ActivityLog | null
  users: User[]
  onRoute: (recordId: number, targetUserId: number) => void
  loading?: boolean
}

export function RouteDialog({ open, onOpenChange, record, users, onRoute, loading }: Props) {
  const [selectedUser, setSelectedUser] = useState<string>('')

  const handleRoute = () => {
    if (record && selectedUser) {
      onRoute(record.id, Number(selectedUser))
      setSelectedUser('')
      onOpenChange(false)
    }
  }

  const availableUsers = users.filter((u) => u.id !== record?.created_by)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Route Record</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-4">
          {record && (
            <p className="text-sm text-muted-foreground">
              Route <strong>{record.municipality}</strong> — {record.title_no || record.lo_claimant || 'Unnamed'} to another user
            </p>
          )}
          <Select value={selectedUser} onValueChange={setSelectedUser}>
            <SelectTrigger>
              <SelectValue placeholder="Select user..." />
            </SelectTrigger>
            <SelectContent>
              {availableUsers.map((u) => (
                <SelectItem key={u.id} value={String(u.id)}>
                  {u.username} ({u.role})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {availableUsers.length === 0 && (
            <p className="text-sm text-muted-foreground">No other users available to route to.</p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleRoute} disabled={!selectedUser || loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Send className="h-4 w-4 mr-2" />}
            Route
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
