import api from "./api";

const data = request => request.then(response => response.data);

export const getSellerSubscription = () => data(api.get("/api/seller/subscription"));
export const getSellerEntitlements = () => data(api.get("/api/seller/subscription/entitlements"));
export const getAvailableSubscriptionPlans = () => data(api.get("/api/seller/subscription/plans"));
export const getSellerSubscriptionPayments = () => data(api.get("/api/seller/subscription/payments"));
export const createSubscriptionCheckout = planId => data(api.post(`/api/seller/subscription/checkout/${planId}`));
export const verifySubscriptionCheckout = payload => data(api.post("/api/seller/subscription/verify", payload));

export const getAdminSubscriptionOverview = () => data(api.get("/api/admin/subscriptions/overview"));
export const getAdminSubscriptionPlans = () => data(api.get("/api/admin/subscriptions/plans"));
export const getAdminSubscriptionFeatures = () => data(api.get("/api/admin/subscriptions/features"));
export const saveAdminSubscriptionPlan = (id, payload) => data(id ? api.put(`/api/admin/subscriptions/plans/${id}`, payload) : api.post("/api/admin/subscriptions/plans", payload));
export const getAdminSellerSubscriptions = () => data(api.get("/api/admin/subscriptions/sellers"));
export const getAdminSubscriptionPayments = () => data(api.get("/api/admin/subscriptions/payments"));
export const extendSellerSubscription = (sellerId, payload) => data(api.patch(`/api/admin/subscriptions/sellers/${sellerId}/extend`, payload));
export const setSellerSubscriptionSuspended = (sellerId, suspended, payload) => data(api.patch(`/api/admin/subscriptions/sellers/${sellerId}/${suspended ? "suspend" : "reactivate"}`, payload));
