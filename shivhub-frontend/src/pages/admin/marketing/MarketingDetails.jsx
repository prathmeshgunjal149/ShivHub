import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createMarketing, getMarketing, updateMarketing } from "../../../services/marketingService";
import { uploadAdminCampaignBanner } from "../../../services/adminManagementService";
import { API_BASE_URL } from "../../../services/api";
import { getAdminCategories, getCustomers, getProducts } from "../../../services/adminService";
import "./MarketingDashboard.css";

const initial = { title: "", description: "", bannerUrl: "", placement: "", targetFilters: "", audience: "CUSTOMER", couponCode: "", discountPercent: "", productId: "", targetCustomerId: "", active: true, sendEmail: false, startsAt: "", endsAt: "" };
const imageLimit = placement => placement === "CUSTOMER_CATEGORY_TILE" ? 4 : placement === "CUSTOMER_CATEGORY" ? 5 : 1;
const blankAssetCard = () => ({ bannerUrl: "", title: "", description: "" });

export default function MarketingDetails({ fixedType }) {
    const { type, id } = useParams();
    const navigate = useNavigate();
    const kind = fixedType || (type || "offers").replace(/s$/, "").toUpperCase();
    const isOffer = kind === "OFFER";
    const isCoupon = kind === "COUPON";
    const isBanner = kind === "BANNER";
    const [form, setForm] = useState({ ...initial, type: kind });
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [uploadingBanner, setUploadingBanner] = useState(false);
    const [bannerUrls, setBannerUrls] = useState([]);
    const [localBannerUrls, setLocalBannerUrls] = useState([]);
    const [assetCards, setAssetCards] = useState([]);

    useEffect(() => {
        if (isOffer) getProducts().then(setProducts).catch(() => setError("Products could not be loaded."));
        if (isBanner) getAdminCategories().then(items => setCategories(Array.isArray(items) ? items : [])).catch(() => setError("Categories could not be loaded."));
        if (isCoupon) getCustomers().then(setCustomers).catch(() => setError("Customers could not be loaded."));
        if (id && id !== "new") getMarketing(id)
            .then(item => {
                const loaded = { ...initial, ...item, sendEmail: false, startsAt: item.startsAt?.slice(0, 16) || "", endsAt: item.endsAt?.slice(0, 16) || "" };
                setForm(loaded);
                if (["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_TILE"].includes(loaded.placement)) {
                    setAssetCards(Array.from({ length: imageLimit(loaded.placement) }, (_, index) => index === 0
                        ? { bannerUrl: loaded.bannerUrl || "", title: loaded.title || "", description: loaded.description || "" }
                        : blankAssetCard()));
                }
            })
            .catch(() => setError("Could not load this offer."));
    }, [id, isOffer, isCoupon, isBanner]);

    const change = event => {
        const { name, type, checked, value } = event.target;
        setForm(current => ({ ...current, [name]: type === "checkbox" ? checked : value }));
        if (name === "placement") setAssetCards(["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_TILE"].includes(value) ? Array.from({ length: imageLimit(value) }, blankAssetCard) : []);
    };
    const uploadBanner = async event => {
        const files = Array.from(event.target.files || []);
        if (!files.length) return;
        const maximum = imageLimit(form.placement);
        if (files.length > maximum) { setError(`Choose up to ${maximum} image${maximum === 1 ? "" : "s"} for this placement.`); event.target.value = ""; return; }
        setLocalBannerUrls(files.map(file => URL.createObjectURL(file)));
        setUploadingBanner(true);
        setError("");
        try {
            const uploaded = await Promise.all(files.map(uploadAdminCampaignBanner));
            const urls = uploaded.map(item => item.bannerUrl).filter(Boolean);
            setLocalBannerUrls([]);
            setBannerUrls(urls);
            if (["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_TILE"].includes(form.placement)) setAssetCards(current => Array.from({ length: maximum }, (_, index) => ({ ...(current[index] || blankAssetCard()), bannerUrl: urls[index] || current[index]?.bannerUrl || "" })));
            setForm(current => ({ ...current, bannerUrl: urls[0] || current.bannerUrl }));
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Banner upload failed. Use JPG, PNG or WEBP up to 5 MB.");
        } finally { setUploadingBanner(false); event.target.value = ""; }
    };
    const uploadAssetCard = async (index, event) => {
        const file = event.target.files?.[0];
        if (!file) return;
        const localPreview = URL.createObjectURL(file);
        setAssetCards(current => current.map((card, cardIndex) => cardIndex === index ? { ...card, localPreview } : card));
        setUploadingBanner(true); setError("");
        try {
            const uploaded = await uploadAdminCampaignBanner(file);
            setAssetCards(current => current.map((card, cardIndex) => cardIndex === index ? { ...card, bannerUrl: uploaded.bannerUrl || "", localPreview: "" } : card));
        } catch (requestError) { setError(requestError.response?.data?.message || "Image upload failed. Use JPG, PNG or WEBP up to 5 MB."); }
        finally { setUploadingBanner(false); event.target.value = ""; }
    };
    const updateAssetCard = (index, key, value) => setAssetCards(current => current.map((card, cardIndex) => cardIndex === index ? { ...card, [key]: value } : card));
    const selectProduct = event => {
        const productId = event.target.value;
        const product = products.find(item => String(item.id) === productId);
        setForm(current => ({
            ...current,
            productId,
            sendEmail: productId ? true : current.sendEmail,
            title: product && !current.title ? `${product.name} special offer` : current.title
        }));
    };
    const save = async event => {
        event.preventDefault();
        setError("");
        const discount = form.discountPercent === "" ? null : Number(form.discountPercent);
        if (form.productId && (discount === null || discount <= 0 || discount > 100)) {
            setError("A selected product needs a discount from 1 to 100%.");
            return;
        }
        setSaving(true);
        try {
            const payload = { ...form, type: kind, productId: form.productId ? Number(form.productId) : null, targetCustomerId: form.targetCustomerId ? Number(form.targetCustomerId) : null, discountPercent: discount, startsAt: form.startsAt || null, endsAt: form.endsAt || null };
            const configuredAssets = assetCards.filter(card => card.bannerUrl);
            const imageCampaigns = isBanner && configuredAssets.length ? configuredAssets.map(card => ({ ...payload, bannerUrl: card.bannerUrl, title: card.title.trim() || payload.title, description: card.description.trim() || payload.description })) : isBanner && bannerUrls.length ? bannerUrls.map(bannerUrl => ({ ...payload, bannerUrl })) : [payload];
            if (id && id !== "new") {
                await updateMarketing(id, imageCampaigns[0]);
                if (imageCampaigns.length > 1) await Promise.all(imageCampaigns.slice(1).map(createMarketing));
            } else await Promise.all(imageCampaigns.map(createMarketing));
            navigate(`/admin/marketing/${kind.toLowerCase()}s`);
        } catch (requestError) { setError(requestError.response?.data?.message || "Could not save the offer."); }
        finally { setSaving(false); }
    };

    return <main className="marketing">
        <Link className="back-link" to={`/admin/marketing/${kind.toLowerCase()}s`}>← Back</Link>
        <h1>{id && id !== "new" ? "Edit" : "Create"} {isOffer ? "offer" : kind.toLowerCase()}</h1>
        {isOffer && <p className="lead">Choose a product to publish a customer-only product discount and email the offer to all customers.</p>}
        {error && <p className="marketing-error">{error}</p>}
        <form className="marketing-form" onSubmit={save}>
            {isOffer && <label>Specific product (optional)<select name="productId" value={form.productId || ""} onChange={selectProduct}><option value="">General offer — no product discount</option>{products.map(product => <option key={product.id} value={product.id}>{product.name} — ₹{Number(product.price || 0).toLocaleString("en-IN")}</option>)}</select><small>Selecting a product applies this percentage directly to that product for customers.</small></label>}
            <label>Offer title<input required name="title" value={form.title} onChange={change} placeholder="iPhone 15 – 10% OFF" /></label>
            {isBanner && <><label>Customer dashboard placement<select name="placement" value={form.placement || ""} onChange={change}><option value="">General banner / carousel</option><option value="CUSTOMER_HERO">Top hero carousel (multiple active banners rotate)</option><option value="CUSTOMER_SECOND_HAND">Second-hand market card</option><option value="CUSTOMER_REFER_EARN">Refer &amp; Earn card</option><option value="CUSTOMER_ORDERS_HERO">My Orders page hero image</option><option value="CUSTOMER_SUPPORT_HERO">Help, policy &amp; support page hero</option><option value="CUSTOMER_CATEGORY">Category hero slider (up to 5 banners rotate)</option><option value="CUSTOMER_CATEGORY_TILE">Category launch / offer tile (up to 4 tiles)</option></select><small>Use Help, policy &amp; support page hero with its exact page key in “Target filters”, such as help, contact, grievance, warranty-policy, about or privacy-policy.</small></label>{["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_TILE"].includes(form.placement) && <label>Show this image for category<select required name="targetFilters" value={form.targetFilters || ""} onChange={change}><option value="">Choose category</option>{categories.map(category => <option key={category.id} value={category.name}>{category.name}</option>)}{form.targetFilters && !categories.some(category => category.name === form.targetFilters) && <option value={form.targetFilters}>{form.targetFilters}</option>}</select><small>Binding is exact: a Mobiles image appears only on Mobiles; Men’s Fashion and Electronics have their own separate slider and tiles.</small></label>}{form.placement === "CUSTOMER_SUPPORT_HERO" && <label>Target page key<input required name="targetFilters" value={form.targetFilters || ""} onChange={change} placeholder="help, contact, grievance, warranty-policy…" /><small>The same uploaded image appears only on this exact public page. A separate banner can be added for each page.</small></label>}</>}
            {!form.productId && <label>Audience<select name="audience" value={form.audience} onChange={change}><option value="CUSTOMER">Customer panel</option><option value="SELLER">Seller panel</option><option value="ALL">Customer & seller panels</option></select></label>}
            {isCoupon && <label>Assign to a specific customer (optional)<select name="targetCustomerId" value={form.targetCustomerId || ""} onChange={change}><option value="">All eligible customers</option>{customers.map(customer => <option key={customer.id} value={customer.id}>{customer.name} — {customer.email}</option>)}</select><small>A selected customer alone can use this coupon and receives the email directly.</small></label>}
            {form.productId && <input type="hidden" name="audience" value="CUSTOMER" />}
            <label>Message<textarea required name="description" value={form.description || ""} onChange={change} placeholder="iPhone 15 has 10% off. Let's enjoy shopping!" /></label>
            {isBanner && ["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_TILE"].includes(form.placement) ? <>
                <label className="marketing-upload-zone">Upload all images together<input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={uploadingBanner} onChange={uploadBanner} /><small>{uploadingBanner ? "Uploading images…" : `Choose up to ${imageLimit(form.placement)} images. They are placed in the cards below, then you can change every card separately.`}</small></label>
                <section className="marketing-asset-card-grid" aria-label="Category visual cards">
                    {assetCards.map((asset, index) => <article className="marketing-asset-card" key={index}>
                        <div className="marketing-asset-card-head"><strong>{form.placement === "CUSTOMER_CATEGORY_TILE" ? `Offer tile ${index + 1}` : `Slider image ${index + 1}`}</strong><span>{asset.localPreview ? "Previewing" : asset.bannerUrl ? "Ready" : "Optional"}</span></div>
                        <div className="marketing-asset-preview">{asset.localPreview || asset.bannerUrl ? <><img src={asset.localPreview || (/^https?:\/\//i.test(asset.bannerUrl) ? asset.bannerUrl : `${API_BASE_URL}${asset.bannerUrl}`)} alt={`${form.targetFilters || "Category"} visual ${index + 1}`} /><button type="button" onClick={() => setAssetCards(current => current.map((card, cardIndex) => cardIndex === index ? { ...card, bannerUrl: "", localPreview: "" } : card))}>Remove image</button></> : <p>Upload an image or paste its URL</p>}</div>
                        <label>Image URL<input value={asset.bannerUrl} maxLength="2000" onChange={event => updateAssetCard(index, "bannerUrl", event.target.value)} placeholder="https://…" /></label>
                        <label className="marketing-card-upload">Upload image<input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingBanner} onChange={event => uploadAssetCard(index, event)} /></label>
                        <label>Card title<input value={asset.title} maxLength="255" onChange={event => updateAssetCard(index, "title", event.target.value)} placeholder={form.title || "Optional title"} /></label>
                        <label>Card message<textarea value={asset.description} maxLength="2000" onChange={event => updateAssetCard(index, "description", event.target.value)} placeholder={form.description || "Optional message"} /></label>
                    </article>)}
                </section>
            </> : <>
                <label className={isBanner ? "marketing-upload-zone" : ""}>Offer banner image (optional)<input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingBanner} onChange={uploadBanner} /><small>{uploadingBanner ? "Uploading image…" : "JPG, PNG or WEBP; maximum 5 MB."}</small></label>
                <label>Or banner image URL<input name="bannerUrl" value={form.bannerUrl || ""} maxLength="2000" onChange={event => { setBannerUrls([]); change(event); }} placeholder="https://…" /></label>
                {(localBannerUrls.length ? localBannerUrls : bannerUrls.length ? bannerUrls : form.bannerUrl ? [form.bannerUrl] : []).length > 0 && <section className="marketing-banner-preview-grid" aria-label="Banner image preview">{(localBannerUrls.length ? localBannerUrls : bannerUrls.length ? bannerUrls : [form.bannerUrl]).map((url, index) => <figure key={`${url}-${index}`}><img src={localBannerUrls.length ? url : /^https?:\/\//i.test(url) ? url : `${API_BASE_URL}${url}`} alt={`Banner preview ${index + 1}`} /><figcaption>{localBannerUrls.length ? "Local preview" : `Image ${index + 1}`}</figcaption>{bannerUrls.length > 0 && <button type="button" onClick={() => { const next = bannerUrls.filter((_, itemIndex) => itemIndex !== index); setBannerUrls(next); setForm(current => ({ ...current, bannerUrl: next[0] || "" })); }}>Remove</button>}</figure>)}</section>}
            </>}
            <div className="two"><label>Coupon code (optional)<input name="couponCode" value={form.couponCode || ""} onChange={change} /></label><label>Discount %<input required={Boolean(form.productId)} type="number" min="0" max="100" name="discountPercent" value={form.discountPercent || ""} onChange={change} /></label></div>
            <div className="two"><label>Starts<input type="datetime-local" name="startsAt" value={form.startsAt} onChange={change} /></label><label>Ends<input type="datetime-local" name="endsAt" value={form.endsAt} onChange={change} /></label></div>
            <label className="check"><input type="checkbox" name="active" checked={form.active} onChange={change} /> Show this offer to customers</label>
            {form.targetCustomerId ? <p className="product-offer-mail-note">This coupon will be emailed only to the selected customer.</p> : form.productId ? <p className="product-offer-mail-note">This product offer will be emailed automatically to all enabled customers when it is created.</p> : <label className="check"><input type="checkbox" name="sendEmail" checked={form.sendEmail} onChange={change} /> Email this offer to all customers now</label>}
            <button className="primary" disabled={saving}>{saving ? "Saving…" : form.productId ? "Save product offer" : "Save offer"}</button>
        </form>
    </main>;
}
