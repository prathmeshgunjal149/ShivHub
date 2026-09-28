import { useEffect, useState } from "react";
import { ArrowRight, Copy, Gift, MessageCircle, PackagePlus, Recycle, UsersRound } from "lucide-react";
import { getActiveOffers } from "../../../services/marketingService";
import { getMyReferral } from "../../../services/referralService";
import { API_BASE_URL } from "../../../services/api";

const imageUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;

/** Dashboard-only surfaces backed by active admin BANNER records and the real referral API. */
export default function CustomerJourneyPromos({ onMarket, onReferral }) {
    const [assets, setAssets] = useState([]);
    const [referral, setReferral] = useState(null);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        let live = true;
        Promise.all([getActiveOffers("CUSTOMER"), getMyReferral().catch(() => null)])
            .then(([campaigns, details]) => { if (live) { setAssets(Array.isArray(campaigns) ? campaigns : []); setReferral(details); } })
            .catch(() => { if (live) setAssets([]); });
        return () => { live = false; };
    }, []);

    const banner = placement => assets.find(item => item.type === "BANNER" && item.placement === placement);
    const market = banner("CUSTOMER_SECOND_HAND");
    const refer = banner("CUSTOMER_REFER_EARN");
    const copy = async () => {
        if (!referral?.referralCode) return;
        try { await navigator.clipboard.writeText(referral.referralCode); setCopied(true); window.setTimeout(() => setCopied(false), 1600); } catch { /* browser blocks clipboard: referral page still provides the code */ }
    };

    return <section className="customer-journey-promos" aria-label="Customer marketplace shortcuts">
        <article className="journey-promo journey-market">
            {market?.bannerUrl ? <img src={imageUrl(market.bannerUrl)} alt="" className="journey-promo-art" loading="lazy" onError={event => { event.currentTarget.style.display = "none"; }} /> : <Recycle className="journey-promo-fallback" aria-hidden="true" />}
            <div className="journey-promo-copy"><p><Recycle size={15} /> SECOND-HAND MARKET</p><h2>{market?.title || "Buy or sell pre-owned products near you"}</h2><span>{market?.description || "List mobiles, vehicles, furniture, property rentals and more. Every listing is reviewed before it becomes public."}</span><div><button type="button" onClick={onMarket}><PackagePlus size={17} /> Open market <ArrowRight size={16} /></button><button type="button" className="journey-secondary" onClick={onMarket}>List your item</button></div></div>
        </article>
        <article className="journey-promo journey-refer">
            {refer?.bannerUrl ? <img src={imageUrl(refer.bannerUrl)} alt="" className="journey-promo-art" loading="lazy" onError={event => { event.currentTarget.style.display = "none"; }} /> : <Gift className="journey-promo-fallback" aria-hidden="true" />}
            <div className="journey-promo-copy"><p><Gift size={15} /> REFER &amp; EARN</p><h2>{refer?.title || "Invite a new customer and earn rewards"}</h2><span>{refer?.description || "Share your personal referral link. Rewards unlock after an eligible first order."}</span>{referral?.referralCode && <button type="button" className="journey-code" onClick={copy} aria-label="Copy referral code"><span>Your referral code<strong>{referral.referralCode}</strong></span><Copy size={18} />{copied && <small>Copied</small>}</button>}<div><button type="button" onClick={onReferral}><UsersRound size={17} /> Refer a friend <ArrowRight size={16} /></button><button type="button" className="journey-secondary" onClick={onReferral}><MessageCircle size={17} /> Share invitation</button></div></div>
        </article>
    </section>;
}
