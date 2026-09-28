import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { BookOpen, CalendarDays, ChevronRight, CircleHelp, Headphones, Mail, MapPin, MessageCircle, Phone, Search, Send, ShieldCheck } from "lucide-react";
import { websiteContentApi } from "../../services/websiteContentApi";
import { getActiveOffers } from "../../services/marketingService";
import { API_BASE_URL } from "../../services/api";
import useAuth from "../../hooks/useAuth";
import CustomerPublicHeader from "../../components/CustomerPublicHeader";
import "./PolicyPage.css";

const labels = {
    contact: "Contact & support",
    help: "Help centre",
    grievance: "Complaint escalation"
};

export default function PolicyPage({ slug }) {
    const { user, isAuthenticated } = useAuth();
    const [settings, setSettings] = useState(null);
    const [policy, setPolicy] = useState(null);
    const [faqs, setFaqs] = useState([]);
    const [form, setForm] = useState({ name: "", email: "", mobile: "", category: "General", subject: "", message: "", orderReference: "" });
    const [ticket, setTicket] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [faqQuery, setFaqQuery] = useState("");
    const [heroBanner, setHeroBanner] = useState(null);

    useEffect(() => {
        let active = true;
        setLoading(true);
        Promise.all([
            websiteContentApi.getSettings().catch(() => null),
            websiteContentApi.getPolicy(slug),
            slug === "help" ? websiteContentApi.getFaqs().catch(() => []) : Promise.resolve([])
        ]).then(([settingsData, policyData, faqData]) => {
            if (!active) return;
            setSettings(settingsData);
            setPolicy(policyData);
            setFaqs(faqData || []);
            document.title = `${policyData.title} | ShivHub`;
        }).catch(err => {
            if (active) setError(err.response?.data?.message || "Could not load this page.");
        }).finally(() => {
            if (active) setLoading(false);
        });
        return () => { active = false; };
    }, [slug]);

    useEffect(() => {
        let active = true;
        getActiveOffers("CUSTOMER").then(items => {
            if (!active) return;
            const all = Array.isArray(items) ? items : [];
            const match = all.find(item => item.type === "BANNER" && item.placement === "CUSTOMER_SUPPORT_HERO" && String(item.targetFilters || "").trim().toLowerCase() === slug.toLowerCase());
            setHeroBanner(match || null);
        }).catch(() => { if (active) setHeroBanner(null); });
        return () => { active = false; };
    }, [slug]);

    // A signed-in customer should never need to retype their verified account identity.
    // Guests continue to use the same public, validated support form.
    useEffect(() => {
        if (!isAuthenticated || !user) return;
        setForm(current => ({
            ...current,
            name: current.name || user.name || "",
            email: current.email || user.email || ""
        }));
    }, [isAuthenticated, user]);

    const sections = useMemo(() => String(policy?.content || "").split(/\n{2,}/).filter(Boolean), [policy]);
    const visibleFaqs = useMemo(() => {
        const text = faqQuery.trim().toLowerCase();
        return !text ? faqs : faqs.filter(faq => `${faq.question} ${faq.answer} ${faq.category}`.toLowerCase().includes(text));
    }, [faqs, faqQuery]);
    const isSupportPage = ["contact", "help", "grievance", "warranty-policy"].includes(slug);
    const heading = slug === "help" ? "Frequently asked questions" : slug === "contact" ? "Contact ShivHub support" : slug === "grievance" ? "Raise a complaint" : policy?.title || "Policy";

    const submit = async event => {
        event.preventDefault();
        setError("");
        setTicket("");
        try {
            const response = await websiteContentApi.submitEnquiry(form);
            setTicket(response.ticketReference);
            setForm({ name: "", email: "", mobile: "", category: "General", subject: "", message: "", orderReference: "" });
        } catch (err) {
            setError(err.response?.data?.message || "Could not submit your enquiry.");
        }
    };

    if (loading) return <main className="policy-page"><section className="policy-card"><p>Loading…</p></section></main>;

    return (
        <main className={`policy-page policy-page--${slug}`}>
            <CustomerPublicHeader />
            <section className={`policy-hero ${heroBanner?.bannerUrl ? "has-admin-banner" : ""}`} style={heroBanner?.bannerUrl ? { "--policy-hero-image": `url("${/^https?:\/\//i.test(heroBanner.bannerUrl) ? heroBanner.bannerUrl : `${API_BASE_URL}/${String(heroBanner.bannerUrl).replace(/^\/+/, "")}`}")` } : undefined}>
                <div>
                    <p>{labels[slug] || "ShivHub policy"}</p>
                    <h1>{heroBanner?.title || heading}</h1>
                    <span>{heroBanner?.description || policy?.description || "Clear information, dependable support and secure shopping with ShivHub."}</span>
                    <div className="policy-meta"><span><CalendarDays size={15} aria-hidden="true" /> Effective: {policy?.effectiveDate || "Not set"}</span><span><BookOpen size={15} aria-hidden="true" /> Updated: {policy?.lastPublishedAt ? new Date(policy.lastPublishedAt).toLocaleDateString("en-IN") : "Not published"}</span></div>
                </div>
                <div className="policy-hero-badge" aria-hidden="true">{isSupportPage ? <Headphones size={48} /> : <ShieldCheck size={48} />}</div>
                {(policy?.demoData || policy?.requiresReview || settings?.demoData) && <strong>DEMO / REQUIRES REVIEW — admin must replace or confirm before production launch.</strong>}
            </section>

            {error && <p className="policy-error">{error}</p>}

            <section className="policy-layout">
                <aside className="policy-side-nav">
                    <h3>On this page</h3>
                    {sections.map((section, index) => <a key={index} href={`#section-${index + 1}`}><span>{index + 1}</span>{section.split("\n")[0].slice(0, 34) || `Section ${index + 1}`}</a>)}
                    <div className="policy-side-help"><Headphones size={22} aria-hidden="true" /><strong>Need help?</strong><span>Our team is here to assist.</span>{settings?.supportPhone && <a href={`tel:${settings.supportPhone.replace(/\s/g, "")}`}><Phone size={14} />{settings.supportPhone}</a>}{settings?.supportEmail && <a href={`mailto:${settings.supportEmail}`}><Mail size={14} />{settings.supportEmail}</a>}<Link to="/contact">Contact support <ChevronRight size={15} /></Link></div>
                </aside>

                <article className="policy-card">
                    {slug === "help" && <div className="policy-faq-search"><Search size={18} aria-hidden="true" /><input value={faqQuery} onChange={event => setFaqQuery(event.target.value)} placeholder="Search order, return, payment, warranty…" aria-label="Search frequently asked questions" /></div>}
                    {sections.map((section, index) => (
                        <section className="policy-content-section" key={index} id={`section-${index + 1}`}>
                            <span className="policy-section-number">{index + 1}</span>
                            <div>{section.split("\n").map((line, lineIndex) => lineIndex === 0 ? <h2 key={lineIndex}>{line}</h2> : <p key={lineIndex}>{line}</p>)}</div>
                        </section>
                    ))}

                    {slug === "help" && !!faqs.length && (
                        <section className="faq-list">
                            <h2><CircleHelp size={22} aria-hidden="true" /> Popular questions</h2>
                            {visibleFaqs.map(faq => (
                                <details key={faq.id}>
                                    <summary><span>{faq.question}</span><ChevronRight size={18} aria-hidden="true" /></summary>
                                    <p>{faq.answer}</p>
                                </details>
                            ))}
                            {!visibleFaqs.length && <p className="policy-no-results">No published FAQ matches this search.</p>}
                        </section>
                    )}

                    {(slug === "contact" || slug === "help" || slug === "grievance") && (
                        <form className="enquiry-form" onSubmit={submit}>
                            <h2><Send size={21} aria-hidden="true" /> Send an enquiry / complaint</h2>
                            {ticket && <p className="ticket-success">Your request is recorded as <strong>{ticket}</strong>. We will send an acknowledgement and update you by email when support takes action.</p>}
                            <div>
                                <label>Name<input required maxLength="120" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
                                <label>Email<input required type="email" maxLength="180" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
                                <label>Mobile optional<input maxLength="20" value={form.mobile} onChange={e => setForm({ ...form, mobile: e.target.value })} /></label>
                                <label>Category<select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })}><option>General</option><option>Order</option><option>Payment / Refund</option><option>Return / Replacement</option><option>Warranty</option><option>Seller issue</option><option>Privacy</option></select></label>
                                <label>Subject<input required maxLength="180" value={form.subject} onChange={e => setForm({ ...form, subject: e.target.value })} /></label>
                                <label>Order reference optional<input maxLength="80" value={form.orderReference} onChange={e => setForm({ ...form, orderReference: e.target.value })} /></label>
                            </div>
                            <label>Message<textarea required maxLength="5000" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} /></label>
                            <button><Send size={16} aria-hidden="true" /> Send to ShivHub Support</button>
                        </form>
                    )}
                </article>
                <aside className="policy-support-panel">
                    <div><Headphones size={27} aria-hidden="true" /><h3>Customer support</h3><p>{settings?.supportHours || "Support hours are not published yet."}</p></div>
                    {settings?.supportPhone && <a href={`tel:${settings.supportPhone.replace(/\s/g, "")}`}><Phone size={17} /> Call support</a>}
                    {settings?.supportEmail && <a href={`mailto:${settings.supportEmail}`}><Mail size={17} /> Email support</a>}
                    {settings?.businessAddress && <p><MapPin size={17} /> {settings.businessAddress}</p>}
                    <Link to="/help"><MessageCircle size={17} /> Browse help centre</Link>
                </aside>
            </section>
        </main>
    );
}
