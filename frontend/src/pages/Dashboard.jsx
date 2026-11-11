import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import StatCard from "../components/StatCard";
import StatusBadge from "../components/StatusBadge";
import { inr } from "../utils/format";

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/dashboard/summary")
      .then((response) => setSummary(response.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) {
    return <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>;
  }
  if (!summary) {
    return <p className="text-sm text-stone-500">Loading dashboard...</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-3xl">Kitchen desk</h1>
        <Link to="/orders" className="rounded-full bg-orange-600 px-4 py-2 text-sm font-medium text-white">Open orders</Link>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Restaurants" value={summary.activeRestaurants} />
        <StatCard label="Dishes" value={summary.totalMenuItems} />
        <StatCard label="Placed orders" value={summary.placedOrders} />
        <StatCard label="Revenue" value={inr(summary.revenue)} />
      </div>
      <section className="rounded-2xl bg-white p-4 shadow-sm">
        <h2 className="mb-3 font-semibold">Orders by status</h2>
        <div className="grid gap-2 sm:grid-cols-2">
          {Object.entries(summary.ordersByStatus || {}).map(([status, count]) => (
            <div key={status} className="flex items-center justify-between rounded-xl bg-stone-50 px-3 py-2">
              <StatusBadge status={status} />
              <span className="font-medium">{count}</span>
            </div>
          ))}
        </div>
      </section>
      <section className="rounded-2xl bg-white p-4 shadow-sm">
        <h2 className="mb-3 font-semibold">Recent orders</h2>
        {summary.recentOrders?.length === 0 && <p className="text-sm text-stone-500">No orders yet.</p>}
        <ul className="divide-y divide-stone-100">
          {summary.recentOrders?.map((order) => (
            <li key={order.id} className="flex items-center justify-between gap-3 py-3 text-sm">
              <div>
                <Link to={`/orders/${order.id}`} className="font-medium text-orange-700">#{order.id} {order.restaurantName}</Link>
                <p className="text-stone-500">{order.customerName} · {inr(order.totalAmount)}</p>
              </div>
              <StatusBadge status={order.status} />
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
