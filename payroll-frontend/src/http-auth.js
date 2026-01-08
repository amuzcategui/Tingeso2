import axios from "axios";
import Keycloak from "keycloak-js";

// OJO: usa el mismo Keycloak instance que usas en React.
// Si tú ya lo creas en otro archivo (ej: keycloak.js), impórtalo en vez de crear uno nuevo.
// Aquí asumo que ya tienes un export `keycloak` desde algún lado.
import keycloak from "../keycloak"; // AJUSTA ESTA RUTA a tu proyecto

const api = axios.create({
  baseURL: "http://localhost:8070",
});

// Interceptor: agrega Bearer token a cada request
api.interceptors.request.use(async (config) => {
  if (keycloak?.authenticated) {
    // refresca token si está por expirar (30s)
    try {
      await keycloak.updateToken(30);
    } catch (e) {
      console.warn("No se pudo refrescar token", e);
    }

    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default api;
