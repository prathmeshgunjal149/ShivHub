import { useMemo, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { BarChart3, Boxes, ClipboardList, LayoutDashboard, MessageCircle, Search, Settings, Store, Users } from "lucide-react";
import WorkspacePreferences from "./WorkspacePreferences";
import "./AdminModuleNavigation.css";

const navigation = [
    { to: "/admin/dashboard", label: "Dashboard", icon: LayoutDashboard },
    { to: "/admin/customers", label: "Customers", icon: Users },
    { to: "/admin/sellers", label: "Sellers", icon: Store },
    { to: "/admin/products", label: "Products", icon: Boxes },
    { to: "/admin/orders", label: "Orders", icon: ClipboardList },
    { to: "/admin/notifications", label: "WhatsApp", icon: MessageCircle },
    { to: "/admin/reports", label: "Reports", icon: BarChart3 },
    { to: "/admin/settings", label: "Settings", icon: Settings }
];

/** Shared sticky navigation for every admin module outside the dashboard. */
export default function AdminModuleNavigation() {
    const { pathname } = useLocation();
    const navigate = useNavigate();
    const [query, setQuery] = useState("");
    const matches = useMemo(() => {
        const value = query.trim().toLowerCase();
        return value ? navigation.filter(item => item.label.toLowerCase().includes(value)).slice(0, 5) : [];
    }, [query]);
    if (!pathname.startsWith("/admin/") || pathname === "/admin/dashboard") return null;

    return (
        <header className="admin-module-navigation">
            <NavLink className="admin-module-brand" to="/admin/dashboard" aria-label="ShivHub admin dashboard">
                <span>SH</span><strong>ShivHub <em>ADMIN</em></strong>
            </NavLink>
            <form className="admin-module-search" onSubmit={event => { event.preventDefault(); if (matches[0]) { navigate(matches[0].to); setQuery(""); } }}>
                <Search size={17} aria-hidden="true" />
                <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search admin modules…" aria-label="Search admin modules" />
                {matches.length > 0 && <div className="admin-module-search-results">{matches.map(item => <button type="button" key={item.to} onClick={() => { navigate(item.to); setQuery(""); }}>{item.label}</button>)}</div>}
            </form>
            <WorkspacePreferences scope="admin" compact />
            <nav aria-label="Admin modules">
                {navigation.map(({ to, label, icon: Icon }) => (
                    <NavLink key={to} to={to} className={({ isActive }) => isActive || (to !== "/admin/dashboard" && pathname.startsWith(`${to}/`)) ? "active" : ""}>
                        <Icon size={16} /><span>{label}</span>
                    </NavLink>
                ))}
            </nav>
        </header>
    );
}
