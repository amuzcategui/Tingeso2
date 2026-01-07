import httpClient from '../http-common.js';

// ===================== INVENTORY (tools) =====================

const getAllTools = () => httpClient.get('/tools/all');

const getAllToolsForAdmin = () => httpClient.get('/tools/inventory/all');

const saveTool = (toolData, rutAdmin) => {
  return httpClient.post('/tools/save', toolData, { params: { rutPerson: rutAdmin } });
};

const deactivateTool = (toolId, quantity, rutAdmin) => {
  return httpClient.put(`/tools/${toolId}/deactivate`, null, {
    params: {
      rutPerson: rutAdmin,
      quantity: quantity
    }
  });
};

const repairTool = (toolId, quantity, rutAdmin) => {
  return httpClient.put(`/tools/${toolId}/repair`, null, {
    params: {
      rutPerson: rutAdmin,
      quantity: quantity
    }
  });
};

const availableTool = (toolId, rutAdmin, quantity) => {
  return httpClient.put(`/tools/${toolId}/available`, null, {
    params: {
      rutPerson: rutAdmin,
      quantity: quantity
    }
  });
};

// ⚠️ OJO: este endpoint no está en el ToolController que pegaste.
// Si no existe en tu backend actual, esto fallará.
const updateToolFee = (toolId, newFee) => {
  return httpClient.put(`/tool/update-fee?id=${toolId}&fee=${newFee}`);
};

// ===================== PRICING (PricingConfigController) =====================

// GET /api/v1/pricing/config
const getPricingConfig = () => {
  return httpClient.get('/pricing/config');
};

// GET /api/v1/pricing/rental-fee-daily  -> devuelve double
const getRentalFeeDaily = () => {
  return httpClient.get('/pricing/rental-fee-daily');
};

// PUT /api/v1/pricing/config/rental-fee-daily  body: { rentalFeeDaily: 5000 }
const updateRentalFeeDaily = (newValue) => {
  return httpClient.put('/pricing/config/rental-fee-daily', { rentalFeeDaily: newValue });
};

// PUT /api/v1/pricing/tools/{idTool}/value  body: { toolValue: 15000 }
const updateReplacementValue = (toolId, newValue) => {
  return httpClient.put(`/pricing/tools/${toolId}/value`, { toolValue: newValue });
};

export {
  // inventory
  getAllTools,
  getAllToolsForAdmin,
  saveTool,
  deactivateTool,
  repairTool,
  availableTool,
  updateToolFee,

  // pricing
  getPricingConfig,
  getRentalFeeDaily,
  updateRentalFeeDaily,
  updateReplacementValue
};
