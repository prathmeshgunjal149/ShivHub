import { useLocation, useNavigate } from "react-router-dom";
import SellerSidebar from "./SellerSidebar";
import "./SellerModulePlaceholder.css";

/* Prevents authenticated sellers from being sent to login while a module is being built. */
export default function SellerModulePlaceholder() {
    const navigate = useNavigate();
    const location = useLocation();
    const title = location.pathname.split("/").pop().replace(/-/g, " ");
    return <div className="seller-module-page"><SellerSidebar /><main className="seller-module-main"><span>SHIVHUB SELLER MODULE</span><h1>{title}</h1><p>This module is reserved for your seller account and is being connected to its dedicated backend workflow.</p><button type="button" onClick={() => navigate("/seller/dashboard")}>← Back to dashboard</button></main></div>;
}
