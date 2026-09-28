import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { MotionConfig } from "framer-motion";

import AppRoutes from "./routes/AppRoutes";
import AuthProvider from "./context/AuthContext";

import "./index.css";
import "./styles/ShivHubTheme.css";
import "./styles/WorkspacePreferences.css";
import "./styles/CustomerExperience.css";

/*
 * ShivHub React Application Entry Point
 *
 * Application flow:
 *
 * BrowserRouter
 *      ↓
 * AuthProvider
 *      ↓
 * AppRoutes
 *      ↓
 * Pages / Dashboards
 */

createRoot(document.getElementById("root")).render(
    <StrictMode>

        <MotionConfig reducedMotion="user">
            <BrowserRouter>

                <AuthProvider>

                    <AppRoutes />

                </AuthProvider>

            </BrowserRouter>
        </MotionConfig>

    </StrictMode>
);
