import { splitOptions } from "./productUtils";

export default function ProductSpecifications({ product, compact = false }) {
    const dynamicSpecifications = (() => {
        const raw = product.productSpecifications || product.specificationDetails;
        if (!raw || typeof raw !== "string" || !raw.trim().startsWith("{")) return [];
        try {
            const parsed = JSON.parse(raw);
            const source = parsed.specs && typeof parsed.specs === "object" ? parsed.specs : parsed;
            return Object.entries(source)
                .filter(([key, value]) => !["module", "specs"].includes(key) && value !== null && value !== undefined && String(value).trim())
                .map(([key, value]) => [key.replace(/([A-Z])/g, " $1").replace(/^./, c => c.toUpperCase()), Array.isArray(value) ? value.join(", ") : String(value)]);
        } catch { return []; }
    })();
    const specs = [
        ["Brand", product.brand],
        ["Model", product.model],
        ["RAM", product.ram],
        ["Storage", product.storage],
        ["Colours", product.colorOptions],
        ["HSN/SAC", product.hsnCode],
        ["Seller SKU", product.sellerSku],
        ["Barcode", product.barcode],
        ["Serial tracking", product.serialTrackingRequired ? "Required" : null],
        ["Category", product.categoryEntity?.name || product.category],
        ["Subcategory", product.subCategory?.name]
    ].filter(([, value]) => value);

    const allSpecs = [...specs, ...dynamicSpecifications.filter(([label]) => !specs.some(([base]) => base === label))];
    if (!allSpecs.length && !product.specificationDetails) return null;

    return (
        <section className={compact ? "shp-specs compact" : "shp-specs"}>
            {!compact && (
                <div className="shp-section-head">
                    <div>
                        <p>Specifications</p>
                        <h2>Technical details</h2>
                    </div>
                </div>
            )}
            <div className="shp-spec-grid">
                {allSpecs.map(([label, value]) => (
                    <div key={label}>
                        <span>{label}</span>
                        <strong>{splitOptions(value).join(", ") || value}</strong>
                    </div>
                ))}
            </div>
            {product.specificationDetails && !dynamicSpecifications.length && <p className="shp-full-spec">{product.specificationDetails}</p>}
        </section>
    );
}
