import { Navigate, Outlet } from "react-router-dom";
import useAuth from "../hooks/useAuth";

/*
 * RoleRoute
 *
 * Restricts a route based on user's role.
 *
 * Allowed roles:
 *
 * CUSTOMER
 * SELLER
 * ADMIN
 */

const RoleRoute = ({ allowedRoles }) => {

    const { user, isAuthenticated } = useAuth();

    /*
     * User is not logged in.
     */
    if (!isAuthenticated) {

        return <Navigate to="/login" replace />;
    }

    /*
     * Check whether user's role is allowed.
     */
    if (!allowedRoles.includes(user?.role)) {

        /*
         * User doesn't have permission.
         *
         * Send them to their correct dashboard.
         */

        if (user?.role === "ADMIN") {

            return (
                <Navigate
                    to="/admin/dashboard"
                    replace
                />
            );
        }

        if (user?.role === "SELLER") {

            return (
                <Navigate
                    to="/seller/dashboard"
                    replace
                />
            );
        }

        /*
         * Default dashboard for CUSTOMER.
         */
        return (
            <Navigate
                to="/customer/dashboard"
                replace
            />
        );
    }

    /*
     * User has correct role.
     */
    return <Outlet />;
};

export default RoleRoute;