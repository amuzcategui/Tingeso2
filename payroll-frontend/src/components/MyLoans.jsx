import React, { useState, useEffect } from 'react';
import { getMyLoans } from '../services/loan.service.js';
import { useKeycloak } from '@react-keycloak/web';
import '../index.css'; 

const MyLoans = () => {
  const [loans, setLoans] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const { keycloak } = useKeycloak();

useEffect(() => {
    if (keycloak && keycloak.tokenParsed) {
      const fetchLoans = async () => {
        try {
          const userRut = keycloak.tokenParsed.rut;
          const response = await getMyLoans(userRut);
          setLoans(response.data);
        } catch (err) {
          setError("No se pudo cargar el historial de préstamos.");
          console.error("Error fetching loans:", err);
        } finally {
          setIsLoading(false);
        }
      };
      fetchLoans();
    } else if (keycloak) {
      
      setIsLoading(false);
    }
  }, [keycloak]);

  
  const getLoanStatus = (loan) => {
    const today = new Date().toISOString().split('T')[0]; 
    if (loan.endDate) {
      return { text: 'Devuelto', className: 'status-returned' };
    }
    if (loan.dueDate < today) {
      return { text: 'Atrasado', className: 'status-overdue' };
    }
    return { text: 'Activo', className: 'status-active' };
  };

  if (isLoading) return <div>Cargando historial...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <h1>Mi Historial de Préstamos</h1>
      <table className="tools-table">
        <thead>
          <tr>
            <th>ID Préstamo</th>
            <th>Herramientas</th>
            <th>Fecha Inicio</th>
            <th>Fecha Devolución</th>
            <th>Estado</th>
            <th>Multa</th>
          </tr>
        </thead>
        <tbody>
          {loans.map((loan) => {
            const status = getLoanStatus(loan);
            return (
              <tr key={loan.id}>
                <td>{loan.id}</td>
                <td>{loan.toolNames.join(', ')}</td>
                <td>{loan.startDate}</td>
                <td>{loan.dueDate}</td>
                <td>
                  <span className={`status-badge ${status.className}`}>
                    {status.text}
                  </span>
                </td>
                <td>${(loan.fine || 0).toFixed(2)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default MyLoans;