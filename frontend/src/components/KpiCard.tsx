export function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[14px] border border-edge/10 bg-card p-4">
      <p className="text-xs font-medium text-stone">{label}</p>
      <p className="mt-1.5 text-2xl font-semibold tabular-nums text-ink">{value}</p>
    </div>
  )
}
