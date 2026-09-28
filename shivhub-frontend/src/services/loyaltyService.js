import api from "./api";

export const getLoyaltySummary = async () => (await api.get("/api/customer/loyalty/summary")).data;

export const getLoyaltyTransactions = async (page = 0, size = 20) =>
    (await api.get("/api/customer/loyalty/transactions", { params: { page, size } })).data;

/** Lets checkout/profile flows request a safe replacement refresh without sharing mutable balances. */
export const notifyLoyaltyChanged = () => window.dispatchEvent(new Event("shivhub:loyalty-changed"));
