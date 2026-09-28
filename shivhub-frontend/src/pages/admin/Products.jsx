import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getProducts, updateProductOffer } from "../../services/adminService";
import "./AdminManagement.css";

export default function Products() {

    const [items, setItems] = useState([]);
    const [error, setError] = useState("");
    const [offerDrafts, setOfferDrafts] = useState({});
    const [savingOfferId, setSavingOfferId] = useState(null);
    const [categoryFilter, setCategoryFilter] = useState("ALL");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [query, setQuery] = useState("");

    const categories = useMemo(() => [...new Set(items.map(product => product.category || "Uncategorised"))].sort(), [items]);
    const visibleItems = useMemo(() => items.filter(product => {
        const name = `${product.name || ""} ${product.brand || ""} ${product.model || ""}`.toLowerCase();
        return (categoryFilter === "ALL" || (product.category || "Uncategorised") === categoryFilter)
            && (statusFilter === "ALL" || product.approvalStatus === statusFilter)
            && (!query.trim() || name.includes(query.trim().toLowerCase()));
    }), [items, categoryFilter, statusFilter, query]);

    useEffect(() => {

        loadProducts();

    }, []);


    const loadProducts = async () => {

        try {

            setError("");

            const data = await getProducts();

            setItems(data);

            setOfferDrafts(
                Object.fromEntries(
                    data.map(product => [product.id, product.offerPercentage ?? ""])
                )
            );

        } catch (error) {

            setError(
                error.response?.data?.message ||
                "Could not load products."
            );
        }
    };

    const saveOffer = async (productId) => {
        const value = offerDrafts[productId];
        const discount = value === "" ? null : Number(value);

        if (discount !== null && (discount < 0 || discount > 100)) {
            setError("Offer discount must be between 0 and 100%.");
            return;
        }

        try {
            setSavingOfferId(productId);
            setError("");
            const updated = await updateProductOffer(productId, discount);
            setItems(previous => previous.map(product => product.id === productId ? updated : product));
            setOfferDrafts(previous => ({ ...previous, [productId]: updated.offerPercentage ?? "" }));
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Could not update the product offer.");
        } finally {
            setSavingOfferId(null);
        }
    };


    return (

        <section className="admin-management">

            {/* HEADER */}

            <header>

                <div>

                    <p className="eyebrow">
                        Catalogue Management
                    </p>

                    <h1>
                        Products
                    </h1>

                    <p>
                        Manage seller products and add
                        your own ShivHub products.
                    </p>

                </div>


                <div className="admin-product-actions">

                    <Link
                        className="primary-button"
                        to="/admin/products/add"
                    >
                        + Add Product
                    </Link>


                    <Link
                        className="back-link"
                        to="/admin/dashboard"
                    >
                        Dashboard
                    </Link>

                </div>

            </header>


            {/* ERROR */}

            {error && (

                <p className="admin-error">
                    {error}
                </p>

            )}

            <div className="management-card filter-bar">
                <label>Category<select value={categoryFilter} onChange={event => setCategoryFilter(event.target.value)}><option value="ALL">All categories</option>{categories.map(category => <option key={category}>{category}</option>)}</select></label>
                <label>Status<select value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="ALL">All statuses</option><option value="PENDING">Pending approval</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label>
                <label>Search<input value={query} onChange={event => setQuery(event.target.value)} placeholder="Product, brand or model" /></label>
                <strong>{visibleItems.length} product(s)</strong>
            </div>


            {/* PRODUCTS TABLE */}

            <div className="management-card">

                <table>

                    <thead>

                        <tr>

                            <th>
                                Product
                            </th>

                            <th>
                                Seller
                            </th>

                            <th>
                                Price
                            </th>

                            <th>
                                Status
                            </th>

                            <th>
                                Customer Offer
                            </th>

                            <th>
                            </th>

                        </tr>

                    </thead>


                    <tbody>

                        {visibleItems.map((p) => (

                            <tr key={p.id}>

                                <td>

                                    <strong>
                                        {p.name}
                                    </strong>

                                    <small>
                                        {p.category ||
                                            "Uncategorised"}
                                    </small>

                                </td>

                                <td>

                                    {p.seller?.name ||
                                        "ShivHub"}

                                </td>


                                <td>

                                    ₹
                                    {Number(
                                        p.price || 0
                                    ).toLocaleString(
                                        "en-IN"
                                    )}

                                </td>


                                <td>

                                    <span
                                        className={`status ${
                                            p.approvalStatus
                                                ?.toLowerCase()
                                        }`}
                                    >
                                        {p.approvalStatus}
                                    </span>

                                </td>

                                <td><div className="product-offer-control"><input type="number" min="0" max="100" step="0.01" value={offerDrafts[p.id] ?? ""} onChange={(event) => setOfferDrafts(previous => ({ ...previous, [p.id]: event.target.value }))} placeholder="0" aria-label={`${p.name} offer discount percentage`} /><span>%</span><button type="button" disabled={savingOfferId === p.id} onClick={() => saveOffer(p.id)}>{savingOfferId === p.id ? "Saving…" : "Save"}</button></div></td>


                                <td>

                                    <Link
                                        className="text-link"
                                        to={`/admin/products/${p.id}`}
                                    >
                                        View details
                                    </Link>

                                </td>

                            </tr>

                        ))}

                    </tbody>

                </table>


                {!visibleItems.length && !error && (

                    <p className="empty">
                        No products match these filters.
                    </p>

                )}

            </div>

        </section>
    );
}
