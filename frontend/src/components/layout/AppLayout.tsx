import { Link, useLocation, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationCount } from '@/hooks/useNotifications'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  LayoutDashboard,
  ClipboardList,
  Clock,
  CheckCircle,
  Archive,
  Bell,
  Users,
  Activity,
  Menu,
  X,
  LogOut,
  User,
  ChevronLeft,
} from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/my-work-logs', label: 'My Work Logs', icon: ClipboardList },
  { to: '/pending', label: 'Pending Records', icon: Clock },
  { to: '/completed', label: 'Completed', icon: CheckCircle },
  { to: '/archives', label: 'Archives', icon: Archive },
  { to: '/notifications', label: 'Notifications', icon: Bell, badge: true },
]

const adminItems = [
  { to: '/users', label: 'Manage Users', icon: Users },
  { to: '/activities', label: 'All Activities', icon: Activity },
]

export function AppLayout() {
  const location = useLocation()
  const { user, logout } = useAuthStore()
  const { data: notifData } = useNotificationCount()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const notifCount = notifData?.count ?? 0

  const NavLink = ({ to, icon: Icon, label, end, badge }: { to: string; icon: React.ElementType; label: string; end?: boolean; badge?: boolean }) => {
    const isActive = end ? location.pathname === to : location.pathname.startsWith(to)
    return (
      <Link
        to={to}
        className={cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
          isActive
            ? 'bg-sidebar-primary text-sidebar-primary-foreground'
            : 'text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'
        )}
        onClick={() => setSidebarOpen(false)}
      >
        <Icon className="h-4 w-4" />
        <span className="flex-1">{label}</span>
        {badge && notifCount > 0 && (
          <Badge variant="destructive" className="ml-auto h-5 min-w-5 px-1 text-xs">{notifCount}</Badge>
        )}
      </Link>
    )
  }

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-black/50 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Sidebar */}
      <aside className={cn(
        'fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r bg-sidebar transition-transform duration-200 lg:static lg:translate-x-0',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      )}>
        <div className="flex h-14 items-center gap-2 border-b px-4">
          <img src="./assets/images/dar-logo-white-bg.png" alt="DAR Logo" className="h-8 w-auto" />
          <span className="font-semibold text-sidebar-foreground">DAR Activity Logs</span>
          <Button variant="ghost" size="icon" className="ml-auto lg:hidden" onClick={() => setSidebarOpen(false)}>
            <X className="h-4 w-4" />
          </Button>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {navItems.map((item) => (
            <NavLink key={item.to} {...item} />
          ))}
          {user?.role === 'admin' && (
            <>
              <Separator className="my-2" />
              <p className="px-3 text-xs font-medium text-muted-foreground">Admin</p>
              {adminItems.map((item) => (
                <NavLink key={item.to} {...item} />
              ))}
            </>
          )}
        </nav>
        <div className="border-t p-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-sidebar-primary text-sidebar-primary-foreground">
                  <User className="h-4 w-4" />
                </div>
                <div className="flex-1 text-left">
                  <p className="text-sm font-medium">{user?.username}</p>
                  <p className="text-xs text-muted-foreground capitalize">{user?.role}</p>
                </div>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>My Account</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={logout}>
                <LogOut className="h-4 w-4" />
                Sign Out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-14 shrink-0 items-center gap-4 border-b bg-background px-4 lg:px-6">
          <Button variant="ghost" size="icon" className="lg:hidden" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </Button>
          <Button variant="ghost" size="icon" className="hidden lg:flex" onClick={() => setSidebarOpen(!sidebarOpen)}>
            <ChevronLeft className="h-5 w-5" />
          </Button>
          <div className="flex-1" />
          <span className="text-sm text-muted-foreground">Welcome, {user?.username}</span>
        </header>
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
