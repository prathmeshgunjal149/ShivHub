import api from "./api";

export const getMobileSpecifications = async (query = "") => {
    const response = await api.get("/api/mobile-specs", {
        params: query ? { q: query } : {}
    });
    return response.data;
};

export const getAdminMobileSpecifications = async (query = "") => {
    const response = await api.get("/api/admin/mobile-specs", {
        params: query ? { q: query } : {}
    });
    return response.data;
};

export const fetchExternalMobileSpecification = async (brand, model) => {
    const response = await api.get("/api/admin/mobile-specs/fetch", {
        params: { brand, model }
    });
    return response.data;
};

export const createMobileSpecification = async payload =>
    (await api.post("/api/admin/mobile-specs", payload)).data;

export const updateMobileSpecification = async (id, payload) =>
    (await api.put(`/api/admin/mobile-specs/${id}`, payload)).data;

export const activateMobileSpecification = async id =>
    (await api.patch(`/api/admin/mobile-specs/${id}/activate`)).data;

export const deactivateMobileSpecification = async id =>
    (await api.patch(`/api/admin/mobile-specs/${id}/deactivate`)).data;
