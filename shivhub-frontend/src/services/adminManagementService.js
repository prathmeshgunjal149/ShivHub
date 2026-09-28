import api from "./api";

const data = request => request.then(response => response.data);
const page = request => data(request).then(payload => {
    const source = Array.isArray(payload) ? payload : payload?.content;
    const content = Array.isArray(source) ? source : [];
    return {
        content,
        totalElements: Number(payload?.totalElements ?? content.length),
        totalPages: Number(payload?.totalPages ?? (content.length ? 1 : 0)),
        page: Number(payload?.page ?? 0),
        size: Number(payload?.size ?? content.length)
    };
});

export const listAdminSellers = params => page(api.get("/api/admin/sellers", { params })).then(result => ({
    ...result,
    content: result.content.map(seller => ({
        ...seller,
        ownerName: seller.ownerName ?? seller.name ?? "Unnamed seller",
        shopName: seller.shopName ?? seller.businessName ?? "Shop not configured",
        approvalStatus: seller.approvalStatus ?? seller.status ?? "PENDING",
        totalProducts: seller.totalProducts ?? 0,
        availableStockQuantity: seller.availableStockQuantity ?? 0,
        stockValue: seller.stockValue ?? 0,
        offlineSales: seller.offlineSales ?? 0,
        onlineSales: seller.onlineSales ?? 0,
        totalRevenue: seller.totalRevenue ?? 0,
        totalCustomers: seller.totalCustomers ?? 0,
        customerReceivable: seller.customerReceivable ?? 0
    }))
}));
export const getAdminSeller = id => data(api.get(`/api/admin/sellers/${id}`));
export const getAdminSellerSales = (id, params) => data(api.get(`/api/admin/sellers/${id}/sales`, { params }));
export const getAdminSellerCustomers = (id, params) => data(api.get(`/api/admin/sellers/${id}/customers`, { params }));
export const getAdminSellerPayments = id => data(api.get(`/api/admin/sellers/${id}/payments`));
export const suspendAdminSeller = id => data(api.put(`/api/admin/sellers/${id}/suspend`));
export const updateAdminSellerProduct = (sellerId, productId, status) => data(api.put(`/api/admin/sellers/${sellerId}/products/${productId}/status`, { status }));
export const downloadAdminSellerOfflineInvoice = (sellerId, billId) =>
    api.get(`/api/admin/sellers/${sellerId}/sales/OFFLINE_BILL/${billId}/invoice`, { responseType: "blob" })
        .then(response => response.data);
export const downloadAdminOrderInvoice = orderId =>
    api.get(`/api/admin/orders/${orderId}/invoice`, { responseType: "blob" })
        .then(response => response.data);
export const listAdminCustomers = params => page(api.get("/api/admin/customers", { params })).then(result => ({
    ...result,
    content: result.content.map(customer => ({
        ...customer,
        customerType: customer.customerType ?? "ONLINE",
        associatedSellers: customer.associatedSellers ?? [],
        totalOrdersAndBills: customer.totalOrdersAndBills ?? 0,
        totalPurchaseAmount: customer.totalPurchaseAmount ?? 0,
        pendingReceivable: customer.pendingReceivable ?? 0
    }))
}));
export const getAdminCustomer = id => data(api.get(`/api/admin/customers/${id}`));

export const listAdminCampaigns = () => data(api.get("/api/admin/campaigns"));
export const getAdminCampaign = id => data(api.get(`/api/admin/campaigns/${id}`));
export const createAdminCampaign = payload => data(api.post("/api/admin/campaigns", payload));
export const uploadAdminCampaignBanner = file => {
    const formData = new FormData();
    formData.append("file", file);
    return data(api.post("/api/admin/campaigns/banner", formData, {
        headers: { "Content-Type": "multipart/form-data" }
    }));
};
export const updateAdminCampaign = (id, payload) => data(api.put(`/api/admin/campaigns/${id}`, payload));
export const previewAdminCampaignAudience = payload => data(api.post("/api/admin/campaigns/audience-preview", payload));
export const sendAdminCampaign = id => data(api.post(`/api/admin/campaigns/${id}/send`));
export const retryFailedAdminCampaign = id => data(api.post(`/api/admin/campaigns/${id}/retry-failed`));
export const scheduleAdminCampaign = (id, scheduledAt) => data(api.post(`/api/admin/campaigns/${id}/schedule`, { scheduledAt }));
export const testAdminCampaign = (id, email) => data(api.post(`/api/admin/campaigns/${id}/test`, { email }));
export const getAdminCampaignReport = (id, params) => data(api.get(`/api/admin/campaigns/${id}/report`, { params }));
