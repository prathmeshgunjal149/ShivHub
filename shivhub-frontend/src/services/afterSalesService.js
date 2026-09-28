import api from "./api";

const customerBase = "/api/customer/after-sales";
const sellerBase = "/api/seller/after-sales";

export const afterSalesService = {
  eligiblePurchases: () => api.get(`${customerBase}/eligible-purchases`),
  createRequest: payload => api.post(`${customerBase}/requests`, payload),
  myRequests: () => api.get(`${customerBase}/requests`),
  myRequest: id => api.get(`${customerBase}/requests/${id}`),
  decideEstimate: (id, payload) => api.post(`${customerBase}/requests/${id}/estimate-decision`, payload),
  cancel: (id, remarks) => api.post(`${customerBase}/requests/${id}/cancel`, { remarks }),
  sellerRequests: status => api.get(`${sellerBase}/requests`, { params: status ? { status } : {} }),
  sellerSummary: () => api.get(`${sellerBase}/summary`),
  sellerOfflineReturnBill: billNumber => api.get(`${sellerBase}/offline-returns/bill/${encodeURIComponent(billNumber)}`),
  sellerOfflineReturnLookup: code => api.get(`${sellerBase}/offline-returns/lookup/${encodeURIComponent(code)}`),
  createSellerOfflineReturn: payload => api.post(`${sellerBase}/offline-returns`, payload),
  sellerRequest: id => api.get(`${sellerBase}/requests/${id}`),
  status: (id, payload) => api.post(`${sellerBase}/requests/${id}/status`, payload),
  receive: (id, payload) => api.post(`${sellerBase}/requests/${id}/receive`, payload),
  inspection: (id, payload) => api.put(`${sellerBase}/requests/${id}/inspection`, payload),
  estimate: (id, payload) => api.put(`${sellerBase}/requests/${id}/estimate`, payload),
  technician: (id, payload) => api.post(`${sellerBase}/requests/${id}/technician`, payload),
  dispatch: (id, payload) => api.post(`${sellerBase}/requests/${id}/dispatch`, payload),
  replacement: (id, payload) => api.post(`${sellerBase}/requests/${id}/replacement`, payload),
  refund: (id, payload) => api.post(`${sellerBase}/requests/${id}/refunds`, payload),
  completeRefund: (id, refundId, payload) => api.post(`${sellerBase}/requests/${id}/refunds/${refundId}/complete`, payload),
  customerAttachment: (id, type, file) => { const data = new FormData(); data.append("type", type); data.append("file", file); return api.post(`${customerBase}/requests/${id}/attachments`, data); },
  sellerAttachment: (id, type, file) => { const data = new FormData(); data.append("type", type); data.append("file", file); return api.post(`${sellerBase}/requests/${id}/attachments`, data); },
  customerDocument: id => api.get(`${customerBase}/requests/${id}/documents/JOB_SHEET`, { responseType: "blob" }),
  sellerDocument: (id, type) => api.get(`${sellerBase}/requests/${id}/documents/${type}`, { responseType: "blob" }),
  adminPolicies: () => api.get("/api/admin/after-sales/policies"),
  saveAdminPolicy: (id, payload) => id ? api.put(`/api/admin/after-sales/policies/${id}`, payload) : api.post("/api/admin/after-sales/policies", payload),
  deactivateAdminPolicy: id => api.delete(`/api/admin/after-sales/policies/${id}`),
  adminRequests: params => api.get("/api/admin/after-sales/requests", { params }),
  adminReport: params => api.get("/api/admin/after-sales/reports", { params }),
  adminOverride: (id, payload) => api.post(`/api/admin/after-sales/requests/${id}/override`, payload)
};
