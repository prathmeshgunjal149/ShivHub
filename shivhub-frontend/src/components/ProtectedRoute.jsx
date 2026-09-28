import { Navigate, Outlet } from "react-router-dom";
import useAuth from "../hooks/useAuth";

/*
 * ProtectedRoute
 *
 * Allows access only to authenticated users.
 *
 * If the user is not logged in,
 * they are redirected to Login page.
 */

const ProtectedRoute = () => {

    const { isAuthenticated } = useAuth();

    /*
     * User is not logged in
     */
    if (!isAuthenticated) {

        return <Navigate to="/login" replace />;
    }

    /*
     * User is authenticated.
     *
     * Outlet renders the child route.
     */
    return <Outlet />;
};

export default ProtectedRoute;