import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import api, { API_BASE_URL } from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./SellerCustomers.css";

/** Seller-scoped customer messages. The banner is optional and is rendered in the real email, not only in this preview. */
export default function SellerCustomers() {
  const location = useLocation();
  const isGreeting = location.pathname.endsWith("messages");
  const [customers, setCustomers] = useState([]);
  const [selected, setSelected] = useState([]);
  const [channel, setChannel] = useState("EMAIL");
  const [allCustomers, setAllCustomers] = useState(false);
  const [subject, setSubject] = useState(isGreeting ? "Greetings from our shop" : "Special offer for you");
  const [message, setMessage] = useState(isGreeting ? "Thank you for being a valued customer. We look forward to serving you again!" : "We have a special offer waiting for you. Visit our shop today!");
  const [bannerUrl, setBannerUrl] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const [uploadingBanner, setUploadingBanner] = useState(false);

  const load = () => api.get("/api/seller/customers")
    .then(({ data }) => setCustomers(data || []))
    .catch(err => setError(err?.response?.data?.message || "Unable to load offline customers."));
  useEffect(() => { load(); }, []);

  const recipientKey = channel === "WHATSAPP" ? "mobile" : "email";
  const allEmails = useMemo(() => [...new Set(customers.map(customer => customer[recipientKey]).filter(Boolean))], [customers, recipientKey]);
  const previewBanner = bannerUrl && (/^https?:\/\//i.test(bannerUrl) ? bannerUrl : `${API_BASE_URL}${bannerUrl}`);
  const toggle = email => setSelected(current => current.includes(email) ? current.filter(item => item !== email) : [...current, email]);

  const uploadBanner = async event => {
    const file = event.target.files?.[0];
    if (!file) return;
    setUploadingBanner(true);
    setError("");
    try {
      const formData = new FormData();
      formData.append("file", file);
      const { data } = await api.post("/api/seller/customers/email-campaign/banner", formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      setBannerUrl(data.bannerUrl || "");
    } catch (err) {
      setError(err?.response?.data?.message || "Banner upload failed. Use JPG, PNG or WEBP up to 5 MB.");
    } finally {
      setUploadingBanner(false);
    }
  };

  const send = async event => {
    event.preventDefault();
    if (!selected.length && !allCustomers) {
      setError("Select at least one customer or choose All my customers.");
      return;
    }
    setSending(true);
    setError("");
    setNotice("");
    try {
      if (channel === "WHATSAPP") {
        const { data } = await api.post("/api/seller/customers/whatsapp-offer", {
          allCustomers, recipientMobiles: selected, subject, message, bannerUrl: bannerUrl.trim()
        });
        setNotice(`WhatsApp: ${data.accepted} accepted by provider, ${data.skipped} skipped (consent or eligibility), ${data.failed} failed.`);
        return;
      }
      const { data } = await api.post("/api/seller/customers/email-campaign", {
        recipientEmails: allCustomers ? allEmails : selected,
        subject,
        message,
        bannerUrl: bannerUrl.trim() || null
      });
      setNotice(`Email notification sent to ${data.sent} customer(s). ${data.skipped ? `${data.skipped} could not be sent.` : ""}`);
    } catch (err) {
      setError(err?.response?.data?.message || "Offer could not be sent. Check channel configuration.");
    } finally {
      setSending(false);
    }
  };

  return <div className="seller-customers-page">
    <SellerSidebar />
    <main className="seller-customers-main">
      <header><div><p>YOUR SHOP CUSTOMERS</p><h1>{isGreeting ? "Send greeting" : "Customers & offers"}</h1><span>Send an email or WhatsApp image offer to your customers. WhatsApp offers go only to customers who agreed to receive them.</span></div></header>
      {error && <div className="customer-error">{error}</div>}
      {notice && <div className="customer-ok">✓ {notice}</div>}
      <section className="customer-layout">
        <section className="customer-list">
          <div className="customer-list-head"><h2>Customer list</h2><button type="button" onClick={() => setSelected(selected.length === allEmails.length ? [] : allEmails)}>{selected.length === allEmails.length ? "Clear all" : "Select all"}</button></div>
          {customers.length ? customers.map(customer => <label className="customer-row" key={customer.email || customer.mobile}>
            <input type="checkbox" disabled={allCustomers || !customer[recipientKey]} checked={allCustomers || selected.includes(customer[recipientKey])} onChange={() => toggle(customer[recipientKey])} />
            <span><strong>{customer.name}</strong><small>{customer.email || "No email"} · {customer.mobile || "No mobile"}</small></span><b>₹{Number(customer.totalSpend || 0).toLocaleString("en-IN")}</b>
          </label>) : <p className="customer-empty">No offline-bill customer emails available yet.</p>}
        </section>
        <form className="customer-email-card" onSubmit={send}>
          <span>CUSTOMER OFFER</span><h2>{isGreeting ? "Greeting message" : "Offer message"}</h2>
          <label>Send via<select value={channel} disabled={sending} onChange={event => { setChannel(event.target.value); setSelected([]); setAllCustomers(false); }}><option value="EMAIL">Email</option><option value="WHATSAPP">WhatsApp image offer</option></select></label>
          <label><input type="checkbox" checked={allCustomers} disabled={sending} onChange={event => setAllCustomers(event.target.checked)} /> All my customers</label>
          <p>{allCustomers ? "All eligible customers belonging to your shop" : `${selected.length} recipient(s) selected`}</p>
          <label>Subject<input value={subject} maxLength="150" onChange={event => setSubject(event.target.value)} required /></label>
          <label>Message<textarea value={message} maxLength="3000" onChange={event => setMessage(event.target.value)} required /></label>
          <label>Offer banner image {channel === "WHATSAPP" ? "(required)" : "(optional)"}<input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingBanner} onChange={uploadBanner} /><small>{uploadingBanner ? "Uploading banner…" : bannerUrl ? "Banner attached" : "JPG, PNG or WEBP; maximum 5 MB"}</small></label>
          <label>Or banner image URL<input value={bannerUrl} required={channel === "WHATSAPP"} maxLength="2000" placeholder="https://…" onChange={event => setBannerUrl(event.target.value)} /></label>
          <div className="email-preview">{previewBanner && <img className="email-preview-banner" src={previewBanner} alt="Selected offer banner" />}<small>Email preview</small><strong>{subject}</strong><p>{message}</p><footer>Thank you for visiting your shop.<br />Powered by ShivHub</footer></div>
          <button disabled={sending || uploadingBanner}>{sending ? "Sending…" : channel === "WHATSAPP" ? "Send WhatsApp image offer" : "Send email notification"}</button>
        </form>
      </section>
    </main>
  </div>;
}
