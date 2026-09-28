import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import DynamicAttributeField from "./DynamicAttributeField";
import "./variants.css";
import { listItem, noTransform } from "../../utils/animationVariants";
import { newVariant } from "./productCommonFields";

export default function VariantEditor({ fields, variants, onChange }) {
    const reducedMotion = useReducedMotion();
    const rows = Array.isArray(variants) ? variants : [];
    const attributeFields = Array.isArray(fields) ? fields : [];
    const update = (index, changes) => onChange(rows.map((row, itemIndex) => itemIndex === index ? { ...row, ...changes } : row));

    return (
        <section className="variant-editor">
            <h2>Variants</h2>
            <p>Each combination has its own GST-inclusive price and stock. Total stock is derived from these rows.</p>
            <AnimatePresence initial={false}>
                {rows.map((row, index) => (
                    <motion.fieldset
                        key={row.id || row.clientKey || index}
                        variants={reducedMotion ? noTransform : listItem}
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                        layout={!reducedMotion}
                    >
                        <legend>Variant {index + 1}</legend>
                        <div className="add-other-grid">
                            {attributeFields.map(field => (
                                <DynamicAttributeField
                                    key={field.id}
                                    field={field}
                                    required
                                    value={row.attributes?.[field.specificationKey] || ""}
                                    onChange={value => update(index, { attributes: { ...row.attributes, [field.specificationKey]: value } })}
                                />
                            ))}
                            {[["SKU", "variantSku"], ["Barcode (optional)", "barcode"], ["Price including GST", "sellingPriceIncludingGst"], ["Compare-at price (optional)", "compareAtPrice"], ["Stock quantity", "stockQuantity"], ["Image URL (optional)", "imageUrl"]].map(([label, key]) => (
                                <label key={key}>{label}
                                    <input
                                        value={row[key] ?? ""}
                                        required={key === "sellingPriceIncludingGst" || key === "stockQuantity"}
                                        type={["sellingPriceIncludingGst", "compareAtPrice", "stockQuantity"].includes(key) ? "number" : key === "imageUrl" ? "url" : "text"}
                                        min={key === "sellingPriceIncludingGst" ? "0.01" : "0"}
                                        step={key === "stockQuantity" ? "1" : "0.01"}
                                        onChange={event => update(index, { [key]: event.target.value })}
                                    />
                                </label>
                            ))}
                            <label>Purchase price (optional)
                                <input type="number" min="0" step="0.01" value={row.purchasePrice ?? ""} onChange={event => update(index, { purchasePrice: event.target.value })} />
                            </label>
                        </div>
                        {row.reservedQuantity > 0 && <p>{row.reservedQuantity} units are reserved for existing orders.</p>}
                        <label><input type="checkbox" checked={row.active !== false} onChange={event => update(index, { active: event.target.checked })} /> Active</label>
                        <button type="button" onClick={() => onChange(rows.filter((_, itemIndex) => itemIndex !== index))} disabled={rows.length === 1}>Remove variant</button>
                    </motion.fieldset>
                ))}
            </AnimatePresence>
            <button type="button" onClick={() => onChange([...rows, newVariant()])} disabled={rows.length >= 100}>Add variant</button>
        </section>
    );
}
