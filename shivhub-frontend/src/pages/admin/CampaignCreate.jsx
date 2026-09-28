import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createAdminCampaign, listAdminSellers, previewAdminCampaignAudience, uploadAdminCampaignBanner } from "../../services/adminManagementService";
import "./AdminGrowth.css";

const initial = { title: "", messageType: "OFFER", festivalName: "", subject: "", messageContent: "", bannerUrl: "", couponCode: "", offerDetails: "", channels: ["EMAIL"], sellerIds: [], customerType: "ALL", targetAudience: "ALL_CUSTOMERS", city: "", inactiveDays: "", minimumPurchaseAmount: "", productId: "", productCategory: "", validityStartsAt: "", validityEndsAt: "", scheduledAt: "" };

export default function CampaignCreate() {
  const nav = useNavigate();
  const [form, setForm] = useState(initial);
  const [sellers, setSellers] = useState([]);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const [localBannerPreview, setLocalBannerPreview] = useState("");
  const payload = useMemo(() => ({
    ...form,
    sellerIds: form.sellerIds.filter(Boolean).map(Number),
    inactiveDays: form.inactiveDays ? Number(form.inactiveDays) : null,
    minimumPurchaseAmount: form.minimumPurchaseAmount ? Number(form.minimumPurchaseAmount) : null,
    productId: form.productId ? Number(form.productId) : null,
    productCategory: form.productCategory.trim() || null,
    validityStartsAt: form.validityStartsAt || null,
    validityEndsAt: form.validityEndsAt || null,
    scheduledAt: form.scheduledAt || null
  }), [form]);
  useEffect(() => {
    listAdminSellers({ page: 0, size: 100 }).then(response => setSellers(response.content)).catch(() => setError("Seller options could not be loaded. You can still target all customers."));
  }, []);
  const change = (key, value) => setForm(current => ({ ...current, [key]: value }));
  
  const toggleSeller = sellerId => change("sellerIds", form.sellerIds.includes(sellerId) ? form.sellerIds.filter(item => item !== sellerId) : [...form.sellerIds, sellerId]);
  const uploadBanner = async event => {
    const file = event.target.files?.[0];
    if (!file) return;
    setLocalBannerPreview(URL.createObjectURL(file));
    setUploadingBanner(true);
    try {
      const response = await uploadAdminCampaignBanner(file);
      change("bannerUrl", response.bannerUrl);
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Banner upload failed. Use a JPG, PNG or WEBP image up to 5 MB.");
    } finally {
      setUploadingBanner(false);
    }
  };
  const previewAudience = async () => {
    setBusy(true);
    try {
      setPreview(await previewAdminCampaignAudience(payload));
      setError("");
    } catch {
      setError("Audience preview could not be calculated. Check the selected filters.");
    } finally {
      setBusy(false);
    }
  };
  const save = async mode => {
    if (!form.title.trim() || !form.messageContent.trim()) {
      setError("Campaign title and message are required.");
      return;
    }
    if (!form.channels.length) {
      setError("Select at least one delivery channel.");
      return;
    }
    if (mode === "schedule" && !form.scheduledAt) {
      setError("Choose a future date and time before scheduling.");
      return;
    }
    if (mode === "send" && !window.confirm("Send this campaign to the selected audience now?")) return;
    setBusy(true);
    try {
      const campaign = await createAdminCampaign({ ...payload, draft: mode === "draft", sendNow: mode === "send" });
      nav(`/admin/campaigns/${campaign.id}/report`);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Campaign could not be saved.");
    } finally {
      setBusy(false);
    }
  };

  return <main className="growth-page">
    <Link className="back-link" to="/admin/campaigns">← Campaigns</Link>
    <header className="growth-heading"><div><p className="eyebrow">Campaign builder</p><h1>Create an offer or festival message</h1><p>Messages are only delivered to consented customer profiles.</p></div></header>
    {error && <p className="growth-error">{error}</p>}
    <div className="campaign-layout"><form className="growth-card campaign-form" onSubmit={event => { event.preventDefault(); save("draft"); }}>
      <h2>Message</h2>
      <label>Campaign title<input value={form.title} onChange={event => change("title", event.target.value)} placeholder="Diwali special offer" /></label>
      <label>Message type<select value={form.messageType} onChange={event => change("messageType", event.target.value)}><option>OFFER</option><option>FESTIVAL</option><option>BIRTHDAY</option><option>GENERAL</option></select></label>
      <label>Festival name (optional)<input value={form.festivalName} onChange={event => change("festivalName", event.target.value)} placeholder="Diwali 2026" /></label>
      <label>Email subject<input value={form.subject} onChange={event => change("subject", event.target.value)} placeholder="Your ShivHub offer is here" /></label>
      <label>Message content<textarea rows="6" value={form.messageContent} onChange={event => change("messageContent", event.target.value)} placeholder="Write the message customers will receive" /></label>
      <label>Banner image upload<input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingBanner} onChange={uploadBanner} /><small>{uploadingBanner ? "Uploading banner…" : form.bannerUrl ? "Banner uploaded" : "JPG, PNG or WEBP, maximum 5 MB"}</small></label>
      <label>Or banner image URL<input value={form.bannerUrl} onChange={event => change("bannerUrl", event.target.value)} placeholder="https://…" /></label>
      {(localBannerPreview || form.bannerUrl) && <figure className="campaign-image-preview"><img src={localBannerPreview || form.bannerUrl} alt="Campaign banner preview" /><figcaption>Image preview — this is what will be used in the campaign.</figcaption></figure>}
      <label>Coupon code (optional)<input value={form.couponCode} onChange={event => change("couponCode", event.target.value)} /></label>
      <label>Offer details (optional)<textarea rows="2" value={form.offerDetails} onChange={event => change("offerDetails", event.target.value)} placeholder="Terms, discount details or coupon instructions" /></label>
      <p className="form-hint">Email delivery is queued in the background and tracked per recipient.</p>
      <h2>Target audience</h2>
      <label>Recipient audience<select value={form.targetAudience} onChange={event => change("targetAudience", event.target.value)}><option value="ALL_CUSTOMERS">All customers</option><option value="SHIVHUB_ONLINE_CUSTOMERS">ShivHub online customers</option><option value="SELLER_CUSTOMERS">Seller customers</option><option value="OFFLINE_REGISTERED_CUSTOMERS">Offline registered customers</option><option value="WALK_IN_CUSTOMERS_WITH_EMAIL">Walk-in customers with email</option><option value="CUSTOMER_BY_SELLER">Customer by seller</option><option value="CUSTOMER_BY_CITY">Customer by city</option><option value="CUSTOMER_BY_CATEGORY_PURCHASE">Customer by category purchase</option><option value="ACTIVE_CUSTOMERS">Active customers</option><option value="INACTIVE_CUSTOMERS">Inactive customers</option><option value="HIGH_VALUE_CUSTOMERS">High-value customers</option></select></label>
      <fieldset className="seller-targeting"><legend>Selected sellers (optional)</legend><p>Leave blank for all ShivHub customers. Select one or more shops for seller-specific campaigns.</p><div>{sellers.map(seller => <label key={seller.id}><input type="checkbox" checked={form.sellerIds.includes(seller.id)} onChange={() => toggleSeller(seller.id)} /> {seller.shopName || seller.ownerName}</label>)}{!sellers.length && <small>No seller options available.</small>}</div></fieldset>
      <div className="form-grid"><label>City / address<input value={form.city} onChange={event => change("city", event.target.value)} placeholder="Pune" /></label><label>Inactive for days<input type="number" min="1" value={form.inactiveDays} onChange={event => change("inactiveDays", event.target.value)} placeholder="30, 60 or 90" /></label><label>Minimum purchase ₹<input type="number" min="0" value={form.minimumPurchaseAmount} onChange={event => change("minimumPurchaseAmount", event.target.value)} /></label><label>Product ID purchased<input type="number" min="1" value={form.productId} onChange={event => change("productId", event.target.value)} /></label><label>Product category purchased<input value={form.productCategory} onChange={event => change("productCategory", event.target.value)} placeholder="Mobiles" /></label></div>
      <h2>Offer validity & delivery</h2>
      <div className="form-grid"><label>Start<input type="datetime-local" value={form.validityStartsAt} onChange={event => change("validityStartsAt", event.target.value)} /></label><label>End<input type="datetime-local" value={form.validityEndsAt} onChange={event => change("validityEndsAt", event.target.value)} /></label><label>Schedule later<input type="datetime-local" value={form.scheduledAt} onChange={event => change("scheduledAt", event.target.value)} /></label></div>
      <div className="form-actions"><button type="button" className="secondary-button" disabled={busy || uploadingBanner} onClick={previewAudience}>Preview audience</button><button className="secondary-button" disabled={busy || uploadingBanner}>Save draft</button><button type="button" className="secondary-button" disabled={busy || uploadingBanner} onClick={() => save("schedule")}>Schedule</button><button type="button" className="primary-button" disabled={busy || uploadingBanner} onClick={() => save("send")}>{busy ? "Working…" : "Send now"}</button></div>
    </form><aside className="growth-card audience-preview"><p className="eyebrow">Audience preview</p><strong>{preview?.count ?? "—"}</strong><span>eligible customers</span><p>Preview before sending to verify seller targeting, communication consent, and customer types.</p>{preview?.recipients?.map(recipient => <div key={recipient.id}><b>{recipient.name}</b><small>{recipient.email || "No email"} · {recipient.type}</small></div>)}</aside></div>
  </main>;
}
