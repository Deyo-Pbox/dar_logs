import { useState } from 'react'
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser } from '@/hooks/useUsers'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { UserPlus, Trash2, Shield, User } from 'lucide-react'

export default function UsersPage() {
  const { data, isPending } = useUsers()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()
  const deleteUser = useDeleteUser()
  const [open, setOpen] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'user' | 'admin'>('user')

  const users = data?.data ?? []

  const handleCreate = () => {
    if (!username || !password) return
    createUser.mutate(
      { username, password, role },
      {
        onSuccess: () => { toast.success('User created'); setOpen(false); setUsername(''); setPassword(''); setRole('user') },
        onError: (e) => toast.error((e as Error).message || 'Failed to create user'),
      }
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Manage Users</h1>
          <p className="text-muted-foreground">Create, update, and manage system accounts</p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <UserPlus className="h-4 w-4 mr-2" /> Add User
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          {isPending ? (
            <div className="space-y-2 p-4">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="px-4 py-2 text-left">Username</th>
                    <th className="px-4 py-2 text-left">Role</th>
                    <th className="px-4 py-2 text-left">Status</th>
                    <th className="px-4 py-2 text-left">Last Activity</th>
                    <th className="px-4 py-2 text-right w-32">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} className="border-b hover:bg-muted/30">
                      <td className="px-4 py-2 font-medium">{u.username}</td>
                      <td className="px-4 py-2">
                        <Badge variant={u.role === 'admin' ? 'default' : 'secondary'}>
                          {u.role === 'admin' ? <Shield className="h-3 w-3 mr-1" /> : <User className="h-3 w-3 mr-1" />}
                          {u.role}
                        </Badge>
                      </td>
                      <td className="px-4 py-2">
                        <Badge variant={u.approved ? 'success' : 'warning'}>
                          {u.approved ? 'Approved' : 'Pending'}
                        </Badge>
                      </td>
                      <td className="px-4 py-2 text-muted-foreground text-xs">
                        {u.last_activity ? new Date(u.last_activity).toLocaleDateString() : 'Never'}
                      </td>
                      <td className="px-4 py-2 text-right">
                        <div className="flex justify-end gap-1">
                          {!u.approved && (
                            <Button variant="ghost" size="sm" onClick={() => updateUser.mutate({ id: u.id, payload: { approved: true } }, { onSuccess: () => toast.success('Approved') })}>
                              Approve
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-destructive"
                            onClick={() => {
                              if (confirm(`Delete ${u.username}?`)) {
                                deleteUser.mutate(u.id, { onSuccess: () => toast.success('User deleted') })
                              }
                            }}
                            disabled={deleteUser.isPending}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr><td colSpan={5} className="py-12 text-center text-muted-foreground">No users found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create User</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="new-user">Username</Label>
              <Input id="new-user" value={username} onChange={(e) => setUsername(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-pass">Password</Label>
              <Input id="new-pass" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <Select value={role} onValueChange={(v) => setRole(v as 'admin' | 'user')}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="user">User</SelectItem>
                  <SelectItem value="admin">Admin</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={createUser.isPending}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
