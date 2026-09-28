import api from "./api";

const suggestions = response => response?.data?.suggestions || [];

export const getCustomerSearchSuggestions = query =>
    api.get("/api/customer/search/suggestions", { params: { q: query, limit: 8 } }).then(suggestions);

export const getSellerSearchSuggestions = (type, query) =>
    api.get("/api/seller/search/suggestions", { params: { type, q: query, limit: 8 } }).then(suggestions);

export const getSellerProductSearchSuggestions = query => getSellerSearchSuggestions("PRODUCT", query);
export const getSellerCustomerSearchSuggestions = query => getSellerSearchSuggestions("CUSTOMER", query);
export const getSellerDistributorSearchSuggestions = query => getSellerSearchSuggestions("DISTRIBUTOR", query);

export const getAdminSearchSuggestions = (type, query) =>
    api.get("/api/admin/search/suggestions", { params: { type, q: query, limit: 8 } }).then(suggestions);
