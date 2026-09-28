import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Box, CircleHelp, ClipboardList, Home, Package, RotateCcw, Search, ShoppingBag, ShoppingCart, Wrench } from "lucide-react";
import { afterSalesService } from "../../services/afterSalesService";
import "./AfterSales.css";
import "./AfterSalesCustomer.css";

const types = ["WARRANTY_CLAIM", "PAID_REPAIR", "RETURN", "REPLACEMENT", "REFUND", "DOA_MANUFACTURING_DEFECT"];
const readable = value => String(value || "-").replaceAll("_", " ");
const date = value => value ? new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "-";

export default function AfterSales() {
  const navigate = useNavigate();
  const [purchases, setPurchases] = useState([]);
  const [requests, setRequests] = useState([]);
  const [selected, setSelected] = useState(null);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [type, setType] = useState("WARRANTY_CLAIM");
  const [issueCategory, setIssueCategory] = useState("");
  const [issue, setIssue] = useState("");
  const [pickupType, setPickupType] = useState("SHOP_VISIT");
  const [evidence, setEvidence] = useState(null);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true); setError("");
    try {
      const [purchaseResponse, requestResponse] = await Promise.all([afterSalesService.eligiblePurchases(), afterSalesService.myRequests()]);
      setPurchases(Array.isArray(purchaseResponse.data) ? purchaseResponse.data : []);
      setRequests(Array.isArray(requestResponse.data) ? requestResponse.data : []);
    } catch (e) { setError(e.response?.data?.message || "Could not load your service information."); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);
  const filtered = useMemo(() => requests.filter(request => {
    const text = `${request.requestNumber} ${request.productName} ${request.requestType} ${request.status}`.toLowerCase();
    return (!query || text.includes(query.toLowerCase())) && (!status || request.status === status);
  }), [requests, query, status]);
  const openRequest = async id => {
    setError("");
    try { const response = await afterSalesService.myRequest(id); setSelectedRequest(response.data); }
    catch (e) { setError(e.response?.data?.message || "Could not load request details."); }
  };
  const submit = async event => {
    event.preventDefault(); if (!selected) return;
    setSaving(true); setError(""); setMessage("");
    try {
      const response = await afterSalesService.createRequest({ requestType: type, orderId: selected.orderId, orderItemId: selected.orderItemId, offlineBillId: selected.offlineBillId, offlineBillItemId: selected.offlineBillItemId, purchaseSerialId: selected.purchaseSerialId, issueCategory, customerIssue: issue, pickupType });
      let uploadWarning = "";
      if (evidence) {
        try { await afterSalesService.customerAttachment(response.data.id, evidence.type.startsWith("video/") ? "VIDEO" : evidence.type === "application/pdf" ? "DOCUMENT" : "IMAGE", evidence); }
        catch { uploadWarning = " The request was created, but its evidence could not be uploaded."; }
      }
      setMessage(`Request ${response.data.requestNumber} was created.${uploadWarning}`); setSelected(null); setIssue(""); setIssueCategory(""); setEvidence(null); await load(); await openRequest(response.data.id);
    } catch (e) { setError(e.response?.data?.message || "The request was not created."); }
    finally { setSaving(false); }
  };
  const decide = async approved => {
    if (!selectedRequest) return; setSaving(true); setError("");
    try { const response = await afterSalesService.decideEstimate(selectedRequest.id, { approved }); setSelectedRequest(response.data); setMessage(approved ? "Estimate approved." : "Estimate rejected."); await load(); }
    catch (e) { setError(e.response?.data?.message || "Estimate decision could not be saved."); } finally { setSaving(false); }
  };
  const cancel = async () => {
    if (!selectedRequest || !window.confirm("Cancel this request?")) return; setSaving(true); setError("");
    try { const response = await afterSalesService.cancel(selectedRequest.id, "Cancelled by customer"); setSelectedRequest(response.data); setMessage("Request cancelled."); await load(); }
    catch (e) { setError(e.response?.data?.message || "Request could not be cancelled."); } finally { setSaving(false); }
  };

  return <main className="after-sales-page">
    <nav className="after-sales-nav" aria-label="Customer navigation"><button type="button" className="after-sales-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingBag size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/orders")}><Package size={18} /> Orders</button><button type="button" className="active" onClick={() => navigate("/customer/after-sales")}><Wrench size={18} /> Service</button><button type="button" onClick={() => navigate("/customer/cart")}><ShoppingCart size={18} /> Cart</button></div></nav>
    <header><div><p className="after-sales-eyebrow">CUSTOMER SUPPORT</p><h1><Wrench size={35} /> My Service & Returns</h1><p>Use only your completed ShivHub purchases to request warranty support, repair, return, replacement, or refund.</p></div><aside><CircleHelp size={28} /><div><strong>Need help?</strong><small>Contact support from the available order or service request.</small></div><button className="after-sales-secondary" onClick={load} disabled={loading}>Refresh</button></aside></header>
    {error && <p className="after-sales-alert error">{error}</p>}{message && <p className="after-sales-alert success">{message}</p>}
    <section className="after-sales-purchases"><div className="after-sales-title-row"><div><p>YOUR PURCHASES</p><h2>Eligible purchases</h2></div><span>{purchases.length} available</span></div>{loading ? <p>Loading eligible purchases…</p> : purchases.length === 0 ? <div className="after-sales-empty"><Box size={34} /><p>No completed eligible purchases were found.</p><button type="button" onClick={() => navigate("/customer/orders")}>View My Orders</button></div> : <div className="after-sales-grid">{purchases.map((purchase, index) => <article className="after-sales-card" key={`${purchase.source}-${purchase.orderItemId || purchase.offlineBillItemId}-${purchase.purchaseSerialId || index}`}><div className="after-sales-product">{purchase.productImageUrl ? <img src={purchase.productImageUrl} alt={purchase.productName} /> : <span className="after-sales-image-placeholder"><Box size={25} /></span>}<div><h3>{purchase.productName}</h3><p>{purchase.invoiceNumber} · Purchased {date(purchase.purchaseDate)}</p><p>{purchase.maskedSerial || "Non-serialised item"}</p></div></div><dl><div><dt>Warranty</dt><dd>{purchase.warrantyActive ? `Active to ${date(purchase.warrantyEndDate)}` : "Unavailable / expired"}</dd></div><div><dt>Return</dt><dd>{purchase.returnEligible ? "Eligible" : "Not eligible"}</dd></div><div><dt>Service</dt><dd>Available</dd></div></dl><button onClick={() => { setSelected(purchase); setMessage(""); setError(""); }}><Wrench size={17} /> Create request</button><button type="button" className="after-sales-view-order" onClick={() => purchase.orderId && navigate(`/customer/orders/${purchase.orderId}`)} disabled={!purchase.orderId}><ClipboardList size={17} /> View order</button></article>)}</div>}</section>
    {selected && <section className="after-sales-modal" role="dialog" aria-modal="true"><form className="after-sales-form" onSubmit={submit}><div className="after-sales-form-header"><h2>Create request</h2><button type="button" className="after-sales-link" onClick={() => setSelected(null)}>Close</button></div><p><strong>{selected.productName}</strong> · {selected.invoiceNumber}</p><label>Request type<select value={type} onChange={e => setType(e.target.value)}>{types.map(item => <option key={item} value={item}>{readable(item)}</option>)}</select></label><label>Issue category<input value={issueCategory} onChange={e => setIssueCategory(e.target.value)} maxLength="80" placeholder="For example: display, battery, delivery damage" /></label><label>Problem description<textarea value={issue} onChange={e => setIssue(e.target.value)} minLength="5" maxLength="5000" required /></label><label>Photos, document, or short video (optional)<input type="file" accept="image/jpeg,image/png,image/webp,application/pdf,video/mp4,video/quicktime" onChange={e => setEvidence(e.target.files?.[0] || null)} /></label><label>Handover option<select value={pickupType} onChange={e => setPickupType(e.target.value)}><option value="SHOP_VISIT">Shop visit</option><option value="SELLER_PICKUP">Seller pickup</option><option value="DELIVERY_PICKUP">Delivery pickup</option></select></label><button disabled={saving}>{saving ? "Submitting…" : "Submit request"}</button></form></section>}
    <section className="after-sales-history"><div className="after-sales-section-heading"><div><p><RotateCcw size={15} /> SERVICE HISTORY</p><h2>Service history</h2></div><div><label><Search size={16} /><input aria-label="Search service history" placeholder="Request or product" value={query} onChange={e => setQuery(e.target.value)} /></label><select aria-label="Filter status" value={status} onChange={e => setStatus(e.target.value)}><option value="">All statuses</option>{[...new Set(requests.map(item => item.status))].map(item => <option value={item} key={item}>{readable(item)}</option>)}</select></div></div><div className="after-sales-table">{filtered.map(request => <button className="after-sales-row" onClick={() => openRequest(request.id)} key={request.id}><span><strong>{request.requestNumber}</strong><small>{request.productName}</small></span><span>{readable(request.requestType)}</span><span className="after-sales-status">{readable(request.status)}</span><span>{date(request.requestDate)}</span></button>)}{!loading && filtered.length === 0 && <div className="after-sales-empty"><Search size={34} /><p>No requests match this filter.</p></div>}</div></section>
    {selectedRequest && <section className="after-sales-modal" role="dialog" aria-modal="true"><article className="after-sales-detail"><div className="after-sales-form-header"><div><p className="after-sales-eyebrow">{selectedRequest.requestNumber}</p><h2>{readable(selectedRequest.status)}</h2></div><button className="after-sales-link" onClick={() => setSelectedRequest(null)}>Close</button></div><p><strong>{selectedRequest.productName}</strong> · {readable(selectedRequest.requestType)} · {selectedRequest.maskedSerial || "Non-serialised item"}</p><p>{selectedRequest.customerIssue}</p>{selectedRequest.customerVisibleRemarks && <p className="after-sales-remark">{selectedRequest.customerVisibleRemarks}</p>}{selectedRequest.estimate && <div className="after-sales-estimate"><h3>Repair estimate</h3><p>Total: ₹{selectedRequest.estimate.grandTotal} · Remaining: ₹{selectedRequest.estimate.remainingAmount}</p><p>Status: {readable(selectedRequest.estimate.customerApprovalStatus)}</p>{selectedRequest.customerCanDecideEstimate && <p><button disabled={saving} onClick={() => decide(true)}>Approve estimate</button><button disabled={saving} className="after-sales-secondary" onClick={() => decide(false)}>Reject</button></p>}</div>}<h3>Timeline</h3><ol className="after-sales-timeline">{selectedRequest.history?.map((entry, index) => <li key={`${entry.changedAt}-${index}`}><strong>{readable(entry.newStatus)}</strong><span>{entry.remarks || "Status updated"}</span><small>{date(entry.changedAt)}</small></li>)}</ol>{selectedRequest.customerCanCancel && <button disabled={saving} className="after-sales-danger" onClick={cancel}>Cancel request</button>}</article></section>}
  </main>;
}
