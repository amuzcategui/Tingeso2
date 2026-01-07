import httpClient from '../http-common.js';

// ===================== K A R D E X - S E R V I C E =====================

const getToolHistory = (toolName) => {
  return httpClient.get('/kardex/tool-history', { params: { toolName } });
};

const getMovementsInRange = (from, to, movementType) => {
  return httpClient.get('/kardex/range', {
    params: { from, to, movementType }
  });
};

const getAllKardex = () => {
  return httpClient.get('/kardex/all');
};

// ===================== R E P O R T I N G - S E R V I C E =====================

// Antes estaba mal: /kardex/loans/active/grouped ❌
// Correcto: /reporting/loans/active/grouped ✅
const getActiveLoansGrouped = (from, to) => {
  const params = {};
  if (from) params.from = from;
  if (to) params.to = to;
  return httpClient.get('/reporting/loans/active/grouped', { params });
};

// Antes estaba mal: /kardex/tools/top ❌
// Correcto: /reporting/tools/top ✅
const getTopTools = (from, to, limit) => {
  const params = {};
  if (from) params.from = from;
  if (to) params.to = to;
  if (limit) params.limit = limit;
  return httpClient.get('/reporting/tools/top', { params });
};

export {
  getToolHistory,
  getMovementsInRange,
  getActiveLoansGrouped,
  getTopTools,
  getAllKardex
};
