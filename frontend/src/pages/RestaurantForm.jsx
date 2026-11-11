import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api, { errorMessage } from "../api/axios";

const empty = {
  name: "",
  cuisineType: "",
  address: "",
  phone: "",
  rating: "4.0",
  active: true,
};

export default function RestaurantForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(empty);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!id) return;
    api.get(`/restaurants/${id}`).then((response) => {
      const restaurant = response.data;
      setForm({
        name: restaurant.name || "",
        cuisineType: restaurant.cuisineType || "",
        address: restaurant.address || "",
        phone: restaurant.phone || "",
        rating: restaurant.rating ?? "0",
        active: restaurant.active,
      });
    }).catch((err) => setError(errorMessage(err)));
  }, [id]);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function onSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const payload = {
      ...form,
      rating: Number(form.rating),
    };
    try {
      if (id) {
        await api.put(`/restaurants/${id}`, payload);
        navigate(`/restaurants/${id}`);
      } else {
        const response = await api.post("/restaurants", payload);
        navigate(`/restaurants/${response.data.id}`);
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function deactivate() {
    await api.delete(`/restaurants/${id}`);
    navigate("/");
  }

  return (
    <form onSubmit={onSubmit} className="mx-auto max-w-xl space-y-4 rounded-3xl bg-white p-6 shadow-sm">
      <h1 className="font-display text-3xl">{id ? "Edit restaurant" : "New restaurant"}</h1>
      <label className="block text-sm">Name<input className="mt-1" value={form.name} onChange={(e) => update("name", e.target.value)} required /></label>
      <label className="block text-sm">Cuisine<input className="mt-1" value={form.cuisineType} onChange={(e) => update("cuisineType", e.target.value)} /></label>
      <label className="block text-sm">Address<input className="mt-1" value={form.address} onChange={(e) => update("address", e.target.value)} /></label>
      <label className="block text-sm">Phone<input className="mt-1" value={form.phone} onChange={(e) => update("phone", e.target.value)} maxLength={15} /></label>
      <label className="block text-sm">Rating<input className="mt-1" type="number" min="0" max="5" step="0.1" value={form.rating} onChange={(e) => update("rating", e.target.value)} /></label>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={form.active} onChange={(e) => update("active", e.target.checked)} />
        Accepting orders
      </label>
      {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="flex gap-3">
        <button type="submit" disabled={busy} className="rounded-full bg-orange-600 px-5 py-2 text-sm font-semibold text-white">
          {busy ? "Saving..." : "Save"}
        </button>
        {id && (
          <button type="button" onClick={deactivate} className="rounded-full border border-stone-300 px-5 py-2 text-sm">
            Pause restaurant
          </button>
        )}
      </div>
    </form>
  );
}
