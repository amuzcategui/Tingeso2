import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";
import { CartProvider } from './components/CartContext.jsx';

ReactDOM.createRoot(document.getElementById('root')).render(
  
  <ReactKeycloakProvider authClient={keycloak}>
    <CartProvider>
      <App />
    </CartProvider>
  </ReactKeycloakProvider>
)
