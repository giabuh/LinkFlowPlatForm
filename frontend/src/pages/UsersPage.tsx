import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { ApiError, deleteUser, listUsers } from '../lib/api'
import type { User } from '../lib/types'
import { UserFormModal } from '../components/UserFormModal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { iconBtnClass } from '../lib/ui'
import { useAuthStore } from '../stores/auth'

export default function UsersPage() {
  const qc = useQueryClient()
  const meId = useAuthStore((s) => s.user?.id)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null)

  const { data, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: () => listUsers({ page: 1, size: 50 }),
    retry: false,
  })
  const forbidden = error instanceof ApiError && error.code === 403

  const remove = useMutation({
    mutationFn: (id: string) => deleteUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      setDeleteTarget(null)
    },
  })

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }
  function openEdit(u: User) {
    setEditing(u)
    setFormOpen(true)
  }

  return (
    <div className="space-y-6 px-8 py-8 lg:px-12">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">Users</h1>
          <p className="mt-1 text-sm text-stone">Manage platform users (admin only).</p>
        </div>
        {!forbidden && (
          <button
            type="button"
            onClick={openCreate}
            className="flex cursor-pointer items-center gap-2 rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90"
          >
            <Plus className="h-4 w-4" /> New user
          </button>
        )}
      </header>

      <div className="overflow-hidden rounded-[18px] border border-edge/10 bg-card">
        {forbidden ? (
          <div className="p-6 text-sm text-coral">You don't have access to this page.</div>
        ) : error ? (
          <div className="p-6 text-sm text-coral">Couldn't load users.</div>
        ) : isLoading ? (
          <div className="p-6 text-sm text-stone">Loading…</div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-edge/10 text-xs uppercase tracking-wide text-stone">
              <tr>
                <th className="px-6 py-3 font-medium">Email</th>
                <th className="px-6 py-3 font-medium">Name</th>
                <th className="px-6 py-3 font-medium">Role</th>
                <th className="px-6 py-3 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-edge/10">
              {data?.content.map((u) => (
                <tr key={u.id} className="hover:bg-paper/60">
                  <td className="px-6 py-3 font-medium text-ink">{u.email}</td>
                  <td className="px-6 py-3 text-stone">{u.fullName || '—'}</td>
                  <td className="px-6 py-3">
                    <span className="rounded-full bg-lavender px-2.5 py-0.5 text-xs font-medium text-orchid">{u.role}</span>
                  </td>
                  <td className="px-6 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button type="button" aria-label="Edit user" onClick={() => openEdit(u)} className={iconBtnClass}>
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        aria-label="Delete user"
                        disabled={u.id === meId}
                        title={u.id === meId ? "You can't delete your own account" : undefined}
                        onClick={() => setDeleteTarget(u)}
                        className={iconBtnClass}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <UserFormModal open={formOpen} user={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={deleteTarget != null}
        title="Delete user"
        message={deleteTarget ? `Delete ${deleteTarget.email}? This can't be undone.` : ''}
        loading={remove.isPending}
        error={remove.isError ? 'Failed to delete. Try again.' : null}
        onConfirm={() => deleteTarget && remove.mutate(deleteTarget.id)}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  )
}
