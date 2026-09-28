import api from "./api";

/** Resolves one exact scanner value against the authenticated seller's billable stock. */
export const scanBillingProduct = async (code) => {
    const response = await api.get("/api/seller/offline-billing/scan", {
        params: { code }
    });
    return response.data;
};
