import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import StatusBadge from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import { formatWhen, inr } from "../utils/format";

const nextActions = {
  PLACED: [
    { status: "CONFIRMED", label: "Confirm" },
    { status: "CANCELLED", label: "Cancel order" },
  ],
  CONFIRMED: [
    { status: "PREPARING", label: "Start preparing" },
    { status: "CANCELLED", label: "Cancel order" },
  ],
  PREPARING: [{ status: "OUT_FOR_DELIVERY", label: "Send for delivery" }],
  OUT_FOR_DELIVERY: [{ status: "DELIVERED", label: "Mark delivered" }],
};

export default function OrderDetail() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/orders/${id}`)
      .then((response) => {
        setOrder(response.data);
        setError("");
      })
      .catch((err) => setError(errorMessage(err)));
  }, [id]);

  async function saveStatus(status) {
    try {
      const response = await api.patch(`/orders/${id}/status`, { status });
      setOrder(response.data);
      setError("");
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (error && !order) {
    return <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>;
  }
  if (!order) {
    return <p className="text-sm text-stone-500">Loading order...</p>;
  }

  return (
    <div className="space-y-4">
      <Link to="/orders" className="text-sm text-orange-700">Back to orders</Link>
      <div className="rounded-3xl bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-sm text-stone-500">Order #{order.id} · {formatWhen(order.createdAt)}</p>
            <h1 className="font-display text-3xl">{order.restaurantName}</h1>
            <p className="mt-2 text-sm text-stone-600">{order.customerName} · {order.customerPhone}</p>
            <p className="text-sm text-stone-600">{order.deliveryAddress}</p>
          </div>
          <div className="flex gap-2">
            <StatusBadge status={order.status} />
            <StatusBadge status={order.paymentMethod} />
            <StatusBadge status={order.paymentStatus} />
          </div>
        </div>
        <ul className="mt-6 divide-y divide-stone-100">
          {order.items.map((item) => (
            <li key={item.id} className="flex justify-between py-3 text-sm">
              <span>{item.quantity} × {item.name}</span>
              <span>{inr(item.lineTotal)}</span>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-right font-semibold">{inr(order.totalAmount)}</p>
        {order.stripePaymentId && <p className="mt-2 text-xs text-stone-500">Stripe: {order.stripePaymentId}</p>}
      </div>

      {isAdmin && (
        <div className="flex flex-wrap gap-2 rounded-2xl bg-orange-50 p-4">
          {(nextActions[order.status] || []).map((action) => (
            <button
              key={action.status}
              type="button"
              onClick={() => saveStatus(action.status)}
              className="rounded-full bg-stone-900 px-4 py-2 text-sm text-white"
            >
              {action.label}
            </button>
          ))}
          {(nextActions[order.status] || []).length === 0 && (
            <p className="text-sm text-stone-600">This order is finished. No further status changes.</p>
          )}
        </div>
      )}
      {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
    </div>
  );
}
