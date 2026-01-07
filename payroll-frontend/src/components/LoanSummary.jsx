import React, { useState } from 'react';
import { useCart } from '../components/CartContext';
import { useKeycloak } from '@react-keycloak/web';
import { createLoan } from '../services/loan.service.js';
import { useNavigate } from 'react-router-dom';
import '../index.css';

const LoanSummary = () => {
  const { cartItems, removeFromCart, clearCart } = useCart();
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();

  const [startDate, setStartDate] = useState(''); 
  const [dueDate, setDueDate] = useState('');     
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Función para obtener el RUT del token de Keycloak
  const getRutFromToken = () => {
    const tp = keycloak?.tokenParsed || {};
   
    return tp.rut || tp.preferred_username || tp.sub || '';
  };

  const handleConfirmLoan = async () => {
    setError('');
    setSuccess('');

    if (cartItems.length === 0) {
      setError('El carrito está vacío.');
      return;
    }
    if (!startDate || !dueDate) {
      setError('Por favor, selecciona ambas fechas.');
      return;
    }
    if (dueDate < startDate) {
      setError('La fecha de devolución no puede ser anterior a la fecha de inicio.');
      return;
    }


    const loanData = {
      rutCustomer: getRutFromToken(),
      toolNames: cartItems.map(item => item.name),
      startDate, 
      dueDate    
    };


    console.log('Enviando datos del préstamo:', loanData);

    // Intenta crear el préstamo con los datos proporcionados
    try {
      setIsSubmitting(true);
      await createLoan(loanData);
      setSuccess('¡Préstamo creado con éxito! Serás redirigido al inicio.');
      clearCart();
      setTimeout(() => navigate('/home'), 1500);
    } catch (err) {
      const msg = err?.response?.data || 'Error al crear el préstamo.';
      setError(msg);
      setIsSubmitting(false);
    }
  };

  if (cartItems.length === 0 && !success) {
    return (
      <div>
        <h1>Carrito de Préstamos</h1>
        <p>Tu carrito está vacío. Agrega herramientas desde el catálogo.</p>
      </div>
    );
  }

  const totalDailyFee = cartItems.reduce((sum, item) => sum + (item.rentalFee || 0), 0);

  return (
    <div>
      <h1>Resumen del Préstamo</h1>
      <table className="tools-table">
        <thead>
          <tr>
            <th>Herramienta</th>
            <th>Tarifa Diaria</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {cartItems.map(item => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>${(item.rentalFee || 0).toFixed(2)}</td>
              <td>
                <button onClick={() => removeFromCart(item.id)} className="action-button-remove">
                  Quitar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Total Tarifa Diaria: ${totalDailyFee.toFixed(2)}</h3>

      <div className="loan-dates">
        <div>
          <label>Fecha de Inicio:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)} // ← string YYYY-MM-DD
          />
        </div>
        <div>
          <label>Fecha de Devolución:</label>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)} // ← string YYYY-MM-DD
          />
        </div>
      </div>

      <div className="yesLoan">
        <button
          onClick={handleConfirmLoan}
          className="action-button"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Procesando...' : 'Confirmar Préstamo'}
        </button>
      </div>

      {error && <p className="error-message" style={{ marginTop: '10px' }}>{error}</p>}
      {success && <p style={{ color: 'green', marginTop: '10px' }}>{success}</p>}
    </div>
  );
};

export default LoanSummary;
