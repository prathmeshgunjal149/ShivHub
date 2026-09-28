import { motion, useReducedMotion } from "framer-motion";
import "./variants.css";

/**
 * Mobile inventory remains owned by the established product and IMEI flow.
 * This selector only moves customers between compatible sibling listings.
 */
export default function MobileOptionSelector({ options, currentProductId, onSelect }) {
    const reducedMotion = useReducedMotion();
    const rows = Array.isArray(options) ? options : [];
    const current = rows.find(option => Number(option.productId) === Number(currentProductId)) || rows[0];
    const selected = { color: current?.color || "", ram: current?.ram || "", storage: current?.storage || "" };
    const keys = [["color", "Colour"], ["ram", "RAM"], ["storage", "Storage"]]
        .filter(([key]) => rows.some(option => option[key]));

    const choicesFor = key => [...new Set(rows.map(option => option[key]).filter(Boolean))];
    const valid = (key, value) => rows.some(option => option.availableStock > 0 && option[key] === value);
    const choose = (key, value) => {
        const next = { ...selected, [key]: value };
        const match = rows.find(option => option.availableStock > 0
            && keys.every(([field]) => !next[field] || option[field] === next[field]))
            || rows.find(option => option.availableStock > 0 && option[key] === value);
        if (match) onSelect(match);
    };

    if (!rows.length || !keys.length) return null;

    return (
        <section className="variant-selector mobile-option-selector">
            <h3>Choose your mobile configuration</h3>
            {keys.map(([key, label]) => (
                <div key={key}>
                    <span>Select {label}</span>
                    <div className="variant-options">
                        {choicesFor(key).map(value => {
                            const isSelected = selected[key] === value;
                            const available = valid(key, value);
                            return (
                                <motion.button
                                    type="button"
                                    key={value}
                                    aria-pressed={isSelected}
                                    disabled={!available}
                                    animate={reducedMotion ? undefined : { scale: isSelected ? 1.015 : 1 }}
                                    whileTap={reducedMotion || !available ? undefined : { scale: 0.98 }}
                                    transition={{ duration: 0.16 }}
                                    onClick={() => choose(key, value)}
                                >
                                    {value}
                                </motion.button>
                            );
                        })}
                    </div>
                </div>
            ))}
            {current && <p className="selected-attributes">{current.availableStock > 0 ? `${current.availableStock <= 2 ? `Only ${current.availableStock} left` : "Available"} · ₹${Number(current.price || 0).toLocaleString("en-IN")}` : "Out of stock"}</p>}
        </section>
    );
}
