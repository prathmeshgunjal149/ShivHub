import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2, CircleDashed, Clock3, MessageCircle, RefreshCw, ShieldCheck } from "lucide-react";
import { getWhatsAppDeliveryLogs, getWhatsAppDiagnostics, getWhatsAppEvents, updateWhatsAppCampaignMapping } from "../../services/adminService";
import "./AdminGrowth.css";
import "./Notifications.css";

const eventDetails = {
  "generic-notification": ["General", "General customer update"],
  "order-placed": ["Orders", "Order placed"],
  "order-confirmed": ["Orders", "Order confirmed"],
  "order-processing": ["Orders", "Order processing"],
  "order-packed": ["Orders", "Order packed"],
  "order-shipped": ["Orders", "Order shipped"],
  "order-out-for-delivery": ["Orders", "Out for delivery"],
  "order-delivered": ["Orders", "Order delivered"],
  "order-cancelled": ["Orders", "Order cancelled"],
  "invoice-generated": ["Orders", "Invoice generated"],
  "payment-success": ["Payments", "Payment received"],
  "coupon-applied": ["Payments", "Coupon applied"],
  "payment-reminder": ["Payments", "Payment reminder"],
  "refund-completed": ["Payments", "Refund completed"],
  "delivery-time-updated": ["Orders", "Delivery time updated"],
  "loyalty-points-updated": ["Customer care", "Loyalty points updated"],
  "after-sales-updated": ["Customer care", "After-sales request updated"],
  "support-ticket-received": ["Customer care", "Support request received"],
  "support-ticket-updated": ["Customer care", "Support request updated"],
  "marketplace-listing-submitted": ["Marketplace", "Listing submitted"],
  "marketplace-listing-approved": ["Marketplace", "Listing approved"],
  "marketplace-listing-rejected": ["Marketplace", "Listing rejected"],
  "marketplace-enquiry": ["Marketplace", "New listing enquiry"],
  "marketplace-item-sold": ["Marketplace", "Listing marked sold"],
  "marketplace-listing-expired": ["Marketplace", "Listing expired"],
  "marketplace-listing-archived": ["Marketplace", "Listing archived"],
  "emi-schedule": ["Finance", "EMI schedule"],
  "emi-reminder": ["Finance", "EMI reminder"],
  "emi-overdue": ["Finance", "EMI overdue"],
  "offer-notification": ["Marketing", "Offer notification"],
  "offer-image-notification": ["Marketing", "Offer image notification"],
  "seller-account-approved": ["Seller operations", "Seller account approved"],
  "seller-account-rejected": ["Seller operations", "Seller account update"],
  "seller-product-approved": ["Seller operations", "Product approved"],
  "seller-product-rejected": ["Seller operations", "Product requires changes"],
  "seller-payment-reminder": ["Seller operations", "Daily payment reminder"],
  "seller-purchase-invoice": ["Seller operations", "Purchase invoice saved"],
  "seller-purchase-payment": ["Seller operations", "Purchase payment recorded"],
  "seller-low-stock": ["Seller operations", "Low-stock alert"],
  "seller-expense-recorded": ["Seller operations", "Expense recorded"],
  "seller-subscription-updated": ["Seller operations", "Subscription update"],
  "staff-account-assigned": ["Seller operations", "Staff account assigned"],
  "distributor-workflow-updated": ["Supplier operations", "Distributor workflow update"],
  "distributor-payment-recorded": ["Supplier operations", "Distributor payment recorded"],
  "referral-invitation": ["Customer care", "Referral invitation"],
  "otp-registration": ["Authentication", "Registration verification code"],
  "otp-login": ["Authentication", "Login verification code"],
  "password-reset": ["Authentication", "Password reset"],
};

const formatDate = value => value ? new Date(value).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "—";
const displayName = key => eventDetails[key]?.[1] || String(key || "legacy-update").replace(/-/g, " ");
const maskMobile = value => {
  const digits = String(value || "").replace(/\D/g, "");
  return digits.length < 4 ? "Hidden" : `••••••${digits.slice(-4)}`;
};

export default function Notifications() {
  const [diagnostics, setDiagnostics] = useState(null);
  const [events, setEvents] = useState([]);
  const [logs, setLogs] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 });
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [mappingNotice, setMappingNotice] = useState("");
  const [selectedEventKey, setSelectedEventKey] = useState("");
  const [mappingDraft, setMappingDraft] = useState({ templateName: "", campaignName: "", enabled: false });
  const [savingMapping, setSavingMapping] = useState(false);

  const load = useCallback(async (requestedPage = 0, eventKey = "") => {
    setLoading(true);
    setError("");
    try {
      const [provider, configuredEvents, deliveryLogs] = await Promise.all([
        getWhatsAppDiagnostics(),
        getWhatsAppEvents(),
        getWhatsAppDeliveryLogs(requestedPage, 30),
      ]);
      setDiagnostics(provider);
      setEvents(configuredEvents || []);
      setLogs(deliveryLogs || { content: [] });
      setPage(deliveryLogs?.number ?? requestedPage);
      const availableEvents = configuredEvents || [];
      const currentEvent = availableEvents.find(item => item.key === eventKey) || availableEvents[0];
      if (currentEvent) {
        setSelectedEventKey(currentEvent.key);
        setMappingDraft({
          templateName: currentEvent.templateName || "",
          campaignName: currentEvent.campaignName || "",
          enabled: Boolean(currentEvent.enabled),
        });
      }
    } catch (requestError) {
      setError(requestError.response?.data?.message || "WhatsApp operations could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(0); }, [load]);

  const selectMappingEvent = eventKey => {
    const event = events.find(item => item.key === eventKey);
    setSelectedEventKey(eventKey);
    setMappingNotice("");
    setMappingDraft({
      templateName: event?.templateName || "",
      campaignName: event?.campaignName || "",
      enabled: Boolean(event?.enabled),
    });
  };

  const saveMapping = async event => {
    event.preventDefault();
    if (!selectedEventKey) return;
    setSavingMapping(true);
    setMappingNotice("");
    try {
      await updateWhatsAppCampaignMapping(selectedEventKey, mappingDraft);
      setMappingNotice("Campaign mapping saved. It will be used for the next matching notification.");
      await load(page, selectedEventKey);
    } catch (requestError) {
      setMappingNotice(requestError.response?.data?.message || "Campaign mapping could not be saved.");
    } finally {
      setSavingMapping(false);
    }
  };

  const configuredCount = events.filter(item => item.campaignConfigured).length;
  const groupedEvents = useMemo(() => events.reduce((groups, item) => {
    const group = eventDetails[item.key]?.[0] || "Other";
    (groups[group] ||= []).push(item);
    return groups;
  }, {}), [events]);
  const latestFailure = logs.content?.find(item => item.failureReason);

  return (
    <main className="growth-page whatsapp-operations-page">
      <Link className="back-link" to="/admin/settings">← Settings</Link>
      <header className="growth-heading whatsapp-operations-heading">
        <div>
          <p className="eyebrow">Customer communication</p>
          <h1><MessageCircle size={30} aria-hidden="true" /> WhatsApp operations</h1>
          <p>Review provider readiness, API campaign mappings and delivery attempts. Provider credentials remain outside the browser in environment variables.</p>
        </div>
        <button className="secondary-button" type="button" onClick={() => load(page, selectedEventKey)} disabled={loading}>
          <RefreshCw size={16} aria-hidden="true" className={loading ? "spinning" : ""} /> Refresh
        </button>
      </header>

      {error && <p className="growth-error">{error}</p>}

      <section className="metric-grid whatsapp-metrics" aria-label="WhatsApp status">
        <article>
          <span>Provider</span>
          <strong>{diagnostics?.provider || "Checking…"}</strong>
          <small>Configured through environment variables</small>
        </article>
        <article>
          <span>Connection</span>
          <strong className={diagnostics?.configured ? "metric-ok" : "metric-warn"}>{diagnostics?.configured ? "Ready" : "Not ready"}</strong>
          <small>API key and provider status are never shown here</small>
        </article>
        <article>
          <span>Campaign mappings</span>
          <strong>{configuredCount} / {events.length || "—"}</strong>
          <small>Variants linked to a live campaign</small>
        </article>
        <article>
          <span>Recorded attempts</span>
          <strong>{logs.totalElements ?? 0}</strong>
          <small>Recipient numbers are masked for privacy</small>
        </article>
      </section>

      {!diagnostics?.configured && !loading && (
        <section className="whatsapp-callout warning">
          <AlertTriangle size={20} aria-hidden="true" />
          <div><strong>Provider connection is not ready.</strong><span>Set the provider configuration only in the server environment. Do not place the AiSensy API key in this screen or frontend code.</span></div>
        </section>
      )}

      {latestFailure && (
        <section className="whatsapp-callout failure">
          <AlertTriangle size={20} aria-hidden="true" />
          <div><strong>Latest provider issue</strong><span>{latestFailure.failureReason}</span></div>
        </section>
      )}

      <section className="growth-card whatsapp-guide">
        <div>
          <ShieldCheck size={22} aria-hidden="true" />
          <div><h2>Safe setup sequence</h2><p>Create and approve the WhatsApp template in AiSensy, create a matching API campaign and set it Live, then save its exact names below. The checklists show every supported ShivHub variant.</p></div>
        </div>
        <span>The API key remains server-only. This screen stores only non-secret template and campaign names.</span>
      </section>

      <section className="growth-card whatsapp-mapping-editor">
        <div className="section-title-row">
          <div><p className="eyebrow">Admin setup</p><h2>Map an approved template to a live campaign</h2></div>
          <span>One central ShivHub WhatsApp number</span>
        </div>
        <form onSubmit={saveMapping}>
          <label>
            Notification type
            <select value={selectedEventKey} onChange={event => selectMappingEvent(event.target.value)} disabled={!events.length || savingMapping}>
              {events.map(item => <option key={item.key} value={item.key}>{displayName(item.key)}</option>)}
            </select>
          </label>
          <div className="whatsapp-mapping-fields">
            <label>
              Approved Meta template name
              <input value={mappingDraft.templateName} maxLength="200" onChange={event => setMappingDraft(current => ({ ...current, templateName: event.target.value }))} placeholder="e.g. shivhub_invoice_update" disabled={!selectedEventKey || savingMapping} />
            </label>
            <label>
              Live AiSensy campaign name
              <input value={mappingDraft.campaignName} maxLength="200" onChange={event => setMappingDraft(current => ({ ...current, campaignName: event.target.value }))} placeholder="e.g. shivhub invoice" disabled={!selectedEventKey || savingMapping} />
            </label>
          </div>
          <label className="whatsapp-enable-toggle">
            <input type="checkbox" checked={mappingDraft.enabled} onChange={event => setMappingDraft(current => ({ ...current, enabled: event.target.checked }))} disabled={!selectedEventKey || savingMapping} />
            <span>Template is approved and the AiSensy campaign is Live</span>
          </label>
          {selectedEventKey === "offer-image-notification" && <p>Use an approved IMAGE-header marketing template with three body parameters in this order: shop name, offer title, offer message. Create a Live API campaign using that template. Seller banners are sent as the image header.</p>}
          <div className="whatsapp-mapping-actions">
            <button className="primary-button" type="submit" disabled={!selectedEventKey || savingMapping}>{savingMapping ? "Saving…" : "Save mapping"}</button>
            {mappingNotice && <small role="status">{mappingNotice}</small>}
          </div>
        </form>
      </section>

      <section className="whatsapp-variant-section">
        <div className="section-title-row"><div><p className="eyebrow">Template variants</p><h2>Campaign readiness by customer event</h2></div><span>{events.length ? "Loaded from backend" : "Loading variants…"}</span></div>
        <div className="whatsapp-variant-groups">
          {Object.entries(groupedEvents).map(([group, items]) => (
            <article className="whatsapp-variant-card" key={group}>
              <h3>{group}</h3>
              <ul>{items.map(item => <li key={item.key}><span>{displayName(item.key)}</span><b className={item.campaignConfigured ? "ready" : "pending"}>{item.campaignConfigured ? <><CheckCircle2 size={14} /> Mapped</> : <><CircleDashed size={14} /> Awaiting campaign</>}</b></li>)}</ul>
            </article>
          ))}
          {!events.length && !loading && <p className="empty-state">No event variants were returned by the server.</p>}
        </div>
      </section>

      <section className="growth-card table-scroll whatsapp-log-card">
        <div className="section-title-row"><div><p className="eyebrow">Delivery audit</p><h2>Recent WhatsApp attempts</h2></div><span><Clock3 size={15} aria-hidden="true" /> Latest first</span></div>
        <table><thead><tr><th>Event</th><th>Recipient</th><th>Status</th><th>Provider detail</th><th>Attempted</th></tr></thead>
          <tbody>{logs.content?.map(log => <tr key={log.id}><td><strong>{displayName(log.eventType)}</strong><small>{log.eventType || "Legacy delivery record"}</small></td><td>{maskMobile(log.recipient)}</td><td><span className={`pill ${String(log.status || "").toLowerCase()}`}>{log.status || "UNKNOWN"}</span></td><td className="whatsapp-detail">{log.failureReason || log.providerStatus || "—"}</td><td>{formatDate(log.sentAt || log.createdAt)}</td></tr>)}</tbody>
        </table>
        {!loading && !logs.content?.length && <p className="empty-state">No WhatsApp delivery attempts have been recorded yet.</p>}
        {loading && <p className="empty-state">Loading WhatsApp operations…</p>}
        {logs.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {logs.totalPages}</span><button type="button" disabled={page === 0 || loading} onClick={() => load(page - 1)}>Previous</button><button type="button" disabled={page >= logs.totalPages - 1 || loading} onClick={() => load(page + 1)}>Next</button></div>}
      </section>
    </main>
  );
}
