import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import WorkspacePreferences from "../../components/WorkspacePreferences";
import { getSellerEntitlements } from "../../services/subscriptionService";

import "./SellerSidebar.css";

const menuItems = [
    {
        label: "Dashboard",
        icon: "⌂",
        path: "/seller/dashboard"
    },
    {
        label: "Products",
        icon: "▣",
        path: "/seller/products"
    },
    {
        label: "Add Mobile",
        icon: "M",
        path: "/seller/add-product", feature: "PRODUCT_MANAGEMENT"
    },
    {
        label: "Add Product",
        icon: "+",
        path: "/seller/add-other-product", feature: "PRODUCT_MANAGEMENT"
    },
    {
        label: "Add Accessories",
        icon: "A",
        path: "/seller/add-accessories", feature: "PRODUCT_MANAGEMENT"
    },
    {
        label: "General & Pre-owned",
        icon: "GP",
        path: "/seller/general-preowned", feature: "PRODUCT_MANAGEMENT"
    },
    {
        label: "Inventory",
        icon: "▤",
        path: "/seller/inventory", feature: "INVENTORY_MANAGEMENT"
    },
    {
        label: "Orders",
        icon: "🛒",
        path: "/seller/orders"
    },
    {
        label: "Billing",
        icon: "🧾",
        path: "/seller/billing", feature: "OFFLINE_POS"
    },
    {
        label: "Shop Register",
        icon: "▤",
        path: "/seller/shop-register", feature: "OFFLINE_POS"
    },
    {
        label: "Instant Bill",
        icon: "⚡",
        path: "/seller/instant-bills", feature: "OFFLINE_POS"
    },
    {
        label: "POS Returns",
        icon: "R",
        path: "/seller/offline-returns", feature: "OFFLINE_POS"
    },
    {
        label: "Customers",
        icon: "👥",
        path: "/seller/customers", feature: "CUSTOMER_MANAGEMENT"
    },
    {
        label: "Distributors",
        icon: "🚚",
        path: "/seller/distributors", feature: "DISTRIBUTOR_MANAGEMENT"
    },
    {
        label: "After-Sales",
        icon: "AS",
        path: "/seller/after-sales",
        feature: "OFFLINE_POS"
    },
    {
        label: "Staff",
        icon: "👨‍💼",
        path: "/seller/staff", feature: "STAFF_MANAGEMENT"
    },
    {
        label: "Offers",
        icon: "🎁",
        path: "/seller/offers", feature: "MARKETING"
    },
    {
        label: "GST",
        icon: "₹",
        path: "/seller/gst", feature: "GST_REPORTS"
    },
    {
        label: "Reports",
        icon: "📊",
        path: "/seller/reports", feature: "ADVANCED_REPORTS"
    },
    {
        label: "Expenses",
        icon: "💰",
        path: "/seller/expenses", feature: "EXPENSE_MANAGEMENT"
    },
    {
        label: "Customer Payments",
        icon: "₹",
        path: "/seller/payments", feature: "RECEIVABLE_MANAGEMENT"
    },
    {
        label: "Customer Ledger",
        icon: "₹",
        path: "/seller/customer-ledger", feature: "RECEIVABLE_MANAGEMENT"
    },
    {
        label: "Distributor Payments",
        icon: "₹",
        path: "/seller/distributor-payments", feature: "DISTRIBUTOR_MANAGEMENT"
    },
    {
        label: "Credit Notes",
        icon: "CN",
        path: "/seller/distributor-credit-notes", feature: "DISTRIBUTOR_MANAGEMENT"
    },
    {
        label: "Stock Transfers",
        icon: "ST",
        path: "/seller/stock-transfers", feature: "INVENTORY_MANAGEMENT"
    },
    {
        label: "Accounting",
        icon: "AC",
        path: "/seller/accounting", feature: "CA_REPORTS"
    },
    {
        label: "CA Reports",
        icon: "CA",
        path: "/seller/ca-reports", feature: "CA_REPORTS"
    },
    {
        label: "Messages",
        icon: "💬",
        path: "/seller/messages"
    },
    {
        label: "Branches",
        icon: "🏪",
        path: "/seller/branches",
        feature: "STAFF_MANAGEMENT"
    },
    {
        label: "Finance / EMI",
        icon: "₹",
        path: "/seller/finance",
        feature: "OFFLINE_POS"
    },
    {
        label: "Subscription",
        icon: "★",
        path: "/seller/subscription"
    },
    {
        label: "Settings",
        icon: "⚙",
        path: "/seller/settings"
    }
];

const SellerSidebar = ({ open = false, onClose = () => {} }) => {

    const { logout } = useAuth();
    const [entitlements, setEntitlements] = useState(null);

    useEffect(() => {
        let active = true;
        getSellerEntitlements().then(value => { if (active) setEntitlements(value); }).catch(() => { if (active) setEntitlements(null); });
        return () => { active = false; };
    }, []);

    const handleLogout = () => {

        logout();

        window.location.href = "/login";
    };

    return (
        <>
            {open && (
                <button
                    type="button"
                    className="seller-sidebar-overlay"
                    aria-label="Close seller menu"
                    onClick={onClose}
                />
            )}

            <aside
                className={
                    `seller-sidebar ${
                        open ? "seller-sidebar-open" : ""
                    }`
                }
            >

                {/* BRAND */}

                <div className="seller-sidebar-brand">

                    <div className="seller-logo">
                        S
                    </div>

                    <div>
                        <h2>
                            Shiv<span>Hub</span>
                        </h2>

                        <small>
                            Seller Panel
                        </small>
                    </div>

                    <button
                        type="button"
                        className="seller-mobile-close"
                        onClick={onClose}
                        aria-label="Close menu"
                    >
                        ×
                    </button>

                </div>


                {/* NAVIGATION */}

                <nav className="seller-sidebar-nav">

                    {menuItems.map((item) => {
                        const locked = Boolean(item.feature && entitlements && entitlements.features?.[item.feature] === false);
                        return (

                        <NavLink
                            key={item.path}
                            to={locked ? "/seller/subscription" : item.path}
                            end={
                                item.path ===
                                "/seller/dashboard"
                            }
                            className={({ isActive }) =>
                                `seller-nav-link ${locked ? "locked" : ""} ${
                                    isActive
                                        ? "active"
                                        : ""
                                }`
                            }
                            onClick={onClose}
                            title={locked ? "Subscription required — renew to unlock" : undefined}
                        >

                            <span className="seller-nav-icon">
                                {item.icon}
                            </span>

                            <span>
                                {item.label}
                            </span>
                            {locked && <span className="seller-nav-lock" aria-label="Subscription required">🔒</span>}

                        </NavLink>

                    );
                    })}

                </nav>


                {/* LOGOUT */}

                <div className="seller-sidebar-bottom">

                    <WorkspacePreferences scope="seller" compact />

                    <button
                        type="button"
                        className="seller-logout-link"
                        onClick={handleLogout}
                    >

                        <span>
                            ↪
                        </span>

                        Logout

                    </button>

                </div>

            </aside>
        </>
    );
};

export default SellerSidebar;
