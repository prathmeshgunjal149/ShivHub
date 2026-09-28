import { createContext } from "react";

/*
 * AuthContextValue
 *
 * This context stores authentication information
 * shared across the ShivHub application.
 *
 * It is used by:
 *
 * - AuthProvider
 * - useAuth hook
 * - Login
 * - Register
 * - Protected Routes
 * - Role Routes
 */

const AuthContext = createContext(null);

export default AuthContext;