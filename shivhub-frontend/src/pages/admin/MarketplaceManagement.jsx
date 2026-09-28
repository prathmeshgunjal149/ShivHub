import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api, { API_BASE_URL } from "../../services/api";
import "./MarketplaceManagement.css";

const date = value => value ? new Date(value).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "—";
const money = value => Number(value || 0).toLocaleString("en-IN", { style: "currency", currency: "INR" });
const imageUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;

export default function MarketplaceManagement() {
  const [tab, setTab] = useState("LISTINGS");
  const [status, setStatus] = useState("PENDING_ADMIN_APPROVAL");
  const [rows, setRows] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState("");
  const [category, setCategory] = useState("ALL");
  const load = useCallback(async () => {
    setError("");
    try {
      const endpoint = tab === "LISTINGS" ? "/api/marketplace/admin/listings" : tab === "SELLERS" ? "/api/marketplace/admin/owners" : "/api/marketplace/admin/inquiries";
      const params = { page };
      if (tab === "LISTINGS" && status) params.status = status;
      if (tab === "INQUIRIES" && status) params.status = status;
      setRows((await api.get(endpoint, { params })).data);
    } catch (e) { setError(e.response?.data?.message || "Marketplace administration data could not be loaded."); }
  }, [tab, status, page]);
  useEffect(() => { void load(); }, [load]);
  const switchTab = value => { setTab(value); setPage(0); setCategory("ALL"); setStatus(value === "LISTINGS" ? "PENDING_ADMIN_APPROVAL" : ""); };
  const decide = async (id, approved) => {
    const remarks = approved ? "Approved by admin" : window.prompt("Rejection reason");
    if (!approved && !remarks?.trim()) return;
    setBusy(`listing-${id}`);
    try { await api.post(`/api/marketplace/admin/listings/${id}/decision`, { approved, remarks }); setNotice(`Listing ${approved ? "approved" : "rejected"}.`); await load(); }
    catch (e) { setError(e.response?.data?.message || "Listing decision could not be saved."); }
    finally { setBusy(""); }
  };
  const listingCategories = useMemo(() => [...new Set((rows.content || []).map(row => row.categoryName).filter(Boolean))].sort(), [rows.content]);
  const visibleListings = useMemo(() => (rows.content || []).filter(row => category === "ALL" || row.categoryName === category), [rows.content, category]);
  const categoryGroups = useMemo(() => visibleListings.reduce((groups, row) => { const key = row.categoryName || "Uncategorised"; (groups[key] ||= []).push(row); return groups; }, {}), [visibleListings]);
  return <main className="market-admin-page">
    <header className="market-admin-header"><div><p>ADMIN MODERATION</p><h1>Second-hand Marketplace</h1><span>Review listings, customer sellers and private buyer enquiries without exposing private contact data in listing cards.</span></div><Link to="/admin/dashboard">Dashboard</Link></header>
    <div className="market-admin-tabs"><button className={tab === "LISTINGS" ? "active" : ""} onClick={() => switchTab("LISTINGS")}>Listings</button><button className={tab === "SELLERS" ? "active" : ""} onClick={() => switchTab("SELLERS")}>Customer sellers</button><button className={tab === "INQUIRIES" ? "active" : ""} onClick={() => switchTab("INQUIRIES")}>Buyer enquiries</button></div>
    {error && <p className="market-alert error">{error}</p>}{notice && <p className="market-alert">{notice}</p>}
    <section className="market-admin-filter">{tab === "LISTINGS" && <><label>Listing status<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">All listings</option>{["PENDING_ADMIN_APPROVAL", "ACTIVE", "REJECTED", "SOLD", "EXPIRED", "ARCHIVED"].map(value => <option key={value}>{value}</option>)}</select></label><label>Category<select value={category} onChange={e => setCategory(e.target.value)}><option value="ALL">All categories</option>{listingCategories.map(value => <option key={value}>{value}</option>)}</select></label></>}{tab === "INQUIRIES" && <label>Enquiry status<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">All enquiries</option><option value="OPEN">OPEN</option></select></label>}</section>
    {tab === "LISTINGS" && <section className="market-admin-listings">{Object.entries(categoryGroups).map(([categoryName, listings]) => <section className="market-approval-group" key={categoryName}><header><div><span>{status === "PENDING_ADMIN_APPROVAL" ? "APPROVAL QUEUE" : "LISTINGS"}</span><h2>{categoryName}</h2></div><b>{listings.length} {listings.length === 1 ? "listing" : "listings"}</b></header><div className="market-admin-card-grid">{listings.map(row => <article className="market-admin-listing-card" key={row.id}>{row.imageUrls?.[0] ? <img src={imageUrl(row.imageUrls[0])} alt="" /> : <div className="market-admin-image-placeholder">No photo</div>}<div className="market-admin-listing-copy"><small>{row.status.replaceAll("_", " ")} · Customer seller: {row.ownerName || "Customer"}</small><h3>{row.title}</h3><strong>{money(row.price)}</strong><p>{row.subcategoryName ? `${row.subcategoryName} · ` : ""}{row.condition || "Condition not specified"}</p><p>{row.city || "No city"} · Submitted {date(row.createdAt)}</p>{row.status === "PENDING_ADMIN_APPROVAL" && <div className="market-admin-decision"><button disabled={!!busy} onClick={() => void decide(row.id, true)}>{busy === `listing-${row.id}` ? "Saving…" : "Approve"}</button><button className="quiet" disabled={!!busy} onClick={() => void decide(row.id, false)}>Reject</button></div>}</div></article>)}</div></section>)}{!visibleListings.length && <div className="market-admin-empty">No listings match this status and category.</div>}</section>}
    {tab === "SELLERS" && <AdminTable headers={["Customer seller", "Contact", "Registered", "Listings", "Active", "Sold", "Enquiries"]} rows={rows.content?.map(row => [<><strong>{row.name}</strong><small>Customer #{row.customerId}</small></>, <>{row.mobile || "—"}<small>{row.email || "—"}</small></>, date(row.registeredAt), row.totalListings, row.activeListings, row.soldListings, row.enquiriesReceived])} empty="No customer has posted a second-hand listing yet." />}
    {tab === "INQUIRIES" && <AdminTable headers={["When", "Listing", "Customer seller", "Interested buyer", "Message / details requested", "Status"]} rows={rows.content?.map(row => [date(row.createdAt), <><strong>{row.listingTitle}</strong><small>Listing #{row.listingId}</small></>, <>{row.ownerName}<small>{row.ownerMobile || "—"}</small></>, <>{row.buyerName}<small>{row.buyerMobile || "—"} · {row.buyerEmail || "—"}</small></>, row.message, row.status])} empty="No buyer enquiries have been submitted yet." />}
    <div className="market-pagination"><button disabled={!page} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {page + 1}</span><button disabled={page + 1 >= (rows.totalPages || 0)} onClick={() => setPage(value => value + 1)}>Next</button></div>
  </main>;
}
function AdminTable({ headers, rows = [], empty }) { return <section className="market-admin-table"><table><thead><tr>{headers.map(header => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((value, cell) => <td key={cell}>{value}</td>)}</tr>)}</tbody></table>{!rows.length && <p>{empty}</p>}</section>; }
