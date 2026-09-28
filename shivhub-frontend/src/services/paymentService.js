import api from "./api";

const data = request => request.then(response => response.data);

export const createOnlineRazorpayOrder = orderId => data(api.post("/api/customer/payments/online-order/create", { orderId }));
export const verifyOnlineRazorpayPayment = payload => data(api.post("/api/customer/payments/verify", payload));
export const getOnlinePaymentStatus = orderId => data(api.get(`/api/customer/payments/order/${orderId}`));

export const createOfflineRazorpayOrder = billId => data(api.post(`/api/seller/offline-bills/${billId}/payments/razorpay/create`));
export const createOfflineRazorpayPaymentLink = billId => data(api.post(`/api/seller/offline-bills/${billId}/payments/razorpay/link`));
export const verifyOfflineRazorpayPayment = (billId, payload) => data(api.post(`/api/seller/offline-bills/${billId}/payments/razorpay/verify`, payload));
export const recordOfflineManualPayment = (billId, payload) => data(api.post(`/api/seller/offline-bills/${billId}/payments/manual`, payload));
export const getOfflinePaymentSummary = billId => data(api.get(`/api/seller/offline-bills/${billId}/payments/summary`));
export const getOfflinePaymentHistory = billId => data(api.get(`/api/seller/offline-bills/${billId}/payments`));
