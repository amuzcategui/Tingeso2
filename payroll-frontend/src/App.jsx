import React, { useState, useEffect } from 'react'; 
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useKeycloak } from "@react-keycloak/web";

import Navbar from "./components/Navbar";
import Home from './components/Home';
import Inventory from './components/Inventory';
import NotFound from './components/NotFound';
import LoanSummary from './components/LoanSummary';
import Menu from './components/Menu';
import MyLoans from './components/MyLoans';
import Kardex from './components/Kardex'; 
import Customers from './components/Customers';
import ReturnLoan from './components/ReturnLoan';

const PrivateRoute = ({ children }) => {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    return <div>Cargando...</div>;
  }
  if (!keycloak.authenticated) {
    keycloak.login();
    return <div>Redirigiendo al login...</div>;
  }
  return children;
};

function App() {
 
  const [isDrawerOpen, setDrawerOpen] = useState(false);

  const toggleDrawer = (open) => (event) => {
    if (event && event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')) {
      return;
    }
    setDrawerOpen(open);
  };

  const { keycloak, initialized } = useKeycloak();

  
  useEffect(() => {
    if (initialized && keycloak.token) {
      
      window.kcToken = keycloak.token;
      console.log("Token guardado en 'window.kcToken' para depuración.");
    }
  }, [initialized, keycloak.token]); 


  return (
    <BrowserRouter>
      <Navbar onMenuClick={toggleDrawer(true)} />
      <Menu open={isDrawerOpen} toggleDrawer={toggleDrawer} />
      <div className="container">
        <Routes>
          <Route path="/home" element={<PrivateRoute><Home /></PrivateRoute>} />
          <Route path="/inventory" element={<PrivateRoute><Inventory /></PrivateRoute>} />
          <Route path="/loan-summary" element={<PrivateRoute><LoanSummary /></PrivateRoute>} />
          <Route path="/my-loans" element={<PrivateRoute><MyLoans /></PrivateRoute>} />
          <Route path="/kardex" element={<PrivateRoute><Kardex /></PrivateRoute>} />
          <Route path="/customers" element={<PrivateRoute><Customers /></PrivateRoute>} />
          <Route path="/return-loan" element={<PrivateRoute><ReturnLoan /></PrivateRoute>} />
          
          <Route path="/" element={<Navigate to="/home" />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;