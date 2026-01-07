import axios from "axios";
import keycloak from "./services/keycloak";

const apiUrl = import.meta.env.VITE_API_URL;          // ej: http://localhost:8070
const apiPrefix = import.meta.env.VITE_API_PREFIX || ""; // ej: /api/v1

if (!apiUrl) {
  console.error("Falta VITE_API_URL en el .env (ej: http://localhost:8070)");
}

const api = axios.create({
  baseURL: `${apiUrl}${apiPrefix}`,
  headers: { "Content-Type": "application/json" }
});

// interceptor: agrega token
api.interceptors.request.use(async (config) => {
  if (keycloak?.authenticated) {
    await keycloak.updateToken(30);
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
}, (error) => Promise.reject(error));

export default api;

