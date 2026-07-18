import { useNotifications, useMarkNotificationRead, useMarkAllNotificationsRead } from '@/hooks/useNotifications'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from 'sonner'
import { Bell, CheckCheck, MailOpen } from 'lucide-react'
import { format } from 'date-fns'

export default function NotificationsPage() {
  const { data, isPending } = useNotifications()
  const markRead = useMarkNotificationRead()
  const markAllRead = useMarkAllNotificationsRead()

  const notifications = data?.data ?? []
  const unreadCount = notifications.filter((n) => !n.is_read).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Notifications</h1>
          <p className="text-muted-foreground">
            {unreadCount > 0 ? `${unreadCount} unread` : 'All caught up!'}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button variant="outline" onClick={() => markAllRead.mutate(undefined, { onSuccess: () => toast.success('All marked read') })} disabled={markAllRead.isPending}>
            <CheckCheck className="h-4 w-4 mr-2" /> Mark All Read
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="p-0">
          {isPending ? (
            <div className="space-y-2 p-4">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
            </div>
          ) : notifications.length === 0 ? (
            <div className="flex flex-col items-center py-12 text-center">
              <Bell className="h-8 w-8 text-muted-foreground/40" />
              <p className="mt-2 text-sm text-muted-foreground">No notifications yet</p>
            </div>
          ) : (
            <div className="divide-y">
              {notifications.map((n) => (
                <div key={n.id} className={`flex items-start gap-3 p-4 ${!n.is_read ? 'bg-muted/30' : ''}`}>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm">{n.message}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {format(new Date(n.created_at), 'MMM d, yyyy h:mm a')}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {!n.is_read && (
                      <Badge variant="secondary" className="text-xs">New</Badge>
                    )}
                    {!n.is_read && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => markRead.mutate(n.id, { onSuccess: () => toast.success('Marked read') })}
                        disabled={markRead.isPending}
                      >
                        <MailOpen className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
