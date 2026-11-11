import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { inr } from "../utils/format";

export default function Account() {
  const { isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .get("/auth/me")
      .then((response) => setProfile(response.data))
      .catch((err) => setError(errorMessage(err)));
    if (!isAdmin) {
      api
        .get("/orders")
        .then((response) => setOrders(response.data))
        .catch((err) => setError(errorMessage(err)));
    }
  }, [isAdmin]);

  async function changePassword(event) {
    event.preventDefault();
    setPasswordError("");
    if (newPassword !== confirmPassword) {
      setPasswordError("New password and confirmation do not match.");
      return;
    }
    setBusy(true);
    try {
      await api.post("/auth/password", { currentPassword, newPassword });
      logout();
      navigate("/login", {
        replace: true,
        state: { notice: "Password changed. Sign in with your new password." },
      });
    } catch (err) {
      setPasswordError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (error && !profile) {
    return <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>;
  }
  if (!profile) {
    return <p className="text-sm text-stone-500">Loading account...</p>;
  }

  const lastAddress = orders[0]?.deliveryAddress;

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="font-display text-3xl">Account</h1>
      <section className="space-y-2 rounded-3xl bg-white p-6 shadow-sm">
        <p><span className="text-stone-500">Name</span> · {profile.name}</p>
        <p><span className="text-stone-500">User ID</span> · {profile.id}</p>
        <p><span className="text-stone-500">Email</span> · {profile.email}</p>
        {!isAdmin && (
          <p>
            <span className="text-stone-500">Last delivery address</span> · {lastAddress || "No delivery address yet"}
          </p>
        )}
      </section>

      {!isAdmin && (
        <section className="rounded-3xl bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Recent orders</h2>
          {orders.length === 0 ? (
            <p className="mt-3 text-sm text-stone-500">You haven&apos;t placed any orders yet</p>
          ) : (
            <ul className="mt-3 divide-y divide-stone-100">
              {orders.slice(0, 5).map((order) => (
                <li key={order.id} className="flex items-center justify-between gap-3 py-3 text-sm">
                  <Link to={`/orders/${order.id}`} className="font-medium text-orange-700">
                    Order #{order.id} · {order.restaurantName}
                  </Link>
                  <span>{inr(order.totalAmount)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <section className="rounded-3xl bg-white p-6 shadow-sm">
        {!showPassword ? (
          <button type="button" onClick={() => setShowPassword(true)} className="rounded-full bg-stone-900 px-4 py-2 text-sm text-white">
            Change password
          </button>
        ) : (
          <form onSubmit={changePassword} className="space-y-3">
            <h2 className="font-semibold">Change password</h2>
            <label className="block text-sm">
              Current password
              <input className="mt-1" type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
            </label>
            <label className="block text-sm">
              New password
              <input className="mt-1" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
            </label>
            <label className="block text-sm">
              Confirm new password
              <input className="mt-1" type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
            </label>
            {passwordError && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{passwordError}</p>}
            <button type="submit" disabled={busy} className="rounded-full bg-orange-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
              {busy ? "Saving..." : "Save password"}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
