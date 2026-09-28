import { useCallback, useEffect, useState } from "react";
import { exportAdminReferrals, listAdminReferrals } from "../../services/adminReferralService";
import "./AdminReferrals.css";

const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));
const date = value => value ? new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—";
const empty = { content: [], summary: {}, topReferrers: [], totalElements: 0, totalPages: 0, page: 0, size: 20 };

export default function AdminReferrals() {
    const [result, setResult] = useState(empty);
    const [filters, setFilters] = useState({ search: "", status: "", from: "", to: "", page: 0, size: 20 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [exporting, setExporting] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError("");
        try { setResult(await listAdminReferrals(filters)); }
        catch (requestError) { setError(requestError.response?.data?.message || "Referral data could not be loaded."); }
        finally { setLoading(false); }
    }, [filters]);

    useEffect(() => { const timer = window.setTimeout(load, 260); return () => window.clearTimeout(timer); }, [load]);
    const update = (name, value) => setFilters(previous => ({ ...previous, [name]: value, page: name === "page" ? value : 0 }));
    const reset = () => setFilters({ search: "", status: "", from: "", to: "", page: 0, size: 20 });
    const exportCsv = async () => { setExporting(true); try { await exportAdminReferrals(filters); } catch { setError("Referral CSV could not be exported."); } finally { setExporting(false); } };
    const statusClass = status => String(status || "PENDING").toLowerCase();

    return <main className="admin-referrals-page">
        <header className="admin-referrals-heading">
            <div><p>Customer growth</p><h1>Referral Management</h1><span>Trace referral relationships, rewards and qualifying order value from existing ShivHub records.</span></div>
            <button type="button" onClick={exportCsv} disabled={exporting || loading}>{exporting ? "Exporting…" : "Export CSV"}</button>
        </header>

        <section className="referral-summary" aria-label="Referral summary">
            <article><span>Total referrals</span><strong>{result.summary?.total || 0}</strong></article>
            <article><span>Pending</span><strong>{result.summary?.pending || 0}</strong></article>
            <article><span>Rewarded</span><strong>{result.summary?.rewarded || 0}</strong></article>
            <article><span>Registered</span><strong>{result.summary?.registered || 0}</strong></article>
        </section>

        <section className="referral-filters" aria-label="Referral filters">
            <input value={filters.search} onChange={event => update("search", event.target.value)} placeholder="Search customer, mobile, email or referral code" />
            <select value={filters.status} onChange={event => update("status", event.target.value)}><option value="">All statuses</option><option value="PENDING">Pending</option><option value="REGISTERED">Registered</option><option value="REWARDED">Rewarded</option><option value="EXPIRED">Expired</option><option value="CANCELLED">Cancelled</option></select>
            <label>From<input type="date" value={filters.from} onChange={event => update("from", event.target.value)} /></label>
            <label>To<input type="date" value={filters.to} onChange={event => update("to", event.target.value)} /></label>
            <button type="button" onClick={reset}>Clear filters</button>
        </section>

        {error && <p className="referral-error" role="alert">{error}<button type="button" onClick={load}>Retry</button></p>}
        <section className="referral-layout">
            <article className="referral-table-card">
                <div className="referral-card-head"><div><p>Referral relationships</p><h2>{loading ? "Loading…" : `${result.totalElements} record${result.totalElements === 1 ? "" : "s"}`}</h2></div><span>Admin-only data</span></div>
                <div className="referral-table-wrap"><table><thead><tr><th>Referrer</th><th>Referred customer</th><th>Code & date</th><th>Status</th><th>Reward</th><th>Order value</th></tr></thead><tbody>
                    {loading && <tr><td colSpan="6" className="referral-empty">Loading referral records…</td></tr>}
                    {!loading && !result.content.length && <tr><td colSpan="6" className="referral-empty">No referral records match these filters.</td></tr>}
                    {!loading && result.content.map(row => <tr key={row.id}>
                        <td><strong>{row.referrer?.name || "Unknown"}</strong><small>{row.referrer?.email || row.referrer?.mobile || "No contact recorded"}</small></td>
                        <td><strong>{row.referredCustomer?.name || "Unknown"}</strong><small>{row.referredCustomer?.email || row.referredCustomer?.mobile || "No contact recorded"}</small></td>
                        <td><strong>{row.referralCode || "Not recorded"}</strong><small>{date(row.referralDate)}</small></td>
                        <td><span className={`referral-status ${statusClass(row.status)}`}>{row.status}</span></td>
                        <td><strong>{row.rewardCoupon?.code || "No reward"}</strong><small>{row.rewardCoupon?.issued ? (row.rewardCoupon.used ? "Used" : "Issued, unused") : "Not issued"}</small></td>
                        <td><strong>{row.qualifyingOrderRevenue != null ? money(row.qualifyingOrderRevenue) : "—"}</strong><small>{row.qualifyingOrderNumber || "No qualifying order"}</small></td>
                    </tr>)}
                </tbody></table></div>
                <footer className="referral-pagination"><span>Page {result.page + 1} of {Math.max(1, result.totalPages)}</span><div><button type="button" disabled={loading || result.page <= 0} onClick={() => update("page", result.page - 1)}>Previous</button><button type="button" disabled={loading || result.page + 1 >= result.totalPages} onClick={() => update("page", result.page + 1)}>Next</button></div></footer>
            </article>
            <aside className="top-referrers"><div><p>Growth leaders</p><h2>Top referrers</h2></div>{loading && <p>Loading…</p>}{!loading && !result.topReferrers?.length && <p>No referral activity yet.</p>}{result.topReferrers?.map((customer, index) => <article key={customer.customerId}><b>{index + 1}</b><div><strong>{customer.name}</strong><small>{customer.email || customer.mobile || "No contact"}</small></div><span><strong>{customer.referralCount}</strong><small>referrals</small></span></article>)}</aside>
        </section>
    </main>;
}
