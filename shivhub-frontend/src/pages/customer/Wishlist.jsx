const getToken = () => {
    return localStorage.getItem("shivhub_token");
};
const getAuthConfig = () => {

        const token = getToken();

        return {

            headers: {

                Authorization: `Bearer ${token}`

            }

        };
    };
const Navigation = ({ navigate }) => <nav className="wishlist-nav" aria-label="Customer navigation"><button type="button" className="wishlist-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingBag size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/products")}><Package size={18} /> Products</button><button type="button" className="active" onClick={() => navigate("/customer/wishlist")}><Heart size={18} /> Wishlist</button><button type="button" onClick={() => navigate("/customer/cart")}><ShoppingCart size={18} /> Cart</button></div></nav>;

import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertCircle, Box, Heart, Home, Package, ShoppingBag, ShoppingCart, Trash2 } from "lucide-react";
import api from "../../services/api";

import "./Wishlist.css";


/*
 * =========================================================
 * Wishlist
 * =========================================================
 *
 * Customer Wishlist Page
 *
 * Features:
 *
 * 1. Load customer's wishlist
 * 2. Display product information
 * 3. Remove product
 * 4. Move product to cart
 * 5. View product
 * 6. Loading state
 * 7. Error state
 * 8. Empty wishlist state
 *
 * =========================================================
 */

const Wishlist = () => {
    const navigate = useNavigate();

    const [wishlist, setWishlist] = useState([]);

    const [products, setProducts] = useState({});

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [removingId, setRemovingId] = useState(null);
    const [addingId, setAddingId] = useState(null);
    


    /*
     * =====================================================
     * GET JWT TOKEN
     * =====================================================
     *
     * Your login system may store token under one
     * of these common names.
     *
     * =====================================================
     */



    /*
     * =====================================================
     * AXIOS CONFIG
     * =====================================================
     */

    


    /*
     * =====================================================
     * LOAD WISHLIST
     * =====================================================
     */

    const fetchWishlist = useCallback(async () => {

        try {

            setLoading(true);

            setError("");


            /*
             * Get wishlist items.
             */

            const response =
                await api.get(
                    "/api/wishlist",
                    getAuthConfig()
                );


            const wishlistData =
                Array.isArray(response.data)
                    ? response.data
                    : [];


            setWishlist(
                wishlistData
            );


            /*
             * =================================================
             * LOAD PRODUCT DETAILS
             * =================================================
             *
             * Wishlist backend currently returns:
             *
             * customerId
             * productId
             * createdAt
             *
             * Therefore we fetch product information separately.
             *
             * =================================================
             */

            const productResults = {};


            await Promise.all(

                wishlistData.map(
                    async (item) => {

                        try {

                            const productResponse =
                                await api.get(
                                    `/api/products/${item.productId}`
                                );


                            productResults[
                                item.productId
                            ] = productResponse.data;


                        } catch (productError) {

                            console.error(
                                "Failed to load product:",
                                item.productId,
                                productError
                            );

                        }

                    }
                )
            );


            setProducts(
                productResults
            );


        } catch (error) {

            console.error(
                "Failed to load wishlist:",
                error
            );


            if (
                error.response?.status === 401 ||
                error.response?.status === 403
            ) {

                setError(
                    "Please login again to view your wishlist."
                );

            } else {

                setError(
                    error.response?.data?.message ||
                    "Unable to load wishlist."
                );
            }


        } finally {

            setLoading(false);
        }
    }, []);


    /*
     * =====================================================
     * INITIAL LOAD
     * =====================================================
     */

    useEffect(() => {

        fetchWishlist();

    }, [fetchWishlist]);


    /*
     * =====================================================
     * REMOVE FROM WISHLIST
     * =====================================================
     */

    const handleRemove = async (
        productId
    ) => {

        try {

            setRemovingId(
                productId
            );


            await api.delete(
                `/api/wishlist/${productId}`,
                getAuthConfig()
            );


            /*
             * Remove from UI immediately.
             */

            setWishlist(
                (previous) =>
                    previous.filter(
                        (item) =>
                            item.productId !== productId
                    )
            );


        } catch (error) {

            console.error(
                "Failed to remove wishlist item:",
                error
            );


            alert(
                error.response?.data?.message ||
                "Unable to remove product from wishlist."
            );


        } finally {

            setRemovingId(
                null
            );
        }
    };


    /*
     * =====================================================
     * ADD TO CART
     * =====================================================
     *
     * NOTE:
     *
     * Cart API is not yet connected here because we are
     * developing the customer shopping flow step by step.
     *
     * For now this function provides the UI action.
     *
     * =====================================================
     */

    const handleAddToCart = async (
        product
    ) => {

        if (!product) {

            return;
        }

        if (product.variantsEnabled) { window.location.href = `/product/${product.id}`; return; }
        if (addingId != null) return;


        if (product.stock <= 0) {

            alert(
                "This product is currently out of stock."
            );

            return;
        }


        setAddingId(product.id); setError("");
        try {
            await api.post("/api/cart", { productId: product.id, quantity: 1 }, getAuthConfig());
            window.location.href = "/customer/cart";
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Could not add this product to cart.");
        } finally { setAddingId(null); }
    };


    /*
     * =====================================================
     * VIEW PRODUCT
     * =====================================================
     */

    const handleViewProduct = (
        productId
    ) => {

        window.location.href =
            `/product/${productId}`;
    };


    /*
     * =====================================================
     * RETRY
     * =====================================================
     */

    const handleRetry = () => {

        fetchWishlist();
    };


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    if (loading) {

        return (

            <div className="wishlist-page">
                <Navigation navigate={navigate} />

                <div className="wishlist-loading">

                    <div className="wishlist-spinner">
                    </div>

                    <p>
                        Loading your wishlist...
                    </p>

                </div>

            </div>

        );
    }


    /*
     * =====================================================
     * ERROR
     * =====================================================
     */

    if (error) {

        return (

            <div className="wishlist-page">
                <Navigation navigate={navigate} />

                <div className="wishlist-error">

                    <div className="wishlist-error-icon"><AlertCircle size={42} /></div>

                    <h2>
                        Something went wrong
                    </h2>

                    <p>
                        {error}
                    </p>

                    <button
                        onClick={handleRetry}
                        className="wishlist-retry-btn"
                    >
                        Try Again
                    </button>

                </div>

            </div>

        );
    }


    /*
     * =====================================================
     * EMPTY WISHLIST
     * =====================================================
     */

    if (wishlist.length === 0) {

        return (

            <div className="wishlist-page">
                <Navigation navigate={navigate} />

                <section className="wishlist-header">

                    <div>

                        <span>
                            SHIVHUB
                        </span>

                        <h1>
                            My Wishlist
                        </h1>

                        <p>
                            Save products you love
                            and come back to them anytime.
                        </p>

                    </div>

                </section>


                <div className="wishlist-empty">

                    <div className="wishlist-empty-icon"><Heart size={55} /></div>

                    <h2>
                        Your Wishlist is Empty
                    </h2>

                    <p>
                        You haven't saved any products yet.
                    </p>

                    <button
                        className="wishlist-shop-btn"
                        onClick={() => {
                            window.location.href =
                                "/customer/products";
                        }}
                    >
                        Explore Products
                    </button>

                </div>

            </div>

        );
    }


    /*
     * =====================================================
     * MAIN UI
     * =====================================================
     */

    return (

        <div className="wishlist-page">
            <Navigation navigate={navigate} />


            {/* =================================================
                HEADER
            ================================================= */}

            <section className="wishlist-header">

                <div>

                    <span>
                        SHIVHUB
                    </span>

                        <h1>My Wishlist <Heart size={30} /></h1>

                    <p>
                        Products you've saved for later.
                    </p>

                </div>


                <div className="wishlist-count">

                    <strong>
                        {wishlist.length}
                    </strong>

                    <small>
                        Saved Products
                    </small>

                </div>

            </section>


            {/* =================================================
                PRODUCTS
            ================================================= */}

            <section className="wishlist-grid">

                {wishlist.map(
                    (item) => {

                        const product =
                            products[item.productId];


                        /*
                         * Product API failed.
                         */

                        if (!product) {

                            return (

                                <div
                                    className="wishlist-card wishlist-card-missing"
                                    key={item.id}
                                >

                                    <div className="wishlist-missing-icon"><Box size={40} /></div>

                                    <h3>
                                        Product unavailable
                                    </h3>

                                    <button
                                        className="wishlist-remove-btn"
                                        onClick={() =>
                                            handleRemove(
                                                item.productId
                                            )
                                        }
                                        disabled={
                                            removingId ===
                                            item.productId
                                        }
                                    >
                                        {removingId ===
                                        item.productId
                                            ? "Removing..."
                                            : "Remove"}
                                    </button>

                                </div>

                            );
                        }


                        return (

                            <article
                                className="wishlist-card"
                                key={item.id}
                            >


                                {/* =================================================
                                    IMAGE
                                ================================================= */}

                                <div className="wishlist-image-wrapper">

                                    {product.imageUrl ? (

                                        <img
                                            src={
                                                product.imageUrl
                                            }
                                            alt={
                                                product.name
                                            }
                                            className="wishlist-product-image"
                                        />

                                    ) : (

                                        <div className="wishlist-no-image"><Box size={45} /></div>

                                    )}


                                    {/* Wishlist heart */}

                                    <button
                                        className="wishlist-heart-btn"
                                        onClick={() =>
                                            handleRemove(
                                                product.id
                                            )
                                        }
                                        title="Remove from wishlist"
                                        disabled={
                                            removingId ===
                                            product.id
                                        }
                                    ><Heart size={19} fill="currentColor" /></button>

                                </div>


                                {/* =================================================
                                    PRODUCT DETAILS
                                ================================================= */}

                                <div className="wishlist-details">


                                    <div className="wishlist-product-top">

                                        <span>
                                            {product.category ||
                                                "Product"}
                                        </span>


                                        {product.stock > 0 ? (

                                            <span className="wishlist-stock">
                                                In Stock
                                            </span>

                                        ) : (

                                            <span className="wishlist-out-stock">
                                                Out of Stock
                                            </span>

                                        )}

                                    </div>


                                    <h2>
                                        {product.name}
                                    </h2>


                                    {product.description && <p className="wishlist-description">{product.description}</p>}


                                    <div className="wishlist-price-row">

                                        <strong>
                                            ₹
                                            {Number(
                                                product.finalSellingPrice ?? product.price ?? 0
                                            ).toLocaleString(
                                                "en-IN"
                                            )}
                                        </strong>


                                        {Number(product.offerPercentage || 0) > 0 && Number(product.finalSellingPrice ?? product.price ?? 0) < Number(product.price || 0) && (
                                            <em className="wishlist-offer-badge">
                                                {product.offerPercentage}% OFF
                                            </em>
                                        )}


                                        <small>
                                            Stock:{" "}
                                            {product.stock}
                                        </small>

                                    </div>


                                    {/* =================================================
                                        ACTIONS
                                    ================================================= */}

                                    <div className="wishlist-actions">

                                        <button
                                            className="wishlist-view-btn"
                                            onClick={() =>
                                                handleViewProduct(
                                                    product.id
                                                )
                                            }
                                        ><Package size={16} /> View Product</button>


                                        <button
                                            className="wishlist-cart-btn"
                                            onClick={() =>
                                                handleAddToCart(
                                                    product
                                                )
                                            }
                                            disabled={
                                                product.stock <= 0 || addingId != null
                                            }
                                        ><ShoppingCart size={16} /> {addingId === product.id ? "Adding…" : "Add to Cart"}</button>

                                    </div>


                                    <button
                                        className="wishlist-remove-text"
                                        onClick={() =>
                                            handleRemove(
                                                product.id
                                            )
                                        }
                                        disabled={
                                            removingId ===
                                            product.id
                                        }
                                    ><Trash2 size={15} />{removingId === product.id ? "Removing…" : "Remove from Wishlist"}</button>

                                </div>

                            </article>

                        );
                    }
                )}

            </section>

        </div>

    );
};


export default Wishlist;
