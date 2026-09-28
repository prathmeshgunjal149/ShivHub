import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import { productName, valuesForAttribute } from "./productAttributeFilters";

const options = raw => {
    if (!raw) return [];
    try { const parsed = JSON.parse(raw); return Array.isArray(parsed) ? parsed : []; }
    catch { return String(raw).split(",").map(value => value.trim()).filter(Boolean); }
};
const categoryMatches = (product, category) => category === "All" || productName(product.categoryEntity || product.category) === category || productName(product.subCategory) === category;

export default function DynamicAttributeFilters({ products = [], activeCategory, values = {}, onChange }) {
    const [subcategoryId, setSubcategoryId] = useState("");
    const [fields, setFields] = useState([]);
    const [loading, setLoading] = useState(false);
    const scopedSubcategories = useMemo(() => {
        const seen = new Map();
        products.filter(product => categoryMatches(product, activeCategory)).forEach(product => {
            const subcategory = product.subCategory;
            if (subcategory?.id && subcategory?.name) seen.set(String(subcategory.id), subcategory);
        });
        return [...seen.values()].sort((left, right) => left.name.localeCompare(right.name));
    }, [products, activeCategory]);

    useEffect(() => {
        const exact = scopedSubcategories.find(item => item.name === activeCategory);
        setSubcategoryId(current => scopedSubcategories.some(item => String(item.id) === current) ? current : String(exact?.id || ""));
        onChange?.({});
    }, [activeCategory, scopedSubcategories, onChange]);

    useEffect(() => {
        let current = true;
        setFields([]);
        if (!subcategoryId) return () => { current = false; };
        setLoading(true);
        api.get(`/api/customer/catalogue/subcategories/${subcategoryId}/filters`)
            .then(({ data }) => { if (current) setFields(Array.isArray(data) ? data : []); })
            .catch(() => { if (current) setFields([]); })
            .finally(() => { if (current) setLoading(false); });
        return () => { current = false; };
    }, [subcategoryId]);

    const scopedProducts = useMemo(() => products.filter(product => String(product.subCategory?.id || "") === subcategoryId), [products, subcategoryId]);
    if (!scopedSubcategories.length) return null;
    const changeSubcategory = value => { setSubcategoryId(value); onChange?.({}); };
    const setValue = (key, value) => {
        const next = { ...values };
        if (value) next[key] = value; else delete next[key];
        onChange?.(next);
    };
    return <>
        <label>
            Product type
            <select value={subcategoryId} onChange={event => changeSubcategory(event.target.value)}>
                <option value="">Select for more filters</option>
                {scopedSubcategories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
        </label>
        {loading && <small role="status">Loading product filters…</small>}
        {fields.map(field => {
            const realValues = [...new Set(scopedProducts.flatMap(product => valuesForAttribute(product, field.specificationKey)))];
            const choices = [...new Set([...options(field.optionsJson), ...realValues])].sort((left, right) => left.localeCompare(right));
            const selected = values[field.specificationKey] || "";
            return <label key={field.id}>{field.displayLabel}
                {choices.length ? <select value={selected} onChange={event => setValue(field.specificationKey, event.target.value)}>
                    <option value="">Any {field.displayLabel}</option>
                    {choices.map(value => <option key={value} value={value}>{value}</option>)}
                </select> : <input value={selected} onChange={event => setValue(field.specificationKey, event.target.value)} placeholder={`Any ${field.displayLabel}`} />}
            </label>;
        })}
    </>;
}
