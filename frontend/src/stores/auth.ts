import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '../lib/types'

interface AuthState {
  token: string | null
  user: User | null
  setAuth: (token: string, user: User) => void
  setUser: (user: User) => void
  clear: () => void
}

/**
 * Persists the access token + user so a page refresh stays logged in. The
 * long-lived refresh token stays in an HttpOnly cookie owned by the backend.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      setUser: (user) => set({ user }),
      clear: () => set({ token: null, user: null }),
    }),
    { name: 'linkflow-auth' },
  ),
)
