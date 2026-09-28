import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "./Reports.css";

const today = () => new Date().toISOString().slice(0, 10);
const monthStart = () => {
    const date = new Date();
    date.setDate(1);
    return date.toISOString().slice(0, 10);
};

const money = value => Number(value || 0).toLocaleString("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
});

const MetricCard = ({ title, value, detail, tone }) => (
    <article className={`report-card ${tone}`}>
        <span>{title}</span>
        <strong>{value ?? 0}</strong>
        <small>{detail}</small>
    </article>
);

export default function Reports() {
    const [report, setReport] = useState(null);
    const [sellerReport, setSellerReport] = useState(null);
    const [startDate, setStartDate] = useState(monthStart());
    const [endDate, setEndDate] = useState(today());
    const [search, setSearch] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);

    const load = useCallback(async () => {
        try {
            setLoading(true);
            setError("");
            const [summaryResponse, sellerResponse] = await Promise.all([
                api.get("/api/admin/reports/summary"),
                api.get("/api/admin/reports/seller-performance", {
                    params: { startDate, endDate }
                })
            ]);
            setReport(summaryResponse.data);
            setSellerReport(sellerResponse.data);
        } catch (e) {
            setError(e.response?.data?.message || "Could not load live reports.");
        } finally {
            setLoading(false);
        }
    }, [startDate, endDate]);

    useEffect(() => {
        const requestTimer = window.setTimeout(() => void load(), 0);
        return () => window.clearTimeout(requestTimer);
    }, [load]);

    const visibleSellers = useMemo(() => {
        const term = search.trim().toLowerCase();
        const rows = sellerReport?.sellers || [];
        if (!term) return rows;
        return rows.filter(row => [
            row.sellerName,
            row.shopName,
            row.email,
            row.mobile,
            row.gstin
        ].some(value => String(value || "").toLowerCase().includes(term)));
    }, [sellerReport, search]);

    const exportCsv = () => {
        if (!sellerReport) return;
        const headers = [
            "Seller", "Shop", "Email", "Mobile", "GSTIN", "Customers", "Online Customers",
            "Offline Customers", "Walk-in Customers", "Total Sales", "Online Sales", "Offline Sales",
            "Bills/Orders", "Average Order Value", "GST Collected", "Purchase Amount", "Expenses",
            "Pending Receivables", "Gross Profit Estimate"
        ];
        const rows = visibleSellers.map(row => [
            row.sellerName, row.shopName, row.email, row.mobile, row.gstin, row.totalCustomers,
            row.onlineCustomers, row.offlineCustomers, row.walkInCustomers, row.totalSales,
            row.onlineSales, row.offlineSales, row.totalBillsOrders, row.averageOrderValue,
            row.gstCollected, row.purchaseAmount, row.expenses, row.pendingReceivables,
            row.grossProfitEstimate
        ]);
        const csv = [headers, ...rows]
            .map(row => row.map(cell => `"${String(cell ?? "").replace(/"/g, '""')}"`).join(","))
            .join("\n");
        const blob = new Blob([csv], { type: "text/csv" });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = `shivhub-seller-performance-${startDate}-to-${endDate}.csv`;
        anchor.click();
        URL.revokeObjectURL(url);
    };

    return (
        <main className="reports-page">
            <header>
                <div>
                    <p className="reports-eyebrow">Business intelligence</p>
                    <h1>Reports</h1>
                    <p>Live operational metrics from ShivHub.</p>
                </div>
                <div className="report-actions">
                    <Link to="/admin/dashboard">Dashboard</Link>
                    <button onClick={load}>Refresh</button>
                    <button className="export" onClick={exportCsv} disabled={!sellerReport}>Export Seller CSV</button>
                </div>
            </header>

            {error && <p className="report-error">{error}</p>}

            {loading ? (
                <p className="reports-loading">Loading live report...</p>
            ) : report && (
                <>
                    <section className="report-grid">
                        <MetricCard title="Customers" value={report.customers.total} detail={`${report.customers.active} active · ${report.customers.blocked} blocked`} tone="blue" />
                        <MetricCard title="Sellers" value={report.sellers.total} detail={`${report.sellers.approved} approved · ${report.sellers.pending} pending`} tone="purple" />
                        <MetricCard title="Products" value={report.products.total} detail={`${report.products.approved} approved · ${report.products.pending} pending`} tone="green" />
                        <MetricCard title="Marketing" value={report.marketing.total} detail={`${report.marketing.active} active campaigns`} tone="orange" />
                    </section>

                    <section className="report-table">
                        <h2>Seller Performance</h2>
                        <div className="report-filters">
                            <label>From<input type="date" value={startDate} onChange={event => setStartDate(event.target.value)} /></label>
                            <label>To<input type="date" value={endDate} onChange={event => setEndDate(event.target.value)} /></label>
                            <label>Search<input value={search} onChange={event => setSearch(event.target.value)} placeholder="Seller, shop, email, GSTIN..." /></label>
                        </div>
                        <div className="seller-report-summary">
                            <article><span>Total Sales</span><strong>{money(sellerReport?.summary?.totalSales)}</strong></article>
                            <article><span>GST Collected</span><strong>{money(sellerReport?.summary?.gstCollected)}</strong></article>
                            <article><span>Pending Receivables</span><strong>{money(sellerReport?.summary?.pendingReceivables)}</strong></article>
                            <article><span>Expenses</span><strong>{money(sellerReport?.summary?.expenses)}</strong></article>
                        </div>
                        <div className="admin-report-table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Seller / Shop</th>
                                        <th>Customers</th>
                                        <th>Sales</th>
                                        <th>Bills</th>
                                        <th>Avg Order</th>
                                        <th>GST</th>
                                        <th>Purchases</th>
                                        <th>Expenses</th>
                                        <th>Receivables</th>
                                        <th>Gross Profit Est.</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {!visibleSellers.length ? (
                                        <tr><td colSpan="10">No seller data for this period.</td></tr>
                                    ) : visibleSellers.map(row => (
                                        <tr key={row.sellerId}>
                                            <td><b>{row.shopName || row.sellerName}</b><small>{row.email} · {row.gstin || "No GSTIN"}</small></td>
                                            <td>{row.totalCustomers}<small>{row.onlineCustomers} online · {row.offlineCustomers} offline · {row.walkInCustomers} walk-in</small></td>
                                            <td>{money(row.totalSales)}<small>{money(row.onlineSales)} online · {money(row.offlineSales)} offline</small></td>
                                            <td>{row.totalBillsOrders}</td>
                                            <td>{money(row.averageOrderValue)}</td>
                                            <td>{money(row.gstCollected)}</td>
                                            <td>{money(row.purchaseAmount)}</td>
                                            <td>{money(row.expenses)}</td>
                                            <td>{money(row.pendingReceivables)}</td>
                                            <td><b>{money(row.grossProfitEstimate)}</b><small>Sales - purchases</small></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <small>Generated {new Date(sellerReport?.generatedAt || report.generatedAt).toLocaleString("en-IN")}</small>
                    </section>
                </>
            )}
        </main>
    );
}
