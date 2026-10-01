import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2, CircleDashed, Clock3, CreditCard, Globe2, KeyRound, Mail, MessageCircle, RefreshCw, ShieldCheck } from "lucide-react";
import { getAdminIntegrationCredentials, getAdminSocialLoginConfiguration, getWhatsAppDeliveryLogs, getWhatsAppDiagnostics, getWhatsAppEvents, updateAdminFacebookAppId, updateAdminGoogleClientId, updateAdminRazorpayCredentials, updateAdminSmtpCredentials, updateWhatsAppCampaignMapping } from "../../services/adminService";
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
  const [mappingDraft, setMappingDraft] = useState({ templateName: "", campaignName: "", templateParameterCount: "", enabled: false });
  const [savingMapping, setSavingMapping] = useState(false);
  const [integrationCredentials, setIntegrationCredentials] = useState(null);
  const [smtpDraft, setSmtpDraft] = useState({ username: "", password: "" });
  const [razorpayDraft, setRazorpayDraft] = useState({ keyId: "", keySecret: "" });
  const [credentialNotice, setCredentialNotice] = useState("");
  const [savingCredential, setSavingCredential] = useState("");
  const [socialLoginConfiguration, setSocialLoginConfiguration] = useState(null);
  const [googleClientIdDraft, setGoogleClientIdDraft] = useState("");
  const [facebookAppIdDraft, setFacebookAppIdDraft] = useState("");
  const [socialNotice, setSocialNotice] = useState("");
  const [savingSocial, setSavingSocial] = useState("");

  const load = useCallback(async (requestedPage = 0, eventKey = "") => {
    setLoading(true);
    setError("");
    const sections = [
      ["WhatsApp provider", getWhatsAppDiagnostics, data => setDiagnostics(data)],
      ["Campaign mappings", getWhatsAppEvents, data => {
        const availableEvents = data || [];
        setEvents(availableEvents);
        const currentEvent = availableEvents.find(item => item.key === eventKey) || availableEvents[0];
        if (currentEvent) {
          setSelectedEventKey(currentEvent.key);
          setMappingDraft({
            templateName: currentEvent.templateName || "",
            campaignName: currentEvent.campaignName || "",
            templateParameterCount: currentEvent.templateParameterCount ?? "",
            enabled: Boolean(currentEvent.enabled),
          });
        }
      }],
      ["Delivery logs", () => getWhatsAppDeliveryLogs(requestedPage, 30), data => {
        setLogs(data || { content: [] });
        setPage(data?.number ?? requestedPage);
      }],
      ["Email/payment settings", getAdminIntegrationCredentials, data => {
        setIntegrationCredentials(data || null);
        setSmtpDraft(current => ({ ...current, username: data?.mailUsername || "" }));
        setRazorpayDraft(current => ({ ...current, keyId: data?.razorpayKeyId || "" }));
      }],
      ["Social login settings", getAdminSocialLoginConfiguration, data => {
        setSocialLoginConfiguration(data || null);
        setGoogleClientIdDraft(data?.googleClientId || "");
        setFacebookAppIdDraft(data?.facebookAppId || "");
      }],
    ];
    const results = await Promise.allSettled(sections.map(async ([, request, apply]) => {
      const data = await request();
      apply(data);
    }));
    const failures = results.flatMap((result, index) => {
      if (result.status === "fulfilled") return [];
      const status = result.reason?.response?.status;
      const detail = status === 401 || status === 403
        ? "Access denied. Sign in again as an administrator; if this persists, restart the updated backend."
        : status === 404
          ? "Endpoint unavailable. Restart the updated backend."
          : status ? "Server returned HTTP " + status + ". Check backend logs."
            : "Cannot reach the backend. Check that it is running and retry.";
      return [sections[index][0] + ": " + detail];
    });
    setError(failures.join(" "));
    setLoading(false);
  }, []);

  useEffect(() => { void load(0); }, [load]);

  const selectMappingEvent = eventKey => {
    const event = events.find(item => item.key === eventKey);
    setSelectedEventKey(eventKey);
    setMappingNotice("");
    setMappingDraft({
      templateName: event?.templateName || "",
      campaignName: event?.campaignName || "",
      templateParameterCount: event?.templateParameterCount ?? "",
      enabled: Boolean(event?.enabled),
    });
  };

  const saveMapping = async event => {
    event.preventDefault();
    if (!selectedEventKey) return;
    setSavingMapping(true);
    setMappingNotice("");
    try {
      const parameterCount = String(mappingDraft.templateParameterCount ?? "").trim();
      await updateWhatsAppCampaignMapping(selectedEventKey, {
        ...mappingDraft,
        templateParameterCount: parameterCount === "" ? null : Number(parameterCount),
      });
      setMappingNotice("Campaign mapping saved. It will be used for the next matching notification.");
      await load(page, selectedEventKey);
    } catch (requestError) {
      setMappingNotice(requestError.response?.data?.message || "Campaign mapping could not be saved.");
    } finally {
      setSavingMapping(false);
    }
  };

  const saveSmtpCredentials = async event => {
    event.preventDefault();
    setCredentialNotice("");
    setSavingCredential("smtp");
    try {
      const saved = await updateAdminSmtpCredentials(smtpDraft);
      setIntegrationCredentials(saved);
      setSmtpDraft(current => ({ ...current, password: "" }));
      setCredentialNotice("SMTP credentials saved securely. The password is encrypted and never shown again.");
    } catch (requestError) {
      setCredentialNotice(requestError.response?.data?.message || "SMTP credentials could not be saved.");
    } finally {
      setSavingCredential("");
    }
  };

  const saveRazorpayCredentials = async event => {
    event.preventDefault();
    setCredentialNotice("");
    setSavingCredential("razorpay");
    try {
      const saved = await updateAdminRazorpayCredentials(razorpayDraft);
      setIntegrationCredentials(saved);
      setRazorpayDraft(current => ({ ...current, keySecret: "" }));
      setCredentialNotice("Razorpay credentials saved securely. The secret is encrypted and never shown again.");
    } catch (requestError) {
      setCredentialNotice(requestError.response?.data?.message || "Razorpay credentials could not be saved.");
    } finally {
      setSavingCredential("");
    }
  };
  const saveGoogleClientId = async event => {
    event.preventDefault();
    setSocialNotice("");
    setSavingSocial("google");
    try {
      const saved = await updateAdminGoogleClientId({ clientId: googleClientIdDraft });
      setSocialLoginConfiguration(saved);
      setGoogleClientIdDraft(saved?.googleClientId || "");
      setSocialNotice("Google OAuth client ID saved. It is active on the login page after refresh.");
    } catch (requestError) {
      setSocialNotice(requestError.response?.data?.message || "Google configuration could not be saved.");
    } finally {
      setSavingSocial("");
    }
  };

  const saveFacebookAppId = async event => {
    event.preventDefault();
    setSocialNotice("");
    setSavingSocial("facebook");
    try {
      const saved = await updateAdminFacebookAppId({ clientId: facebookAppIdDraft });
      setSocialLoginConfiguration(saved);
      setFacebookAppIdDraft(saved?.facebookAppId || "");
      setSocialNotice("Facebook App ID saved. It is active on the login page after refresh.");
    } catch (requestError) {
      setSocialNotice(requestError.response?.data?.message || "Facebook configuration could not be saved.");
    } finally {
      setSavingSocial("");
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
          <p>Review WhatsApp readiness, API campaign mappings and delivery attempts. WhatsApp API credentials remain server-only; SMTP and Razorpay can be stored securely below.</p>
        </div>
        <button className="secondary-button" type="button" onClick={() => load(page, selectedEventKey)} disabled={loading}>
          <RefreshCw size={16} aria-hidden="true" className={loading ? "spinning" : ""} /> Refresh
        </button>
      </header>

      {error && <p className="growth-error">{error}</p>}

      <section className="growth-card integration-credentials-card">
        <div className="section-title-row">
          <div><p className="eyebrow">Admin-only credentials</p><h2><KeyRound size={20} aria-hidden="true" /> Email and payment setup</h2></div>
          <span>{integrationCredentials?.encryptionReady ? <><ShieldCheck size={15} aria-hidden="true" /> Encrypted at rest</> : <><AlertTriangle size={15} aria-hidden="true" /> Encryption key required</>}</span>
        </div>
        <p className="integration-credentials-description">Save the SMTP username/password and Razorpay key ID/secret here. Secret values are encrypted in the database, write-only in this screen, and never returned by the API.</p>
        {!integrationCredentials?.encryptionReady && !loading && <div className="integration-credentials-warning"><AlertTriangle size={18} aria-hidden="true" /><span>Before saving, add <code>SHIVHUB_ADMIN_CREDENTIALS_ENCRYPTION_KEY</code> (a new 32+ character private value) to the backend server environment. Keep that value unchanged after credentials are saved.</span></div>}
        <div className="integration-credentials-grid">
          <form className="integration-credential-form" onSubmit={saveSmtpCredentials}>
            <div className="integration-credential-title"><Mail size={20} aria-hidden="true" /><div><h3>SMTP email</h3><p>{integrationCredentials?.mailConfigured ? `Configured from ${String(integrationCredentials.mailSource || "").toLowerCase()}` : "Not configured"}</p></div></div>
            <label>Gmail / SMTP username<input type="email" value={smtpDraft.username} onChange={event => setSmtpDraft(current => ({ ...current, username: event.target.value }))} placeholder="name@gmail.com" autoComplete="username" required /></label>
            <label>Gmail app password<input type="password" value={smtpDraft.password} onChange={event => setSmtpDraft(current => ({ ...current, password: event.target.value }))} placeholder={integrationCredentials?.mailConfigured ? "Enter a replacement password" : "16-character app password"} autoComplete="new-password" required /></label>
            <button className="primary-button" type="submit" disabled={!integrationCredentials?.encryptionReady || savingCredential === "smtp"}>{savingCredential === "smtp" ? "Saving..." : "Save SMTP credentials"}</button>
          </form>
          <form className="integration-credential-form" onSubmit={saveRazorpayCredentials}>
            <div className="integration-credential-title"><CreditCard size={20} aria-hidden="true" /><div><h3>Razorpay</h3><p>{integrationCredentials?.razorpayConfigured ? `Configured from ${String(integrationCredentials.razorpaySource || "").toLowerCase()}` : "Not configured"}</p></div></div>
            <label>Razorpay key ID<input value={razorpayDraft.keyId} onChange={event => setRazorpayDraft(current => ({ ...current, keyId: event.target.value }))} placeholder="rzp_live_..." autoComplete="off" required /></label>
            <label>Razorpay key secret<input type="password" value={razorpayDraft.keySecret} onChange={event => setRazorpayDraft(current => ({ ...current, keySecret: event.target.value }))} placeholder={integrationCredentials?.razorpayConfigured ? "Enter a replacement secret" : "Razorpay key secret"} autoComplete="new-password" required /></label>
            <button className="primary-button" type="submit" disabled={!integrationCredentials?.encryptionReady || savingCredential === "razorpay"}>{savingCredential === "razorpay" ? "Saving..." : "Save Razorpay credentials"}</button>
          </form>
        </div>
        {credentialNotice && <p className="integration-credentials-notice" role="status">{credentialNotice}</p>}
      </section>
      <section className="growth-card integration-credentials-card">
        <div className="section-title-row"><h2><Globe2 size={20} /> Google and Facebook login</h2><span>Public browser identifiers</span></div>
        <p>These IDs are public. Keep OAuth client secrets and the JWT secret only in the backend environment.</p>
        <div className="integration-credentials-grid">
          <form className="integration-credential-form" onSubmit={saveGoogleClientId}>
            <h3>Google OAuth</h3>
            <p>{socialLoginConfiguration?.googleConfigured ? "Configured" : "Not configured"}</p>
            <label>Google OAuth Client ID<input value={googleClientIdDraft} onChange={event => setGoogleClientIdDraft(event.target.value)} maxLength={400} placeholder="...apps.googleusercontent.com" required /></label>
            <p>Configure your frontend origin in the Google Cloud OAuth web client.</p>
            <button className="primary-button" disabled={loading || Boolean(savingSocial)}>{savingSocial === "google" ? "Saving..." : "Save Google Client ID"}</button>
          </form>
          <form className="integration-credential-form" onSubmit={saveFacebookAppId}>
            <h3>Facebook login</h3>
            <p>{socialLoginConfiguration?.facebookConfigured ? "Configured" : "Not configured"}</p>
            <label>Facebook App ID<input value={facebookAppIdDraft} onChange={event => setFacebookAppIdDraft(event.target.value)} maxLength={400} inputMode="numeric" required /></label>
            <p>Configure your frontend domain and Facebook Login settings in the Meta app dashboard.</p>
            <button className="primary-button" disabled={loading || Boolean(savingSocial)}>{savingSocial === "facebook" ? "Saving..." : "Save Facebook App ID"}</button>
          </form>
        </div>
        {socialNotice && <p className="integration-credentials-notice" role="status">{socialNotice}</p>}
      </section>
      {error && <p className="growth-error" role="alert">{error}</p>}
      <section className="metric-grid whatsapp-metrics" aria-label="WhatsApp status">
        <article>
          <span>Provider</span>
          <strong>{diagnostics?.provider || "Checking…"}</strong>
          <small>Configured through environment variables</small>
        </article>
        <article>
          <span>Connection</span>
          <strong className={diagnostics?.configured ? "metric-ok" : "metric-warn"}>{diagnostics == null ? (loading ? "Checking?" : "Unavailable") : diagnostics.configured ? "Ready" : "Not ready"}</strong>
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

      {diagnostics?.configured === false && !loading && (
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
        <span>The API key remains server-only. Campaign mappings store only non-secret names and body parameter counts.</span>
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
          <label>
            Body parameter count
            <input type="number" min="0" max="20" step="1" value={mappingDraft.templateParameterCount} onChange={event => setMappingDraft(current => ({ ...current, templateParameterCount: event.target.value }))} placeholder="Blank: send all event parameters" disabled={!selectedEventKey || savingMapping} />
          </label>
          <p>Enter the exact number of body variables in this Live campaign's template (0 for none). ShivHub sends the first N event values in their existing order, trimming only trailing values. Sending is skipped if fewer than N values are available. Blank preserves all event values. Header media and OTP button parameters are separate; do not include them in this count. Match the template variable order before enabling.</p>
          {selectedEventKey === "payment-success" && <p>Payment success requires its own exact Live campaign mapping. Missing or disabled mappings are skipped; no invoice or generic campaign is substituted.</p>}
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
