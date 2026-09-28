import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listAdminCustomers } from "../../services/adminManagementService";
import "./AdminGrowth.css";

const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));
const date = value => value ? new Date(value).toLocaleDateString("en-IN", { dateStyle: "medium" }) : "—";

export default function AdminCustomers() {
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0, page: 0 });
  const [filters, setFilters] = useState({ search: "", type: "", city: "", from: "", to: "", minimumAmount: "", maximumAmount: "", page: 0, size: 12 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async next => {
    const params = { ...filters, ...next };
    Object.keys(params).forEach(key => { if (params[key] === "" || params[key] == null) delete params[key]; });
    setLoading(true);
    try {
      setResult(await listAdminCustomers(params));
      setError("");
    } catch {
      setError("Customer data could not be loaded. Please sign in as an administrator.");
    } finally {
      setLoading(false);
    }
  }, [filters]);
  useEffect(() => { load(); }, [load]);
  const set = (key, value) => {
    const next = { ...filters, [key]: value, page: 0 };
    setFilters(next);
    load(next);
  };

  return <main className="growth-page">
    <header className="growth-heading"><div><p className="eyebrow">Admin control centre</p><h1>All Customers</h1><p>Combined online and seller/POS customer profiles. Seller access stays restricted to seller-owned relationships.</p></div><Link className="outline-button" to="/admin/campaigns">Create campaign</Link></header>
    <section className="metric-grid"><article><span>Customer profiles</span><strong>{result.totalElements}</strong></article><article><span>Online on this page</span><strong>{result.content.filter(customer => customer.customerType === "ONLINE" || customer.customerType === "BOTH").length}</strong></article><article><span>Seller/POS on this page</span><strong>{result.content.filter(customer => customer.customerType === "SELLER" || customer.customerType === "BOTH").length}</strong></article><article><span>Page purchase value</span><strong>{money(result.content.reduce((sum, customer) => sum + Number(customer.totalPurchaseAmount || 0), 0))}</strong></article></section>
    <section className="growth-card filter-bar"><input value={filters.search} onChange={event => set("search", event.target.value)} placeholder="Search name, mobile or email" /><select value={filters.type} onChange={event => set("type", event.target.value)}><option value="">All customer types</option><option value="ONLINE">Online only</option><option value="SELLER">Seller/POS only</option><option value="BOTH">Both</option></select><input value={filters.city} onChange={event => set("city", event.target.value)} placeholder="City / address" /><label>Purchased from<input type="date" value={filters.from} onChange={event => set("from", event.target.value)} /></label><label>Purchased to<input type="date" value={filters.to} onChange={event => set("to", event.target.value)} /></label><input type="number" min="0" value={filters.minimumAmount} onChange={event => set("minimumAmount", event.target.value)} placeholder="Min. purchase ₹" /><input type="number" min="0" value={filters.maximumAmount} onChange={event => set("maximumAmount", event.target.value)} placeholder="Max. purchase ₹" /></section>
    {error && <p className="growth-error">{error}</p>}
    <section className="growth-card table-scroll"><table><thead><tr><th>Customer</th><th>Type</th><th>Associated sellers</th><th>Orders / bills</th><th>Purchase amount</th><th>Pending</th><th>Last purchase</th><th /></tr></thead><tbody>{result.content.map(customer => <tr key={customer.id}><td><strong>{customer.name}</strong><small>{customer.mobile || customer.email || "No contact"}</small></td><td><span className="pill neutral">{customer.customerType}</span></td><td>{customer.associatedSellers?.join(", ") || "No seller purchase"}</td><td>{customer.totalOrdersAndBills}</td><td>{money(customer.totalPurchaseAmount)}</td><td>{money(customer.pendingReceivable)}</td><td>{date(customer.lastPurchaseDate)}</td><td><Link className="text-button" to={`/admin/customers/${customer.id}`}>Profile</Link></td></tr>)}</tbody></table>{loading && <p className="empty-state">Loading customer profiles…</p>}{!loading && !result.content.length && <p className="empty-state">No customers match these filters.</p>}</section>
    <nav className="pagination"><button disabled={result.page <= 0 || loading} onClick={() => { const next = { ...filters, page: filters.page - 1 }; setFilters(next); load(next); }}>Previous</button><span>Page {result.page + 1} of {Math.max(result.totalPages, 1)}</span><button disabled={result.page + 1 >= result.totalPages || loading} onClick={() => { const next = { ...filters, page: filters.page + 1 }; setFilters(next); load(next); }}>Next</button></nav>
  </main>;
}
