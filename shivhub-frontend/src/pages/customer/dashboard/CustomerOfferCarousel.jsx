import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { getActiveOffers } from "../../../services/marketingService";
import { API_BASE_URL } from "../../../services/api";
const bannerSource = value => {
    if (!value || value.startsWith("http") || value.startsWith("/assets/") || value.startsWith("/src/")) return value || "";
    return `${API_BASE_URL}/${value.replace(/^\/+/, "")}`;
};

/** E-commerce offers are backed only by active admin campaigns. */
export default function CustomerOfferCarousel({ onExplore, t = value => value }) {
    const [offers, setOffers] = useState([]);
    const [activeIndex, setActiveIndex] = useState(0);
    const [paused, setPaused] = useState(false);

    useEffect(() => {
        let active = true;
        getActiveOffers("CUSTOMER")
            .then(data => { if (active) setOffers(Array.isArray(data) ? data : []); })
            .catch(() => { if (active) setOffers([]); });
        return () => { active = false; };
    }, []);

    const slides = useMemo(
        () => offers.filter(offer => offer?.id && offer?.title),
        [offers]
    );

    useEffect(() => {
        if (slides.length < 2 || paused) return undefined;
        const timer = window.setInterval(() => setActiveIndex(current => (current + 1) % slides.length), 5500);
        return () => window.clearInterval(timer);
    }, [slides.length, paused]);

    useEffect(() => { setActiveIndex(current => Math.min(current, Math.max(slides.length - 1, 0))); }, [slides.length]);

    const slide = slides[activeIndex];
    const offerLabel = useMemo(() => slide?.messageType || slide?.type || "OFFER", [slide]);

    const move = direction => setActiveIndex(current => (current + direction + slides.length) % slides.length);

    if (!slide) return null;

    return (
        <section
            className="customer-offer-carousel"
            aria-label={t("offers")}
            onMouseEnter={() => setPaused(true)}
            onMouseLeave={() => setPaused(false)}
            onFocus={() => setPaused(true)}
            onBlur={() => setPaused(false)}
        >
            <div className="customer-offer-carousel-track" style={{ transform: `translateX(-${activeIndex * 100}%)` }}>
                {slides.map((offer, index) => (
                    <article className="customer-offer-slide" key={offer.id} aria-hidden={index !== activeIndex}>
                        {offer.bannerUrl && <img src={bannerSource(offer.bannerUrl)} alt="" loading={index === activeIndex ? "eager" : "lazy"} onError={event => { event.currentTarget.style.display = "none"; }} />}
                        <div className="customer-offer-overlay" />
                        <div className="customer-offer-copy">
                            <span>{offer.messageType || offer.type || "SHIVHUB OFFER"}</span>
                            <h1>{offer.title}</h1>
                            {offer.description && <p>{offer.description}</p>}
                            <div>
                                {offer.discountPercent && <b>Up to {offer.discountPercent}% off</b>}
                                {offer.couponCode && <code>Code: {offer.couponCode}</code>}
                            </div>
                            <button type="button" onClick={onExplore} tabIndex={index === activeIndex ? 0 : -1}>{t("shopOffer")} <ChevronRight size={16} aria-hidden="true" /></button>
                        </div>
                    </article>
                ))}
            </div>
            {slides.length > 1 && <>
                <button type="button" className="customer-carousel-arrow customer-carousel-prev" onClick={() => move(-1)} aria-label="Previous offer"><ChevronLeft size={20} /></button>
                <button type="button" className="customer-carousel-arrow customer-carousel-next" onClick={() => move(1)} aria-label="Next offer"><ChevronRight size={20} /></button>
                <div className="customer-offer-dots">{slides.map((offer, index) => <button type="button" key={offer.id} className={index === activeIndex ? "active" : ""} onClick={() => setActiveIndex(index)} aria-label={`Show ${offer.title}`} />)}</div>
            </>}
            <span className="customer-offer-live">{offerLabel} · LIVE</span>
        </section>
    );
}
