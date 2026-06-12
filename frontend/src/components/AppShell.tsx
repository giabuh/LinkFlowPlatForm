import { useEffect } from 'react'
import { Outlet, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import Sidebar from './Sidebar'
import { ApiError, getMe } from '../lib/api'
import { useAuthStore } from '../stores/auth'

export default function AppShell() {
  const setUser = useAuthStore((s) => s.setUser)
  const clear = useAuthStore((s) => s.clear)
  const navigate = useNavigate()
  const { data, error } = useQuery({ queryKey: ['me'], queryFn: getMe, retry: false })

  useEffect(() => {
    if (data) setUser(data)
  }, [data, setUser])

  useEffect(() => {
    if (error instanceof ApiError && error.code === 401) {
      clear()
      navigate({ to: '/login' })
    }
  }, [error, clear, navigate])

  return (
    <div className="flex h-dvh bg-card">
      <Sidebar />
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
