import { useEffect, useMemo, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, PackageSearch } from "lucide-react";

const readable = value => typeof value === "string" ? value.trim() : value?.name || value?.title || "";
const categoryFor = product => readable(product.categoryEntity) || readable(product.category) || readable(product.subCategory);

/** A navigable category rail derived from the same live catalogue loaded by the dashboard. */
export default function CustomerCategoryCarousel({ categories, products, activeCategory, onSelect, t = value => value }) {
    const rail = useRef(null);
    const [paused, setPaused] = useState(false);
    const choices = useMemo(() => categories.filter(category => category.name !== "All"), [categories]);
    const counts = useMemo(() => new Map(choices.map(category => [category.name, products.filter(product => categoryFor(product).toLowerCase() === category.name.toLowerCase()).length])), [choices, products]);

    const scroll = direction => {
        const element = rail.current;
        if (!element) return;
        const amount = Math.max(210, Math.round(element.clientWidth * .5));
        const atEnd = element.scrollLeft + element.clientWidth >= element.scrollWidth - 6;
        if (direction > 0 && atEnd) element.scrollTo({ left: 0, behavior: "smooth" });
        else element.scrollBy({ left: amount * direction, behavior: "smooth" });
    };

    useEffect(() => {
        if (paused || choices.length < 2) return undefined;
        const timer = window.setInterval(() => scroll(1), 3800);
        return () => window.clearInterval(timer);
    }, [paused, choices.length]);

    if (!choices.length) return null;

    return (
        <section className="customer-category-showcase" aria-label={t("browseCategories")}>
            <div className="customer-category-showcase-heading"><div><span>{t("browseCategories")}</span><h2>{t("categoryTitle")}</h2></div><p>{t("autoMove")}</p></div>
            <div className="customer-category-rail-wrap" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)} onFocus={() => setPaused(true)} onBlur={() => setPaused(false)}>
                <button type="button" className="customer-category-rail-arrow previous" onClick={() => scroll(-1)} aria-label="Previous categories"><ChevronLeft size={18} /></button>
                <div className="customer-category-rail" ref={rail}>
                    {choices.map(category => <button type="button" key={category.name} className={activeCategory === category.name ? "customer-category-card active" : "customer-category-card"} onClick={() => onSelect(category.name)}><span className="customer-category-icon"><PackageSearch size={21} aria-hidden="true" /></span><strong>{category.name}</strong><small>{counts.get(category.name) || 0} {t("liveProductsLabel")}</small><i>{t("explore")} <ChevronRight size={14} aria-hidden="true" /></i></button>)}
                </div>
                <button type="button" className="customer-category-rail-arrow next" onClick={() => scroll(1)} aria-label="Next categories"><ChevronRight size={18} /></button>
            </div>
        </section>
    );
}
