import axios from "axios";

/*
 * =========================================================
 * SHIVHUB API CONFIGURATION
 * =========================================================
 *
 * React:
 * http://localhost:5173
 *
 * Spring Boot:
 * http://localhost:8081 (override with VITE_API_BASE_URL)
 *
 * Every protected API request automatically receives:
 *
 * Authorization: Bearer <JWT>
 * =========================================================
 */

export const API_BASE_URL = (
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8081"
).replace(/\/$/, "");

const api = axios.create({

    baseURL: API_BASE_URL,

    headers: {
        "Content-Type": "application/json"
    }

});


/*
 * =========================================================
 * GET JWT TOKEN
 * =========================================================
 *
 * We check all token keys currently used by ShivHub.
 *
 * Primary key:
 *     shivhub_token
 *
 * Fallback keys:
 *     token
 *     jwtToken
 *     accessToken
 *
 * =========================================================
 */

const getToken = () => {

    return (
        localStorage.getItem("shivhub_token") ||
        localStorage.getItem("token") ||
        localStorage.getItem("jwtToken") ||
        localStorage.getItem("accessToken")
    );

};


/*
 * =========================================================
 * REQUEST INTERCEPTOR
 * =========================================================
 *
 * Runs before every API request.
 *
 * If JWT exists:
 *
 * Authorization: Bearer <token>
 *
 * =========================================================
 */

api.interceptors.request.use(

    (config) => {

        const token = getToken();

        if (config.data instanceof FormData && config.headers) {
            delete config.headers["Content-Type"];
            delete config.headers["content-type"];
        }


        // Public authentication endpoints must not inherit a stale token from
        // local storage; sign-in and social configuration work anonymously.
        const isPublicAuthRequest = String(config.url || "").startsWith("/api/auth/");

        if (token && !isPublicAuthRequest) {

            config.headers =
                config.headers || {};

            config.headers.Authorization =
                `Bearer ${token}`;

        }


        return config;

    },

    (error) => {

        return Promise.reject(error);

    }

);


/*
 * =========================================================
 * RESPONSE INTERCEPTOR
 * =========================================================
 *
 * We DON'T automatically redirect to login here.
 *
 * This is important because the actual page/component
 * should decide how to handle authentication errors.
 *
 * =========================================================
 */

api.interceptors.response.use(

    (response) => {

        return response;

    },

    (error) => {

        if (
            error.response?.status === 401 ||
            error.response?.status === 403
        ) {

            console.warn(
                "Authentication error:",
                error.response.status,
                error.config?.url
            );

        }


        return Promise.reject(error);

    }

);


export default api;
