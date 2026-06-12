import { useQuery } from '@tanstack/react-query'

interface HealthStatus {
  status: string
}

async function fetchHealth(): Promise<HealthStatus> {
  const res = await fetch('/actuator/health')
  if (!res.ok) throw new Error(`Backend returned ${res.status}`)
  return res.json() as Promise<HealthStatus>
}

export default function HealthPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    retry: false,
  })

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-bold tracking-tight">Backend health</h1>
      <p className="text-sm text-gray-600">
        Demonstrates TanStack Query reaching the backend through the dev proxy
        (<code className="rounded bg-gray-100 px-1.5 py-0.5">/actuator/health</code>).
      </p>
      <div className="rounded-lg border bg-white p-6 text-sm">
        {isLoading && <span className="text-gray-500">Checking…</span>}
        {isError && (
          <span className="text-red-600">
            Unreachable: {(error as Error).message}. Is the backend running on :8080?
          </span>
        )}
        {data && (
          <span className="font-medium text-green-600">status: {data.status}</span>
        )}
      </div>
    </section>
  )
}
