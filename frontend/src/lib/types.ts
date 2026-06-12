export type UserRole = 'USER' | 'ADMIN'

export interface User {
  id: string
  email: string
  fullName: string | null
  role: UserRole
}

export interface LoginResult {
  accessToken: string
  user: User
}

export interface LinkItem {
  id: string
  originalUrl: string
  shortCode: string
  title: string | null
  clickCount: number
  active: boolean
  expiresAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface Page<T> {
  page: number
  size: number
  totalPages: number
  totalElements: number
  content: T[]
}

/** POST /links and PUT /links/{id} body (active is toggled separately). */
export interface LinkPayload {
  originalUrl: string
  title: string | null
  expiresAt: string | null
}

/** POST /users and PUT /users/{id} body. */
export interface UserPayload {
  email: string
  password: string
  fullName: string
  role: UserRole
}
