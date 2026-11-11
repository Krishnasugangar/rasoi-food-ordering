import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import StatusBadge from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import { formatWhen, inr } from "../utils/format";

export default function OrderList() {
  const { isAdmin } = useAuth();
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  function load() {
    api.get("/orders")
      .then((response) => setOrders(response.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
  }, []);

  async function confirm(id) {
    await api.patch(`/orders/${id}/status`, { status: "CONFIRMED" });
    load();
  }

  return (
    <div className="space-y-4">
      <h1 className="font-display text-3xl">{isAdmin ? "All orders" : "Your orders"}</h1>
      {error && <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {orders.length === 0 && !error && <p className="text-sm text-stone-500">No orders yet. Browse a kitchen and check out with cash.</p>}
      <div className="space-y-3">
        {orders.map((order) => (
          <article key={order.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white p-4 shadow-sm">
            <div>
              <p className="text-sm text-stone-500">Order #{order.id} · {formatWhen(order.createdAt)}</p>
              <h2 className="font-medium">{order.restaurantName}</h2>
              <p className="text-sm text-stone-500">{order.customerName} · {inr(order.totalAmount)}</p>
            </div>
            <div className="flex items-center gap-2">
              <StatusBadge status={order.status} />
              <StatusBadge status={order.paymentMethod} />
              {isAdmin && order.status === "PLACED" && (
                <button type="button" onClick={() => confirm(order.id)} className="rounded-full bg-orange-600 px-3 py-1.5 text-xs font-semibold text-white">
                  Confirm
                </button>
              )}
              <Link to={`/orders/${order.id}`} className="text-sm font-medium text-orange-700">Details</Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
