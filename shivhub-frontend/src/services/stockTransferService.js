import api from "./api";

export const listStockTransfers = async (status) => {
    const params = status && status !== "ALL" ? { status } : {};
    return (await api.get("/api/stock-transfers", { params })).data;
};

export const createStockTransfer = async (payload) => (await api.post("/api/stock-transfers", payload)).data;
export const confirmStockTransferHandover = async (id) => (await api.put(`/api/stock-transfers/${id}/confirm-handover`)).data;
export const confirmStockTransferAdjustment = async (id, payload) => (await api.put(`/api/stock-transfers/${id}/confirm-adjustment`, payload)).data;
export const getAvailableImeis = async (productId) => (await api.get("/api/offline-bills/imei/available", { params: { productId } })).data;

