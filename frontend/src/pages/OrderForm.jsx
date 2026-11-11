import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { inr } from "../utils/format";

export default function OrderForm() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const restaurant = location.state?.restaurant;
  const items = location.state?.items || [];
  const [customerName, setCustomerName] = useState(user?.name || "");
  const [customerPhone, setCustomerPhone] = useState(user?.phone || "");
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const total = items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

  if (!restaurant || items.length === 0) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p>Pick dishes from a restaurant before checkout.</p>
        <Link to="/" className="mt-3 inline-block text-sm font-medium text-orange-700">Back to restaurants</Link>
      </div>
    );
  }

  async function onSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await api.post("/orders", {
        restaurantId: restaurant.id,
        customerName,
        customerPhone,
        deliveryAddress,
        paymentMethod: "COD",
        items: items.map((item) => ({ menuItemId: item.id, quantity: item.quantity })),
      });
      navigate(`/orders/${response.data.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-6 lg:grid-cols-[1fr_280px]">
      <div className="space-y-4 rounded-3xl bg-white p-6 shadow-sm">
        <h1 className="font-display text-3xl">Checkout · {restaurant.name}</h1>
        <label className="block text-sm">Name<input className="mt-1" value={customerName} onChange={(e) => setCustomerName(e.target.value)} required maxLength={100} /></label>
        <label className="block text-sm">Phone<input className="mt-1" value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} required maxLength={15} /></label>
        <label className="block text-sm">Delivery address<textarea className="mt-1" value={deliveryAddress} onChange={(e) => setDeliveryAddress(e.target.value)} required maxLength={300} /></label>
        <p className="rounded-xl bg-orange-50 px-3 py-2 text-sm text-stone-700">Payment: cash on delivery. You pay the rider when the order arrives.</p>
        {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        <button type="submit" disabled={busy} className="rounded-full bg-orange-600 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60">
          {busy ? "Placing order..." : "Place order"}
        </button>
      </div>
      <aside className="h-fit rounded-3xl bg-orange-50 p-5">
        <h2 className="font-semibold">Bill</h2>
        <ul className="mt-3 space-y-2 text-sm">
          {items.map((item) => (
            <li key={item.id} className="flex justify-between gap-3">
              <span>{item.quantity} × {item.name}</span>
              <span>{inr(item.price * item.quantity)}</span>
            </li>
          ))}
        </ul>
        <p className="mt-4 flex justify-between border-t border-orange-200 pt-3 font-semibold">
          <span>Total</span>
          <span>{inr(total)}</span>
        </p>
        <p className="mt-2 text-xs text-stone-500">The kitchen prices the order. This total is checked again on the server.</p>
      </aside>
    </form>
  );
}
