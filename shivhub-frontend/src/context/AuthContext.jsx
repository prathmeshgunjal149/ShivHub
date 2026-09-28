import { useCallback, useEffect, useState } from "react";

import AuthContext from "./AuthContextValue";
import authService from "../services/authService";
import { getLoyaltySummary } from "../services/loyaltyService";

/*
 * AuthProvider
 *
 * Provides authentication information
 * to the complete ShivHub application.
 *
 * Handles:
 *
 * 1. Current logged-in user
 * 2. Login
 * 3. Register
 * 4. Logout
 * 5. Authentication status
 */

const AuthProvider = ({ children }) => {

    /*
     * Get previously logged-in user
     * from localStorage.
     */
    const storedUser =
        localStorage.getItem("shivhub_user");

    /*
     * Current user state.
     *
     * If a user was already logged in,
     * restore that user from localStorage.
     */
    const [user, setUser] = useState(
        storedUser
            ? JSON.parse(storedUser)
            : null
    );
    const [loyaltySummary, setLoyaltySummary] = useState(null);

    const refreshLoyalty = useCallback(async () => {
        if (user?.role !== "CUSTOMER" || !localStorage.getItem("shivhub_token")) {
            setLoyaltySummary(null);
            return null;
        }
        try {
            const summary = await getLoyaltySummary();
            setLoyaltySummary(summary);
            return summary;
        } catch {
            // A loyalty display failure must never invalidate a working login.
            setLoyaltySummary(null);
            return null;
        }
    }, [user?.role]);

    useEffect(() => { refreshLoyalty(); }, [refreshLoyalty]);
    useEffect(() => {
        window.addEventListener("shivhub:loyalty-changed", refreshLoyalty);
        return () => window.removeEventListener("shivhub:loyalty-changed", refreshLoyalty);
    }, [refreshLoyalty]);


    /*
     * LOGIN
     *
     * Sends email and password
     * to the Spring Boot backend.
     */
    const login = async (loginData) => {

        /*
         * Call login API.
         */
        const response =
            await authService.login(loginData);

        // Changed: customer and seller login pauses here until their email OTP is verified.
        if (response.otpRequired) {
            return response;
        }

        return saveAuthenticatedUser(response);
    };

    // Changed: completes the OTP flow and persists the JWT only after backend verification.
    const verifyLoginOtp = async (email, otp) => {
        const response = await authService.verifyLoginOtp(email, otp);
        return saveAuthenticatedUser(response);
    };

    const saveAuthenticatedUser = (response) => {

        /*
         * Save JWT token.
         */
        localStorage.setItem(
            "shivhub_token",
            response.token
        );

        /*
         * Create safe user object.
         *
         * We don't need to store the password.
         */
        const userData = {

            userId: response.userId,

            name: response.name,

            email: response.email,

            role: response.role
        };

        /*
         * Save user information.
         */
        localStorage.setItem(
            "shivhub_user",
            JSON.stringify(userData)
        );

        /*
         * Update React state.
         */
        setUser(userData);

        /*
         * Return backend response.
         *
         * Login page uses role
         * for dashboard redirection.
         */
        return response;
    };


    /*
     * REGISTER
     *
     * Sends customer registration
     * information to backend.
     */
    const register = async (userData) => {

        return await authService.register(userData);
    };


    /*
     * LOGOUT
     *
     * Remove JWT and user information.
     */
    const logout = () => {

        authService.logout();

        setUser(null);
        setLoyaltySummary(null);
    };


    /*
     * AUTHENTICATION STATUS
     *
     * User is authenticated only when:
     *
     * 1. User information exists
     * 2. JWT token exists
     */
    const isAuthenticated =
        user !== null &&
        localStorage.getItem("shivhub_token") !== null;


    /*
     * Provide authentication data
     * to all child components.
     */
    return (
        <AuthContext.Provider
            value={{
                user,
                login,
                verifyLoginOtp,
                register,
                logout,
                isAuthenticated,
                loyaltySummary,
                refreshLoyalty
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export default AuthProvider;
