import api from "./api";

/*
 * Authentication Service
 *
 * Handles:
 * - Customer registration
 * - Login
 * - Logout
 */

const authService = {

    /*
     * Customer Registration
     */
    register: async (userData) => {

        const response = await api.post(
            "/api/auth/register",
            userData
        );

        return response.data;
    },


    /*
     * Login
     *
     * Sends email and password to backend.
     */
    login: async (loginData) => {

        const response = await api.post(
            "/api/auth/login",
            loginData
        );

        return response.data;
    },

    sendWhatsAppRegistrationOtp: async email => (await api.post("/api/auth/register/send-whatsapp-otp", { email })).data,
    resendWhatsAppRegistrationOtp: async email => (await api.post("/api/auth/register/resend-whatsapp-otp", { email })).data,
    verifyWhatsAppRegistrationOtp: async (email, otp) => (await api.post("/api/auth/register/verify-whatsapp-otp", { email, otp })).data,

    // Changed: exchanges the emailed login code for a JWT after OTP verification.
    verifyLoginOtp: async (email, otp) => {
        const response = await api.post("/api/auth/verify-login-otp", { email, otp });
        return response.data;
    },

    // Changed: keeps both registration requests on the shared API client.
    registerSeller: async (userData) => {
        const response = await api.post("/api/auth/register/seller", userData);
        return response.data;
    },

    requestSellerRegistrationOtp: async (userData) => {
        const response = await api.post("/api/auth/register/seller/request-otp", userData);
        return response.data;
    },

    verifySellerRegistrationOtp: async (email, otp) => {
        const response = await api.post("/api/auth/register/seller/verify-otp", { email, otp });
        return response.data;
    },


    /*
     * Logout
     */
    logout: () => {

        localStorage.removeItem("shivhub_token");
        localStorage.removeItem("shivhub_user");
    }
};

export default authService;
