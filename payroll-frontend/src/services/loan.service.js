import httpClient from '../http-common.js';

const createLoan = (loanData) => {
  return httpClient.post('/loan/create', loanData);
};

const returnLoan = (idLoan, returnData) => {
  const params = new URLSearchParams();


  params.append('dailyLateFee', returnData.dailyLateFee);


  }
  params.append('repairCost', returnData.repairCost);

  if (returnData.damagedTools && Array.isArray(returnData.damagedTools)) {
    returnData.damagedTools.forEach(tool => params.append('damaged', tool));
  }
  if (returnData.discardedTools && Array.isArray(returnData.discardedTools)) {
    returnData.discardedTools.forEach(tool => params.append('discarded', tool));
  }

  return httpClient.put(`/loan/return/${idLoan}`, null, { params });


const getLoanById = (idLoan) => {
  return httpClient.get(`/loan/${idLoan}`);
};

// ✅ FIX: tu controller es PUT /{idLoan}/pay
const markAsPaid = (idLoan) => {
  return httpClient.put(`/loan/${idLoan}/pay`);
};

const getMyLoans = (rut) => {
  return httpClient.get(`/loan/my-loans?rut=${rut}`);
};

export {
  createLoan,
  returnLoan,
  markAsPaid,
  getMyLoans,
  getLoanById
};
