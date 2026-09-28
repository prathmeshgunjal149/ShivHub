import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listAdminSellers } from "../../services/adminManagementService";
import SearchAutocomplete from "../../components/common/SearchAutocomplete/SearchAutocomplete";
import { getAdminSearchSuggestions } from "../../services/searchSuggestionService";
import "./AdminGrowth.css";

const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));

export default function AdminSellerManagement() {
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0, page: 0 });
  const [filters, setFilters] = useState({ search: "", approvalStatus: "", city: "", registeredFrom: "", registeredTo: "", active: "", gstVerified: "", page: 0, size: 10 });
  const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const load = useCallback(async next => { const params = { ...filters, ...next }; Object.keys(params).forEach(key => { if (params[key] === "" || params[key] == null) delete params[key]; }); setLoading(true); try { setResult(await listAdminSellers(params)); setError(""); } catch { setError("Seller data could not be loaded. Please sign in as an administrator."); } finally { setLoading(false); } }, [filters]);
  useEffect(() => { load(); }, [load]);
  const set = (key, value) => { const next = { ...filters, [key]: value, page: 0 }; setFilters(next); load(next); };
  const cards = [
    ["Registered sellers", result.totalElements],
    ["Active on this page", result.content.filter(s => s.active).length],
    ["Page turnover", money(result.content.reduce((sum, seller) => sum + Number(seller.totalRevenue || 0), 0))],
    ["Pending review", result.content.filter(s => s.approvalStatus === "PENDING").length]
  ];
  return <main className="growth-page">
    <header className="growth-heading"><div><p className="eyebrow">Admin control centre</p><h1>Seller Management</h1><p>Live performance, inventory, receivables and customer relationships by seller.</p></div><Link className="outline-button" to="/admin/dashboard">Dashboard</Link></header>
    <section className="metric-grid">{cards.map(([label, value]) => <article key={label}><span>{label}</span><strong>{value}</strong></article>)}</section>
    <section className="growth-card filter-bar"><SearchAutocomplete inputProps={{ "aria-label": "Search sellers" }} value={filters.search} onChange={value => set("search", value)} onSelect={suggestion => set("search", suggestion.label || "")} onEnterWithoutSelection={() => {}} fetchSuggestions={query => getAdminSearchSuggestions("SELLER", query)} placeholder="Search owner, shop, email, mobile or GSTIN" /><select value={filters.approvalStatus} onChange={event => set("approvalStatus", event.target.value)}><option value="">All approval statuses</option><option>PENDING</option><option>APPROVED</option><option>REJECTED</option><option>SUSPENDED</option></select><input value={filters.city} onChange={event => set("city", event.target.value)} placeholder="City" /><label>Registered from<input type="date" value={filters.registeredFrom} onChange={event => set("registeredFrom", event.target.value)} /></label><label>Registered to<input type="date" value={filters.registeredTo} onChange={event => set("registeredTo", event.target.value)} /></label><select value={filters.active} onChange={event => set("active", event.target.value)}><option value="">All shop states</option><option value="true">Active</option><option value="false">Inactive</option></select><select value={filters.gstVerified} onChange={event => set("gstVerified", event.target.value)}><option value="">GST status</option><option value="true">GST verified</option><option value="false">GST not verified</option></select></section>
    {error && <p className="growth-error">{error}</p>}
    <section className="growth-card table-scroll"><table><thead><tr><th>Seller / shop</th><th>Contact</th><th>Status</th><th>Products & stock</th><th>Sales</th><th>Customers</th><th /></tr></thead><tbody>{result.content.map(seller => <tr key={seller.id}><td className="person-cell">{seller.shopLogoUrl ? <img src={seller.shopLogoUrl} alt="" /> : <b>{seller.ownerName?.slice(0, 1)}</b>}<div><strong>{seller.ownerName}</strong><small>{seller.shopName || "Shop not configured"} · {seller.city || "City not set"}</small></div></td><td>{seller.email}<small>{seller.mobile || "No mobile"}</small></td><td><span className={`pill ${seller.approvalStatus?.toLowerCase()}`}>{seller.approvalStatus}</span><small>{seller.active ? "Shop active" : "Shop inactive"}</small></td><td><strong>{seller.totalProducts}</strong><small>{seller.availableStockQuantity} units · {money(seller.stockValue)}</small></td><td><strong>{money(seller.totalRevenue)}</strong><small>Online {money(seller.onlineSales)} · POS {money(seller.offlineSales)}</small></td><td><strong>{seller.totalCustomers}</strong><small>Receivable {money(seller.customerReceivable)}</small></td><td><Link className="text-button" to={`/admin/sellers/${seller.id}`}>View profile</Link></td></tr>)}</tbody></table>{loading && <p className="empty-state">Loading live seller data…</p>}{!loading && !result.content.length && <p className="empty-state">No sellers match these filters.</p>}</section>
    <nav className="pagination"><button disabled={result.page <= 0 || loading} onClick={() => { const next = { ...filters, page: filters.page - 1 }; setFilters(next); load(next); }}>Previous</button><span>Page {result.page + 1} of {Math.max(result.totalPages, 1)}</span><button disabled={result.page + 1 >= result.totalPages || loading} onClick={() => { const next = { ...filters, page: filters.page + 1 }; setFilters(next); load(next); }}>Next</button></nav>
  </main>;
}
