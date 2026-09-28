import api from "./api";

const data = (request) => request.then((response) => response.data);

export const validateCoupon = (code, subtotal) =>
    data(api.get("/api/coupons/validate", { params: { code, subtotal } }));

export const getAvailableCoupons = (subtotal) =>
    data(api.get("/api/coupons", { params: subtotal ? { subtotal } : {} }));

export const listCoupons = () => data(api.get("/api/admin/coupons"));
export const createCoupon = (coupon) => data(api.post("/api/admin/coupons", coupon));
export const updateCoupon = (id, coupon) => data(api.put(`/api/admin/coupons/${id}`, coupon));
export const deleteCoupon = (id) => data(api.delete(`/api/admin/coupons/${id}`));
