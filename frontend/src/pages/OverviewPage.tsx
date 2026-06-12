import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { ExternalLink, Search } from 'lucide-react'
import { listLinks } from '../lib/api'
import type { LinkItem } from '../lib/types'
import { useAuthStore } from '../stores/auth'
import { KpiCard } from '../components/KpiCard'
import { formatLongDate, formatNumber, formatRelativeTime, isExpiringSoon } from '../lib/format'

type Tab = 'All' | 'Active' | 'Inactive' | 'Expiring'
const TABS: Tab[] = ['All', 'Active', 'Inactive', 'Expiring']
const AVATAR_BG = ['bg-mint', 'bg-peach', 'bg-lavender', 'bg-tealmist']

function greeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

function matchesTab(link: LinkItem, tab: Tab): boolean {
  if (tab === 'Active') return link.active
  if (tab === 'Inactive') return !link.active
  if (tab === 'Expiring') return link.active && isExpiringSoon(link.expiresAt)
  return true
}

export default function OverviewPage() {
  const user = useAuthStore((s) => s.user)
  const name = user?.fullName || user?.email || ''
  const [tab, setTab] = useState<Tab>('All')
  const [query, setQuery] = useState('')

  const { data, isLoading, isError } = useQuery({
    queryKey: ['links', 'overview'],
    queryFn: () => listLinks({ page: 1, size: 100, sortBy: 'createdAt', sortDirection: 'desc' }),
  })

  const links = useMemo(() => data?.content ?? [], [data])
  const total = data?.totalElements ?? 0
  const active = links.filter((l) => l.active).length
  const totalClicks = links.reduce((sum, l) => sum + (l.clickCount ?? 0), 0)
  const expiring = links.filter((l) => l.active && isExpiringSoon(l.expiresAt)).length

  const recent = useMemo(() => {
    const q = query.trim().toLowerCase()
    return links
      .filter((l) => matchesTab(l, tab))
      .filter((l) =>
        !q
          ? true
          : l.shortCode.toLowerCase().includes(q) ||
            (l.title ?? '').toLowerCase().includes(q) ||
            l.originalUrl.toLowerCase().includes(q),
      )
      .slice(0, 6)
  }, [links, tab, query])

  return (
    <div className="px-8 py-8 lg:px-12">
      <div className="flex items-center gap-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#518bdb,#36bab8)] text-lg font-semibold text-white">
          {(name || '?').slice(0, 1).toUpperCase()}
        </div>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">
            {greeting()}, {name}
          </h1>
          <p className="mt-0.5 text-sm text-stone">
            {formatLongDate()}
            {data ? ` · ${formatNumber(total)} links · ${formatNumber(totalClicks)} clicks` : ''}
          </p>
        </div>
      </div>

      <div className="mt-7">
        <div className="flex items-center gap-2.5 rounded-xl border border-edge/15 bg-card px-4 py-2.5 focus-within:border-azure focus-within:ring-2 focus-within:ring-azure/30">
          <Search className="h-4 w-4 shrink-0 text-stone" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search your links…"
            aria-label="Search your links"
            className="w-full bg-transparent text-sm text-ink placeholder:text-pebble outline-none"
          />
        </div>
      </div>

      {isError ? (
        <p className="mt-10 text-sm text-coral">Couldn't load your links. Is the backend running on :8080?</p>
      ) : (
        <>
          <div className="mt-8">
            <p className="mb-3 text-xs font-medium text-stone">Overview</p>
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <KpiCard label="Total links" value={isLoading ? '—' : formatNumber(total)} />
              <KpiCard label="Active" value={isLoading ? '—' : formatNumber(active)} />
              <KpiCard label="Total clicks" value={isLoading ? '—' : formatNumber(totalClicks)} />
              <KpiCard label="Expiring soon" value={isLoading ? '—' : formatNumber(expiring)} />
            </div>
          </div>

          <div className="mt-9">
            <div className="flex items-center justify-between">
              <p className="text-xs font-medium text-stone">Recent links</p>
              <Link to="/links" className="text-xs font-medium text-azure hover:underline">
                View all
              </Link>
            </div>

            <div className="mt-2 flex items-center gap-5 border-b border-edge/10 pb-2">
              {TABS.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setTab(t)}
                  className={`cursor-pointer text-sm transition-colors ${
                    t === tab ? 'font-semibold text-ink' : 'text-stone hover:text-ink'
                  }`}
                >
                  {t}
                </button>
              ))}
            </div>

            {isLoading ? (
              <p className="py-6 text-sm text-stone">Loading…</p>
            ) : recent.length === 0 ? (
              <div className="py-10 text-center">
                <p className="text-sm text-stone">{query || tab !== 'All' ? 'No links match.' : 'No links yet.'}</p>
                {!query && tab === 'All' && (
                  <Link to="/links" className="mt-3 inline-block rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white">
                    Go to My Links
                  </Link>
                )}
              </div>
            ) : (
              <ul className="mt-1">
                {recent.map((l, i) => (
                  <li
                    key={l.id}
                    className="flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-paper/70"
                  >
                    <div
                      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold text-ink ${AVATAR_BG[i % AVATAR_BG.length]}`}
                    >
                      {l.shortCode.slice(0, 1).toUpperCase()}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`h-1.5 w-1.5 shrink-0 rounded-full ${l.active ? 'bg-teal' : 'bg-pebble'}`}
                          aria-hidden
                        />
                        <p className="truncate text-sm font-medium text-ink">/{l.shortCode}</p>
                      </div>
                      <p className="truncate text-xs text-stone">{l.title || l.originalUrl}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-4 text-xs text-stone">
                      <span className="tabular-nums">{formatNumber(l.clickCount)} clicks</span>
                      <span className="tabular-nums">{formatRelativeTime(l.createdAt)}</span>
                      <a
                        href={l.originalUrl}
                        target="_blank"
                        rel="noreferrer"
                        aria-label="Open original URL"
                        className="text-stone hover:text-azure"
                      >
                        <ExternalLink className="h-4 w-4" />
                      </a>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="mt-9 rounded-[18px] border border-dashed border-edge/15 p-6">
            <p className="text-sm font-semibold text-ink">Click analytics</p>
            <p className="mt-1 text-sm text-stone">
              Trend charts arrive once the analytics pipeline (Kafka + click events) is built.
            </p>
          </div>
        </>
      )}
    </div>
  )
}
