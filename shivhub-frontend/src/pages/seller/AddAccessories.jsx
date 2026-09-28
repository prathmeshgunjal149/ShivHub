import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { getProductCategories, getProductSubcategories } from "../../services/purchaseService";
import SellerSidebar from "./SellerSidebar";
import "./SellerWorkspaces.css";

const MIN_PRODUCT_IMAGES = 5;
const MAX_PRODUCT_IMAGES = 15;

const createEmptyImage = () => ({
    type: "",
    file: null,
    url: "",
    preview: ""
});

const createEmptyImages = () =>
    Array.from(
        { length: MIN_PRODUCT_IMAGES },
        createEmptyImage
    );

const accessoryTypes = [
    "Covers and cases",
    "Tempered glass and screen protectors",
    "Chargers and charging adapters",
    "USB/data/charging cables",
    "Wired earphones and headphones",
    "Bluetooth headsets and earbuds",
    "Power banks",
    "Mobile batteries",
    "Memory cards and OTG devices",
    "Converters and adapters",
    "Mobile holders and stands",
    "Selfie sticks and tripods",
    "Other mobile accessories"
];

const specFields = {
    "Chargers and charging adapters": ["Wattage", "Ports", "Charging protocols", "Connector type"],
    "USB/data/charging cables": ["Connector ends", "Length", "Supported power", "Data capability"],
    "Covers and cases": ["Compatible models", "Material", "Case type"],
    "Tempered glass and screen protectors": ["Compatible models", "Material", "Protector type"],
    "Wired earphones and headphones": ["Connectivity", "Microphone", "Cable length"],
    "Bluetooth headsets and earbuds": ["Connectivity", "Microphone", "Battery", "Playback"],
    "Power banks": ["Capacity", "Output ports", "Charging input", "Compatibility"],
    "Mobile batteries": ["Capacity", "Compatible models", "Battery type"],
    "Memory cards and OTG devices": ["Capacity", "Speed class", "Interface"],
    "Converters and adapters": ["Input", "Output", "Compatibility"],
    "Mobile holders and stands": ["Material", "Mount type", "Compatibility"],
    "Selfie sticks and tripods": ["Connectivity", "Length", "Mount type"]
};

const initialForm = {
    name: "",
    brand: "",
    model: "",
    sellerSku: "",
    categoryId: "",
    subCategoryId: "",
    accessoryType: accessoryTypes[0],
    shortHighlights: "",
    description: "",
    colorOptions: "",
    compatibility: "Universal",
    warranty: "",
    packageContents: "",
    hsnCode: "",
    gstRate: "18",
    mrp: "",
    price: "",
    reorderThreshold: "",
    serialTracking: false
};

export default function AddAccessories() {
    const navigate = useNavigate();
    const [form, setForm] = useState(initialForm);
    const [specs, setSpecs] = useState({});
    const [images, setImages] = useState(createEmptyImages());
    const [categories, setCategories] = useState([]);
    const [subcategories, setSubcategories] = useState([]);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        getProductCategories()
            .then(data => setCategories(Array.isArray(data) ? data : []))
            .catch(() => setError("Categories could not be loaded."));
    }, []);

    useEffect(() => {
        if (!form.categoryId) {
            setSubcategories([]);
            return;
        }
        getProductSubcategories(form.categoryId)
            .then(data => setSubcategories(Array.isArray(data) ? data : []))
            .catch(() => setSubcategories([]));
    }, [form.categoryId]);

    const fields = useMemo(() => specFields[form.accessoryType] || ["Compatibility", "Material", "Notes"], [form.accessoryType]);

    const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
    const updateSpec = (key, value) => setSpecs(current => ({ ...current, [key]: value }));

    const changeImageType = (index, type) => {
        setImages(current => current.map((image, imageIndex) => {
            if (imageIndex !== index) {
                return image;
            }
            if (image.preview?.startsWith("blob:")) {
                URL.revokeObjectURL(image.preview);
            }
            return {
                type,
                file: null,
                url: "",
                preview: ""
            };
        }));
        setError("");
        setMessage("");
    };

    const handleFileChange = (index, file) => {
        if (!file) {
            return;
        }
        const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
        if (!allowedTypes.includes(file.type)) {
            setError("Only JPG, PNG and WEBP images are allowed.");
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            setError(`${file.name} is larger than 10MB.`);
            return;
        }
        const preview = URL.createObjectURL(file);
        setImages(current => current.map((image, imageIndex) => {
            if (imageIndex !== index) {
                return image;
            }
            if (image.preview?.startsWith("blob:")) {
                URL.revokeObjectURL(image.preview);
            }
            return {
                type: "FILE",
                file,
                url: "",
                preview
            };
        }));
        setError("");
        setMessage("");
    };

    const handleUrlChange = (index, value) => {
        setImages(current => current.map((image, imageIndex) => {
            if (imageIndex !== index) {
                return image;
            }
            if (image.preview?.startsWith("blob:")) {
                URL.revokeObjectURL(image.preview);
            }
            return {
                type: value.trim() ? "URL" : "",
                file: null,
                url: value,
                preview: value.trim()
            };
        }));
        setError("");
        setMessage("");
    };

    const clearImage = index => {
        setImages(current => current.map((image, imageIndex) => {
            if (imageIndex !== index) {
                return image;
            }
            if (image.preview?.startsWith("blob:")) {
                URL.revokeObjectURL(image.preview);
            }
            return createEmptyImage();
        }));
        setError("");
        setMessage("");
    };

    const addImageSlot = () => {
        setImages(current =>
            current.length >= MAX_PRODUCT_IMAGES
                ? current
                : [...current, createEmptyImage()]
        );
        setError("");
        setMessage("");
    };

    const removeImageSlot = index => {
        setImages(current => {
            if (current.length <= MIN_PRODUCT_IMAGES) {
                return current;
            }
            const image = current[index];
            if (image?.preview?.startsWith("blob:")) {
                URL.revokeObjectURL(image.preview);
            }
            return current.filter((_, imageIndex) => imageIndex !== index);
        });
        setError("");
        setMessage("");
    };

    const validateImages = () => {
        const filledImages = images.filter(image =>
            (image.type === "FILE" && image.file) ||
            (image.type === "URL" && image.url.trim())
        );

        if (filledImages.length < MIN_PRODUCT_IMAGES || filledImages.length > MAX_PRODUCT_IMAGES) {
            return `Upload between ${MIN_PRODUCT_IMAGES} and ${MAX_PRODUCT_IMAGES} product images.`;
        }

        for (let index = 0; index < images.length; index += 1) {
            const image = images[index];
            if (!image.type && !image.file && !image.url.trim()) {
                continue;
            }
            if (image.type === "FILE" && !image.file) {
                return `Please add Image ${index + 1}.`;
            }
            if (image.type === "URL") {
                if (!image.url.trim()) {
                    return `Please enter Image ${index + 1} URL.`;
                }
                try {
                    new URL(image.url.trim());
                } catch {
                    return `Image ${index + 1} URL is not valid.`;
                }
            }
            if (image.type !== "FILE" && image.type !== "URL") {
                return `Please choose Computer Upload or Image URL for Image ${index + 1}.`;
            }
        }

        return null;
    };

    const submit = async event => {
        event.preventDefault();
        setError("");
        setMessage("");

        if (!form.name.trim() || !form.brand.trim() || !form.categoryId || !form.price) {
            setError("Product name, brand, category and GST-inclusive selling price are required.");
            return;
        }
        const imageError = validateImages();
        if (imageError) {
            setError(imageError);
            return;
        }

        const specificationDetails = JSON.stringify({
            module: "ACCESSORY",
            accessoryType: form.accessoryType,
            compatibility: form.compatibility,
            warranty: form.warranty,
            packageContents: form.packageContents,
            reorderThreshold: form.reorderThreshold,
            serialTracking: form.serialTracking,
            specs
        });

        const product = {
            name: form.name.trim(),
            brand: form.brand.trim(),
            model: form.model.trim(),
            sellerSku: form.sellerSku.trim(),
            productType: "ACCESSORY",
            physicalCondition: "NEW",
            purchaseTaxTreatment: "REGULAR_GST",
            saleTaxTreatment: "REGULAR_GST",
            accessoryType: form.accessoryType,
            compatibility: form.compatibility.trim(),
            warrantyDetails: form.warranty.trim(),
            packageContents: form.packageContents.trim(),
            reorderThreshold: form.reorderThreshold ? Number(form.reorderThreshold) : null,
            serialTrackingRequired: form.serialTracking,
            taxTreatmentBasis: "Default accessory workflow. Purchase and sale tax treatment are stored independently and must be reviewed by seller/accountant when changed.",
            categoryId: Number(form.categoryId),
            subCategoryId: form.subCategoryId ? Number(form.subCategoryId) : null,
            description: form.description.trim(),
            shortHighlights: form.shortHighlights.trim(),
            colorOptions: form.colorOptions.trim(),
            hsnCode: form.hsnCode.trim(),
            gstRate: Number(form.gstRate || 0),
            price: Number(form.price),
            stock: 0,
            specificationDetails
        };

        const multipart = new FormData();
        multipart.append("product", new Blob([JSON.stringify(product)], { type: "application/json" }));
        images
            .filter(image => image.type === "FILE" && image.file)
            .forEach(image => multipart.append("images", image.file));
        const imageUrls = images
            .filter(image => image.type === "URL" && image.url.trim())
            .map(image => image.url.trim());
        // The seller API receives this part as JSON. Keeping it identical to
        // the normal Add Product workflow allows URL-only and mixed uploads.
        multipart.append(
            "imageUrls",
            new Blob([JSON.stringify(imageUrls)], { type: "application/json" })
        );

        try {
            setSaving(true);
            await api.post("/api/products/seller", multipart);
            setMessage("Accessory submitted for admin approval. Stock will be received through purchase entry.");
            setForm(initialForm);
            setSpecs({});
            setImages(createEmptyImages());
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Accessory could not be submitted.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="seller-workspace-page">
            <SellerSidebar />
            <main className="seller-workspace-main">
                <header className="workspace-hero">
                    <div>
                        <span>ACCESSORIES</span>
                        <h1>Add Accessories</h1>
                        <p>Create mobile accessories without phone-only fields like RAM processor or IMEI. Stock still comes from purchases.</p>
                    </div>
                    <button type="button" onClick={() => navigate("/seller/products")}>My products</button>
                </header>

                {error && <div className="workspace-alert error">{error}</div>}
                {message && <div className="workspace-alert success">{message}</div>}

                <form className="workspace-card accessory-form" onSubmit={submit}>
                    <section>
                        <h2>Basic details</h2>
                        <div className="workspace-grid three">
                            <label>Accessory type<select value={form.accessoryType} onChange={event => update("accessoryType", event.target.value)}>{accessoryTypes.map(type => <option key={type}>{type}</option>)}</select></label>
                            <label>Product name<input value={form.name} onChange={event => update("name", event.target.value)} placeholder="25W USB-C fast charger" /></label>
                            <label>Brand<input value={form.brand} onChange={event => update("brand", event.target.value)} placeholder="Samsung" /></label>
                            <label>Model<input value={form.model} onChange={event => update("model", event.target.value)} placeholder="EP-TA800" /></label>
                            <label>SKU / Barcode<input value={form.sellerSku} onChange={event => update("sellerSku", event.target.value)} /></label>
                            <label>Colour / variants<input value={form.colorOptions} onChange={event => update("colorOptions", event.target.value)} placeholder="Black, White" /></label>
                        </div>
                    </section>

                    <section>
                        <h2>Category & tax</h2>
                        <div className="workspace-grid four">
                            <label>Category<select value={form.categoryId} onChange={event => update("categoryId", event.target.value)}><option value="">Select category</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
                            <label>Subcategory<select value={form.subCategoryId} onChange={event => update("subCategoryId", event.target.value)}><option value="">All subcategories</option>{subcategories.map(subcategory => <option key={subcategory.id} value={subcategory.id}>{subcategory.name}</option>)}</select></label>
                            <label>HSN<input value={form.hsnCode} onChange={event => update("hsnCode", event.target.value)} /></label>
                            <label>GST %<input type="number" min="0" step="0.01" value={form.gstRate} onChange={event => update("gstRate", event.target.value)} /></label>
                            <label>MRP incl. GST<input type="number" min="0" step="0.01" value={form.mrp} onChange={event => update("mrp", event.target.value)} /></label>
                            <label>Selling price incl. GST<input type="number" min="0" step="0.01" value={form.price} onChange={event => update("price", event.target.value)} /></label>
                            <label>Reorder threshold<input type="number" min="0" value={form.reorderThreshold} onChange={event => update("reorderThreshold", event.target.value)} /></label>
                            <label className="check-row"><input type="checkbox" checked={form.serialTracking} onChange={event => update("serialTracking", event.target.checked)} /> Optional serial tracking</label>
                        </div>
                    </section>

                    <section>
                        <h2>Accessory specifications</h2>
                        <div className="workspace-grid three">
                            {fields.map(field => <label key={field}>{field}<input value={specs[field] || ""} onChange={event => updateSpec(field, event.target.value)} /></label>)}
                            <label>Compatibility<input value={form.compatibility} onChange={event => update("compatibility", event.target.value)} placeholder="Universal or model names" /></label>
                            <label>Warranty<input value={form.warranty} onChange={event => update("warranty", event.target.value)} /></label>
                            <label>Package contents<input value={form.packageContents} onChange={event => update("packageContents", event.target.value)} /></label>
                        </div>
                    </section>

                    <section>
                        <h2>Content & images</h2>
                        <div className="workspace-grid two">
                            <label>Highlights<textarea value={form.shortHighlights} onChange={event => update("shortHighlights", event.target.value)} placeholder="One highlight per line" /></label>
                            <label>Detailed description<textarea value={form.description} onChange={event => update("description", event.target.value)} /></label>
                        </div>
                        <div className="accessory-image-note">
                            <strong>Minimum 5 images required</strong>
                            <span>Use computer uploads, image URLs, or both. First image is treated as the main image.</span>
                        </div>
                        <div className="accessory-image-grid">
                            {images.map((image, index) => {
                                const isFile = image.type === "FILE";
                                const isUrl = image.type === "URL";

                                return (
                                    <article className="accessory-image-slot" key={index}>
                                        <div className="accessory-image-slot-head">
                                            <strong>Image {index + 1}</strong>
                                            {index === 0 && <span>Main</span>}
                                        </div>

                                        <div className="accessory-image-type-row">
                                            <button
                                                type="button"
                                                className={isFile ? "active" : ""}
                                                onClick={() => changeImageType(index, "FILE")}
                                                disabled={saving}
                                            >
                                                Computer
                                            </button>
                                            <button
                                                type="button"
                                                className={isUrl ? "active" : ""}
                                                onClick={() => changeImageType(index, "URL")}
                                                disabled={saving}
                                            >
                                                Image URL
                                            </button>
                                        </div>

                                        {isFile && (
                                            <label className="accessory-file-picker">
                                                <input
                                                    type="file"
                                                    accept="image/jpeg,image/png,image/webp"
                                                    onChange={event => handleFileChange(index, event.target.files?.[0])}
                                                    disabled={saving}
                                                />
                                                {image.preview ? (
                                                    <img src={image.preview} alt={`Accessory ${index + 1}`} />
                                                ) : (
                                                    <span>Choose JPG / PNG / WEBP</span>
                                                )}
                                            </label>
                                        )}

                                        {isUrl && (
                                            <div className="accessory-url-picker">
                                                <input
                                                    type="url"
                                                    value={image.url}
                                                    onChange={event => handleUrlChange(index, event.target.value)}
                                                    placeholder="Paste image URL..."
                                                    disabled={saving}
                                                />
                                                {image.preview && (
                                                    <img
                                                        src={image.preview}
                                                        alt={`Accessory URL ${index + 1}`}
                                                        onError={event => event.currentTarget.classList.add("image-preview-error")}
                                                    />
                                                )}
                                            </div>
                                        )}

                                        {(image.file || image.url) && (
                                            <button type="button" className="accessory-clear-image" onClick={() => clearImage(index)} disabled={saving}>
                                                Clear image
                                            </button>
                                        )}

                                        {images.length > MIN_PRODUCT_IMAGES && (
                                            <button type="button" className="accessory-remove-slot" onClick={() => removeImageSlot(index)} disabled={saving}>
                                                Remove slot
                                            </button>
                                        )}
                                    </article>
                                );
                            })}
                        </div>
                        <button
                            type="button"
                            className="accessory-add-image-slot"
                            onClick={addImageSlot}
                            disabled={saving || images.length >= MAX_PRODUCT_IMAGES}
                        >
                            + Add more image slot <span>{images.length}/{MAX_PRODUCT_IMAGES}</span>
                        </button>
                    </section>

                    <footer>
                        <button type="button" onClick={() => navigate("/seller/dashboard")}>Cancel</button>
                        <button type="submit" disabled={saving}>{saving ? "Submitting…" : "Submit accessory for approval"}</button>
                    </footer>
                </form>
            </main>
        </div>
    );
}
