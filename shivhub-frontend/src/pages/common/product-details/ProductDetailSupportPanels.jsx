import {
    BadgeCheck,
    Box,
    Check,
    Gem,
    PackageCheck,
    RotateCcw,
    ShieldCheck,
    Truck,
    Wrench
} from "lucide-react";
import { splitOptions } from "./productUtils";

const readSpecs = product => {
    try {
        const raw = product?.productSpecifications || product?.specificationDetails;
        const parsed = typeof raw === "string" && raw.trim().startsWith("{") ? JSON.parse(raw) : {};
        return parsed.specs && typeof parsed.specs === "object" ? parsed.specs : parsed;
    } catch {
        return {};
    }
};

const values = (...items) => [...new Set(items.flatMap(item => Array.isArray(item) ? item : splitOptions(item)))];

const highlightsFor = product => {
    const specs = readSpecs(product);
    const supplied = String(product?.shortHighlights || specs.highlights || specs.features || "")
        .split(/\r?\n|[|]/)
        .map(value => value.trim())
        .filter(Boolean);
    if (supplied.length) return supplied.slice(0, 6);
    return [product?.warrantyDetails && "Warranty details available", product?.compatibility && "Compatible device information", product?.packageContents && "Package contents listed"]
        .filter(Boolean);
};

export function ProductDetailHighlights({ product, variants = [] }) {
    const specs = readSpecs(product);
    const colors = values(product?.colorOptions, specs.colors, specs.colours, variants.flatMap(variant => [variant?.attributes?.color, variant?.attributes?.colour]));
    const compatible = values(product?.compatibility, specs.compatibleModels, specs.compatibleDevices, specs.compatibility);
    const highlights = highlightsFor(product);

    if (!colors.length && !compatible.length && !highlights.length) return null;

    return (
        <div className="shp-support-stack">
            {!!colors.length && <section className="shp-support-card">
                <div className="shp-support-head"><PackageCheck aria-hidden="true" /><div><p>Available colours</p><h2>Choose a finish</h2></div></div>
                <div className="shp-chip-list">{colors.map(color => <span key={color}>{color}</span>)}</div>
            </section>}
            {!!compatible.length && <section className="shp-support-card">
                <div className="shp-support-head"><Box aria-hidden="true" /><div><p>Compatible models</p><h2>Made to fit</h2></div></div>
                <div className="shp-chip-list">{compatible.map(model => <span key={model}>{model}</span>)}</div>
            </section>}
            {!!highlights.length && <section className="shp-support-card">
                <div className="shp-support-head"><Gem aria-hidden="true" /><div><p>Why you’ll love it</p><h2>Key features</h2></div></div>
                <ul className="shp-feature-list">{highlights.map((highlight, index) => {
                    const Icon = [ShieldCheck, Gem, Wrench, BadgeCheck][index % 4];
                    return <li key={`${highlight}-${index}`}><Icon aria-hidden="true" /><span>{highlight}</span></li>;
                })}</ul>
            </section>}
        </div>
    );
}

export function ProductDetailRail({ product, inStock }) {
    const seller = product?.seller || {};
    const sellerName = seller.businessName || seller.name || "ShivHub seller";
    const warranty = product?.warrantyDetails || readSpecs(product).warranty || readSpecs(product).warrantyDetails;

    return <div className="shp-rail-stack">
        <section className="shp-rail-card shp-delivery-card">
            <p className="shp-panel-label">Delivery & offers</p>
            <div><Truck aria-hidden="true" /><span><strong>Delivery availability</strong><small>Enter or select your address to see the real delivery estimate at checkout.</small></span></div>
            <div><RotateCcw aria-hidden="true" /><span><strong>Returns & replacement</strong><small>Eligibility is confirmed from the product policy after purchase.</small></span></div>
            <div><ShieldCheck aria-hidden="true" /><span><strong>Warranty</strong><small>{warranty || "Warranty details are supplied by the seller."}</small></span></div>
        </section>
        <section className="shp-rail-card shp-seller-card">
            <p className="shp-panel-label">Trust & seller info</p>
            <div className="shp-seller-title"><BadgeCheck aria-hidden="true" /><span><strong>{sellerName}</strong><small>{product?.approvalStatus === "APPROVED" ? "Approved ShivHub listing" : "ShivHub marketplace seller"}</small></span></div>
            <dl>
                {seller.city && <div><dt>Store location</dt><dd>{[seller.city, seller.district, seller.state].filter(Boolean).join(", ")}</dd></div>}
                {seller.supportPhone && <div><dt>Support</dt><dd>{seller.supportPhone}</dd></div>}
                <div><dt>Stock status</dt><dd className={inStock ? "is-available" : "is-unavailable"}><Check aria-hidden="true" />{inStock ? "Available to order" : "Currently out of stock"}</dd></div>
            </dl>
        </section>
    </div>;
}
