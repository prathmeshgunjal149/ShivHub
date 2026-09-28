import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { getPrimaryProductImage } from "../../utils/imageUrl";
import SearchAutocomplete from "../../components/common/SearchAutocomplete/SearchAutocomplete";
import { getSellerProductSearchSuggestions } from "../../services/searchSuggestionService";
import SellerSidebar from "./SellerSidebar";
import "./SellerProducts.css";

const money = (value) =>
    `₹${Number(value || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;

export default function SellerProducts() {
    const navigate = useNavigate();
    const [products, setProducts] = useState([]);
    const [search, setSearch] = useState("");
    const [category, setCategory] = useState("ALL");
    const [status, setStatus] = useState("ALL");
    const [stock, setStock] = useState("ALL");
    const [editing, setEditing] = useState(null);
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const load = () =>
        api
            .get("/api/products/seller")
            .then(({ data }) => setProducts(Array.isArray(data) ? data : []))
            .catch((err) =>
                setError(err.response?.data?.message || "Unable to load products.")
            );

    useEffect(() => {
        void load();
    }, []);

    const categories = useMemo(
        () => [...new Set(products.map((product) => product.category).filter(Boolean))].sort(),
        [products]
    );

    const rows = useMemo(
        () =>
            products.filter((product) => {
                const term = search.toLowerCase();
                return (
                    (!term ||
                        [product.name, product.category, product.id].some((value) =>
                            String(value || "").toLowerCase().includes(term)
                        )) &&
                    (category === "ALL" || product.category === category) &&
                    (status === "ALL" || product.approvalStatus === status) &&
                    (stock === "ALL" ||
                        (stock === "LOW"
                            ? Number(product.stock || 0) <= 5
                            : Number(product.stock || 0) > 5))
                );
            }),
        [products, search, category, status, stock]
    );

    const save = async (event) => {
        event.preventDefault();
        try {
            setSaving(true);
            await api.put(`/api/products/${editing.id}`, {
                ...editing,
                price: Number(editing.price),
                stock: Number(editing.stock),
            });
            setEditing(null);
            await load();
        } catch (err) {
            setError(err.response?.data?.message || "Product could not be updated.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="seller-products-page">
            <SellerSidebar />
            <main className="seller-products-main">
                <header>
                    <div>
                        <span>CATALOGUE</span>
                        <h1>My products</h1>
                        <p>Find, review and update your shop catalogue.</p>
                    </div>
                    <button onClick={() => navigate("/seller/add-product")}>+ Add product</button>
                </header>

                {error && <div className="product-error">{error}</div>}

                <section className="product-filter-bar">
                    <SearchAutocomplete
                        value={search}
                        onChange={setSearch}
                        onSelect={(suggestion) => setSearch(suggestion.label || "")}
                        onEnterWithoutSelection={() => {}}
                        fetchSuggestions={getSellerProductSearchSuggestions}
                        placeholder="Search name, category or product ID"
                    />
                    <select value={category} onChange={(event) => setCategory(event.target.value)}>
                        <option value="ALL">All categories</option>
                        {categories.map((item) => (
                            <option key={item}>{item}</option>
                        ))}
                    </select>
                    <select value={status} onChange={(event) => setStatus(event.target.value)}>
                        <option value="ALL">All approvals</option>
                        <option value="APPROVED">Approved</option>
                        <option value="PENDING">Pending</option>
                        <option value="REJECTED">Rejected</option>
                    </select>
                    <select value={stock} onChange={(event) => setStock(event.target.value)}>
                        <option value="ALL">All stock</option>
                        <option value="LOW">Low stock (≤5)</option>
                        <option value="AVAILABLE">Above low stock</option>
                    </select>
                    <b>{rows.length} products</b>
                </section>

                <section className="seller-product-catalogue">
                    {rows.map((product) => {
                        const photo = getPrimaryProductImage(product);
                        return (
                            <article key={product.id}>
                                <div className="catalogue-image">
                                    {photo && (
                                        <img
                                            src={photo}
                                            alt={product.name}
                                            loading="lazy"
                                            onError={(event) => {
                                                event.currentTarget.style.display = "none";
                                                event.currentTarget.parentElement?.classList.add("image-failed");
                                            }}
                                        />
                                    )}
                                    <span className="image-fallback">📦</span>
                                </div>
                                <div className="catalogue-info">
                                    <div>
                                        <small>{product.category || "Uncategorised"}</small>
                                        <h2 title={product.name}>{product.name}</h2>
                                        <span
                                            className={`catalogue-status ${String(
                                                product.approvalStatus || "pending"
                                            ).toLowerCase()}`}
                                        >
                                            {product.approvalStatus || "PENDING"}
                                        </span>
                                    </div>
                                    <strong>{money(product.price)}</strong>
                                    <p>
                                        Stock: <b>{product.stock ?? 0}</b>
                                    </p>
                                </div>
                                <button className="edit-product" onClick={() => setEditing({ ...product })}>
                                    Edit product
                                </button>
                                {product.variantsEnabled&&<button className="edit-product" onClick={()=>navigate(`/seller/products/${product.id}/variants`)}>Variant prices & stock</button>}
                            </article>
                        );
                    })}
                </section>

                {!rows.length && <div className="product-empty">No product matches these filters.</div>}

                {editing && (
                    <div className="product-modal">
                        <form onSubmit={save}>
                            <div>
                                <h2>Edit product</h2>
                                <button type="button" onClick={() => setEditing(null)}>
                                    ×
                                </button>
                            </div>
                            <label>
                                Product name
                                <input
                                    required
                                    value={editing.name || ""}
                                    onChange={(event) => setEditing({ ...editing, name: event.target.value })}
                                />
                            </label>
                            <label>
                                Category
                                <input
                                    value={editing.category || ""}
                                    onChange={(event) => setEditing({ ...editing, category: event.target.value })}
                                />
                            </label>
                            <label>
                                Selling price
                                <input
                                    required
                                    type="number"
                                    min="0"
                                    value={editing.price ?? ""}
                                    onChange={(event) => setEditing({ ...editing, price: event.target.value })}
                                />
                            </label>
                            <label>
                                Stock
                                <input
                                    required
                                    type="number"
                                    min="0"
                                    value={editing.stock ?? ""}
                                    onChange={(event) => setEditing({ ...editing, stock: event.target.value })}
                                />
                            </label>
                            <label>
                                Image URL
                                <input
                                    value={editing.imageUrl || ""}
                                    onChange={(event) => setEditing({ ...editing, imageUrl: event.target.value })}
                                />
                            </label>
                            <label>
                                Description
                                <textarea
                                    value={editing.description || ""}
                                    onChange={(event) => setEditing({ ...editing, description: event.target.value })}
                                />
                            </label>
                            <p>Saving changes sends this product for admin approval again.</p>
                            <button disabled={saving}>{saving ? "Saving…" : "Save & request approval"}</button>
                        </form>
                    </div>
                )}
            </main>
        </div>
    );
}
