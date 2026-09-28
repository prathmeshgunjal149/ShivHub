const normalize = value => String(value || "").toLowerCase().replace(/[^a-z0-9]/g, "");

export const productName = value => typeof value === "string" ? value : value?.name || value?.title || "";

export function specificationMap(product) {
    const raw = product?.productSpecifications ?? product?.specifications;
    if (raw && typeof raw === "object") return raw;
    try { return raw ? JSON.parse(raw) : {}; } catch { return {}; }
}

const aliases = {
    color: ["color", "colour", "coloroptions"],
    warranty: ["warranty", "warrantydetails", "warrantymonths"],
    model: ["model"],
    modelnumber: ["modelnumber", "partnumber"],
    compatibility: ["compatibility", "compatibledevices", "vehiclecompatibility"],
    ram: ["ram"], storage: ["storage"]
};

export function valuesForAttribute(product, attributeKey) {
    const target = normalize(attributeKey);
    const source = specificationMap(product);
    const values = Object.entries(source)
        .filter(([key]) => normalize(key) === target)
        .flatMap(([, value]) => String(value ?? "").split(","));
    const sourceKeys = aliases[target] || [target];
    for (const [key, value] of Object.entries(product || {})) {
        if (sourceKeys.includes(normalize(key)) && value != null) values.push(...String(value).split(","));
    }
    return [...new Set(values.map(value => value.trim()).filter(Boolean))];
}

export function matchesAttributeFilters(product, selected) {
    return Object.entries(selected || {}).every(([key, expected]) => {
        if (!expected) return true;
        return valuesForAttribute(product, key).some(value => value.localeCompare(expected, undefined, { sensitivity: "accent" }) === 0);
    });
}
