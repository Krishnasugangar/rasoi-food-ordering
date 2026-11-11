import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function onSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await register(form);
      navigate("/", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-md rounded-3xl border border-orange-100 bg-white p-6 shadow-sm">
      <h1 className="font-display text-3xl">Create an account</h1>
      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <label className="block text-sm">
          Name
          <input className="mt-1" value={form.name} onChange={(e) => update("name", e.target.value)} required />
        </label>
        <label className="block text-sm">
          Email
          <input className="mt-1" type="email" value={form.email} onChange={(e) => update("email", e.target.value)} required />
        </label>
        <label className="block text-sm">
          Phone
          <input className="mt-1" value={form.phone} onChange={(e) => update("phone", e.target.value)} maxLength={15} />
        </label>
        <label className="block text-sm">
          Password
          <input className="mt-1" type="password" value={form.password} onChange={(e) => update("password", e.target.value)} minLength={6} required />
        </label>
        {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={busy}
          className="w-full rounded-full bg-orange-600 py-2.5 text-sm font-semibold text-white hover:bg-orange-700 disabled:opacity-60"
        >
          {busy ? "Creating..." : "Register"}
        </button>
      </form>
      <p className="mt-4 text-sm text-stone-500">
        Already have an account? <Link className="font-medium text-orange-700" to="/login">Log in</Link>
      </p>
    </div>
  );
}
