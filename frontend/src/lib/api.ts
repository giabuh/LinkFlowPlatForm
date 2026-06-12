import { useAuthStore } from '../stores/auth'

/** Standard success envelope produced by the backend ApiResponseBodyAdvice. */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** Shape of PageResponse.from(page), used by paginated list endpoints. */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const BASE_URL = '/api/v1'

/**
 * Thin fetch wrapper that unwraps the backend envelope and throws ApiError on
 * failure. Sends credentials so the HttpOnly refresh cookie flows on
 * /auth/refresh, and attaches the access token from the auth store when present.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = useAuthStore.getState().accessToken
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  })

  let body: ApiResponse<T> | undefined
  try {
    body = (await res.json()) as ApiResponse<T>
  } catch {
    throw new ApiError(res.status, res.statusText || 'Network error')
  }

  if (!res.ok) {
    throw new ApiError(body?.code ?? res.status, body?.message ?? 'Request failed')
  }
  return body.data
}
