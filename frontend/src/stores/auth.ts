import { create } from 'zustand'

interface AuthState {
  accessToken: string | null
  setAccessToken: (token: string | null) => void
  clear: () => void
}

/**
 * Holds the short-lived JWT access token in memory. The refresh token lives in
 * an HttpOnly cookie managed by the backend, so it is intentionally not kept here.
 */
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  setAccessToken: (accessToken) => set({ accessToken }),
  clear: () => set({ accessToken: null }),
}))
