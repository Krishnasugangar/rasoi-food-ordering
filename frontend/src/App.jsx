import { Route, Routes } from "react-router-dom";
import AdminRoute from "./components/AdminRoute";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import Account from "./pages/Account";
import Dashboard from "./pages/Dashboard";
import Login from "./pages/Login";
import OrderDetail from "./pages/OrderDetail";
import OrderForm from "./pages/OrderForm";
import OrderList from "./pages/OrderList";
import Register from "./pages/Register";
import RestaurantDetail from "./pages/RestaurantDetail";
import RestaurantForm from "./pages/RestaurantForm";
import RestaurantList from "./pages/RestaurantList";

export default function App() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Routes>
          <Route path="/" element={<RestaurantList />} />
          <Route path="/login" element={<Login />} />
          <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
          <Route path="/register" element={<Register />} />
          <Route path="/restaurants/new" element={<AdminRoute><RestaurantForm /></AdminRoute>} />
          <Route path="/restaurants/:id/edit" element={<AdminRoute><RestaurantForm /></AdminRoute>} />
          <Route path="/restaurants/:id" element={<RestaurantDetail />} />
          <Route path="/orders" element={<ProtectedRoute><OrderList /></ProtectedRoute>} />
          <Route path="/orders/new" element={<ProtectedRoute><OrderForm /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetail /></ProtectedRoute>} />
          <Route path="/dashboard" element={<AdminRoute><Dashboard /></AdminRoute>} />
        </Routes>
      </main>
    </div>
  );
}
