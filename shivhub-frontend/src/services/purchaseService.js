import api from "./api";

/*
 * Seller purchase API client.
 * Keeping endpoint details here makes the purchase UI easy to maintain.
 */
export const getPurchases = async () => (await api.get("/api/purchases")).data;
export const getPurchaseCount = async () => (await api.get("/api/purchases/count")).data;
export const createPurchase = async (payload) => (await api.post("/api/purchases", payload)).data;
export const cancelPurchase = async (purchaseId) => (await api.put(`/api/purchases/${purchaseId}/cancel`)).data;
export const getPurchaseItems = async (purchaseId) => (await api.get(`/api/purchases/${purchaseId}/items`)).data;
export const getSellerProducts = async () => (await api.get("/api/products/seller")).data;
export const searchPurchaseProducts = async (params) => (await api.get("/api/purchases/products/search", { params })).data;
export const getPurchaseVariants = async (productId) => (await api.get(`/api/seller/products/${productId}/variants`)).data;
export const validatePurchaseImei = async (productId, code) =>
    (await api.get("/api/purchases/imeis/validate", { params: { productId, code } })).data;
export const getPurchaseSpecificationTemplates = async (categoryId, subCategoryId) =>
    (await api.get("/api/seller/product-specification-templates", {
        params: { categoryId, ...(subCategoryId ? { subCategoryId } : {}) }
    })).data;
export const getActiveSellerDistributors = async () => (await api.get("/api/seller-distributors/active")).data;
export const getProductCategories = async () => (await api.get("/api/categories")).data;
export const getProductSubcategories = async (categoryId) => (await api.get(`/api/categories/${categoryId}/subcategories`)).data;
export const getPurchasePayments = async (purchaseId) => (await api.get(`/api/purchase-payments/purchase/${purchaseId}`)).data;
export const getPurchasePaymentSummary = async (purchaseId) => (await api.get(`/api/purchase-payments/purchase/${purchaseId}/summary`)).data;
export const addPurchasePayment = async (payload) => (await api.post("/api/purchase-payments", payload)).data;

/* Invoice upload must be multipart, so it intentionally does not use JSON headers. */
export const uploadPurchaseInvoice = async (purchaseId, file) => {
    const formData = new FormData();
    formData.append("file", file);
    return (await api.post(`/api/purchases/${purchaseId}/invoice`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
    })).data;
};

/* Uses the existing ProductImage controller; files are attached to the model,
 * not repeated per physical IMEI/quantity. */
export const uploadPurchasedProductImages = async (productId, files) => {
    const formData = new FormData();
    Array.from(files).forEach(file => formData.append("images", file));
    return (await api.post(`/api/product-images/product/${productId}`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
    })).data;
};
