import { useAuthStore } from '../stores/auth'
import type { ApiEnvelope, LinkItem, LinkPayload, LoginResult, Page, User, UserPayload } from './types'

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const BASE = '/api/v1'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = useAuthStore.getState().token
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  })

  if (res.status === 204) return undefined as T

  let body: ApiEnvelope<T> | undefined
  try {
    body = (await res.json()) as ApiEnvelope<T>
  } catch {
    if (res.ok) return undefined as T
    throw new ApiError(res.status, res.statusText || 'Network error')
  }

  if (!res.ok) {
    throw new ApiError(body?.code ?? res.status, body?.message ?? 'Request failed')
  }
  return body.data
}

export function login(email: string, password: string): Promise<LoginResult> {
  return request<LoginResult>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
}

export function getMe(): Promise<User> {
  return request<User>('/auth/me')
}

export function logout(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' })
}

export interface LinkQuery {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  title?: string
  active?: boolean
}

export function listLinks(query: LinkQuery = {}): Promise<Page<LinkItem>> {
  const params = new URLSearchParams()
  if (query.page) params.set('page', String(query.page))
  if (query.size) params.set('size', String(query.size))
  if (query.sortBy) params.set('sortBy', query.sortBy)
  if (query.sortDirection) params.set('sortDirection', query.sortDirection)
  if (query.title) params.set('title', query.title)
  if (query.active != null) params.set('active', String(query.active))
  const qs = params.toString()
  return request<Page<LinkItem>>(`/links${qs ? `?${qs}` : ''}`)
}

export function createLink(body: LinkPayload): Promise<LinkItem> {
  return request<LinkItem>('/links', { method: 'POST', body: JSON.stringify(body) })
}

export function updateLink(id: string, body: LinkPayload): Promise<LinkItem> {
  return request<LinkItem>(`/links/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function setLinkActive(id: string, active: boolean): Promise<LinkItem> {
  return request<LinkItem>(`/links/${id}/${active ? 'activate' : 'deactivate'}`, { method: 'PATCH' })
}

export function deleteLink(id: string): Promise<void> {
  return request<void>(`/links/${id}`, { method: 'DELETE' })
}

export function listUsers(query: { page?: number; size?: number } = {}): Promise<Page<User>> {
  const params = new URLSearchParams()
  if (query.page) params.set('page', String(query.page))
  if (query.size) params.set('size', String(query.size))
  const qs = params.toString()
  return request<Page<User>>(`/users${qs ? `?${qs}` : ''}`)
}

export function createUser(body: UserPayload): Promise<User> {
  return request<User>('/users', { method: 'POST', body: JSON.stringify(body) })
}

export function updateUser(id: string, body: UserPayload): Promise<User> {
  return request<User>(`/users/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteUser(id: string): Promise<void> {
  return request<void>(`/users/${id}`, { method: 'DELETE' })
}
