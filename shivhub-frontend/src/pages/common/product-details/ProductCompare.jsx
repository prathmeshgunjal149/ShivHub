import { useEffect, useMemo, useState } from "react";
import { imageUrl, money } from "./productUtils";

const KEY = "shivhub_product_compare";
const label = key => String(key).replace(/([A-Z])/g, " $1").replaceAll("_", " ").replace(/^./, value => value.toUpperCase());
const values = product => {
    const details = { Brand: product.brand, Model: product.model, "Model number": product.modelNumber, RAM: product.ram, Storage: product.storage, Colours: product.colorOptions, SKU: product.sellerSku, Barcode: product.barcode, HSN: product.hsnCode, Warranty: product.warrantyDetails };
    try { const raw = product.productSpecifications || product.specificationDetails; const parsed = typeof raw === "string" && raw.trim().startsWith("{") ? JSON.parse(raw) : {}; const extra = parsed.specs && typeof parsed.specs === "object" ? parsed.specs : parsed; Object.entries(extra).forEach(([key, value]) => { if (value != null && String(value).trim()) details[label(key)] = Array.isArray(value) ? value.join(", ") : String(value); }); } catch { /* legacy plain-text details stay on product page */ }
    return Object.fromEntries(Object.entries(details).filter(([, value]) => value != null && String(value).trim()));
};
const sameGroup = (first, second) => (first.categoryEntity?.id || first.category) === (second.categoryEntity?.id || second.category) && (!first.subCategory?.id || !second.subCategory?.id || first.subCategory.id === second.subCategory.id);

export default function ProductCompare({ product, candidates, onMessage }) {
    const [items, setItems] = useState([]); const [open, setOpen] = useState(false); const [choice, setChoice] = useState("");
    useEffect(() => { try { const stored = JSON.parse(localStorage.getItem(KEY) || "[]"); setItems(Array.isArray(stored) ? stored.filter(item => item?.id) : []); } catch { setItems([]); } }, []);
    const save = next => { setItems(next); localStorage.setItem(KEY, JSON.stringify(next)); };
    const add = item => { if (!item) return; if (!sameGroup(product, item)) { onMessage?.("Compare products from the same category/subcategory only."); return; } if (items.some(row => Number(row.id) === Number(item.id))) return; if (items.length >= 3) { onMessage?.("You can compare up to 3 products."); return; } save([...items, item]); };
    const listed = useMemo(() => candidates.filter(item => sameGroup(product, item) && Number(item.id) !== Number(product.id)), [candidates, product]);
    const rows = useMemo(() => [...new Set(items.flatMap(item => Object.keys(values(item))))], [items]);
    return <section className="shp-compare"><div><p className="shp-tab-eyebrow">COMPARE PRODUCTS</p><h2>Choose what suits you</h2><span>Compare up to 3 real products using their saved specifications.</span></div><div className="shp-compare-actions"><button type="button" onClick={() => add(product)}>{items.some(item => Number(item.id) === Number(product.id)) ? "Added" : "Add this product"}</button><select value={choice} onChange={event => { setChoice(event.target.value); const next = listed.find(item => Number(item.id) === Number(event.target.value)); add(next); }}><option value="">Add similar product…</option>{listed.map(item => <option value={item.id} key={item.id}>{item.name}</option>)}</select><button type="button" disabled={items.length < 2} onClick={() => setOpen(true)}>Compare ({items.length})</button></div>{open && <div className="shp-compare-modal" role="dialog" aria-modal="true"><article><button className="shp-compare-close" onClick={() => setOpen(false)}>Close</button><h2>Product comparison</h2><div className="shp-compare-table"><table><thead><tr><th>Specification</th>{items.map(item => <th key={item.id}><img src={imageUrl(item.images?.[0]?.imageUrl || item.imageUrl)} alt="" /><strong>{item.name}</strong><span>{money(item.finalSellingPrice ?? item.price)}</span><button onClick={() => save(items.filter(row => Number(row.id) !== Number(item.id)))}>Remove</button></th>)}</tr></thead><tbody>{rows.map(row => <tr key={row}><td>{row}</td>{items.map(item => <td key={item.id}>{values(item)[row] || "Not specified"}</td>)}</tr>)}</tbody></table></div></article></div>}</section>;
}
