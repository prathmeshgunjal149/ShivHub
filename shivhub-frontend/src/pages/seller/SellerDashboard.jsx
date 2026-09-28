import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import { API_BASE_URL } from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./SellerDashboard.css";

const money = (amount) => `₹${Number(amount || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;

const dueDateLabel = (value) => value ? new Date(`${value}T00:00:00`).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }) : "Today";

const SellerDashboard = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [sidebarOpen, setSidebarOpen] = useState(false);

    /* One secure dashboard API supplies all business figures. */
    const loadDashboard = useCallback(async () => {
        try {
            setLoading(true); setError("");
            const token = localStorage.getItem("shivhub_token");
            if (!token) throw new Error("Seller session not found. Please login again.");
            const response = await axios.get(`${API_BASE_URL}/api/seller/dashboard`, { headers: { Authorization: `Bearer ${token}` } });
            setDashboard(response.data);
        } catch (err) {
            setError(err?.response?.data?.message || err.message || "Unable to load seller dashboard data.");
        } finally { setLoading(false); }
    }, []);

    useEffect(() => { void loadDashboard(); }, [loadDashboard]);
    const summary = dashboard?.summary || {};
    const products = dashboard?.recentProducts || [];
    const alerts = dashboard?.lowStockProducts || [];
    const todayDues = dashboard?.todayDues || { customerReceivables: [], customerReceivableTotal: 0, distributorPayables: [], distributorPayableTotal: 0 };
    const stats = [
        ["₹", "Today's sales", money(summary.todayTotalSales), "Online + POS", "emerald"],
        ["◫", "Today's orders", (summary.todayOnlineOrders || 0) + (summary.todayOfflineBills || 0), `${summary.todayOnlineOrders || 0} online · ${summary.todayOfflineBills || 0} POS`, "blue"],
        ["▣", "Active products", summary.activeProducts || 0, `${summary.totalProducts || 0} total products`, "violet"],
        ["!", "Low stock", summary.lowStockProducts || 0, "Needs your attention", "orange"]
    ];
    /* Keep dashboard access identical to the seller sidebar navigation. */
    const quickLinks = [
        ["▣", "Products", "/seller/products"],
        ["▤", "Inventory", "/seller/inventory"],
        ["◫", "Orders", "/seller/orders"],
        ["▤", "Billing", "/seller/billing"],
        ["♙", "Customers", "/seller/customers"],
        ["▣", "Distributors", "/seller/distributors"],
        ["♙", "Staff", "/seller/staff"],
        ["✦", "Offers", "/seller/offers"],
        ["₹", "GST", "/seller/gst"],
        ["↗", "Reports", "/seller/reports"],
        ["₹", "Expenses", "/seller/expenses"],
        ["◌", "Messages", "/seller/messages"],
        ["▥", "Branches", "/seller/branches"],
        ["⚙", "Settings", "/seller/settings"]
    ];

    return <div className="seller-app">
        <SellerSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <div className="seller-main">
            <header className="seller-topbar">
                <button type="button" className="seller-menu-button" onClick={() => setSidebarOpen(true)} aria-label="Open menu">☰</button>
                <div className="seller-topbar-title"><span>Business overview</span><small>Live shop performance</small></div>
                <div className="seller-profile"><div className="seller-avatar">{user?.name?.charAt(0)?.toUpperCase() || "S"}</div><div className="seller-user-info"><small>Signed in as</small><strong>{user?.name || "Seller"}</strong></div></div>
            </header>
            <main className="seller-dashboard-content">
                <section className="seller-hero"><div><span className="seller-eyebrow"><i /> SELLER WORKSPACE</span><h1>Good to see you, {user?.name?.split(" ")[0] || "Seller"}.</h1><p>Keep your sales, stock and shop operations moving from one place.</p></div><div className="seller-hero-actions"><button type="button" className="seller-ghost-button" onClick={loadDashboard}>↻ Refresh</button><button type="button" className="seller-ghost-button" onClick={() => navigate("/seller/purchases")}>▣ Distributor</button><button type="button" className="seller-primary-button" onClick={() => navigate("/seller/add-product")}>＋ Add product</button></div></section>
                {error && <div className="seller-error"><span>!</span><div><strong>Dashboard unavailable</strong><p>{error}</p></div><button type="button" onClick={loadDashboard}>Try again</button></div>}
                <section className="seller-stat-grid">{stats.map(([icon, label, value, note, tone]) => <article className={`seller-stat-card seller-stat-${tone}`} key={label}><div className="seller-stat-icon">{icon}</div><div><small>{label}</small><strong>{loading ? "—" : value}</strong><p>{note}</p></div></article>)}</section>
                <section className="seller-due-grid" aria-label="Payments due today and overdue">
                    <article className="seller-due-card seller-due-card-receivable">
                        <div className="seller-due-header"><div><span className="seller-label">COLLECTIONS DUE</span><h2>{loading ? "—" : money(todayDues.customerReceivableTotal)}</h2><p>Customer payments due today or overdue</p></div><button type="button" onClick={() => navigate("/seller/payments")}>Open ledger →</button></div>
                        {loading ? <div className="seller-due-empty">Checking customer receivables…</div> : todayDues.customerReceivables.length ? <div className="seller-due-list">{todayDues.customerReceivables.slice(0, 4).map((due, index) => <button type="button" key={`${due.reference || due.partyName}-${index}`} onClick={() => navigate("/seller/payments")}><span className="seller-due-avatar">{due.partyName?.charAt(0)?.toUpperCase() || "C"}</span><span className="seller-due-person"><strong>{due.partyName || "Customer"}</strong><small>{due.mobile || "No mobile"}{due.reference ? ` · ${due.reference}` : ""}</small></span><span className="seller-due-amount"><b>{money(due.remainingAmount)}</b><small className={due.overdueDays > 0 ? "seller-due-overdue" : ""}>{due.overdueDays > 0 ? `${due.overdueDays}d overdue` : `Due ${dueDateLabel(due.dueDate)}`}</small></span></button>)}</div> : <div className="seller-due-empty"><span>✓</span> No customer collection is due today.</div>}
                    </article>
                    <article className="seller-due-card seller-due-card-payable">
                        <div className="seller-due-header"><div><span className="seller-label">PAYABLES DUE</span><h2>{loading ? "—" : money(todayDues.distributorPayableTotal)}</h2><p>Distributor payments due today or overdue</p></div><button type="button" onClick={() => navigate("/seller/distributor-payments")}>Pay bills →</button></div>
                        {loading ? <div className="seller-due-empty">Checking distributor payables…</div> : todayDues.distributorPayables.length ? <div className="seller-due-list">{todayDues.distributorPayables.slice(0, 4).map((due, index) => <button type="button" key={`${due.reference || due.partyName}-${index}`} onClick={() => navigate("/seller/distributor-payments")}><span className="seller-due-avatar seller-due-avatar-payable">{due.partyName?.charAt(0)?.toUpperCase() || "D"}</span><span className="seller-due-person"><strong>{due.partyName || "Distributor"}</strong><small>{due.mobile || "No mobile"}{due.reference ? ` · ${due.reference}` : ""}</small></span><span className="seller-due-amount"><b>{money(due.remainingAmount)}</b><small className={due.overdueDays > 0 ? "seller-due-overdue" : ""}>{due.overdueDays > 0 ? `${due.overdueDays}d overdue` : `Due ${dueDateLabel(due.dueDate)}`}</small></span></button>)}</div> : <div className="seller-due-empty"><span>✓</span> No distributor payment is due today.</div>}
                    </article>
                </section>
                <section className="seller-dashboard-grid">
                    <section className="seller-section seller-products-section"><div className="seller-section-header"><div><span className="seller-label">CATALOGUE</span><h2>Recent products</h2></div><button type="button" className="seller-text-button" onClick={() => navigate("/seller/add-product")}>Add product →</button></div>{loading ? <div className="seller-loading"><span className="seller-spinner" /> Loading your products…</div> : products.length === 0 ? <div className="seller-empty-state"><div className="seller-empty-icon">▣</div><h3>Your catalogue is empty</h3><p>Add your first product and send it for approval.</p><button type="button" className="seller-primary-button" onClick={() => navigate("/seller/add-product")}>Add product</button></div> : <div className="seller-product-grid">{products.map((product) => <article className="seller-product-card" key={product.id}><div className="seller-product-image">{product.imageUrl ? <img src={product.imageUrl.startsWith("http") ? product.imageUrl : `${API_BASE_URL}/${product.imageUrl.replace(/^\/+/, "")}`} alt={product.name} /> : <span>▣</span>}<small className={`seller-status ${product.approvalStatus?.toLowerCase()}`}>{product.approvalStatus || "PENDING"}</small></div><div className="seller-product-info"><small>{product.category || "Product"}</small><h3>{product.name}</h3><div className="seller-product-bottom"><strong>{money(product.price)}</strong><span>Stock {product.stock ?? 0}</span></div></div></article>)}</div>}</section>
                    <aside className="seller-side-stack"><section className="seller-section seller-sales-card"><span className="seller-label">TODAY'S BREAKDOWN</span><h2>{money(summary.todayTotalSales)}</h2><p>Combined sales across all channels</p><div className="seller-sales-split"><div><span>Online</span><strong>{money(summary.todayOnlineSales)}</strong></div><div><span>In-store</span><strong>{money(summary.todayOfflineSales)}</strong></div></div></section><section className="seller-section seller-alert-card"><div className="seller-section-header"><div><span className="seller-label">INVENTORY ALERTS</span><h2>Low stock</h2></div><span className="seller-alert-count">{summary.lowStockProducts || 0}</span></div>{loading ? <div className="seller-mini-loading">Checking stock…</div> : alerts.length ? <div className="seller-alert-list">{alerts.slice(0, 4).map((product) => <button type="button" key={product.id} onClick={() => navigate("/seller/inventory")}><span className="seller-product-dot">!</span><span><strong>{product.name}</strong><small>{product.stock ?? 0} units remaining</small></span><b>›</b></button>)}</div> : <div className="seller-all-good">✓ Stock levels look good</div>}</section></aside>
                </section>
                <section className="seller-section seller-quick-section"><div className="seller-section-header"><div><span className="seller-label">QUICK ACCESS</span><h2>Run your business</h2></div></div><div className="seller-quick-grid">{quickLinks.map(([icon, label, path]) => <button type="button" key={label} onClick={() => navigate(path)}><span>{icon}</span><strong>{label}</strong><small>Open module</small></button>)}</div></section>
            </main>
        </div>
    </div>;
};
export default SellerDashboard;
