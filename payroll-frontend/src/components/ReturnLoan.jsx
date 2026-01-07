import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { TextField, Button, Radio, RadioGroup, FormControlLabel, FormControl } from '@mui/material';
import httpClient from '../http-common.js';
import '../index.css';
import SearchIcon from '@mui/icons-material/Search';


const fmt = (s) => (s ? `${s.slice(5,7)}/${s.slice(8,10)}/${s.slice(0,4)}` : '');
const calcIsOverdue = (loan) => {
  if (!loan?.dueDate) return false;
  const due = loan.dueDate.slice(0, 10);
  const base = loan.endDate ? loan.endDate.slice(0, 10) : new Date().toISOString().slice(0, 10);
  return base > due;
};

const ReturnLoan = () => {
  const { keycloak } = useKeycloak();
  const isAdmin = !!keycloak?.tokenParsed?.realm_access?.roles?.includes('ADMIN');

  const [loanId, setLoanId] = useState('');
  const [searchedLoan, setSearchedLoan] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [toolStatuses, setToolStatuses] = useState({});
  const [dailyLateFee, setDailyLateFee] = useState(15000);
  const [repairCost, setRepairCost] = useState(0);

  const getLoanById = (id) => httpClient.get(`/loan/${id}`);

  const handleSearchLoan = async () => {
    if (!loanId) {
      setError('Por favor, ingresa un ID de préstamo.');
      return;
    }
    setError('');
    setSuccess('');
    setSearchedLoan(null);
    try {
      const { data } = await getLoanById(loanId);
      setSearchedLoan(data);
    } catch (err) {
      const msg = err?.response?.data || 'Préstamo no encontrado.';
      setError(msg);
    }
  };

  useEffect(() => {
    if (searchedLoan) {
      const initialStatuses = {};
      (searchedLoan.toolNames || []).forEach((name) => (initialStatuses[name] = 'OK'));
      setToolStatuses(initialStatuses);
      setDailyLateFee(15000);
      setRepairCost(0);
    }
  }, [searchedLoan]);

  const handleStatusChange = (toolName, status) => {
    setToolStatuses((prev) => ({ ...prev, [toolName]: status }));
  };

  const handleReturnSubmit = async () => {
    if (!searchedLoan) return;
    setError('');
    setSuccess('');

    const damaged = [];
    const discarded = [];
    Object.entries(toolStatuses).forEach(([toolName, status]) => {
      if (status === 'damaged') damaged.push(toolName);
      else if (status === 'discarded') discarded.push(toolName);
    });

    try {
      const isOverdue = calcIsOverdue(searchedLoan);
      const params = new URLSearchParams();

      if (isOverdue) params.append('dailyLateFee', String(dailyLateFee));

      const rc = damaged.length > 0 ? (Number(repairCost) || 0) : 0;
      params.append('repairCost', String(rc));

      damaged.forEach((n) => params.append('damaged', n));
      discarded.forEach((n) => params.append('discarded', n));

      await httpClient.put(`/loan/return/${searchedLoan.id}?${params.toString()}`, null);

      setSuccess('Devolución procesada con éxito. El préstamo ahora puede ser pagado si corresponde.');
      const { data } = await getLoanById(searchedLoan.id);
      setSearchedLoan(data);
    } catch (err) {
      const msg = err?.response?.data || 'Error al procesar la devolución.';
      setError(msg);
    }
  };

  const handlePayLoan = async (loanIdToPay) => {
    if (!loanIdToPay) return;
    setError('');
    setSuccess('');
    if (!isAdmin) {
      setError('Solo un administrador puede marcar el préstamo como pagado.');
      return;
    }
    if (window.confirm('¿Confirmas que este préstamo ha sido pagado?')) {
      try {
        await httpClient.put(`/loan/${loanIdToPay}/pay`);
        setSuccess('Préstamo marcado como pagado con éxito.');
        const { data } = await getLoanById(loanIdToPay);
        setSearchedLoan(data);
      } catch (err) {
        const msg = err?.response?.data || 'Error al marcar el préstamo como pagado.';
        setError(msg);
      }
    }
  };

  const showRepairCostInput = Object.values(toolStatuses || {}).includes('damaged');
  const isOverdue = searchedLoan ? calcIsOverdue(searchedLoan) : false;

  const rentalFee = Number(searchedLoan?.rentalFee || 0);
  const fine = Number(searchedLoan?.fine || 0);
  const finalAmount = rentalFee + fine;

  return (
    <div>
      <h1>Devolución de Préstamos</h1>

      <div className="search-wrap" style={{ marginBottom: '20px' }}>
        <input
          type="text"
          placeholder="Ingresar ID del Préstamo"
          value={loanId}
          onChange={(e) => setLoanId(e.target.value)}
          className="search-input-smaller"
        />
        <button onClick={handleSearchLoan} className="action-button" >
          <SearchIcon />
        </button>
      </div>

      {error && <p className="error-message">{error}</p>}
      {success && <p style={{ color: 'green' }}>{success}</p>}

      {searchedLoan && (
        <div className="report-section">
          <h2>Detalles del Préstamo #{searchedLoan.id}</h2>
          <p><strong>RUT Cliente:</strong> {searchedLoan.rutCustomer}</p>
          <p><strong>Herramientas:</strong> {(searchedLoan.toolNames || []).join(', ')}</p>
          <p><strong>Fecha Inicio:</strong> {fmt(searchedLoan.startDate)}</p>
          <p><strong>Fecha Límite:</strong> {fmt(searchedLoan.dueDate)}</p>
          <p><strong>Fecha Devolución:</strong> {searchedLoan.endDate ? fmt(searchedLoan.endDate) : '—'}</p>

          <p><strong>Tarifa de arriendo:</strong> ${rentalFee.toFixed(2)}</p>
          <p><strong>Multa acumulada:</strong> ${fine.toFixed(2)}</p>
          <p><strong>Monto Final a Pagar:</strong> ${finalAmount.toFixed(2)}</p>

          {!searchedLoan.endDate && (
            <div className="return-form">
              <h3>Procesar Devolución: Estado de Herramientas</h3>

              {(searchedLoan.toolNames || []).map((name) => (
                <div key={name} className="tool-status-item">
                  <strong style={{ color: 'black' }}>{name}</strong>
                  <FormControl component="fieldset" style={{ marginLeft: '20px' }}>
                    <RadioGroup
                      row
                      value={toolStatuses[name] || 'OK'}
                      onChange={(e) => handleStatusChange(name, e.target.value)}
                    >
                      <FormControlLabel value="OK" control={<Radio />} label="OK" sx={{ color: 'black' }} />
                      <FormControlLabel value="damaged" control={<Radio />} label="Dañada" sx={{ color: 'black' }} />
                      <FormControlLabel value="discarded" control={<Radio />} label="Dar de Baja" sx={{ color: 'black' }} />
                    </RadioGroup>
                  </FormControl>
                </div>
              ))}

 
              {isOverdue && (
                <TextField
                  label="Multa diaria por atraso ($)"
                  type="number"
                  value={dailyLateFee}
                  onChange={(e) => setDailyLateFee(parseFloat(e.target.value) || 0)}
                  fullWidth
                  margin="normal"
                  helperText="Se usa para calcular la multa por días de atraso (solo si la devolución es posterior a la fecha límite)."
                />
              )}

              {showRepairCostInput && (
                <TextField
                  label="Costo total de reparación ($)"
                  type="number"
                  value={repairCost}
                  onChange={(e) => setRepairCost(parseFloat(e.target.value) || 0)}
                  fullWidth
                  margin="normal"
                  helperText="Se sumará a la multa total si hay herramientas dañadas."
                />
              )}

              <button
                onClick={handleReturnSubmit}
                variant="contained"
                color="primary"
                
                className="action-button"
              >
                Confirmar Devolución y Calcular Multa Final
              </button>
            </div>
          )}

          {isAdmin && searchedLoan.endDate && !searchedLoan.paid && (
            <div className="pay-section">
              <h3>Pagos</h3>
              <p>Este préstamo ha sido devuelto pero tiene pago pendiente.</p>
              <p><strong>Monto Final a Pagar:</strong> ${finalAmount.toFixed(2)}</p>
              <Button onClick={() => handlePayLoan(searchedLoan.id)} variant="contained" color="success">
                Marcar como Pagado
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ReturnLoan;
