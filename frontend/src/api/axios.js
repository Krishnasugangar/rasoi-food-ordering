import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("rasoi_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function errorMessage(error) {
  const data = error?.response?.data;
  if (data?.errors) {
    return Object.values(data.errors).join(" ");
  }
  if (data?.message) {
    return data.message;
  }
  return "Could not reach the API.";
}

export default api;
