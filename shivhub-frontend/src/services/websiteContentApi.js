import api from "./api";

export const websiteContentApi = {
    getSettings: () => api.get("/api/public/site-settings").then(response => response.data),
    getPolicy: slug => api.get(`/api/public/policies/${slug}`).then(response => response.data),
    getFaqs: () => api.get("/api/public/faqs").then(response => response.data),
    submitEnquiry: payload => api.post("/api/public/enquiries", payload).then(response => response.data),
    adminSettings: () => api.get("/api/admin/website/settings").then(response => response.data),
    updateAdminSettings: payload => api.put("/api/admin/website/settings", payload).then(response => response.data),
    adminPolicies: () => api.get("/api/admin/website/policies").then(response => response.data),
    adminPolicy: slug => api.get(`/api/admin/website/policies/${slug}`).then(response => response.data),
    savePolicyDraft: (slug, payload) => api.put(`/api/admin/website/policies/${slug}/draft`, payload).then(response => response.data),
    publishPolicy: (slug, payload) => api.put(`/api/admin/website/policies/${slug}/publish`, payload).then(response => response.data),
    policyVersions: slug => api.get(`/api/admin/website/policies/${slug}/versions`).then(response => response.data),
    restorePolicyVersion: (slug, versionId) => api.post(`/api/admin/website/policies/${slug}/versions/${versionId}/restore`).then(response => response.data),
    adminFaqs: () => api.get("/api/admin/website/faqs").then(response => response.data),
    addFaq: payload => api.post("/api/admin/website/faqs", payload).then(response => response.data),
    deleteFaq: id => api.delete(`/api/admin/website/faqs/${id}`),
    enquiries: status => api.get("/api/admin/website/enquiries", { params: status ? { status } : {} }).then(response => response.data),
    updateEnquiry: (id, payload) => api.put(`/api/admin/website/enquiries/${id}`, payload).then(response => response.data)
};
