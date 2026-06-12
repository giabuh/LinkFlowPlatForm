import { Link, Outlet } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'

const linkClass =
  'text-sm text-gray-600 hover:text-violet-600 [&.active]:font-semibold [&.active]:text-violet-600'

export default function RootLayout() {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <header className="border-b bg-white">
        <nav className="mx-auto flex max-w-5xl items-center gap-6 px-6 py-4">
          <span className="text-lg font-semibold text-violet-600">LinkFlow</span>
          <Link to="/" className={linkClass}>
            Home
          </Link>
          <Link to="/health" className={linkClass}>
            Health
          </Link>
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-10">
        <Outlet />
      </main>
      <TanStackRouterDevtools />
    </div>
  )
}
