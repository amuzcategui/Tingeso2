import React, { useEffect, useState } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import * as kardexService from '../services/kardex.service.js';
import { Select, MenuItem, InputLabel, FormControl } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import '../index.css';

const Kardex = () => {
  const { keycloak } = useKeycloak();
  const isAdmin = keycloak.tokenParsed?.realm_access?.roles.includes('ADMIN');

  const [activeSection, setActiveSection] = useState(null);
  const [error, setError] = useState('');


  const [toolName, setToolName] = useState('');
  const [historyResult, setHistoryResult] = useState(null);

  const [topToolsFrom, setTopToolsFrom] = useState('');
  const [topToolsTo, setTopToolsTo] = useState('');
  const [topToolsResult, setTopToolsResult] = useState(null);

  const [rangeFrom, setRangeFrom] = useState('');
  const [rangeTo, setRangeTo] = useState('');
  const [movementType, setMovementType] = useState('Préstamo');
  const [rangeResult, setRangeResult] = useState(null);

  const [activeLoansGrouped, setActiveLoansGrouped] = useState(null);
  const [activeLoansLoading, setActiveLoansLoading] = useState(false);

  const [allKardexResult, setAllKardexResult] = useState(null);
  const [allKardexLoading, setAllKardexLoading] = useState(false);

//open and close
  const toggleSection = (section) => {
    setActiveSection(prev => (prev === section ? null : section));
    setError('');
  };

  useEffect(() => {
    const fetchActiveGrouped = async () => {
      try {
        setActiveLoansLoading(true);
        const resp = await kardexService.getActiveLoansGrouped();
        setActiveLoansGrouped(resp.data);
      } catch {
        setError('Error al obtener los préstamos activos agrupados.');
      } finally {
        setActiveLoansLoading(false);
      }
    };

    const fetchAllKardex = async () => {
      try {
        setAllKardexLoading(true);
        const resp = await kardexService.getAllKardex();
        setAllKardexResult(resp.data);
      } catch {
        setError('Error al obtener el historial completo del kardex.');
      } finally {
        setAllKardexLoading(false);
      }
    };

    if (activeSection === 'activeLoans' && isAdmin) {
      fetchActiveGrouped();
    }
    if (activeSection === 'fullKardex' && isAdmin) {
      fetchAllKardex();
    }
  }, [activeSection, isAdmin]);


  const handleToolHistorySearch = async () => {
    if (!toolName) return setError('Por favor, ingresa un nombre de herramienta.');
    setError('');
    try {
      const response = await kardexService.getToolHistory(toolName);
      setHistoryResult(response.data);
    } catch {
      setError('Error al buscar el historial de la herramienta.');
    }
  };

  const handleTopToolsSearch = async () => {
    setError('');
    try {
      const response = await kardexService.getTopTools(topToolsFrom, topToolsTo, 5);
      setTopToolsResult(response.data);
    } catch {
      setError('Error al buscar las herramientas más arrendadas.');
    }
  };

  const handleRangeSearch = async () => {
    if (!rangeFrom || !rangeTo) return setError('Por favor, selecciona ambas fechas.');
    setError('');
    try {
      const response = await kardexService.getMovementsInRange(rangeFrom, rangeTo, movementType);
      setRangeResult(response.data);
    } catch {
      setError('Error al buscar movimientos.');
    }
  };

  return (
    <div>
      <h1>Reportes del Sistema (Kardex)</h1>
      {error && <p className="error-message">{error}</p>}


      <div className="report-section">
        <h4 className="report-header" onClick={() => toggleSection('toolHistory')}>
          Historial por Herramienta {activeSection === 'toolHistory' ? '▲' : '▼'}
        </h4>
        {activeSection === 'toolHistory' && (
          <div className="report-content">
            <input
              type="text"
              placeholder="Nombre de la Herramienta"
              value={toolName}
              onChange={(e) => setToolName(e.target.value)}
              className="search-input-smaller"
            />
            <button onClick={handleToolHistorySearch} className="action-button"><SearchIcon /></button>

            {historyResult && (
              <table className="tools-table">
                <thead>
                  <tr><th>Fecha</th><th>Tipo Movimiento</th><th>RUT</th><th>Cantidad</th></tr>
                </thead>
                <tbody>
                  {historyResult.map((item) => (
                    <tr key={item.id}>
                      <td>{item.movementDate}</td>
                      <td>{item.movementType}</td>
                      <td>{item.rutCustomer}</td>
                      <td>{item.toolQuantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>


      <div className="report-section">
        <h4 className="report-header" onClick={() => toggleSection('topTools')}>
          Top 5 Herramientas más Arrendadas {activeSection === 'topTools' ? '▲' : '▼'}
        </h4>
        {activeSection === 'topTools' && (
          <div className="report-content">
            <input
              type="date"
              className="date-smaller"
              value={topToolsFrom}
              onChange={(e) => setTopToolsFrom(e.target.value)}
            />
            <input
              type="date"
              className="date-smaller"
              value={topToolsTo}
              onChange={(e) => setTopToolsTo(e.target.value)}
            />
            <button onClick={handleTopToolsSearch} className="action-button"><SearchIcon /></button>

            {topToolsResult && (
              <table className="tools-table">
                <thead><tr><th>Herramienta</th><th>Nº de Préstamos</th></tr></thead>
                <tbody>
                  {topToolsResult.map((item, i) => (
                    <tr key={i}><td>{item[0]}</td><td>{item[1]}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
 {/* Delete this??? */}
      {isAdmin && (
        <>

          <div className="report-section">
            <h4 className="report-header" onClick={() => toggleSection('rangeMovements')}>
              Movimientos por Rango de Fechas {activeSection === 'rangeMovements' ? '▲' : '▼'}
            </h4>
            {activeSection === 'rangeMovements' && (
              <div className="report-content">
                <input
                  type="date"
                  className="date-smaller"
                  value={rangeFrom}
                  onChange={(e) => setRangeFrom(e.target.value)}
                />
                <input
                  type="date"
                  className="date-smaller"
                  value={rangeTo}
                  onChange={(e) => setRangeTo(e.target.value)}
                />
                <FormControl size="small" style={{ minWidth: 120, marginLeft: 10 , marginTop: 23, marginRight: 10 , backgroundColor: '#b497d6', borderRadius: "8px" }}> 
                  <InputLabel>Tipo</InputLabel> 
                  <Select value={movementType} onChange={(e) => setMovementType(e.target.value)}>
                    <MenuItem value="Préstamo">Préstamo</MenuItem>
                    <MenuItem value="Devolución">Devolución</MenuItem>
                    <MenuItem value="Ingreso">Ingreso</MenuItem>
                    <MenuItem value="Baja">Baja</MenuItem>
                  </Select>
                </FormControl>
                <button className="action-button" onClick={handleRangeSearch}><SearchIcon /></button>

                {rangeResult && (
                  <table className="tools-table">
                    <thead><tr><th>Fecha</th><th>Herramienta</th><th>RUT</th><th>Cantidad</th></tr></thead>
                    <tbody>
                      {rangeResult.map((item) => (
                        <tr key={item.id}>
                          <td>{item.movementDate}</td>
                          <td>{item.toolName}</td>
                          <td>{item.rutCustomer}</td>
                          <td>{item.toolQuantity}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </div>

          <div className="report-section">
            <h4 className="report-header" onClick={() => toggleSection('activeLoans')}>
              Préstamos Activos Agrupados (Atrasos / Vigentes) {activeSection === 'activeLoans' ? '▲' : '▼'}
            </h4>
            {activeSection === 'activeLoans' && (
              <div className="report-content">
                {activeLoansLoading && <p>Cargando agrupaciones...</p>}

                {!activeLoansLoading && activeLoansGrouped && (
                  <div style={{ marginTop: '20px' }}>
                    <h3 style={{ color: '#a94442' }}>Atrasos</h3>
                    {activeLoansGrouped.Atrasos?.length > 0 ? (
                      <table className="tools-table">
                        <thead><tr><th>ID</th><th>Cliente</th><th>Herramientas</th><th>Fecha Límite</th></tr></thead>
                        <tbody>
                          {activeLoansGrouped.Atrasos.map((loan) => (
                            <tr key={loan.id}>
                              <td>{loan.id}</td>
                              <td>{loan.rutCustomer}</td>
                              <td>{(loan.toolNames || []).join(', ')}</td>
                              <td>{loan.dueDate}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <p>No hay préstamos atrasados.</p>
                    )}

                    <h3 style={{ color: '#31708f', marginTop: '20px' }}>Vigentes</h3>
                    {activeLoansGrouped.Vigentes?.length > 0 ? (
                      <table className="tools-table">
                        <thead><tr><th>ID</th><th>Cliente</th><th>Herramientas</th><th>Fecha Límite</th></tr></thead>
                        <tbody>
                          {activeLoansGrouped.Vigentes.map((loan) => (
                            <tr key={loan.id}>
                              <td>{loan.id}</td>
                              <td>{loan.rutCustomer}</td>
                              <td>{(loan.toolNames || []).join(', ')}</td>
                              <td>{loan.dueDate}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <p>No hay préstamos vigentes.</p>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="report-section">
            <h4 className="report-header" onClick={() => toggleSection('fullKardex')}>
              Historial Completo (Kardex General) {activeSection === 'fullKardex' ? '▲' : '▼'}
            </h4>
            {activeSection === 'fullKardex' && (
              <div className="report-content">
                {allKardexLoading && <p>Cargando historial completo...</p>}

                {!allKardexLoading && allKardexResult && (
                  <table className="tools-table">
                    <thead>
                      <tr><th>ID</th><th>Fecha</th><th>Tipo</th><th>Herramienta</th><th>Cantidad</th><th>RUT</th></tr>
                    </thead>
                    <tbody>
                      {allKardexResult.map((item) => (
                        <tr key={item.id}>
                          <td>{item.id}</td>
                          <td>{item.movementDate}</td>
                          <td>{item.movementType}</td>
                          <td>{item.toolName}</td>
                          <td>{item.toolQuantity}</td>
                          <td>{item.rutCustomer}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default Kardex;
