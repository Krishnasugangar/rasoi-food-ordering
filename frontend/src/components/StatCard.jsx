export default function StatCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-orange-100 bg-white p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wide text-stone-500">{label}</p>
      <p className="mt-2 font-display text-3xl text-stone-900">{value}</p>
    </div>
  );
}
