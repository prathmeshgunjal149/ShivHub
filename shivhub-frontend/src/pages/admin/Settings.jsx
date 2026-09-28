import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { websiteContentApi } from "../../services/websiteContentApi";
import "./AdminTools.css";
import "./WebsiteSettings.css";

const settingFields = [
  ["brandName", "Brand"],
  ["legalBusinessName", "Legal business name"],
  ["supportEmail", "Support email"],
  ["supportPhone", "Phone / placeholder"],
  ["businessAddress", "Address", "textarea"],
  ["supportHours", "Support hours"],
  ["grievanceContactName", "Grievance contact"],
  ["grievanceEmail", "Grievance email"],
  ["websiteUrl", "Website URL"],
  ["instagramUrl", "Instagram URL"],
  ["facebookUrl", "Facebook URL"],
  ["whatsappUrl", "WhatsApp URL"],
  ["youtubeUrl", "YouTube URL"],
  ["enabledPaymentMethods", "Enabled payment methods comma separated"]
];

const tabs = [
  ["business", "Business Information"],
  ["policies", "Policy Pages"],
  ["faqs", "FAQ Management"],
  ["enquiries", "Enquiries / Complaints"]
];

const groups = [
  ["Business Information", ["brandName", "legalBusinessName", "businessAddress"]],
  ["Contact & Support", ["supportEmail", "supportPhone", "supportHours", "grievanceContactName", "grievanceEmail"]],
  ["Social Links", ["websiteUrl", "instagramUrl", "facebookUrl", "whatsappUrl", "youtubeUrl"]],
  ["Footer Configuration", ["enabledPaymentMethods"]]
];

const fieldMap = Object.fromEntries(settingFields.map(field => [field[0], field]));

export default function Settings() {
  const [tab, setTab] = useState("business");
  const [settings, setSettings] = useState(null);
  const [policies, setPolicies] = useState([]);
  const [selectedSlug, setSelectedSlug] = useState("terms");
  const [policy, setPolicy] = useState(null);
  const [versions, setVersions] = useState([]);
  const [faqs, setFaqs] = useState([]);
  const [newFaq, setNewFaq] = useState({ category: "General", question: "", answer: "", active: true, displayOrder: 0, demoData: true });
  const [enquiries, setEnquiries] = useState([]);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const missingBusiness = useMemo(() => {
    if (!settings) return [];
    return ["brandName", "legalBusinessName", "supportEmail", "businessAddress"].filter(key => !String(settings[key] || "").trim());
  }, [settings]);

  const loadAll = async () => {
    setError("");
    try {
      const [settingsData, policyList, faqList, enquiryList] = await Promise.all([
        websiteContentApi.adminSettings(),
        websiteContentApi.adminPolicies(),
        websiteContentApi.adminFaqs(),
        websiteContentApi.enquiries()
      ]);
      setSettings(settingsData);
      setPolicies(policyList);
      setFaqs(faqList);
      setEnquiries(enquiryList);
    } catch (err) {
      setError(err.response?.data?.message || "Could not load website settings.");
    }
  };

  const loadPolicy = async slug => {
    setSelectedSlug(slug);
    const [policyData, versionData] = await Promise.all([
      websiteContentApi.adminPolicy(slug),
      websiteContentApi.policyVersions(slug)
    ]);
    setPolicy({
      ...policyData,
      editorContent: policyData.draftContent || policyData.content || "",
      confirmProductionReady: false
    });
    setVersions(versionData);
  };

  useEffect(() => { void loadAll(); }, []);
  useEffect(() => { void loadPolicy(selectedSlug).catch(() => {}); }, [selectedSlug]);

  const saveSettings = async event => {
    event.preventDefault();
    try {
      const saved = await websiteContentApi.updateAdminSettings(settings);
      setSettings(saved);
      setNotice("Business information saved to database.");
    } catch (err) {
      setError(err.response?.data?.message || "Could not save settings.");
    }
  };

  const savePolicy = async (publish = false) => {
    try {
      const payload = {
        title: policy.title,
        description: policy.description,
        content: policy.editorContent,
        effectiveDate: policy.effectiveDate,
        demoData: policy.demoData,
        requiresReview: policy.requiresReview,
        confirmProductionReady: policy.confirmProductionReady
      };
      const saved = publish
        ? await websiteContentApi.publishPolicy(policy.slug, payload)
        : await websiteContentApi.savePolicyDraft(policy.slug, payload);
      setNotice(publish ? "Policy published. Public page updated." : "Draft saved. Public page still shows published version.");
      await loadPolicy(saved.slug);
      const list = await websiteContentApi.adminPolicies();
      setPolicies(list);
    } catch (err) {
      setError(err.response?.data?.message || "Could not save policy.");
    }
  };

  const addFaq = async event => {
    event.preventDefault();
    try {
      await websiteContentApi.addFaq(newFaq);
      setNewFaq({ category: "General", question: "", answer: "", active: true, displayOrder: 0, demoData: true });
      setFaqs(await websiteContentApi.adminFaqs());
    } catch (err) {
      setError(err.response?.data?.message || "Could not add FAQ.");
    }
  };

  const updateEnquiry = async (id, status, internalNotes, customerResponse) => {
    try {
      await websiteContentApi.updateEnquiry(id, { status, internalNotes, customerResponse });
      setEnquiries(await websiteContentApi.enquiries());
      setNotice("Customer enquiry updated. A customer-safe action update is emailed when status or response changes.");
    } catch (err) {
      setError(err.response?.data?.message || "Could not update the enquiry.");
    }
  };

  return (
    <main className="admin-tools website-settings">
      <header>
        <div>
          <p className="eyebrow">WEBSITE SETTINGS</p>
          <h1>Footer, policies & support</h1>
          <p>Manage public-safe business information, policy pages, FAQs and customer enquiries from database.</p>
        </div>
        <div className="settings-shortcut-links">
          <Link className="settings-delivery-link" to="/admin/delivery-rules">
            Delivery time & distance
          </Link>
          <Link className="settings-delivery-link" to="/admin/birthday-settings">
            Birthday greetings
          </Link>
          <Link className="settings-delivery-link" to="/admin/notifications">
            WhatsApp operations
          </Link>
          <Link className="settings-delivery-link" to="/admin/finance">Finance / EMI</Link>
          <Link className="settings-delivery-link" to="/admin/subscriptions">
            Seller subscriptions
          </Link>
          <Link className="settings-delivery-link" to="/admin/marketing/banners">
            Customer banners &amp; images
          </Link>
        </div>
      </header>

      {notice && <p className="settings-notice">{notice}</p>}
      {error && <p className="settings-error">{error}</p>}
      {!!missingBusiness.length && <p className="settings-warning">Missing required business information: {missingBusiness.join(", ")}</p>}

      <nav className="website-tabs">
        {tabs.map(([value, label]) => (
          <button key={value} className={tab === value ? "active" : ""} onClick={() => setTab(value)}>{label}</button>
        ))}
      </nav>

      {tab === "business" && settings && (
        <form className="website-settings-grid" onSubmit={saveSettings}>
          <section className="website-card website-visual-assets-card">
            <p className="eyebrow">CUSTOMER VISUAL ASSETS</p>
            <h2>Home, category &amp; promotion banners</h2>
            <p>Upload, preview, activate and order the images shown to customers. Use category hero slider for rotating category banners and category launch / offer tile for the four cards below it.</p>
            <Link className="settings-delivery-link" to="/admin/marketing/banners">Open banner manager</Link>
          </section>
          <section className="website-card website-preview-card">
            <p className="eyebrow">LIVE FOOTER PREVIEW</p>
            <h2>{settings.brandName || "ShivHub"}</h2>
            <p>{settings.legalBusinessName}</p>
            <p>{settings.supportEmail}</p>
            <p>{settings.supportPhone}</p>
            <p>{settings.businessAddress}</p>
            {(settings.demoData || settings.requiresReview) && <strong className="demo-pill">DEMO / REQUIRES REVIEW</strong>}
            <button>Save business settings</button>
          </section>

          {groups.map(([title, names]) => (
            <section className="website-card" key={title}>
              <h2>{title}</h2>
              <div className="website-form-grid two">
                {names.map(name => {
                  const [, label, type] = fieldMap[name];
                  return (
                    <label key={name} className={type === "textarea" ? "wide" : ""}>{label}
                      {type === "textarea"
                        ? <textarea value={settings[name] || ""} onChange={e => setSettings({ ...settings, [name]: e.target.value })} />
                        : <input value={settings[name] || ""} onChange={e => setSettings({ ...settings, [name]: e.target.value })} />}
                    </label>
                  );
                })}
              </div>
            </section>
          ))}

          <section className="website-card">
            <h2>Demo / Review Status</h2>
            <div className="website-checks">
              <label><input type="checkbox" checked={settings.demoData} onChange={e => setSettings({ ...settings, demoData: e.target.checked })} /> Mark as demo data</label>
              <label><input type="checkbox" checked={settings.requiresReview} onChange={e => setSettings({ ...settings, requiresReview: e.target.checked })} /> Requires review</label>
            </div>
          </section>
        </form>
      )}

      {tab === "business" && !settings && <section className="website-card"><p>Loading website settings…</p></section>}

      {tab === "policies" && (
        <section className="website-editor-layout">
          <aside className="website-card">
            <h2>Policy Pages</h2>
            {policies.map(item => (
              <button key={item.slug} className={selectedSlug === item.slug ? "active policy-select" : "policy-select"} onClick={() => loadPolicy(item.slug)}>
                {item.title}
                {(item.demoData || item.requiresReview) && <span>Review</span>}
              </button>
            ))}
          </aside>
          {policy && (
            <article className="website-card">
              <h2>Edit policy</h2>
              <label>Title<input value={policy.title || ""} onChange={e => setPolicy({ ...policy, title: e.target.value })} /></label>
              <label>Description<input value={policy.description || ""} onChange={e => setPolicy({ ...policy, description: e.target.value })} /></label>
              <label>Effective date<input type="date" value={policy.effectiveDate || ""} onChange={e => setPolicy({ ...policy, effectiveDate: e.target.value })} /></label>
              <label>Content<textarea className="policy-editor" value={policy.editorContent || ""} onChange={e => setPolicy({ ...policy, editorContent: e.target.value })} /></label>
              <div className="website-checks">
                <label><input type="checkbox" checked={policy.demoData} onChange={e => setPolicy({ ...policy, demoData: e.target.checked })} /> Demo content</label>
                <label><input type="checkbox" checked={policy.requiresReview} onChange={e => setPolicy({ ...policy, requiresReview: e.target.checked })} /> Requires review</label>
                <label><input type="checkbox" checked={policy.confirmProductionReady} onChange={e => setPolicy({ ...policy, confirmProductionReady: e.target.checked })} /> I reviewed this before production</label>
              </div>
              <div className="website-actions">
                <button type="button" onClick={() => savePolicy(false)}>Save draft</button>
                <button type="button" onClick={() => savePolicy(true)}>Publish</button>
                <a href={`/${policy.slug}`} target="_blank" rel="noreferrer">Preview public page</a>
              </div>
              <h3>Version history</h3>
              <div className="version-list">
                {versions.map(version => (
                  <p key={version.id}>
                    <span>{version.action} · {new Date(version.createdAt).toLocaleString("en-IN")}</span>
                    <button type="button" onClick={async () => { await websiteContentApi.restorePolicyVersion(policy.slug, version.id); await loadPolicy(policy.slug); }}>Restore as draft</button>
                  </p>
                ))}
              </div>
            </article>
          )}
        </section>
      )}

      {tab === "faqs" && (
        <section className="website-card">
          <h2>FAQ Management</h2>
          <form className="website-form-grid" onSubmit={addFaq}>
            <label>Category<input value={newFaq.category} onChange={e => setNewFaq({ ...newFaq, category: e.target.value })} /></label>
            <label>Display order<input type="number" value={newFaq.displayOrder} onChange={e => setNewFaq({ ...newFaq, displayOrder: Number(e.target.value) })} /></label>
            <label className="wide">Question<input required value={newFaq.question} onChange={e => setNewFaq({ ...newFaq, question: e.target.value })} /></label>
            <label className="wide">Answer<textarea required value={newFaq.answer} onChange={e => setNewFaq({ ...newFaq, answer: e.target.value })} /></label>
            <button>Add FAQ</button>
          </form>
          {faqs.map(faq => <article className="mini-row" key={faq.id}><strong>{faq.question}</strong><span>{faq.category}</span><button onClick={async () => { await websiteContentApi.deleteFaq(faq.id); setFaqs(await websiteContentApi.adminFaqs()); }}>Delete</button></article>)}
        </section>
      )}

      {tab === "enquiries" && (
        <section className="website-card">
          <h2>Customer Enquiries / Complaints</h2>
          {enquiries.map(item => (
            <article className="enquiry-row" key={item.id}>
              <div><strong>{item.ticketReference} · {item.subject}</strong><p>{item.name} · {item.email} · {item.category}</p><p>{item.message}</p></div>
              <select value={item.status} onChange={e => updateEnquiry(item.id, e.target.value, item.internalNotes || "", item.customerResponse || "")}>
                <option>OPEN</option><option>IN_PROGRESS</option><option>RESOLVED</option>
              </select>
              <div className="enquiry-notes">
                <label>Customer update <textarea placeholder="Customer-safe action update — this is emailed" defaultValue={item.customerResponse || ""} onBlur={e => updateEnquiry(item.id, item.status, item.internalNotes || "", e.target.value)} /></label>
                <label>Internal notes <textarea placeholder="Never sent to customer" defaultValue={item.internalNotes || ""} onBlur={e => updateEnquiry(item.id, item.status, e.target.value, item.customerResponse || "")} /></label>
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
