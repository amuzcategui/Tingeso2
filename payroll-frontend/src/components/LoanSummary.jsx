import React, { useState, useEffect } from 'react';
import { useCart } from '../components/CartContext';
import { useKeycloak } from '@react-keycloak/web';
import { createLoan } from '../services/loan.service.js';
import * as toolService from '../services/tool.service.js';
import { useNavigate } from 'react-router-dom';
import '../index.css';

const LoanSummary = () => {
  const { cartItems, removeFromCart, clearCart } = useCart();
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();

  const [startDate, setStartDate] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ✅ fee global
  const [rentalFeeDaily, setRentalFeeDaily] = useState(null);

  useEffect(() => {
    const run = async () => {
      if (!initialized) return;
      if (!keycloak?.authenticated) return;

      try {
        const feeRes = await toolService.getRentalFeeDaily(); // devuelve double
        setRentalFeeDaily(feeRes.data);
      } catch (err) {
        // si falla, no bloqueamos; solo mostramos error si quieres
        console.error('No se pudo cargar rentalFeeDaily:', err?.response?.data || err.message);
      }
    };

    run();
  }, [initialized, keycloak?.authenticated]);

  const getRutFromToken = () => {
    const tp = keycloak?.tokenParsed || {};
    return tp.rut || tp.preferred_username || tp.sub || '';
  };

  // ✅ calcular días (si hay fechas válidas)
  const getDaysBetween = (s, d) => {
    if (!s || !d) return 0;
    const start = new Date(s + 'T00:00:00');
    const due = new Date(d + 'T00:00:00');
    const ms = due.getTime() - start.getTime();
    const days = Math.floor(ms / (1000 * 60 * 60 * 24));
    return days > 0 ? days : 0;
  };

  const days = getDaysBetween(startDate, dueDate);
  const estimatedTotal = (rentalFeeDaily != null ? rentalFeeDaily : 0) * days;

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

  return (
    <div>
      <h1>Resumen del Préstamo</h1>

      <table className="tools-table">
        <thead>
          <tr>
            <th>Herramienta</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {cartItems.map(item => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>
                <button onClick={() => removeFromCart(item.id)} className="action-button-remove">
                  Quitar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* ✅ Reemplazo del totalDailyFee */}
      <div style={{ marginTop: '12px' }}>
        <h3>Tarifa diaria (global): {rentalFeeDaily != null ? `$${Number(rentalFeeDaily).toFixed(2)}` : '—'}</h3>
        <h3>Días: {days}</h3>
        <h3>Total estimado del arriendo: ${Number(estimatedTotal).toFixed(2)}</h3>
      </div>

      <div className="loan-dates">
        <div>
          <label>Fecha de Inicio:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>
        <div>
          <label>Fecha de Devolución:</label>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
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
