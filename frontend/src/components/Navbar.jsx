import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const linkClass = ({ isActive }) =>
  `rounded-full px-3 py-2 text-sm font-medium ${
    isActive ? "bg-orange-600 text-white" : "text-stone-600 hover:bg-orange-50"
  }`;

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();

  return (
    <header className="border-b border-orange-100 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
        <NavLink to="/" className="font-display text-2xl tracking-tight text-orange-700">
          Rasoi
        </NavLink>
        <nav className="flex flex-wrap items-center gap-1">
          <NavLink to="/" end className={linkClass}>
            Restaurants
          </NavLink>
          {user && (
            <NavLink to="/orders" className={linkClass}>
              Orders
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/dashboard" className={linkClass}>
              Dashboard
            </NavLink>
          )}
          {user ? (
            <>
              <NavLink to="/account" className={linkClass}>
                {isAdmin ? "Admin" : "User"}
              </NavLink>
              <button
                type="button"
                onClick={logout}
                className="rounded-full px-3 py-2 text-sm font-medium text-stone-600 hover:bg-stone-100"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkClass}>
                Log in
              </NavLink>
              <NavLink to="/register" className={linkClass}>
                Register
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
