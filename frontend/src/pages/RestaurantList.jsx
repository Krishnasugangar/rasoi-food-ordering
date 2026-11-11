import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import RestaurantCard from "../components/RestaurantCard";
import { useAuth } from "../context/AuthContext";

function catalogLine(kitchens, dishes) {
  if (kitchens === 0) {
    return "No kitchens are accepting orders right now";
  }
  const kitchenLabel = kitchens === 1 ? "1 kitchen" : `${kitchens} kitchens`;
  const dishLabel = dishes === 1 ? "1 dish" : `${dishes} dishes`;
  return `${kitchenLabel} · ${dishLabel} available`;
}

export default function RestaurantList() {
  const { isAdmin } = useAuth();
  const [search, setSearch] = useState("");
  const [restaurants, setRestaurants] = useState([]);
  const [error, setError] = useState("");
  const [summary, setSummary] = useState(null);
  const [summaryError, setSummaryError] = useState("");

  useEffect(() => {
    api
      .get("/restaurants/summary")
      .then((response) => {
        setSummary(response.data);
        setSummaryError("");
      })
      .catch(() => setSummaryError("Couldn't load the kitchen summary."));
  }, []);

  useEffect(() => {
    const handle = setTimeout(() => {
      api
        .get("/restaurants", { params: { search: search || undefined } })
        .then((response) => {
          setRestaurants(response.data);
          setError("");
        })
        .catch((err) => setError(errorMessage(err)));
    }, 200);
    return () => clearTimeout(handle);
  }, [search, isAdmin]);

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-3xl bg-gradient-to-br from-orange-700 to-amber-500 px-6 py-8 text-white shadow-sm">
        <p className="text-xs uppercase tracking-[0.25em] text-orange-100">Dinner, delivered</p>
        <h1 className="mt-2 max-w-xl font-display text-4xl leading-tight">What would you like to eat?</h1>
        {summary && !summaryError && (
          <p className="mt-3 text-sm text-orange-50">{catalogLine(summary.kitchens, summary.dishes)}</p>
        )}
        {summaryError && <p className="mt-3 text-sm text-orange-50">{summaryError}</p>}
      </section>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by kitchen, cuisine, or area"
          className="sm:max-w-sm"
        />
        {isAdmin && (
          <Link to="/restaurants/new" className="rounded-full bg-stone-900 px-4 py-2 text-center text-sm font-medium text-white">
            Add restaurant
          </Link>
        )}
      </div>

      {error && <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p>}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {restaurants.map((restaurant) => (
          <RestaurantCard key={restaurant.id} restaurant={restaurant} />
        ))}
      </div>
      {!error && restaurants.length === 0 && (
        <p className="text-sm text-stone-500">No restaurants match that search.</p>
      )}
    </div>
  );
}
