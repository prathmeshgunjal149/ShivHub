import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CalendarDays, CheckCircle2, ChevronDown, Copy, CreditCard, Download, Eye, Home, MapPin, Package, PackageCheck, Search, ShoppingBag, SlidersHorizontal, Truck, XCircle } from "lucide-react";
import api, { API_BASE_URL } from "../../services/api";
import { getActiveOffers } from "../../services/marketingService";
import "./CustomerOrders.css";

const STATUS_FILTERS = [
    { key: "ALL", label: "All Orders", Icon: Package }, { key: "DELIVERED", label: "Delivered", Icon: CheckCircle2 },
    { key: "IN_TRANSIT", label: "In Transit", Icon: Truck }, { key: "CANCELLED", label: "Cancelled", Icon: XCircle }, { key: "RETURNS", label: "Returns", Icon: PackageCheck }
];
const TRACKING_STEPS = ["PENDING", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED"];
const assetUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;
const label = value => String(value || "Pending").replaceAll("_", " ").toLowerCase().replace(/\b\w/g, character => character.toUpperCase());
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const dateTime = value => !value || Number.isNaN(new Date(value).getTime()) ? "Not available" : new Date(value).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
const selectedVariant = item => {
    const value = item?.selectedAttributes;
    if (!value) return "";
    if (typeof value === "string") { try { return Object.entries(JSON.parse(value)).map(([key, itemValue]) => `${key}: ${itemValue}`).join(" · "); } catch { return value; } }
    return Object.entries(value).map(([key, itemValue]) => `${key}: ${itemValue}`).join(" · ");
};

export default function CustomerOrders() {
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]), [hero, setHero] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState("");
    const [query, setQuery] = useState(""), [filter, setFilter] = useState("ALL"), [sort, setSort] = useState("NEWEST"), [downloadingInvoiceId, setDownloadingInvoiceId] = useState(null), [copiedOrderId, setCopiedOrderId] = useState(null);
    const loadOrders = async () => {
        try { setLoading(true); setError(""); const response = await api.get("/api/orders/my"); setOrders(Array.isArray(response.data) ? response.data : []); }
        catch (requestError) { setError(requestError.response?.status === 401 || requestError.response?.status === 403 ? "Your login session has expired. Please login again." : requestError.response?.data?.message || requestError.message || "Unable to load your orders."); }
        finally { setLoading(false); }
    };
    useEffect(() => { loadOrders(); }, []);
    useEffect(() => { let active = true; getActiveOffers("CUSTOMER").then(items => { const banner = (Array.isArray(items) ? items : []).find(item => item.type === "BANNER" && item.placement === "CUSTOMER_ORDERS_HERO" && item.bannerUrl); if (active) setHero(banner || null); }).catch(() => { if (active) setHero(null); }); return () => { active = false; }; }, []);
    const filteredOrders = useMemo(() => orders.filter(order => {
        const status = String(order.orderStatus || "PENDING").toUpperCase();
        const matchesFilter = filter === "ALL" || (filter === "IN_TRANSIT" ? ["PROCESSING", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "CONFIRMED"].includes(status) : filter === "RETURNS" ? /RETURN|REFUND|REPLACEMENT/.test(status) : status === filter);
        const searchText = `${order.orderNumber || ""} ${order.items?.map(item => item.productName || "").join(" ") || ""}`.toLowerCase();
        return matchesFilter && searchText.includes(query.trim().toLowerCase());
    }).sort((left, right) => sort === "OLDEST" ? new Date(left.createdAt || 0) - new Date(right.createdAt || 0) : new Date(right.createdAt || 0) - new Date(left.createdAt || 0)), [orders, filter, query, sort]);
    const countFor = key => key === "ALL" ? orders.length : key === "IN_TRANSIT" ? orders.filter(order => ["PROCESSING", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "CONFIRMED"].includes(String(order.orderStatus || "").toUpperCase())).length : key === "RETURNS" ? orders.filter(order => /RETURN|REFUND|REPLACEMENT/.test(String(order.orderStatus || "").toUpperCase())).length : orders.filter(order => String(order.orderStatus || "").toUpperCase() === key).length;
    const downloadInvoice = async order => {
        if (!order?.orderId) return;
        try { setDownloadingInvoiceId(order.orderId); const response = await api.get(`/api/orders/${order.orderId}/invoice`, { responseType: "blob" }); const url = window.URL.createObjectURL(new Blob([response.data], { type: "application/pdf" })); const anchor = document.createElement("a"); anchor.href = url; anchor.download = `ShivHub-Invoice-${order.orderNumber || order.orderId}.pdf`; document.body.appendChild(anchor); anchor.click(); anchor.remove(); window.URL.revokeObjectURL(url); }
        catch (requestError) { alert(requestError.response?.data?.message || "Invoice download failed. Please try again."); } finally { setDownloadingInvoiceId(null); }
    };
    const copyOrderNumber = async order => { try { await navigator.clipboard?.writeText(order.orderNumber || String(order.orderId)); setCopiedOrderId(order.orderId); window.setTimeout(() => setCopiedOrderId(null), 1400); } catch { /* Clipboard may be unavailable. */ } };
    const statusStep = status => TRACKING_STEPS.findIndex(step => step === status);

    return <main className="customer-orders-page">
        <nav className="customer-orders-nav" aria-label="Customer navigation"><button type="button" className="customer-orders-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingBag size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" className="active" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/products")}><ShoppingBag size={18} /> Shop products</button><button type="button" onClick={() => navigate("/customer/orders")}><Package size={18} /> My Orders</button></div></nav>
        <section className="orders-hero" style={hero?.bannerUrl ? { backgroundImage: `linear-gradient(90deg, rgba(237,248,255,.96) 0%, rgba(237,248,255,.76) 42%, rgba(237,248,255,.12) 100%), url("${assetUrl(hero.bannerUrl)}")` } : undefined}>
            <div><span>SHIVHUB ORDERS</span><h1>{hero?.title || "My Orders"}</h1><p>{hero?.description || "Track, manage and view all your orders in one place."}</p></div>
            <aside aria-label="ShivHub benefits"><p><Truck size={19} /> Fast Delivery</p><p><CheckCircle2 size={19} /> Genuine Products</p><p><CreditCard size={19} /> Secure Payments</p><p><PackageCheck size={19} /> Easy Returns</p></aside>
        </section>
        <section className="orders-workspace">
            <div className="orders-toolbar"><nav className="orders-status-tabs" aria-label="Order status filters">{STATUS_FILTERS.map(({ key, label: title, Icon }) => <button type="button" key={key} className={filter === key ? "active" : ""} onClick={() => setFilter(key)}><Icon size={18} /> {title} <b>{countFor(key)}</b></button>)}</nav><div className="orders-find"><label><Search size={18} /><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search your orders…" aria-label="Search your orders" /></label><label className="orders-sort"><SlidersHorizontal size={17} /><select value={sort} onChange={event => setSort(event.target.value)} aria-label="Sort orders"><option value="NEWEST">Newest first</option><option value="OLDEST">Oldest first</option></select><ChevronDown size={16} /></label></div></div>
            {loading && <section className="orders-state"><span className="orders-spinner" /><h2>Loading your orders</h2><p>Fetching your latest order information.</p></section>}
            {!loading && error && <section className="orders-state orders-state--error"><XCircle size={34} /><h2>Unable to load orders</h2><p>{error}</p><button type="button" onClick={loadOrders}>Try again</button></section>}
            {!loading && !error && filteredOrders.length === 0 && <section className="orders-state"><ShoppingBag size={38} /><h2>{orders.length ? "No matching orders" : "No orders yet"}</h2><p>{orders.length ? "Try changing the search or status filter." : "Your future ShivHub orders will appear here."}</p><button type="button" onClick={() => navigate("/customer/products")}>Start shopping</button></section>}
            {!loading && !error && filteredOrders.length > 0 && <div className="orders-list">{filteredOrders.map(order => {
                const status = String(order.orderStatus || "PENDING").toUpperCase(), stepIndex = statusStep(status);
                return <article className={`customer-order-card status-${status.toLowerCase()}`} key={order.orderId}>
                    <header><div className="customer-order-reference"><span>Order ID:</span><strong>{order.orderNumber || `#${order.orderId}`}</strong><button type="button" aria-label="Copy order ID" title="Copy order ID" onClick={() => copyOrderNumber(order)}><Copy size={15} />{copiedOrderId === order.orderId && <em>Copied</em>}</button></div><div><span><CalendarDays size={16} /> Placed on {dateTime(order.createdAt)}</span><span><CreditCard size={16} /> {label(order.paymentMethod)}</span></div><b className={`customer-order-status status-${status.toLowerCase()}`}>{status === "DELIVERED" ? <CheckCircle2 size={17} /> : status === "CANCELLED" ? <XCircle size={17} /> : <Truck size={17} />}{label(status)}</b></header>
                    <div className="customer-order-card-main"><div className="customer-order-items">{(order.items?.length ? order.items : [null]).map((item, index) => <div className="customer-order-item" key={item?.id || index}><div className="customer-order-item-image">{item?.imageUrl ? <img src={assetUrl(item.imageUrl)} alt={item.productName || "Product"} onError={event => { event.currentTarget.style.display = "none"; }} /> : <Package size={30} />}</div><div><h2>{item?.productName || "Product details unavailable"}</h2>{selectedVariant(item) && <p>{selectedVariant(item)}</p>}<small>Qty: {item?.quantity || 0}{item?.sellerName ? ` · Sold by ${item.sellerName}` : ""}</small></div><strong>{money(item?.totalPrice)}</strong></div>)}</div><div className="customer-orders-timeline" aria-label={`Order status: ${label(status)}`}>{TRACKING_STEPS.filter(step => status !== "CANCELLED" || step === "PENDING").map((step, index) => <div className={index <= stepIndex ? "done" : ""} key={step}><i>{index <= stepIndex ? <CheckCircle2 size={17} /> : <Package size={15} />}</i><strong>{label(step)}</strong>{index === 0 && <small>{dateTime(order.createdAt)}</small>}</div>)}</div></div>
                    <footer><div className="customer-order-address"><MapPin size={23} /><div><strong>Delivery Address</strong><p>{order.shippingAddress || [order.deliveryAddressLine1, order.deliveryAddressLine2, order.deliveryCity, order.deliveryState, order.deliveryPincode].filter(Boolean).join(", ") || "Not available"}</p></div></div><div className="customer-order-actions">{status === "DELIVERED" && <button type="button" className="invoice" disabled={downloadingInvoiceId === order.orderId} onClick={() => downloadInvoice(order)}><Download size={18} />{downloadingInvoiceId === order.orderId ? "Preparing invoice…" : "Download Invoice"}</button>}<button type="button" className="details" onClick={() => navigate(`/customer/orders/${order.orderId}`)}><Eye size={18} /> View Order Details</button><strong className="customer-order-total"><small>Grand total</small>{money(order.grandTotal)}</strong></div></footer>
                </article>;
            })}</div>}
        </section>
    </main>;
}
