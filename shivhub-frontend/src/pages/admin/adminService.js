import api from "./api";

export const getProducts = async () => {
    const response = await api.get("/admin/products");
    return response.data;
};

export const getProduct = async (id) => {
    const response = await api.get(`/admin/products/${id}`);
    return response.data;
};

export const getPendingProducts = async () => {
    const response = await api.get("/admin/products/pending");
    return response.data;
};

export const createAdminProduct = async (formData) => {
    const response = await api.post(
        "/admin/products",
        formData,
        {
            headers: {
                "Content-Type": "multipart/form-data"
            }
        }
    );

    return response.data;
};

export const approveProduct = async (
    id,
    review = ""
) => {

    const response = await api.put(
        `/admin/products/${id}/approve`,
        {
            review
        }
    );

    return response.data;
};

export const rejectProduct = async (
    id,
    reason
) => {

    const response = await api.put(
        `/admin/products/${id}/reject`,
        {
            reason
        }
    );

    return response.data;
};