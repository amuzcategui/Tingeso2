import React, { useState, useEffect } from 'react';
import customerService from '../services/customer.service.js';
import { TextField, Button } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import FilterAltIcon from '@mui/icons-material/FilterAlt';
import '../index.css';

const Customers = () => {
  const [customers, setCustomers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');


  const [filterRut, setFilterRut] = useState('');
  const [filterQuantity, setFilterQuantity] = useState('');

  useEffect(() => {
    fetchAllCustomers();
  }, []);


    const fetchAllCustomers = async () => {
        try {
        setIsLoading(true);
        setError('');
        const response = await customerService.getAllCustomers();
        setCustomers(response.data);
        } catch (err) {
            console.error('getAllCustomers error:', err?.response?.status, err?.response?.data);
        setError('No se pudieron cargar los clientes.');
        } finally {
        setIsLoading(false);
        }
    };


  const handleSearchByRut = async () => {
    if (!filterRut) return;
    try {
      setIsLoading(true);
      setError('');
      const response = await customerService.findCustomerByRut(filterRut);
      setCustomers(response.data ? [response.data] : []); // Muestra solo el cliente encontrado en un array
    } catch (err) {
      setError('Cliente no encontrado.');
      setCustomers([]);
    } finally {
      setIsLoading(false);
    }
  };
  
 
  const handleFilterByLoans = async () => {
    if (!filterQuantity) return;
    const quantity = parseInt(filterQuantity);
    if (isNaN(quantity)) return;
    try {
      setIsLoading(true);
      setError('');
      const response = await customerService.getCustomersWithLoansGreaterThan(quantity);
      setCustomers(response.data);
    } catch (err) {
      setError('Error al filtrar los clientes.');
    } finally {
      setIsLoading(false);
    }
  };
  
 
  

  const handleGetOverdueCustomers = async () => {
    try {
      setIsLoading(true);
      setError('');
      const response = await customerService.getOverdueCustomers();
      setCustomers(response.data); // Actualiza la tabla principal con los resultados
    } catch (err)
 {
      setError('Error al obtener clientes con atrasos.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) return <div>Cargando clientes...</div>;

  return (
    <div>
      <h1>Gestión de Clientes</h1>
      {error && <p className="error-message">{error}</p>}

      {/* actions */}
      <div className="filter-section">
        <TextField label="Buscar por RUT" value={filterRut} onChange={(e) => setFilterRut(e.target.value)} size="small" />
        <Button onClick={handleSearchByRut} variant="contained" style={{ color: "#b497d6", backgroundColor: '#52154E', marginLeft: '10px' }}><SearchIcon /></Button>
        
        <TextField label="Préstamos > que" type="number" value={filterQuantity} onChange={(e) => setFilterQuantity(e.target.value)} size="small" style={{ marginLeft: '20px' }} />
        <Button onClick={handleFilterByLoans} variant="contained" style={{ color: "#b497d6", backgroundColor: '#52154E', marginLeft: '10px' }}><FilterAltIcon /></Button>

        <Button onClick={handleGetOverdueCustomers} variant="contained" color="secondary" style={{ color: "#b497d6", backgroundColor: '#52154E', marginLeft: '20px' }}>
          Ver Clientes con Atrasos
        </Button>

        <Button onClick={fetchAllCustomers} variant="outlined" style={{ color: "#b497d6", backgroundColor: '#52154E', marginLeft: '20px' }}>Mostrar Todos</Button>
      </div>

      <table className="tools-table">
        <thead>
          <tr>
            <th>RUT</th>
            <th>Nombre Completo</th>
            <th>Email</th>
            <th>Teléfono</th>
            <th>Nº Préstamos</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.rut}>
              <td>{customer.rut}</td>
              <td>{`${customer.name} ${customer.lastName}`}</td>
              <td>{customer.email}</td>
              <td>{customer.phone}</td>
              <td>{customer.quantityLoans}</td>
              <td>{customer.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default Customers;