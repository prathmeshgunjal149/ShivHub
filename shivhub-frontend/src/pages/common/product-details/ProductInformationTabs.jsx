import { useState } from "react";
import ProductSpecifications from "./ProductSpecifications";

const text = value => typeof value === "string" && value.trim() ? value.trim() : null;
const specificationWarranty = product => {
    try {
        const raw = product.productSpecifications || product.specificationDetails;
        const parsed = typeof raw === "string" && raw.trim().startsWith("{") ? JSON.parse(raw) : {};
        const values = parsed.specs && typeof parsed.specs === "object" ? parsed.specs : parsed;
        return text(values.warranty || values.warrantyDetails || values.warranty_summary);
    } catch { return null; }
};

export default function ProductInformationTabs({ product, onShowcase }) {
    const [active, setActive] = useState("specifications");
    const summary = text(product.warrantyDetails) || specificationWarranty(product);
    const seller = product.seller || {};
    const serviceType = text(seller.warrantyType);
    const support = [text(seller.supportPhone), text(seller.supportEmail), text(seller.warrantyTerms)].filter(Boolean);
    const sellerName = text(seller.businessName) || text(seller.name);

    return <section className="shp-info-tabs">
        <div className="shp-tabs" role="tablist" aria-label="Product information">
            <button role="tab" aria-selected={active === "showcase"} className={active === "showcase" ? "active" : ""} onClick={() => { setActive("showcase"); onShowcase?.(); }}>Showcase</button>
            <button role="tab" aria-selected={active === "specifications"} className={active === "specifications" ? "active" : ""} onClick={() => setActive("specifications")}>Specifications</button>
            <button role="tab" aria-selected={active === "description"} className={active === "description" ? "active" : ""} onClick={() => setActive("description")}>Description</button>
            <button role="tab" aria-selected={active === "warranty"} className={active === "warranty" ? "active" : ""} onClick={() => setActive("warranty")}>Warranty</button>
            <button role="tab" aria-selected={active === "delivery"} className={active === "delivery" ? "active" : ""} onClick={() => setActive("delivery")}>Delivery & Seller</button>
        </div>
        {active === "showcase" && <div className="shp-tab-body"><p className="shp-tab-eyebrow">PRODUCT SHOWCASE</p><h2>Every uploaded product photo</h2><p>Select any thumbnail above to view it, or use Expand for a full-screen image. {product.images?.length ? `${product.images.length} saved image${product.images.length === 1 ? "" : "s"} available.` : "No product image was uploaded."}</p></div>}
        {active === "specifications" && <ProductSpecifications product={product} />}
        {active === "description" && <div className="shp-tab-body"><p className="shp-tab-eyebrow">DESCRIPTION</p><h2>About this product</h2><p className="shp-tab-copy">{text(product.description) || "Not specified"}</p>{text(product.packageContents) && <><h3>In the box</h3><p className="shp-tab-copy">{product.packageContents}</p></>}{text(product.compatibility) && <><h3>Compatibility</h3><p className="shp-tab-copy">{product.compatibility}</p></>}</div>}
        {active === "warranty" && <div className="shp-tab-body"><p className="shp-tab-eyebrow">WARRANTY</p><h2>Warranty summary</h2><p className="shp-warranty-summary">{summary || "Not specified"}</p><dl className="shp-fact-list"><div><dt>Warranty service type</dt><dd>{serviceType || "Not specified"}</dd></div>{sellerName && <div><dt>Service provider</dt><dd>{sellerName}</dd></div>}{support.length > 0 && <div><dt>Service contact / terms</dt><dd>{support.join(" · ")}</dd></div>}</dl></div>}
        {active === "delivery" && <div className="shp-tab-body"><p className="shp-tab-eyebrow">DELIVERY & SELLER</p><h2>{sellerName || "Seller details"}</h2><dl className="shp-fact-list">{sellerName && <div><dt>Seller</dt><dd>{sellerName}</dd></div>}{text(seller.city) && <div><dt>Store location</dt><dd>{[seller.city, seller.district, seller.state].filter(Boolean).join(", ")}</dd></div>}<div><dt>Delivery</dt><dd>Choose your address during checkout to see the available delivery options.</dd></div></dl></div>}
    </section>;
}
