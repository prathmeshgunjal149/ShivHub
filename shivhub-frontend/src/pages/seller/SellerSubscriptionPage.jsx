import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import RazorpayCheckout from "../../components/payments/RazorpayCheckout";
import { createSubscriptionCheckout, getAvailableSubscriptionPlans, getSellerSubscription, getSellerSubscriptionPayments, verifySubscriptionCheckout } from "../../services/subscriptionService";
import "./SellerSubscriptionPage.css";

const date = value => value ? new Date(`${value}T00:00:00`).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—";
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const interval = plan => !plan ? "—" : `${plan.billingIntervalCount > 1 ? `${plan.billingIntervalCount} ` : ""}${String(plan.billingInterval || "MONTH").replace("_", " ").toLowerCase()}`;

export default function SellerSubscriptionPage() {
    const [subscription, setSubscription] = useState(null);
    const [plans, setPlans] = useState([]);
    const [payments, setPayments] = useState([]);
    const [checkout, setCheckout] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const load = useCallback(async () => {
        setLoading(true); setError("");
        try {
            const [subscriptionData, plansData, paymentData] = await Promise.all([getSellerSubscription(), getAvailableSubscriptionPlans(), getSellerSubscriptionPayments()]);
            setSubscription(subscriptionData); setPlans(Array.isArray(plansData) ? plansData : []); setPayments(Array.isArray(paymentData) ? paymentData : []);
        } catch (requestError) { setError(requestError.response?.data?.message || "Unable to load subscription details."); }
        finally { setLoading(false); }
    }, []);
    useEffect(() => { void load(); }, [load]);

    const statusClass = String(subscription?.status || "").toLowerCase().replace(/_/g, "-");
    const canRenew = ["EXPIRED", "PAST_DUE", "PAYMENT_PENDING", "CANCELLED"].includes(subscription?.status);
    const enabledCount = useMemo(() => Object.values(subscription?.features || {}).filter(Boolean).length, [subscription]);

    const choosePlan = async plan => {
        setError(""); setNotice("");
        try {
            const created = await createSubscriptionCheckout(plan.id);
            setCheckout({ ...created, customerName: created.sellerName, customerEmail: created.sellerEmail, customerMobile: created.sellerMobile });
        } catch (requestError) { setError(requestError.response?.data?.message || "Unable to start secure subscription payment."); }
    };
    const paymentSuccess = async payload => {
        try {
            await verifySubscriptionCheckout({ subscriptionPaymentId: payload.internalOrderId, razorpayOrderId: payload.razorpayOrderId, razorpayPaymentId: payload.razorpayPaymentId, razorpaySignature: payload.razorpaySignature });
            setCheckout(null); setNotice("Payment verified. Your subscription access has been updated."); await load();
        } catch (requestError) { setCheckout(null); setError(requestError.response?.data?.message || requestError.message || "Payment could not be verified."); }
    };

    return <main className="seller-subscription-page">
        {checkout && <RazorpayCheckout payment={checkout} internalId={checkout.subscriptionPaymentId} onSuccess={paymentSuccess} onDismiss={() => setCheckout(null)} onError={requestError => { setCheckout(null); setError(requestError?.message || "Payment was not completed."); }} />}
        <header className="subscription-header"><div><p className="subscription-eyebrow">SELLER SETTINGS</p><h1>Subscription & plans</h1><p>Manage your ShivHub business access securely. Your shop data and order history remain safe even if a plan expires.</p></div><Link to="/seller/settings">Shop settings</Link></header>
        {error && <p className="subscription-message error">{error}</p>}{notice && <p className="subscription-message success">{notice}</p>}
        {loading ? <section className="subscription-loading">Loading your subscription…</section> : <>
            {subscription?.status === "EXPIRED" && <section className="subscription-expired-banner"><div><strong>Your ShivHub subscription has expired.</strong><p>Existing eligible mobile stock can still receive online orders. Offline POS and premium business tools are locked until renewal.</p></div><a href="#plans">Renew subscription</a></section>}
            <section className="subscription-current-card">
                <div><p className="subscription-eyebrow">CURRENT ACCESS</p><h2>{subscription?.currentPlan?.name || "ShivHub free trial"}</h2><span className={`subscription-status ${statusClass}`}>{String(subscription?.status || "LEGACY_ACCESS").replace(/_/g, " ")}</span>{subscription?.status === "TRIAL" && subscription?.currentPlan && <p className="subscription-after-trial">After trial: {money(subscription.currentPlan.price)} / {interval(subscription.currentPlan)}</p>}</div>
                <div className="subscription-days"><strong>{subscription?.daysRemaining || 0}</strong><span>days remaining</span></div>
                <dl><div><dt>Trial started</dt><dd>{date(subscription?.trialStartDate)}</dd></div><div><dt>Trial ends</dt><dd>{date(subscription?.trialEndDate)}</dd></div><div><dt>Plan period</dt><dd>{date(subscription?.subscriptionStartDate)} – {date(subscription?.subscriptionEndDate)}</dd></div><div><dt>Next billing</dt><dd>{date(subscription?.nextBillingDate)}</dd></div><div><dt>Auto renew</dt><dd>{subscription?.autoRenewEnabled ? "Enabled" : "Manual renewal"}</dd></div><div><dt>Enabled modules</dt><dd>{enabledCount}</dd></div></dl>
                {subscription?.legacyAccess && <p className="subscription-legacy-note">This is an existing seller account. Access remains protected while the administrator reviews or assigns its first subscription record.</p>}
            </section>
            {(canRenew || subscription?.status === "TRIAL") && <section className="subscription-reminder"><strong>{subscription?.status === "TRIAL" ? "Your free trial includes full seller access." : "Renew now to unlock all subscribed business tools."}</strong><a href="#plans">View plans</a></section>}
            <section id="plans" className="subscription-plan-section"><div className="subscription-section-heading"><div><p className="subscription-eyebrow">AVAILABLE PLANS</p><h2>Choose the plan that suits your shop</h2></div><span>Prices and modules are controlled by ShivHub Admin</span></div><div className="subscription-plans">{plans.length ? plans.map(plan => <article className={`subscription-plan ${plan.recommended ? "recommended" : ""}`} key={plan.id}>{plan.recommended && <b>Recommended</b>}<h3>{plan.name}</h3><p className="subscription-price">{money(plan.price)} <small>/ {interval(plan)}</small></p><p>{plan.description || "Seller business tools configured by ShivHub Admin."}</p><ul>{(plan.featureCodes || []).slice(0, 8).map(code => <li key={code}>{code.replaceAll("_", " ")}</li>)}</ul><button type="button" onClick={() => choosePlan(plan)} disabled={checkout || (subscription?.currentPlan?.id === plan.id && subscription?.status === "ACTIVE")}>{subscription?.currentPlan?.id === plan.id && subscription?.status === "ACTIVE" ? "Current plan" : "Choose plan"}</button></article>) : <p className="subscription-empty">No subscription plans are available yet. Please contact ShivHub support.</p>}</div></section>
            <section className="subscription-payment-history"><div className="subscription-section-heading"><div><p className="subscription-eyebrow">PAYMENTS</p><h2>Subscription payment history</h2></div></div><div className="subscription-table-wrap"><table><thead><tr><th>Date</th><th>Plan</th><th>Amount</th><th>Method</th><th>Status</th><th>Period</th></tr></thead><tbody>{payments.length ? payments.map(payment => <tr key={payment.id}><td>{payment.paidAt ? new Date(payment.paidAt).toLocaleDateString("en-IN") : new Date(payment.createdAt).toLocaleDateString("en-IN")}</td><td>{payment.planName}</td><td>{money(payment.amount)}</td><td>{payment.paymentMethod || "—"}</td><td><span className={`payment-status ${String(payment.paymentStatus).toLowerCase()}`}>{payment.paymentStatus}</span></td><td>{date(payment.billingPeriodStart)} – {date(payment.billingPeriodEnd)}</td></tr>) : <tr><td colSpan="6">No subscription payments yet.</td></tr>}</tbody></table></div></section>
        </>}
    </main>;
}
