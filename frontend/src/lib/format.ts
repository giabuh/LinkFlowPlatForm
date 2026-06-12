export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatLongDate(date = new Date()): string {
  return date.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat().format(value)
}

export function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const s = Math.max(0, Math.floor(diffMs / 1000))
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h`
  const d = Math.floor(h / 24)
  if (d < 7) return `${d}d`
  const w = Math.floor(d / 7)
  if (w < 5) return `${w}w`
  const mo = Math.floor(d / 30)
  if (mo < 12) return `${mo}mo`
  return `${Math.floor(d / 365)}y`
}

export function isExpiringSoon(expiresAt: string | null, withinDays = 7): boolean {
  if (!expiresAt) return false
  const remaining = new Date(expiresAt).getTime() - Date.now()
  return remaining > 0 && remaining <= withinDays * 24 * 60 * 60 * 1000
}

/** ISO string -> value for <input type="datetime-local"> (local time, yyyy-MM-ddTHH:mm). */
export function toDateTimeLocal(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** <input type="datetime-local"> value -> ISO string (UTC), or null when empty. */
export function fromDateTimeLocal(local: string): string | null {
  if (!local) return null
  return new Date(local).toISOString()
}
