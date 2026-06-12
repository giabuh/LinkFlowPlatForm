import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, createUser, updateUser } from '../lib/api'
import type { User } from '../lib/types'
import { inputClass } from '../lib/ui'
import { Modal } from './Modal'
import { FormField } from './FormField'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(8, 'At least 8 characters'),
  fullName: z.string().min(1, 'Required').max(255, 'Max 255 characters'),
  role: z.enum(['USER', 'ADMIN']),
})
type Values = z.infer<typeof schema>

export function UserFormModal({ open, onClose, user }: { open: boolean; onClose: () => void; user: User | null }) {
  const qc = useQueryClient()
  const editing = user != null
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (open) {
      reset({ email: user?.email ?? '', password: '', fullName: user?.fullName ?? '', role: user?.role ?? 'USER' })
    }
  }, [open, user, reset])

  const mutation = useMutation({
    mutationFn: (v: Values) => (user ? updateUser(user.id, v) : createUser(v)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
  })

  const serverError = mutation.isError
    ? mutation.error instanceof ApiError
      ? mutation.error.message
      : 'Something went wrong'
    : null

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Edit user' : 'New user'}>
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4" noValidate>
        {serverError && <div role="alert" className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{serverError}</div>}
        <FormField label="Email" error={errors.email?.message}>
          <input type="email" className={inputClass} {...register('email')} />
        </FormField>
        <FormField
          label="Password"
          hint={editing ? 'Required — min 8 characters (re-enter to save)' : 'Min 8 characters'}
          error={errors.password?.message}
        >
          <input type="password" autoComplete="new-password" className={inputClass} {...register('password')} />
        </FormField>
        <FormField label="Full name" error={errors.fullName?.message}>
          <input className={inputClass} {...register('fullName')} />
        </FormField>
        <FormField label="Role" error={errors.role?.message}>
          <select className={inputClass} {...register('role')}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
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
            {mutation.isPending ? 'Saving…' : editing ? 'Save changes' : 'Create user'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
