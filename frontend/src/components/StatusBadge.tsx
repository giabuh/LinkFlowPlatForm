export function StatusBadge({ active }: { active: boolean }) {
  if (active) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-teal/15 px-2.5 py-0.5 text-xs font-medium text-teal">
        <span className="h-1.5 w-1.5 rounded-full bg-teal" aria-hidden />
        Active
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-stone/15 px-2.5 py-0.5 text-xs font-medium text-stone">
      <span className="h-1.5 w-1.5 rounded-full bg-stone" aria-hidden />
      Inactive
    </span>
  )
}
