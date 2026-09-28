import { useContext } from "react";

import AuthContext from "../context/AuthContextValue";

/*
 * useAuth
 *
 * Custom hook used by any component
 * that needs authentication information.
 *
 * Example:
 *
 * const {
 *     user,
 *     login,
 *     logout,
 *     isAuthenticated
 * } = useAuth();
 */

const useAuth = () => {

    return useContext(AuthContext);
};

export default useAuth;