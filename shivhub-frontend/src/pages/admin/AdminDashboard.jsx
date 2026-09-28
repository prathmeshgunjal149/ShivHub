const adminModules = [
        { label: "Subcategory fields", to: "/admin/subcategory-fields", terms: "category attribute specification variant configuration" },
        { label: "Delivery rules", to: "/admin/delivery-rules", terms: "mobile delivery distance time eta" },
        { label: "Sellers", to: "/admin/sellers", terms: "seller shop owner approvals" },
        { label: "Customers", to: "/admin/customers", terms: "customer client profiles" },
        { label: "Products", to: "/admin/products", terms: "products catalogue inventory stock" },
        { label: "Orders", to: "/admin/orders", terms: "orders sales online" },
        { label: "Payments", to: "/admin/payments", terms: "payments receivables" },
        { label: "Campaigns", to: "/admin/campaigns", terms: "campaign offers festival messages" },
        { label: "Loyalty", to: "/admin/loyalty", terms: "loyalty credit points rewards" },
        { label: "Birthday", to: "/admin/birthday-settings", terms: "birthday automatic greeting email" },
        { label: "After-sales", to: "/admin/after-sales", terms: "warranty return replacement refund service repair policies" },
        { label: "Reports", to: "/admin/reports", terms: "reports analytics" },
        { label: "Referrals", to: "/admin/referrals", terms: "referral coupon rewards" },
        { label: "Settings", to: "/admin/settings", terms: "settings configuration" }
    ];
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import useAuth from "../../hooks/useAuth";
import api from "../../services/api";
import { listAdminCustomers, listAdminSellers } from "../../services/adminManagementService";
import WorkspacePreferences from "../../components/WorkspacePreferences";
import { fadeUp, noTransform } from "../../utils/animationVariants";
import "./AdminDashboard.css";

const emptyPage = { content: [], totalElements: 0, totalPages: 0, page: 0, size: 6 };
const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));
const number = value => new Intl.NumberFormat("en-IN").format(Number(value || 0));
const initials = value => String(value || "S").split(" ").filter(Boolean).slice(0, 2).map(word => word[0]).join("").toUpperCase();
const formattedDate = value => value ? new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "No activity yet";
const statusClass = value => String(value || "pending").toLowerCase();
const requestProblem = reason => {
    const status = reason?.response?.status;
    return status === 401 || status === 403
        ? "Your admin session has expired. Please sign in again."
        : "Some live data could not be loaded. Use Refresh to try again.";
};

function AdminDashboard() {
    const navigate = useNavigate();
    const reducedMotion = useReducedMotion();
    const { user, logout } = useAuth();
    const [summary, setSummary] = useState({ totalCustomers: 0, totalSellers: 0, pendingSellers: 0, totalProducts: 0, pendingProducts: 0, activeProducts: 0, lowStockProducts: 0, outOfStockProducts: 0, activeCoupons: 0, totalOrders: 0, totalPayments: 0, totalRevenue: 0 });
    const [sellers, setSellers] = useState(emptyPage);
    const [customers, setCustomers] = useState(emptyPage);
    const [customerCounts, setCustomerCounts] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [lastUpdated, setLastUpdated] = useState(null);
    const [adminSearch, setAdminSearch] = useState("");

    const loadDashboard = useCallback(async () => {
        setLoading(true);
        setError("");
        const results = await Promise.allSettled([
            api.get("/api/admin/dashboard"),
            listAdminSellers({ page: 0, size: 6 }),
            listAdminCustomers({ page: 0, size: 6 }),
            api.get("/api/admin/customers/counts")
        ]);
        const [summaryResult, sellerResult, customerResult, countResult] = results;
        const failures = results.filter(result => result.status === "rejected");
        if (summaryResult.status === "fulfilled") setSummary(previous => ({ ...previous, ...summaryResult.value.data }));
        if (sellerResult.status === "fulfilled") setSellers(sellerResult.value);
        if (customerResult.status === "fulfilled") setCustomers(customerResult.value);
        if (countResult.status === "fulfilled") setCustomerCounts(countResult.value.data);
        if (failures.length) setError(requestProblem(failures[0].reason));
        setLastUpdated(new Date());
        setLoading(false);
    }, []);

    useEffect(() => { loadDashboard(); }, [loadDashboard]);

    const pendingSellers = useMemo(() => sellers.content.filter(seller => seller.approvalStatus === "PENDING"), [sellers]);
    const visiblePending = pendingSellers.length || summary.pendingSellers;
    const totalCustomerProfiles = customerCounts ? customerCounts.totalUniqueCustomers : (customers.totalElements || summary.totalCustomers);
    const totalSellerProfiles = sellers.totalElements || summary.totalSellers;
    const quickActions = [
        { label: "Subcategory fields", detail: "Specifications and variant axes", value: "Configure", to: "/admin/subcategory-fields", tone: "indigo" },
        { label: "Delivery rules", detail: "Mobile distance and ETA", value: "Configure", to: "/admin/delivery-rules", tone: "cyan" },
        { label: "Seller management", detail: "Profiles, stock & sales", value: number(totalSellerProfiles), to: "/admin/sellers", tone: "indigo" },
        { label: "Customer directory", detail: "Online and shop customers", value: number(totalCustomerProfiles), to: "/admin/customers", tone: "cyan" },
        { label: "Campaign centre", detail: "Offers and festival messages", value: "Create", to: "/admin/campaigns", tone: "violet" },
        { label: "After-sales", detail: "Warranty, returns & repairs", value: "Manage", to: "/admin/after-sales", tone: "violet" },
        { label: "Product reviews", detail: "Catalogue approvals", value: number(summary.pendingProducts), to: "/admin/products", tone: "amber" }
    ];

    

    const matchingModules = useMemo(() => {
        const query = adminSearch.trim().toLowerCase();
        if (!query) return [];
        return adminModules.filter(module => `${module.label} ${module.terms}`.toLowerCase().includes(query)).slice(0, 5);
    }, [adminSearch]);

    const openFirstSearchResult = event => {
        event.preventDefault();
        if (matchingModules[0]) {
            navigate(matchingModules[0].to);
            setAdminSearch("");
        }
    };

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    return (
        <main className="admin-dashboard-v2">
            <header className="dashboard-topbar">
                <Link className="dashboard-brand" to="/admin/dashboard" aria-label="ShivHub admin home"><span>SH</span><strong>ShivHub <small>ADMIN</small></strong></Link>
                <form className="dashboard-global-search" onSubmit={openFirstSearchResult} role="search">
                    <span aria-hidden="true">⌕</span>
                    <input value={adminSearch} onChange={event => setAdminSearch(event.target.value)} placeholder="Search admin modules…" aria-label="Search admin modules" />
                    {adminSearch && <button type="button" onClick={() => setAdminSearch("")} aria-label="Clear search">×</button>}
                    {matchingModules.length > 0 && <div className="dashboard-search-results">{matchingModules.map(module => <button type="button" key={module.to} onClick={() => { navigate(module.to); setAdminSearch(""); }}><strong>{module.label}</strong><small>Open module</small></button>)}</div>}
                </form>
                <nav className="dashboard-nav" aria-label="Admin navigation"><Link className="active" to="/admin/dashboard">Overview</Link><Link to="/admin/sellers">Sellers</Link><Link to="/admin/approvals">Approvals</Link><Link to="/admin/customers">Customers</Link><Link to="/admin/products">Products</Link><Link to="/admin/products/add">Add Product</Link><Link to="/admin/categories">Categories</Link><Link to="/admin/distributors">Distributors</Link><Link to="/admin/orders">Orders</Link><Link to="/admin/invoices">Invoices</Link><Link to="/admin/payments">Payments</Link><Link to="/admin/reports">Reports</Link><Link to="/admin/after-sales">After-sales</Link><Link to="/admin/marketplace">Second-hand Market</Link><Link to="/admin/campaigns">Campaigns</Link><Link to="/admin/offers">Offers</Link><Link to="/admin/coupons">Coupons</Link><Link to="/admin/referrals">Referrals</Link><Link to="/admin/delivery-rules">Delivery ETA</Link><Link to="/admin/settings">Settings</Link></nav>
                <div className="dashboard-account"><WorkspacePreferences scope="admin" /><div className="account-copy"><strong>{user?.name || "Administrator"}</strong><span>Secure administrator session</span></div><button type="button" className="logout-link" onClick={handleLogout}>Sign out</button></div>
            </header>

            <section className="dashboard-shell">
                <section className="dashboard-hero"><div><p className="dashboard-eyebrow">Operations overview</p><h1>Make every marketplace decision with clarity.</h1><p>Monitor real seller performance, customer activity and pending work from one secure workspace.</p></div><div className="hero-actions"><span>{lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}` : "Loading live data"}</span><button type="button" onClick={loadDashboard} disabled={loading}>{loading ? "Refreshing…" : "Refresh data"}</button></div></section>

                {error && <section className="dashboard-alert" role="alert"><strong>Live data needs attention.</strong><span>{error}</span><button type="button" onClick={loadDashboard}>Try again</button></section>}

                <motion.section className="dashboard-kpis" aria-label="Marketplace key metrics" variants={reducedMotion ? noTransform : fadeUp} initial="hidden" animate="visible">
                    <article className="kpi-card revenue"><span className="kpi-mark">₹</span><p>Marketplace revenue</p><strong>{loading ? "—" : money(summary.totalRevenue)}</strong><small>Completed online orders</small></article>
                    <article className="kpi-card"><span className="kpi-mark">O</span><p>Active orders</p><strong>{loading ? "—" : number(summary.totalOrders)}</strong><small>{number(summary.totalPayments)} paid orders</small></article>
                    <article className="kpi-card"><span className="kpi-mark">S</span><p>Seller network</p><strong>{loading ? "—" : number(totalSellerProfiles)}</strong><small>{visiblePending} awaiting review</small></article>
                    <article className="kpi-card"><span className="kpi-mark">C</span><p>Customer profiles</p><strong>{loading ? "—" : number(totalCustomerProfiles)}</strong><small>Online and seller-linked customers</small></article>
                </motion.section>

                <section className="dashboard-workspace">
                    <article className="attention-panel"><div className="panel-heading"><div><p>Needs attention</p><h2>Seller approvals</h2></div><Link to="/admin/approvals">Review all queues</Link></div>{loading ? <div className="panel-placeholder">Loading approval queue…</div> : pendingSellers.length ? <div className="approval-list">{pendingSellers.slice(0, 3).map(seller => <Link className="approval-row" to={`/admin/sellers/${seller.id}`} key={seller.id}><span className="initial-avatar">{initials(seller.ownerName)}</span><span><strong>{seller.ownerName}</strong><small>{seller.shopName || seller.email}</small></span><em>Pending</em><b>Review</b></Link>)}</div> : <div className="clear-queue"><span>✓</span><div><strong>Approval queue is clear</strong><p>No seller registrations need a decision right now.</p></div></div>}</article>
                    <article className="insight-panel"><p>Business pulse</p><h2>{number(summary.totalProducts)} products are currently in the catalogue.</h2><div className="insight-meter"><span style={{ width: `${Math.min(100, Math.max(8, Number(summary.totalProducts) ? (Number(summary.pendingProducts) / Number(summary.totalProducts)) * 100 : 8))}%` }} /></div><div className="insight-footer"><span>{number(summary.pendingProducts)} product reviews pending</span><Link to="/admin/products">Open catalogue</Link></div></article>
                </section>

                <section className="catalog-health" aria-label="Catalogue and offer health">
                    <Link to="/admin/products"><span>LIVE CATALOGUE</span><strong>{loading ? "—" : number(summary.activeProducts)}</strong><small>Active approved products</small></Link>
                    <Link to="/admin/products"><span>LOW STOCK</span><strong>{loading ? "—" : number(summary.lowStockProducts)}</strong><small>Products at five units or below</small></Link>
                    <Link to="/admin/products"><span>OUT OF STOCK</span><strong>{loading ? "—" : number(summary.outOfStockProducts)}</strong><small>Products that need restocking</small></Link>
                    <Link to="/admin/coupons"><span>LIVE OFFERS</span><strong>{loading ? "—" : number(summary.activeCoupons)}</strong><small>Active coupons in their validity period</small></Link>
                </section>

                <section className="dashboard-section-heading"><div><p>Operational tools</p><h2>Manage the marketplace</h2></div><span>Every view uses your current ShivHub data.</span></section>
                <section className="quick-action-grid">{quickActions.map(action => <Link key={action.label} className={`quick-action ${action.tone}`} to={action.to}><span className="quick-action-icon">{action.label.charAt(0)}</span><div><strong>{action.label}</strong><small>{action.detail}</small></div><b>{action.value}</b><i>→</i></Link>)}</section>

                <section className="live-data-grid">
                    <article className="data-panel"><div className="panel-heading"><div><p>Seller performance</p><h2>Recently registered sellers</h2></div><Link to="/admin/sellers">View sellers</Link></div><div className="responsive-table"><table><thead><tr><th>Seller</th><th>Status</th><th>Revenue</th><th>Customers</th></tr></thead><tbody>{loading ? <tr><td colSpan="4" className="table-message">Loading seller data…</td></tr> : sellers.content.length ? sellers.content.map(seller => <tr key={seller.id}><td><Link className="table-person" to={`/admin/sellers/${seller.id}`}><span>{initials(seller.ownerName)}</span><div><strong>{seller.ownerName}</strong><small>{seller.shopName || seller.email}</small></div></Link></td><td><span className={`status-chip ${statusClass(seller.approvalStatus)}`}>{seller.approvalStatus || "PENDING"}</span></td><td>{money(seller.totalRevenue)}</td><td>{number(seller.totalCustomers)}</td></tr>) : <tr><td colSpan="4" className="table-message">No seller records found yet.</td></tr>}</tbody></table></div></article>
                    <article className="data-panel"><div className="panel-heading"><div><p>Customer activity</p><h2>Latest customer profiles</h2></div><Link to="/admin/customers">View customers</Link></div><div className="customer-list">{loading ? <p className="panel-placeholder">Loading customer data…</p> : customers.content.length ? customers.content.map(customer => <Link className="customer-row" to={`/admin/customers/${customer.id}`} key={customer.id}><span className="initial-avatar customer">{initials(customer.name)}</span><span><strong>{customer.name}</strong><small>{customer.customerType} · {customer.associatedSellers?.[0] || "No shop linked"}</small></span><span className="customer-value"><strong>{money(customer.totalPurchaseAmount)}</strong><small>{formattedDate(customer.lastPurchaseDate)}</small></span></Link>) : <p className="panel-placeholder">No customer records found yet.</p>}</div></article>
                </section>
            </section>
        </main>
    );
}

export default AdminDashboard;
