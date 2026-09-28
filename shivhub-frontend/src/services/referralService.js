import api from "./api";
export const getMyReferral = () => api.get("/api/referrals/me").then(response => response.data);
export const sendReferralInvite = (email) => api.post("/api/referrals/invite", { email });
