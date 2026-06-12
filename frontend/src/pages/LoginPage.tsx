import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from '@tanstack/react-router'
import { ApiError, login as apiLogin } from '../lib/api'
import { useAuthStore } from '../stores/auth'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type FormValues = z.infer<typeof schema>

const inputClass =
  'w-full rounded-lg border border-edge/15 px-3 py-2 text-sm text-ink outline-none focus:border-azure focus:ring-2 focus:ring-azure/40'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  async function onSubmit(values: FormValues) {
    setServerError(null)
    try {
      const result = await apiLogin(values.email, values.password)
      setAuth(result.accessToken, result.user)
      navigate({ to: '/' })
    } catch (e) {
      setServerError(e instanceof ApiError ? e.message : 'Something went wrong')
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-[linear-gradient(to_right,#518bdb,#36bab8)] p-4">
      <div className="w-full max-w-sm rounded-[18px] bg-card p-8 shadow-xl">
        <h1 className="font-display text-3xl font-bold text-ink">LinkFlow</h1>
        <p className="mt-1 text-sm text-stone">Sign in to your dashboard</p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
          {serverError && (
            <div role="alert" className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">
              {serverError}
            </div>
          )}
          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-ink">
              Email
            </label>
            <input id="email" type="email" autoComplete="email" className={inputClass} {...register('email')} />
            {errors.email && <p className="mt-1 text-xs text-coral">{errors.email.message}</p>}
          </div>
          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-ink">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className={inputClass}
              {...register('password')}
            />
            {errors.password && <p className="mt-1 text-xs text-coral">{errors.password.message}</p>}
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full cursor-pointer rounded-lg bg-ink px-4 py-2.5 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
