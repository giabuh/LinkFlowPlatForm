import type { ComponentType } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { LayoutDashboard, Link2, LogOut, Users } from 'lucide-react'
import { logout as apiLogout } from '../lib/api'
import { useAuthStore } from '../stores/auth'

const base = 'flex items-center gap-2.5 rounded-lg px-3 py-1.5 text-sm transition-colors'

function NavItem({
  to,
  icon: Icon,
  label,
  exact,
}: {
  to: string
  icon: ComponentType<{ className?: string }>
  label: string
  exact?: boolean
}) {
  return (
    <Link
      to={to}
      activeOptions={exact ? { exact: true } : undefined}
      className={`${base} text-stone hover:bg-paper`}
      activeProps={{ className: `${base} bg-paper font-medium text-ink` }}
    >
      <Icon className="h-4 w-4" />
      {label}
    </Link>
  )
}

export default function Sidebar() {
  const user = useAuthStore((s) => s.user)
  const clear = useAuthStore((s) => s.clear)
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'

  async function handleLogout() {
    try {
      await apiLogout()
    } catch {
      // Clear the client session even if the server call fails.
    }
    clear()
    navigate({ to: '/login' })
  }

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-edge/10">
      <div className="flex items-center gap-2.5 px-4 py-4">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[linear-gradient(135deg,#518bdb,#36bab8)] text-[11px] font-bold text-white">
          LF
        </div>
        <span className="text-sm font-semibold text-ink">LinkFlow</span>
      </div>

      <nav className="flex-1 space-y-0.5 px-3 py-2">
        <NavItem to="/" icon={LayoutDashboard} label="Overview" exact />
        <NavItem to="/links" icon={Link2} label="My Links" />

        {isAdmin && (
          <>
            <p className="px-3 pb-1 pt-5 text-[11px] font-medium uppercase tracking-wider text-pebble">
              Admin
            </p>
            <NavItem to="/users" icon={Users} label="Users" />
          </>
        )}
      </nav>

      <div className="border-t border-edge/10 p-3">
        <div className="mb-1 flex items-center gap-2.5 px-1">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-tealmist text-xs font-semibold text-ink">
            {(user?.fullName || user?.email || '?').slice(0, 1).toUpperCase()}
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-ink">{user?.fullName || user?.email}</p>
            <p className="truncate text-xs text-stone">{user?.email}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full cursor-pointer items-center gap-2.5 rounded-lg px-3 py-1.5 text-sm text-stone transition-colors hover:bg-paper"
        >
          <LogOut className="h-4 w-4" />
          Log out
        </button>
      </div>
    </aside>
  )
}
