import { useEffect, useState } from "react";
import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { getActiveOffers } from "../../../services/marketingService";
import { API_BASE_URL } from "../../../services/api";

const imageUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;

export default function CustomerHero({
    greeting,
    customerName,
    productCount,
    onExplore,
    t = value => value
}) {
    const [slides, setSlides] = useState([]);
    const [active, setActive] = useState(0);

    useEffect(() => {
        let live = true;
        getActiveOffers("CUSTOMER")
            .then(items => { if (live) setSlides((Array.isArray(items) ? items : []).filter(item => item.type === "BANNER" && item.placement === "CUSTOMER_HERO")); })
            .catch(() => { if (live) setSlides([]); });
        return () => { live = false; };
    }, []);
    useEffect(() => {
        if (slides.length < 2) return undefined;
        const timer = window.setInterval(() => setActive(current => (current + 1) % slides.length), 5200);
        return () => window.clearInterval(timer);
    }, [slides.length]);
    const slide = slides[active];
    const move = direction => setActive(current => (current + direction + slides.length) % slides.length);

    return (

        <section className="marketplace-hero" style={slide?.bannerUrl ? { "--hero-image": `url("${imageUrl(slide.bannerUrl)}")` } : undefined}>

            <div className="marketplace-hero-content">

                <span>
                    SHIVHUB MARKETPLACE
                </span>

                <h1>
                    {slide?.title || <>{greeting},{" "}{customerName}</>}
                    <br />
                    {slide?.description || t("discoverTitle")}
                </h1>

                <p>
                    {slide ? "Explore this active ShivHub customer offer." : t("heroDescription")}
                </p>

                <button
                    type="button"
                    className="marketplace-hero-button"
                    onClick={onExplore}
                >
                    {t("exploreProducts")} <ArrowRight size={17} aria-hidden="true" />
                </button>

            </div>


            <div className="marketplace-hero-count">

                <strong>
                    {productCount}
                </strong>

                <span>
                    {t("liveProducts")}
                </span>

            </div>
            {slides.length > 1 && <><button type="button" className="marketplace-hero-arrow previous" onClick={() => move(-1)} aria-label="Previous hero banner"><ChevronLeft size={21} /></button><button type="button" className="marketplace-hero-arrow next" onClick={() => move(1)} aria-label="Next hero banner"><ChevronRight size={21} /></button><div className="marketplace-hero-dots">{slides.map((item, index) => <button key={item.id} type="button" aria-label={`Show ${item.title}`} className={index === active ? "active" : ""} onClick={() => setActive(index)} />)}</div></>}

        </section>
    );
}
