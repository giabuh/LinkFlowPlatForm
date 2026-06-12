import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Power, Trash2 } from 'lucide-react'
import { deleteLink, listLinks, setLinkActive } from '../lib/api'
import type { LinkItem } from '../lib/types'
import { StatusBadge } from '../components/StatusBadge'
import { LinkFormModal } from '../components/LinkFormModal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { iconBtnClass } from '../lib/ui'
import { formatDate, formatNumber } from '../lib/format'

const PAGE_SIZE = 10

export default function LinksPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(1)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<LinkItem | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<LinkItem | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['links', 'page', page],
    queryFn: () => listLinks({ page, size: PAGE_SIZE, sortBy: 'createdAt', sortDirection: 'desc' }),
    placeholderData: keepPreviousData,
  })

  const toggle = useMutation({
    mutationFn: (l: LinkItem) => setLinkActive(l.id, !l.active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['links'] }),
  })
  const remove = useMutation({
    mutationFn: (id: string) => deleteLink(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['links'] })
      setDeleteTarget(null)
    },
  })

  const links = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }
  function openEdit(l: LinkItem) {
    setEditing(l)
    setFormOpen(true)
  }

  return (
    <div className="space-y-6 px-8 py-8 lg:px-12">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">My Links</h1>
          <p className="mt-1 text-sm text-stone">{data ? formatNumber(data.totalElements) : '—'} links</p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex cursor-pointer items-center gap-2 rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90"
        >
          <Plus className="h-4 w-4" /> New link
        </button>
      </header>

      <div className="overflow-hidden rounded-[18px] border border-edge/10 bg-card">
        {isError ? (
          <div className="p-6 text-sm text-coral">Couldn't load links.</div>
        ) : isLoading && !data ? (
          <div className="p-6 text-sm text-stone">Loading…</div>
        ) : links.length === 0 ? (
          <div className="p-10 text-center">
            <p className="text-sm text-stone">No links yet.</p>
            <button
              type="button"
              onClick={openCreate}
              className="mt-3 cursor-pointer rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white"
            >
              Create your first link
            </button>
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-edge/10 text-xs uppercase tracking-wide text-stone">
              <tr>
                <th className="px-6 py-3 font-medium">Short code</th>
                <th className="px-6 py-3 font-medium">Destination</th>
                <th className="px-6 py-3 font-medium">Clicks</th>
                <th className="px-6 py-3 font-medium">Status</th>
                <th className="px-6 py-3 font-medium">Created</th>
                <th className="px-6 py-3 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-edge/10">
              {links.map((l) => (
                <tr key={l.id} className="hover:bg-paper/60">
                  <td className="px-6 py-3 font-medium text-ink">/{l.shortCode}</td>
                  <td className="max-w-xs truncate px-6 py-3 text-stone">{l.title || l.originalUrl}</td>
                  <td className="px-6 py-3 tabular-nums text-stone">{formatNumber(l.clickCount)}</td>
                  <td className="px-6 py-3">
                    <StatusBadge active={l.active} />
                  </td>
                  <td className="px-6 py-3 text-stone">{formatDate(l.createdAt)}</td>
                  <td className="px-6 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button type="button" aria-label="Edit link" onClick={() => openEdit(l)} className={iconBtnClass}>
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        aria-label={l.active ? 'Deactivate link' : 'Activate link'}
                        disabled={toggle.isPending}
                        onClick={() => toggle.mutate(l)}
                        className={iconBtnClass}
                      >
                        <Power className="h-4 w-4" />
                      </button>
                      <button type="button" aria-label="Delete link" onClick={() => setDeleteTarget(l)} className={iconBtnClass}>
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

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 text-sm">
          <button
            type="button"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
            className="cursor-pointer rounded-lg border border-edge/15 px-3 py-1.5 text-ink disabled:cursor-default disabled:opacity-40"
          >
            Prev
          </button>
          <span className="text-stone">
            Page {page} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="cursor-pointer rounded-lg border border-edge/15 px-3 py-1.5 text-ink disabled:cursor-default disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}

      <LinkFormModal open={formOpen} link={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={deleteTarget != null}
        title="Delete link"
        message={deleteTarget ? `Delete /${deleteTarget.shortCode}? This can't be undone.` : ''}
        loading={remove.isPending}
        error={remove.isError ? 'Failed to delete. Try again.' : null}
        onConfirm={() => deleteTarget && remove.mutate(deleteTarget.id)}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  )
}
