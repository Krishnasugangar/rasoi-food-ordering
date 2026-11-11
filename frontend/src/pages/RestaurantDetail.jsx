import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { formatStatus, inr } from "../utils/format";

const categories = ["STARTER", "MAIN", "DESSERT", "DRINK"];

const emptyDish = {
  name: "",
  description: "",
  price: "",
  category: "MAIN",
  veg: true,
  available: true,
};

export default function RestaurantDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  const [restaurant, setRestaurant] = useState(null);
  const [items, setItems] = useState([]);
  const [qty, setQty] = useState({});
  const [dish, setDish] = useState(emptyDish);
  const [error, setError] = useState("");

  function load() {
    Promise.all([api.get(`/restaurants/${id}`), api.get("/menu-items", { params: { restaurantId: id } })])
      .then(([restaurantResponse, menuResponse]) => {
        setRestaurant(restaurantResponse.data);
        setItems(menuResponse.data);
        setError("");
      })
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
  }, [id]);

  const grouped = useMemo(() => {
    return categories
      .map((category) => ({
        category,
        items: items.filter((item) => item.category === category),
      }))
      .filter((group) => group.items.length > 0);
  }, [items]);

  const cart = items
    .filter((item) => (qty[item.id] || 0) > 0)
    .map((item) => ({ ...item, quantity: qty[item.id] }));
  const cartTotal = cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

  function changeQty(itemId, next) {
    setQty((current) => ({ ...current, [itemId]: Math.max(0, next) }));
  }

  function checkout() {
    if (!user) {
      navigate("/login", { state: { from: { pathname: `/restaurants/${id}` } } });
      return;
    }
    navigate("/orders/new", { state: { restaurant, items: cart } });
  }

  async function addDish(event) {
    event.preventDefault();
    try {
      await api.post("/menu-items", {
        restaurantId: Number(id),
        name: dish.name,
        description: dish.description,
        price: Number(dish.price),
        category: dish.category,
        veg: dish.veg,
        available: dish.available,
      });
      setDish(emptyDish);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (error && !restaurant) {
    return <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>;
  }
  if (!restaurant) {
    return <p className="text-sm text-stone-500">Loading kitchen...</p>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
      <div className="space-y-6">
        <div className="rounded-3xl bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-orange-700">{restaurant.cuisineType}</p>
              <h1 className="font-display text-4xl">{restaurant.name}</h1>
              <p className="mt-2 text-sm text-stone-500">{restaurant.address} · {restaurant.phone}</p>
            </div>
            {isAdmin && (
              <Link to={`/restaurants/${id}/edit`} className="rounded-full border border-stone-300 px-4 py-2 text-sm">
                Edit kitchen
              </Link>
            )}
          </div>
        </div>

        {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}

        {grouped.map((group) => (
          <section key={group.category} className="space-y-3">
            <h2 className="font-display text-2xl">{formatStatus(group.category)}</h2>
            {group.items.map((item) => (
              <article key={item.id} className="flex items-center justify-between gap-4 rounded-2xl bg-white p-4 shadow-sm">
                <div>
                  <div className="flex items-center gap-2">
                    <span className={`h-3 w-3 rounded-sm border ${item.veg ? "border-emerald-600" : "border-red-600"}`}>
                      <span className={`block h-1.5 w-1.5 rounded-full ${item.veg ? "bg-emerald-600" : "bg-red-600"} m-[2px]`} />
                    </span>
                    <h3 className="font-medium">{item.name}</h3>
                    {!item.available && <span className="text-xs text-stone-400">Unavailable</span>}
                  </div>
                  <p className="mt-1 text-sm text-stone-500">{item.description}</p>
                  <p className="mt-2 text-sm font-semibold">{inr(item.price)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <button type="button" className="h-8 w-8 rounded-full border" onClick={() => changeQty(item.id, (qty[item.id] || 0) - 1)}>-</button>
                  <span className="w-4 text-center text-sm">{qty[item.id] || 0}</span>
                  <button type="button" className="h-8 w-8 rounded-full bg-orange-600 text-white" onClick={() => changeQty(item.id, (qty[item.id] || 0) + 1)} disabled={!item.available}>+</button>
                </div>
              </article>
            ))}
          </section>
        ))}

        {isAdmin && (
          <form onSubmit={addDish} className="space-y-3 rounded-2xl border border-dashed border-orange-200 bg-orange-50 p-4">
            <h2 className="font-semibold">Add a dish</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <input placeholder="Name" value={dish.name} onChange={(e) => setDish({ ...dish, name: e.target.value })} required />
              <input placeholder="Price" type="number" min="0" step="1" value={dish.price} onChange={(e) => setDish({ ...dish, price: e.target.value })} required />
              <select value={dish.category} onChange={(e) => setDish({ ...dish, category: e.target.value })}>
                {categories.map((category) => <option key={category}>{category}</option>)}
              </select>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={dish.veg} onChange={(e) => setDish({ ...dish, veg: e.target.checked })} />
                Vegetarian
              </label>
            </div>
            <textarea placeholder="Description" value={dish.description} onChange={(e) => setDish({ ...dish, description: e.target.value })} />
            <button type="submit" className="rounded-full bg-stone-900 px-4 py-2 text-sm text-white">Save dish</button>
          </form>
        )}
      </div>

      <aside className="h-fit rounded-3xl bg-stone-900 p-5 text-white lg:sticky lg:top-4">
        <h2 className="font-display text-2xl">Your plate</h2>
        {cart.length === 0 ? (
          <p className="mt-3 text-sm text-stone-300">Add a dish to start an order.</p>
        ) : (
          <ul className="mt-4 space-y-2 text-sm">
            {cart.map((item) => (
              <li key={item.id} className="flex justify-between gap-3">
                <span>{item.quantity} × {item.name}</span>
                <span>{inr(item.price * item.quantity)}</span>
              </li>
            ))}
          </ul>
        )}
        <p className="mt-4 flex justify-between border-t border-stone-700 pt-3 text-sm">
          <span>Estimate</span>
          <span className="font-semibold">{inr(cartTotal)}</span>
        </p>
        <button
          type="button"
          disabled={cart.length === 0}
          onClick={checkout}
          className="mt-4 w-full rounded-full bg-orange-500 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
        >
          {user ? "Checkout" : "Log in to order"}
        </button>
      </aside>
    </div>
  );
}
