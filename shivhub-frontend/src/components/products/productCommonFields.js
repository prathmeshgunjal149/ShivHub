// Only presentation defaults: all actual attributes/options still come from Admin APIs.
export const variantSummary = raw => {
    if (!raw) return "";
    try {
        const attributes = typeof raw === "string" ? JSON.parse(raw) : raw;
        return Object.entries(attributes).map(([key, value]) => `${key.replaceAll("_", " ")}: ${value}`).join(" | ");
    } catch { return String(raw); }
};
let nextVariantKey = 0;
export const newVariant = () => ({
    clientKey: `new-variant-${++nextVariantKey}`, attributes: {}, variantSku: "", barcode: "",
    sellingPriceIncludingGst: "", purchasePrice: "", compareAtPrice: "", stockQuantity: "0", imageUrl: "", active: true
});
export function productCommonFields(categoryName = "", subcategoryName = "", templates = []) {
    if (!subcategoryName) return [];
    const name = `${categoryName} ${subcategoryName}`.toLowerCase();
    const configured = new Set(templates.map(field => field.specificationKey.toLowerCase().replace(/[^a-z0-9]/g, "")));
    const fields = [];
    const add = (label, key, aliases = [key]) => {
        if (!aliases.some(alias => configured.has(alias.toLowerCase()))) fields.push({ label, key });
    };
    const electronics = /electronic|computer|laptop|television|\btv\b|entertainment|headphone|earphone|speaker|charger|cable|appliance|camera|gaming/.test(name);
    const automotive = /automobile|automotive|vehicle|motor|car part/.test(name);
    const accessory = /accessor|headphone|earphone|charger|cable|cover|adapter/.test(name);
    if (electronics || automotive) {
        add("Model", "model"); add("Model number / Part number", "modelNumber", ["modelNumber", "partNumber"]);
        add("Warranty", "warrantyDetails", ["warranty", "warrantyDetails", "warrantyMonths"]);
    }
    if (!/grocery|food|beauty|personal care/.test(name)) add("Color", "colorOptions", ["color", "colour", "colorOptions"]);
    if (accessory) {
        add("Compatible devices", "compatibility", ["compatibility", "compatibleDevices"]);
        add("Accessory type", "accessoryType", ["accessoryType", "type"]);
    }
    return fields;
}

export function variantRequest(row) {
    // UI keys/reservation information must never become editable backend payload fields.
    return { id: row.id, version: row.version, attributes: row.attributes, variantSku: row.variantSku,
        barcode: row.barcode, sellingPriceIncludingGst: Number(row.sellingPriceIncludingGst), purchasePrice: row.purchasePrice === "" ? null : Number(row.purchasePrice),
        compareAtPrice: row.compareAtPrice ? Number(row.compareAtPrice) : null,
        stockQuantity: Number(row.stockQuantity), imageUrl: row.imageUrl, active: row.active,
        reorderThreshold: row.reorderThreshold };
}
