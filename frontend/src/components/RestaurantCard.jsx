import { Link } from "react-router-dom";

export default function RestaurantCard({ restaurant }) {
  return (
    <Link
      to={`/restaurants/${restaurant.id}`}
      className="flex flex-col justify-between rounded-2xl border border-orange-100 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <div>
        <div className="flex items-start justify-between gap-3">
          <h2 className="font-display text-xl text-stone-900">{restaurant.name}</h2>
          <span className="rounded-full bg-orange-50 px-2 py-1 text-xs font-semibold text-orange-700">
            {Number(restaurant.rating).toFixed(1)}
          </span>
        </div>
        <p className="mt-2 text-sm font-medium text-orange-700">{restaurant.cuisineType}</p>
        <p className="mt-2 text-sm text-stone-500">{restaurant.address}</p>
      </div>
      <p className="mt-4 text-xs uppercase tracking-wide text-stone-400">
        {restaurant.active ? "Open for orders" : "Paused"}
      </p>
    </Link>
  );
}
