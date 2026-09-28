import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getMyReferral, sendReferralInvite } from "../../services/referralService";
import "./Referral.css";

const formatDate = value => value ? new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—";

export default function Referral() {
    const [data, setData] = useState(null);
    const [message, setMessage] = useState("");
    const [email, setEmail] = useState("");
    const [sending, setSending] = useState(false);
    const [copied, setCopied] = useState(false);

    const load = useCallback(async () => {
        try {
            setData(await getMyReferral());
        } catch (error) {
            setMessage(error.response?.data?.message || "Referral details could not be loaded.");
        }
    }, []);

    useEffect(() => { void load(); }, [load]);

    const referralUrl = useMemo(() => data?.referralLink ? `${window.location.origin}${data.referralLink}` : "", [data]);

    const copyLink = async () => {
        if (!referralUrl) return;
        try {
            await navigator.clipboard.writeText(referralUrl);
            setCopied(true);
            window.setTimeout(() => setCopied(false), 1800);
        } catch {
            setMessage("Copy is unavailable in this browser. You can share your referral code instead.");
        }
    };

    const invite = async event => {
        event.preventDefault();
        try {
            setSending(true);
            setMessage("");
            await sendReferralInvite(email.trim());
            setEmail("");
            setMessage("Invitation sent. Your friend will receive a one-time, email-bound referral code.");
            await load();
        } catch (error) {
            setMessage(error.response?.data?.message || "Invitation could not be sent.");
        } finally {
            setSending(false);
        }
    };

    if (!data) return <main className="referral-page referral-page-loading"><p>{message || "Loading your rewards…"}</p></main>;

    const referrals = Array.isArray(data.referrals) ? data.referrals : [];
    return <main className="referral-page">
        <header className="referral-topbar"><Link to="/customer/dashboard">← Marketplace</Link><span>SHIVHUB REWARDS</span></header>
        <section className="referral-hero">
            <div className="referral-hero-copy"><p>REFER &amp; EARN</p><h1>Share ShivHub. Get rewarded together.</h1><span>Invite a new customer. When their first eligible order is delivered, you receive a ₹500 reward coupon.</span><div className="referral-hero-pills"><b>₹500 reward</b><b>One-time code</b><b>Email protected</b></div></div>
            <div className="referral-reward-orb"><small>REWARDED</small><strong>{data.successfulReferrals || 0}</strong><span>friends</span></div>
        </section>
        <section className="referral-stat-grid" aria-label="Referral progress"><article><span>01</span><strong>{data.referralCode || "—"}</strong><small>Your unique referral code</small></article><article><span>02</span><strong>{referrals.length}</strong><small>Total referred customers</small></article><article><span>03</span><strong>{data.successfulReferrals || 0}</strong><small>Rewards unlocked</small></article></section>
        <section className="referral-share-grid">
            <article className="referral-code-card"><p>YOUR SHARE LINK</p><strong>{data.referralCode || "Referral code pending"}</strong><span>Share a secure registration link or invite by email.</span><div><input readOnly value={referralUrl} aria-label="Your referral link" /><button type="button" onClick={copyLink}>{copied ? "Copied" : "Copy link"}</button></div></article>
            <article className="referral-invite-card"><p>EMAIL INVITATION</p><h2>Invite a friend personally</h2><form onSubmit={invite}><label>Friend&apos;s email<input required type="email" value={email} onChange={event => setEmail(event.target.value)} placeholder="friend@example.com" /></label><button disabled={sending}>{sending ? "Sending…" : "Send invitation"} <span>→</span></button></form><small>The code is bound to this email and can be used once only.</small></article>
        </section>
        {message && <p className="referral-message" role="status">{message}</p>}
        <section className="referral-how-it-works"><p>HOW IT WORKS</p><div><article><span>1</span><strong>Invite</strong><small>Send a protected link or email invitation.</small></article><article><span>2</span><strong>Register</strong><small>Your friend signs up using their assigned code.</small></article><article><span>3</span><strong>Unlock</strong><small>Eligible delivered orders unlock the reward.</small></article></div></section>
        <section className="referral-history"><div className="referral-history-heading"><div><p>YOUR NETWORK</p><h2>Referral activity</h2></div><span>{referrals.length} total</span></div>{referrals.length ? <div className="referral-list">{referrals.map((item, index) => { const rewarded = item.status === "REWARDED"; return <article key={`${item.customerName}-${item.createdAt}-${index}`}><span className="referral-person-mark">{String(item.customerName || "F").charAt(0).toUpperCase()}</span><div><strong>{item.customerName || "Customer"}</strong><small>Referred {formatDate(item.createdAt)}</small></div><div className={rewarded ? "referral-status rewarded" : "referral-status"}><b>{rewarded ? "Reward unlocked" : "In progress"}</b><small>{rewarded ? item.rewardCouponCode || "Reward coupon issued" : "Waiting for first eligible delivered order"}</small></div></article>; })}</div> : <div className="referral-empty"><span>✦</span><strong>Your first reward is one invite away.</strong><small>Send a personal email invitation to get started.</small></div>}</section>
    </main>;
}
