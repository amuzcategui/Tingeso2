import httpClient from '../http-common.js';

// POST /api/v1/customer/check-and-create
const completeProfile = () => {
  return httpClient.post('/customer/check-and-create');
};

// GET /api/v1/customer
const getAllCustomers = () => {
  return httpClient.get('/customer/all');
};

const findCustomerByRut = (rut) => {
  return httpClient.get('/customer/findCustomer', {
    params: { rut }
  });
};

const getCustomersWithLoansGreaterThan = (quantity) => {
  return httpClient.get('/customer/allGreatherThan', {
    params: { quantity }
  });
};

// PUT /api/v1/customer/{rut}/status?status=Restringido|Activo
// Mantengo el nombre updateRestriction.
// ✅ Default: "Restringido" para no obligarte a cambiar todas las llamadas del front.
const updateRestriction = (rut, status = 'Restringido') => {
  return httpClient.put(`/customer/${encodeURIComponent(rut)}/status`, null, {
    params: { status }
  });
};

// REPORTING: GET /api/v1/reporting/customers/overdue?from&to
const getOverdueCustomers = (from, to) => {
  const params = {};
  if (from) params.from = from;
  if (to) params.to = to;
  return httpClient.get('/reporting/customers/overdue', { params });
};

export default {
  completeProfile,
  getAllCustomers,
  findCustomerByRut,
  getCustomersWithLoansGreaterThan,
  getOverdueCustomers,
  updateRestriction,
};
