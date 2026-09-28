import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./SellerDataModule.css";

const config = {
    products: { title: "My products", endpoint: "/api/products/seller", empty: "No products added yet." },
    inventory: { title: "Inventory", endpoint: "/api/products/seller", empty: "No inventory found." },
    orders: { title: "ShivHub orders", endpoint: "/api/seller/orders", empty: "No customer orders yet." },
    branches: { title: "My branches", endpoint: "/api/seller/shops", empty: "No branch created yet." }
};

/** Shared real-data screen for seller products, inventory, orders and branches. */
export default function SellerDataModule() {
    const key = useLocation().pathname.split("/").pop();
    const module = config[key] || config.products;
    const navigate = useNavigate();
    const [items, setItems] = useState([]); const [error, setError] = useState(""); const [loading, setLoading] = useState(true);
    useEffect(() => { setLoading(true); api.get(module.endpoint).then(({ data }) => setItems(Array.isArray(data) ? data : [])).catch(err => setError(err?.response?.data?.message || "Unable to load data.")).finally(() => setLoading(false)); }, [module.endpoint]);
    const rows = useMemo(() => items, [items]);
    return <div className="seller-data-page"><SellerSidebar /><main className="seller-data-main"><header><p>SELLER MODULE</p><h1>{module.title}</h1><button type="button" onClick={() => navigate("/seller/dashboard")}>Dashboard</button></header>{error && <div className="seller-data-error">{error}</div>}{loading ? <div className="seller-data-state">Loading…</div> : rows.length === 0 ? <div className="seller-data-state">{module.empty}</div> : <section className="seller-data-list">{rows.map(item => <article key={item.id}><div><strong>{item.name || item.orderNumber}</strong><small>{item.shopCode ? `${item.shopCode} · ${item.city || ""}` : item.category || item.orderStatus || ""}</small></div><div>{key === "orders" ? <><b>₹{Number(item.sellerTotal || 0).toLocaleString("en-IN")}</b><small>{item.itemCount} item(s)</small></> : key === "inventory" ? <><b>{item.stock ?? 0} units</b><small>{item.active ? "Active" : "Inactive"}</small></> : <><b>{item.price ? `₹${Number(item.price).toLocaleString("en-IN")}` : item.active ? "Active" : "Inactive"}</b><small>{item.approvalStatus || item.name}</small></>}</div></article>)}</section>}</main></div>;
}
