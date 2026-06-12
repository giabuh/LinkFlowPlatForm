const STACK = [
  'React 19 + TypeScript (Vite)',
  'TanStack Router + TanStack Query',
  'React Hook Form + Zod',
  'Zustand',
  'Tailwind CSS v4',
  'Recharts',
]

export default function HomePage() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">LinkFlow dashboard</h1>
        <p className="mt-2 text-gray-600">
          Frontend scaffold for the LinkFlowPlatform SPA. Talks to the Spring Boot
          API under{' '}
          <code className="rounded bg-gray-100 px-1.5 py-0.5 text-sm">/api/v1</code>{' '}
          (proxied to{' '}
          <code className="rounded bg-gray-100 px-1.5 py-0.5 text-sm">localhost:8080</code>{' '}
          in dev).
        </p>
      </div>
      <div className="rounded-lg border bg-white p-6">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
          Stack
        </h2>
        <ul className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          {STACK.map((item) => (
            <li key={item} className="flex items-center gap-2 text-sm">
              <span className="h-1.5 w-1.5 rounded-full bg-violet-500" />
              {item}
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
