import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import api from "../../services/api";
import "./AddOtherProduct.css";
import "./AddOtherProductImages.css";
import DynamicAttributeField from "../../components/products/DynamicAttributeField";
import VariantEditor from "../../components/products/VariantEditor";
import { fadeUp, noTransform } from "../../utils/animationVariants";
import { productCommonFields, variantRequest, newVariant } from "../../components/products/productCommonFields";
import SerialStockEditor from "../../components/products/SerialStockEditor";
import { getActiveSellerDistributors } from "../../services/purchaseService";

const emptyImages = () => Array.from({ length: 5 }, () => ({ file: null, url: "" }));
const blankProduct = {
    name: "", brand: "", model: "", modelNumber: "", description: "", price: "", stock: "",
    gstRate: "18", hsnCode: "", colorOptions: "", warrantyDetails: "", sellerSku: "", barcode: "",
    accessoryType: "", compatibility: "", reorderThreshold: "", serialTrackingRequired: false,
    categoryId: "", subCategoryId: ""
};
const emptyPurchase = () => ({ sellerDistributorId: "", invoiceNumber: "", purchaseDate: "", unitPrice: "", gstRate: "18" });
const conditionalKeys = ["model", "modelNumber", "colorOptions", "warrantyDetails", "compatibility", "accessoryType"];

export default function AddOtherProduct() {
    const navigate = useNavigate();
    const reducedMotion = useReducedMotion();
    const [form, setForm] = useState(blankProduct);
    const [categories, setCategories] = useState([]);
    const [subCategories, setSubCategories] = useState([]);
    const [templates, setTemplates] = useState([]);
    const [specs, setSpecs] = useState({});
    const [variants, setVariants] = useState([]);
    const variantFields = templates.filter(field => field.variantEnabled);
    const [customSpecs, setCustomSpecs] = useState([{ key: "", value: "" }]);
    const [images, setImages] = useState(emptyImages);
    const [imagePreviewErrors, setImagePreviewErrors] = useState({});
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [purchase, setPurchase] = useState(emptyPurchase);
    const [serialUnits, setSerialUnits] = useState([]);
    const [distributors, setDistributors] = useState([]);
    const [loadingDistributors, setLoadingDistributors] = useState(false);
    const [loadingTemplates, setLoadingTemplates] = useState(false);
    const [templatesReady, setTemplatesReady] = useState(false);
    const selectedCategory = categories.find(item => String(item.id) === form.categoryId);
    const selectedSubcategory = subCategories.find(item => String(item.id) === form.subCategoryId);
    const relevantFields = productCommonFields(selectedCategory?.name, selectedSubcategory?.name, templates);
    const imageCount = useMemo(() => images.filter(image => image.file || image.url.trim()).length, [images]);

    useEffect(() => {
        api.get("/api/categories")
            .then(({ data }) => setCategories(Array.isArray(data) ? data : []))
            .catch(requestError => setError(requestError.response?.data?.message || "Unable to load active categories."));
    }, []);

    useEffect(() => {
        if (!form.categoryId) { setSubCategories([]); setTemplates([]); return; }
        let active = true;
        setSubCategories([]);
        api.get(`/api/categories/${form.categoryId}/subcategories`)
            .then(({ data }) => { if (active) setSubCategories(Array.isArray(data) ? data : []); })
            .catch(requestError => { if (active) setError(requestError.response?.data?.message || "Unable to load subcategories."); });
        return () => { active = false; };
    }, [form.categoryId]);

    useEffect(() => {
        if (!form.categoryId || !form.subCategoryId) { setTemplates([]); return; }
        let active = true;
        setTemplates([]); setLoadingTemplates(true); setTemplatesReady(false);
        api.get("/api/seller/product-specification-templates", { params: { categoryId: form.categoryId, subCategoryId: form.subCategoryId } })
            .then(({ data }) => { if (active) { setTemplates(Array.isArray(data) ? data : []); setTemplatesReady(true); } })
            .catch(requestError => { if (active) setError(requestError.response?.data?.message || "Unable to load specification fields."); })
            .finally(() => { if (active) setLoadingTemplates(false); });
        return () => { active = false; };
    }, [form.categoryId, form.subCategoryId]);

    useEffect(() => {
        if (!form.serialTrackingRequired) return;
        let active = true;
        setLoadingDistributors(true);
        getActiveSellerDistributors().then(data => { if (active) setDistributors(Array.isArray(data) ? data : []); })
            .catch(() => { if (active) setError("Unable to load your linked distributors. Retry tracking or use Purchases later with zero opening stock."); })
            .finally(() => { if (active) setLoadingDistributors(false); });
        return () => { active = false; };
    }, [form.serialTrackingRequired]);

    const setField = (name, value) => setForm(current => ({ ...current, [name]: value }));
    const setImage = (index, change) => {
        setImagePreviewErrors(current => ({ ...current, [index]: false }));
        setImages(current => current.map((image, imageIndex) => imageIndex === index ? { ...image, ...change } : image));
    };
    const selectCategory = event => {
        if (categories.find(category => String(category.id) === event.target.value)?.name.toLowerCase() === "mobiles") { navigate("/seller/add-product"); return; }
        setForm(current => ({ ...current, ...Object.fromEntries(conditionalKeys.map(key => [key, ""])), categoryId: event.target.value, subCategoryId: "", serialTrackingRequired: false }));
        setSpecs({}); setVariants([]); setCustomSpecs([{ key: "", value: "" }]); setSerialUnits([]); setPurchase(emptyPurchase());
    };

    const submit = async event => {
        event.preventDefault();
        setError(""); setMessage("");
        if (!form.categoryId || !form.subCategoryId) { setError("Select an active category and subcategory."); return; }
        if (imageCount < 5) { setError("Add at least five product images using the existing upload or URL option."); return; }
        if (loadingTemplates || !templatesReady) { setError("The selected subcategory fields must load successfully before submitting. Reselect the subcategory to retry."); return; }
        const requiredTemplate = templates.find(template => (!template.variantEnabled || !variants.length) && template.requiredField && !String(specs[template.specificationKey] || "").trim());
        if (requiredTemplate) { setError(`${requiredTemplate.displayLabel} is required.`); return; }

        const productSpecifications = {
            ...Object.fromEntries(customSpecs.filter(item => item.key.trim() && item.value.trim()).map(item => [item.key.trim(), item.value.trim()])),
            ...Object.fromEntries(Object.entries(specs).filter(([, value]) => String(value).trim()))
        };
        const shownKeys = new Set(relevantFields.map(field => field.key));
        const fieldValue = (key, aliases = []) => {
            if (shownKeys.has(key)) return form[key].trim();
            const normalize = value => value.toLowerCase().replace(/[^a-z0-9]/g, "");
            const normalizedAliases = aliases.map(normalize);
            return Object.entries(specs).find(([specKey, value]) => value && normalizedAliases.includes(normalize(specKey)))?.[1] || "";
        };
        if (form.serialTrackingRequired && (!Number.isInteger(Number(form.stock)) || Number(form.stock) > 500)) { setError("Enter a whole stock quantity, up to 500 serialized units."); return; }
        const units = form.serialTrackingRequired ? Array.from({ length: Math.max(0, Math.min(500, Number(form.stock) || 0)) }, (_, index) => serialUnits[index] || {}) : [];
        if (form.serialTrackingRequired && units.some(unit => !unit.imei1?.trim() && !unit.serialNumber?.trim())) { setError("Enter IMEI 1 or a serial number for every stock unit."); return; }
        const product = {
            name: form.name.trim(), description: form.description.trim(), brand: form.brand.trim(), model: fieldValue("model", ["model"]),
            modelNumber: fieldValue("modelNumber", ["modelNumber", "partNumber"]), price: Number(form.price), stock: Number(form.stock), gstRate: Number(form.gstRate || 0),
            hsnCode: form.hsnCode.trim(), colorOptions: fieldValue("colorOptions", ["color", "colour"]), warrantyDetails: fieldValue("warrantyDetails", ["warranty", "warrantyMonths"]),
            sellerSku: form.sellerSku.trim(), barcode: form.barcode.trim(), accessoryType: fieldValue("accessoryType", ["accessoryType"]),
            compatibility: fieldValue("compatibility", ["compatibleDevices", "compatibility"]), reorderThreshold: form.reorderThreshold ? Number(form.reorderThreshold) : null,
            serialTrackingRequired: form.serialTrackingRequired, productType: "NON_MOBILE",
            variants: variants.length ? variants.map(variantRequest) : undefined,
            initialPurchase: form.serialTrackingRequired && Number(form.stock) > 0 ? { ...purchase, sellerDistributorId: Number(purchase.sellerDistributorId), unitPrice: Number(purchase.unitPrice), gstRate: Number(purchase.gstRate), purchaseDate: purchase.purchaseDate || null, serials: units } : undefined,
            productSpecifications: JSON.stringify(productSpecifications),
            specificationDetails: Object.entries(productSpecifications).map(([key, value]) => `${key}: ${value}`).join("\n"),
            categoryId: Number(form.categoryId), subCategoryId: Number(form.subCategoryId)
        };

        try {
            setSaving(true);
            const body = new FormData();
            body.append("product", new Blob([JSON.stringify(product)], { type: "application/json" }));
            images.filter(image => image.file).forEach(image => body.append("images", image.file));
            body.append("imageUrls", new Blob([JSON.stringify(images.filter(image => image.url.trim()).map(image => image.url.trim()))], { type: "application/json" }));
            await api.post("/api/seller/non-mobile-products", body);
            setMessage("Product submitted as PENDING for Admin approval.");
            setForm(blankProduct); setSubCategories([]); setTemplates([]); setSpecs({}); setVariants([]); setCustomSpecs([{ key: "", value: "" }]); setImages(emptyImages()); setImagePreviewErrors({});
            setSerialUnits([]); setPurchase(emptyPurchase());
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Product could not be submitted.");
        } finally { setSaving(false); }
    };

    const commonField = (label, name, options = {}) => (
        <label>{label}<input {...options} value={form[name]} onChange={event => setField(name, event.target.value)} /></label>
    );

    return <main className="add-other-product">
        <header><button type="button" onClick={() => navigate("/seller/dashboard")}>← Dashboard</button><div><span>SELLER PRODUCTS</span><h1>Add Other Product</h1><p>For every active Admin-created category and subcategory. Mobile creation remains on the dedicated Add Mobile page.</p></div></header>
        <form onSubmit={submit}>
            {error && <p className="form-error">{error}</p>}{message && <p className="form-success">{message}</p>}
            <section><h2>Category and common details</h2><div className="add-other-grid">
                <label>Category<select required value={form.categoryId} onChange={selectCategory}><option value="">Select active category</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
                <label>Subcategory<select required disabled={!form.categoryId} value={form.subCategoryId} onChange={event => { setForm(current => ({ ...current, ...Object.fromEntries(conditionalKeys.map(key => [key, ""])), subCategoryId: event.target.value })); setSpecs({}); setVariants([]); setCustomSpecs([{ key: "", value: "" }]); }}><option value="">Select active subcategory</option>{subCategories.map(subCategory => <option key={subCategory.id} value={subCategory.id}>{subCategory.name}</option>)}</select></label>
                {commonField("Product name", "name", { required: true })}{commonField("Brand", "brand", { required: true })}
                {commonField("Selling price (incl. GST)", "price", { required: true, min: "0.01", step: "0.01", type: "number" })}
                {commonField("GST rate (%)", "gstRate", { required: true, min: "0", max: "100", step: "0.01", type: "number" })}
                {variants.length ? <label>Total variant stock<input readOnly value={variants.reduce((sum, row) => sum + Number(row.stockQuantity || 0), 0)} /></label> : commonField("Stock quantity", "stock", { required: true, min: "0", type: "number" })}
                {commonField("SKU / Product code", "sellerSku")}{commonField("Product barcode (optional)", "barcode")}
                {commonField("HSN code", "hsnCode")}
                {relevantFields.map(field => <div key={field.key}>{commonField(field.label, field.key)}</div>)}
                {commonField("Reorder threshold", "reorderThreshold", { min: "0", type: "number" })}
                <label className="full">Description<textarea required rows="4" value={form.description} onChange={event => setField("description", event.target.value)} /></label>
                <label className="checkbox"><input type="checkbox" checked={form.serialTrackingRequired} onChange={event => { setField("serialTrackingRequired", event.target.checked); if (event.target.checked) setVariants([]); setSerialUnits([]); }} /> IMEI/serial tracking required</label>
            </div></section>
            {form.serialTrackingRequired && <SerialStockEditor quantity={form.stock} purchase={purchase} onPurchaseChange={setPurchase} units={serialUnits} onUnitsChange={setSerialUnits} distributors={distributors} loading={loadingDistributors} />}
            <section><h2>Specifications</h2><p>Fields are loaded from Admin templates. Add relevant key/value specifications for any future category.</p><div className="add-other-grid">
                <AnimatePresence initial={false}>
                    {templates.filter(field => !field.variantEnabled || !variants.length).map(field => (
                        <motion.div key={field.id} variants={reducedMotion ? noTransform : fadeUp} initial="hidden" animate="visible" exit="exit">
                            <DynamicAttributeField field={field} value={specs[field.specificationKey] || ""} onChange={value => setSpecs(current => ({ ...current, [field.specificationKey]: value }))} />
                        </motion.div>
                    ))}
                </AnimatePresence>
            </div>
            {customSpecs.map((item, index) => <div className="add-other-custom-spec" key={index}><input placeholder="Specification name" value={item.key} onChange={event => setCustomSpecs(current => current.map((value, itemIndex) => itemIndex === index ? { ...value, key: event.target.value } : value))} /><input placeholder="Value" value={item.value} onChange={event => setCustomSpecs(current => current.map((value, itemIndex) => itemIndex === index ? { ...value, value: event.target.value } : value))} /><button type="button" onClick={() => setCustomSpecs(current => current.filter((_, itemIndex) => itemIndex !== index))} disabled={customSpecs.length === 1}>Remove</button></div>)}
            <button type="button" className="secondary" onClick={() => setCustomSpecs(current => [...current, { key: "", value: "" }])}>Add specification</button></section>
            {!!variantFields.length && !form.serialTrackingRequired && <section><label className="checkbox"><input type="checkbox" checked={variants.length > 0} onChange={event => setVariants(event.target.checked ? [newVariant()] : [])} /> Separate prices and stock by variant</label><AnimatePresence initial={false}>{variants.length > 0 && <motion.div variants={reducedMotion ? noTransform : fadeUp} initial="hidden" animate="visible" exit="exit"><VariantEditor fields={variantFields} variants={variants} onChange={setVariants} /></motion.div>}</AnimatePresence></section>}
            <section><h2>Specifications preview</h2><dl>{Object.entries(specs).filter(([, value]) => value).map(([key, value]) => <div key={key}><dt>{templates.find(field => field.specificationKey === key)?.displayLabel || key}</dt><dd>{value}</dd></div>)}</dl>{variants.map((row, index) => <p key={index}>{Object.entries(row.attributes).map(([key, value]) => `${key}: ${value}`).join(" | ")} — ₹{row.sellingPriceIncludingGst || "—"} — Stock: {row.stockQuantity}</p>)}</section>
            <section>
                <h2>Product images ({imageCount}/5 minimum)</h2>
                <p className="add-other-image-help">Upload a file or paste a direct <strong>https://</strong> image URL. A URL preview appears here before you submit it for approval.</p>
                <div className="add-other-images">{images.map((image, index) => {
                    const imageUrl = image.url.trim();
                    const canPreview = /^https:\/\/\S+$/i.test(imageUrl);

                    return <div className="add-other-image-card" key={index}>
                        <div className="add-other-image-label"><strong>Image {index + 1}</strong>{imageUrl && <span className="add-other-image-source">URL selected</span>}</div>
                        <label className="add-other-file-input">Choose file
                            <input type="file" accept="image/*" onChange={event => setImage(index, { file: event.target.files?.[0] || null, url: "" })} />
                        </label>
                        <span>or paste image URL</span>
                        <input type="url" maxLength="2000" placeholder="https://image-url" value={image.url} onChange={event => setImage(index, { url: event.target.value, file: null })} />
                        {canPreview && <div className="add-other-image-preview">
                            {imagePreviewErrors[index]
                                ? <p>Image preview could not be loaded. Check that this is a public direct image URL.</p>
                                : <img src={imageUrl} alt={`Product image ${index + 1} preview`} onError={() => setImagePreviewErrors(current => ({ ...current, [index]: true }))} />}
                        </div>}
                    </div>;
                })}</div>
            </section>
            <footer><button type="button" className="secondary" onClick={() => navigate("/seller/products")}>My Products</button><button disabled={saving || loadingTemplates || (form.serialTrackingRequired && loadingDistributors)} type="submit">{saving ? "Submitting…" : "Submit for Admin approval"}</button></footer>
        </form>
    </main>;
}
