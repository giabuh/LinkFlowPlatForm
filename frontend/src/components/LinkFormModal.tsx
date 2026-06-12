import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, createLink, updateLink } from '../lib/api'
import type { LinkItem } from '../lib/types'
import { fromDateTimeLocal, toDateTimeLocal } from '../lib/format'
import { inputClass } from '../lib/ui'
import { Modal } from './Modal'
import { FormField } from './FormField'

const schema = z.object({
  originalUrl: z.string().refine((u) => /^https?:\/\/.+/i.test(u.trim()), 'Enter a valid http(s) URL'),
  title: z.string().max(255, 'Max 255 characters').optional(),
  expiresAt: z.string().optional(),
})
type Values = z.infer<typeof schema>

export function LinkFormModal({ open, onClose, link }: { open: boolean; onClose: () => void; link: LinkItem | null }) {
  const qc = useQueryClient()
  const editing = link != null
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (open) {
      reset({
        originalUrl: link?.originalUrl ?? '',
        title: link?.title ?? '',
        expiresAt: toDateTimeLocal(link?.expiresAt ?? null),
      })
    }
  }, [open, link, reset])

  const mutation = useMutation({
    mutationFn: (v: Values) => {
      const payload = {
        originalUrl: v.originalUrl.trim(),
        title: v.title?.trim() ? v.title.trim() : null,
        expiresAt: fromDateTimeLocal(v.expiresAt ?? ''),
      }
      return link ? updateLink(link.id, payload) : createLink(payload)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['links'] })
      onClose()
    },
  })

  const serverError = mutation.isError
    ? mutation.error instanceof ApiError
      ? mutation.error.message
      : 'Something went wrong'
    : null

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Edit link' : 'New link'}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4" noValidate>
        {serverError && <div role="alert" className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{serverError}</div>}
        <FormField label="Destination URL" error={errors.originalUrl?.message}>
          <input className={inputClass} placeholder="https://example.com/very/long/url" {...register('originalUrl')} />
        </FormField>
        <FormField label="Title" hint="Optional" error={errors.title?.message}>
          <input className={inputClass} {...register('title')} />
        </FormField>
        <FormField label="Expires at" hint="Optional — leave empty for no expiry" error={errors.expiresAt?.message}>
          <input type="datetime-local" className={inputClass} {...register('expiresAt')} />
        </FormField>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className="cursor-pointer rounded-lg px-4 py-2 text-sm text-ink transition-colors hover:bg-paper">
            Cancel
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="cursor-pointer rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving…' : editing ? 'Save changes' : 'Create link'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
