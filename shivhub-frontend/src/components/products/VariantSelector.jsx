import { motion, useReducedMotion } from "framer-motion";
import "./variants.css";

export default function VariantSelector({ variants, selection, onChange }) {
    const reducedMotion = useReducedMotion();
    const rows = Array.isArray(variants) ? variants : [];
    const selected = selection || {};
    const keys = [...new Set(rows.flatMap(variant => Object.keys(variant.attributes || {})))];

    if (!rows.length || !keys.length) return null;

    return (
        <section className="variant-selector">
            <h3>Select product options</h3>
            {keys.map(key => {
                const values = [...new Set(rows.map(variant => variant.attributes?.[key]).filter(Boolean))];
                return (
                    <div key={key}>
                        <span>Select {key.replaceAll("_", " ")}</span>
                        <div className="variant-options">
                            {values.map(value => {
                                const isSelected = selected[key] === value;
                                const available = rows.some(variant => variant.availableStock > 0 && variant.attributes?.[key] === value);
                                return (
                                    <motion.button
                                        type="button"
                                        key={value}
                                        aria-pressed={isSelected}
                                        disabled={!available}
                                        animate={reducedMotion ? undefined : { scale: isSelected ? 1.015 : 1 }}
                                        whileTap={reducedMotion || !available ? undefined : { scale: 0.98 }}
                                        transition={{ duration: 0.16 }}
                                        onClick={() => {
                                            const next = { ...selected, [key]: value };
                                            if (!rows.some(row => row.availableStock > 0 && keys.every(other => !next[other] || row.attributes?.[other] === next[other]))) {
                                                keys.filter(other => other !== key).forEach(other => { delete next[other]; });
                                            }
                                            onChange(next);
                                        }}
                                    >
                                        {value}{!available && <small> · Out of stock</small>}
                                    </motion.button>
                                );
                            })}
                        </div>
                    </div>
                );
            })}
            <button type="button" className="variant-selector__clear" onClick={() => onChange({})}>Clear selection</button>
        </section>
    );
}
