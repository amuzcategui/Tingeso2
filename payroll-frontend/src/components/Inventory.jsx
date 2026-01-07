import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import * as toolService from '../services/tool.service.js';
import { Modal, Box, TextField, Button } from '@mui/material';
import '../index.css';

const Inventory = () => {
  const [tools, setTools] = useState([]);
  const [isLoading, setIsLoading] = useState(true); // Estado de carga inicial.
  const [error, setError] = useState(null); // Estado de error.
  const { keycloak } = useKeycloak(); // keycloak para autenticacion
  const [isFormOpen, setIsFormOpen] = useState(false); // Estado para controlar la visibilidad del formulario

  useEffect(() => {
    fetchTools();
  }, []);

  // Función para cargar las herramientas desde el backend
  const fetchTools = async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const response = await toolService.getAllToolsForAdmin(); // Obtiene todas las herramientas para el administrador
      setTools(response.data);
    } catch (err) {
      setError('No se pudo cargar el inventario. Asegúrate de tener permisos de administrador.');
    } finally {
      setIsLoading(false);
    }
  };

  const getAdminRut = () => keycloak.tokenParsed?.rut;

  // Maneja el guardado de una nueva herramienta
  const handleSave = async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const toolData = {
      name: formData.get('name')?.trim(),
      toolValue: parseFloat(formData.get('toolValue')),
      rentalFee: parseFloat(formData.get('rentalFee')),
      initialState: 'Disponible',
      category: formData.get('category')?.trim(),         
      stock: parseInt(formData.get('stock'), 10),
      
    };

    try {
      await toolService.saveTool(toolData, getAdminRut());
      setIsFormOpen(false);
      fetchTools();
    } catch (err) { setError("No se pudo guardar la herramienta."); }
  };
  
  // Maneja la eliminación (baja) de herramientas
  const handleDelete = async (tool) => {
    const quantity = prompt(`Ingresa la cantidad de "${tool.name}" a dar de baja (máx: ${tool.stock}):`, tool.stock);
    if (quantity === null) return;
    const quantityNum = parseInt(quantity);
    if (!isNaN(quantityNum) && quantityNum > 0 && quantityNum <= tool.stock) {
      try {
        await toolService.deactivateTool(tool.id, quantityNum, getAdminRut());
        fetchTools();
      } catch (err) { setError("No se pudo dar de baja la herramienta."); }
    } else { alert("Cantidad inválida."); }
  };

  // Maneja el envío de herramientas a reparación
  const handleRepair = async (tool) => {
    const quantity = prompt(`Ingresa la cantidad de "${tool.name}" a enviar a reparación (máx: ${tool.stock}):`, 1);
    if (quantity === null) return;
    const quantityNum = parseInt(quantity);
    if (!isNaN(quantityNum) && quantityNum > 0 && quantityNum <= tool.stock) {
      try {
        await toolService.repairTool(tool.id, quantityNum, getAdminRut());
        fetchTools();
      } catch (err) { setError("No se pudo enviar a reparación."); }
    } else { alert("Cantidad inválida."); }
  };

  // Maneja el marcado de herramientas como disponibles
  const handleAvailable = async (tool) => {
    const quantity = prompt(`Ingresa la cantidad de "${tool.name}" a marcar como disponible (máx: ${tool.stock}):`, 1);
    if (quantity === null) return;
    const quantityNum = parseInt(quantity);
    if (!isNaN(quantityNum) && quantityNum > 0 && quantityNum <= tool.stock) {
      try {
        await toolService.availableTool(tool.id, getAdminRut(), quantityNum);
        fetchTools();
      } catch (err) { setError("No se pudo marcar como disponible."); }
    } else { alert("Cantidad inválida."); }
  };

  // Maneja la actualización de la tarifa de arriendo
  const handleUpdateFee = async (tool) => {
    const newFee = prompt(`Ingresa la nueva tarifa para "${tool.name}":`, tool.rentalFee);
    if (newFee === null) return;
    const newFeeNum = parseFloat(newFee);
    if (!isNaN(newFeeNum) && newFeeNum >= 0) {
      try {
        await toolService.updateToolFee(tool.id, newFeeNum);
        fetchTools();
      } catch (err) { setError("No se pudo actualizar la tarifa."); }
    } else { alert("Valor inválido."); }
  };

  const handleUpdateValue = async (tool) => {
    const newValue = prompt(`Ingresa el nuevo valor para "${tool.name}":`, tool.toolValue);
    if (newValue === null) return;
    const newValueNum = parseFloat(newValue);
    if (!isNaN(newValueNum) && newValueNum > 0) {
      try {
        await toolService.updateReplacementValue(tool.id, newValueNum);
        fetchTools();
      } catch (err) { setError("No se pudo actualizar el valor."); }
    } else { alert("Valor inválido."); }
  };

  if (isLoading) return <div>Cargando inventario...</div>;

  return (
    <div>
      <h1>Gestión de Inventario</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {/* Contenedor para el botón de añadir herramienta */}
      <div className="button-container">
      <button 
        className="action-button"
        onClick={() => setIsFormOpen(true)}         
      >
        Añadir Herramienta
      </button>
      </div>


      <table className="tools-table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Stock</th>
            <th>Estado</th>
            <th>Tarifa Arriendo</th>
            <th>Valor Reposición</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {tools.map((tool) => (
            <tr key={tool.id}>
              <td>{tool.name}</td>
              <td>{tool.stock}</td>
              <td>{tool.initialState}</td>
              <td>${tool.rentalFee.toFixed(2)}</td>
              <td>${tool.toolValue.toFixed(2)}</td>
              <td>
                <Button size="small" onClick={() => handleUpdateFee(tool)}>Tarifa</Button>
                <Button size="small" onClick={() => handleUpdateValue(tool)}>Valor</Button>
                <Button size="small" onClick={() => handleRepair(tool)}>Reparar</Button>
                <Button size="small" color="secondary" onClick={() => handleDelete(tool)}>Dar de Baja</Button>
                <Button size="small" onClick={() => handleAvailable(tool)}>Disponible</Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <Modal open={isFormOpen} onClose={() => setIsFormOpen(false)}>
        <Box className="addTool" component="form" onSubmit={handleSave}>
          <h2>Añadir Herramienta</h2>
          <TextField name="name" label="Nombre" fullWidth margin="normal" required />
          <TextField name="toolValue" label="Valor de Reposición ($)" type="number" fullWidth margin="normal" required />
          <TextField name="rentalFee" label="Tarifa de Arriendo Diaria ($)" type="number" fullWidth margin="normal" required />
          <TextField name="category" label="Categoría" fullWidth margin="normal" required />
          <TextField name="stock" label="Stock Inicial" type="number" fullWidth margin="normal" required />
          
          
          <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setIsFormOpen(false)} sx={{ mr: 1 }}>Cancelar</Button>
            <Button type="submit" variant="contained">Guardar</Button>
          </Box>
        </Box>
      </Modal>
    </div>
  );
};



export default Inventory;