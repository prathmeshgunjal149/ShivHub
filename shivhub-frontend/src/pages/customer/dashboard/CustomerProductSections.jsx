import { useEffect, useMemo, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { getProductImage } from "./helpers";

const text = value => typeof value === "string" ? value.trim() : value?.name || value?.title || "";
const categoryName = product => text(product.categoryEntity) || text(product.category) || text(product.subCategory) || "Other products";
const price = product => Number(product.finalPrice ?? product.finalSellingPrice ?? product.price ?? 0);

function ProductRail({ eyebrow, title, products, category, onOpenProduct, onViewAll, t }) {
    const rail = useRef(null);
    const [paused, setPaused] = useState(false);

    const move = direction => {
        const element = rail.current;
        if (!element) return;
        const end = element.scrollLeft + element.clientWidth >= element.scrollWidth - 6;
        if (direction > 0 && end) element.scrollTo({ left: 0, behavior: "smooth" });
        else element.scrollBy({ left: Math.max(230, element.clientWidth * .42) * direction, behavior: "smooth" });
    };

    useEffect(() => {
        if (paused || products.length < 2) return undefined;
        const timer = window.setInterval(() => move(1), 4300);
        return () => window.clearInterval(timer);
    }, [paused, products.length]);

    if (!products.length) return null;

    return (
        <section className="customer-product-rail-section" aria-label={title}>
            <div className="customer-product-rail-heading"><div><span>{eyebrow}</span><h2>{title}</h2></div><button type="button" onClick={onViewAll}>{t("viewAll")} <ChevronRight size={15} aria-hidden="true" /></button></div>
            <div className="customer-product-rail-wrap" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)} onFocus={() => setPaused(true)} onBlur={() => setPaused(false)}>
                <button type="button" className="customer-product-rail-arrow previous" onClick={() => move(-1)} aria-label={`Previous ${title} products`}><ChevronLeft size={18} /></button>
                <div className="customer-product-rail" ref={rail}>
                    {products.map(product => {
                        const original = Number(product.price || 0);
                        const finalPrice = price(product);
                        const discount = Number(product.offerPercentage || 0);
                        const inStock = Number(product.availableStock ?? product.stock ?? 0) > 0;
                        const image = getProductImage(product);
                        return <article className="customer-rail-product-card" key={product.id}>
                            <button type="button" className="customer-rail-image" onClick={() => onOpenProduct(product.id)} aria-label={`View ${product.name}`}>
                                {discount > 0 && <span className="customer-rail-offer">{discount}% OFF</span>}
                                {image ? <img src={image} alt={product.name} /> : <span className="customer-rail-image-placeholder">No image</span>}
                            </button>
                            <div><span>{text(product.brand) || category}</span><h3>{product.name || "Product"}</h3>{original > finalPrice && <small className="customer-rail-mrp">MRP ₹{original.toLocaleString("en-IN")}</small>}<strong>₹{finalPrice.toLocaleString("en-IN")}</strong><small className={inStock ? "customer-rail-stock" : "customer-rail-stock sold-out"}>{inStock ? t("inStock") : t("outOfStock")}</small><button type="button" onClick={() => onOpenProduct(product.id)}>{t("viewProduct")}</button></div>
                        </article>;
                    })}
                </div>
                <button type="button" className="customer-product-rail-arrow next" onClick={() => move(1)} aria-label={`Next ${title} products`}><ChevronRight size={18} /></button>
            </div>
        </section>
    );
}

/** Real catalogue-only trending and category rows. No sales or popularity is fabricated. */
export default function CustomerProductSections({
    products,
    categories,
    onOpenProduct,
    onSelectCategory,
    t = value => value,
    showTrending = true,
    showCategoryPicks = true
}) {
    const trending = useMemo(() => [...products]
        .sort((left, right) => Number(right.offerPercentage || 0) - Number(left.offerPercentage || 0) || Number(right.id || 0) - Number(left.id || 0))
        .slice(0, 10), [products]);

    const groups = useMemo(() => {
        const map = new Map();
        products.forEach(product => {
            const name = categoryName(product);
            const group = map.get(name) || [];
            group.push(product);
            map.set(name, group);
        });
        return [...map.entries()]
            .sort((left, right) => right[1].length - left[1].length || left[0].localeCompare(right[0]))
            .slice(0, 6);
    }, [products]);

    const categoryIcon = name => categories.find(category => category.name.toLowerCase() === name.toLowerCase())?.icon || "✦";

    if (!products.length) return null;

    return <div className="customer-catalogue-sliders">
        {showTrending && <ProductRail eyebrow={t("trendingEyebrow")} title={t("trendingTitle")} products={trending} category={t("trendingTitle")} onOpenProduct={onOpenProduct} onViewAll={() => onSelectCategory("All")} t={t} />}
        {showCategoryPicks && groups.map(([name, entries]) => <ProductRail key={name} eyebrow={`${categoryIcon(name)} ${name.toUpperCase()}`} title={`${name} ${t("productPicks")}`} products={entries.slice(0, 10)} category={name} onOpenProduct={onOpenProduct} onViewAll={() => onSelectCategory(name)} t={t} />)}
    </div>;
}
