import api from "./api";

export const listAdminReferrals = async (params) => {
    const response = await api.get("/api/admin/referrals", { params });
    return response.data;
};

export const exportAdminReferrals = async (params) => {
    const response = await api.get("/api/admin/referrals/export.csv", { params, responseType: "blob" });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = "shivhub-referrals.csv";
    link.click();
    URL.revokeObjectURL(url);
};
