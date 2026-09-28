const getToken = () => {

        return (
            localStorage.getItem("token") ||
            localStorage.getItem("jwtToken") ||
            localStorage.getItem("accessToken")
        );
    };
const getAuthConfig = () => {

        const token = getToken();

        return {
            headers: {
                Authorization: `Bearer ${token}`
            }
        };
    };
import { useEffect, useMemo, useState } from "react";
import { motion, useReducedMotion } from "framer-motion";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Filter, Search, SlidersHorizontal, TrendingUp } from "lucide-react";
import api from "../../services/api";
import { getActiveOffers } from "../../services/marketingService";
import { API_BASE_URL } from "../../services/api";
import useAuth from "../../hooks/useAuth";
import CustomerHeader from "./dashboard/CustomerHeader";
import { CUSTOMER_THEMES, customerText } from "./dashboard/customerI18n";
import { applyWorkspaceAppearance, saveWorkspacePreference } from "../../utils/workspacePreferences";
import { matchesAttributeFilters } from "../../components/products/productAttributeFilters";

import "./CustomerProducts.css";

const specificationValue = (item, label) => {
    const keys = { RAM: ["ram"], "Internal storage": ["storage"], "Network type": ["network", "networkType"], "Battery capacity": ["battery", "batteryCapacity"], "Processor brand": ["processor", "processorBrand"] }[label] || [];
    return keys.map(key => item[key] ?? item.attributes?.[key] ?? item.specifications?.[key]).find(Boolean);
};
const assetUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;
const specLabel = key => String(key).replace(/([A-Z])/g, " $1").replaceAll("_", " ").replace(/^./, value => value.toUpperCase());
const savedSpecifications = product => {
    const values = { ...(product.attributes || {}), ...(product.specifications || {}) };
    ["ram", "storage", "processor", "batteryCapacity", "display", "networkType", "color", "model"].forEach(key => {
        if (product[key] != null && String(product[key]).trim()) values[key] ??= product[key];
    });
    try {
        const raw = product.productSpecifications || product.specificationDetails;
        const parsed = typeof raw === "string" && raw.trim().startsWith("{") ? JSON.parse(raw) : raw;
        const extra = parsed?.specs && typeof parsed.specs === "object" ? parsed.specs : parsed;
        if (extra && typeof extra === "object") Object.entries(extra).forEach(([key, value]) => { if (value != null && String(value).trim()) values[key] ??= Array.isArray(value) ? value.join(", ") : value; });
    } catch { /* A legacy plain-text description remains visible in its existing field. */ }
    return Object.entries(values).filter(([, value]) => value != null && String(value).trim()).slice(0, 5);
};


/*
 * =========================================================
 * CustomerProducts
 * =========================================================
 *
 * Displays all active products available on ShivHub.
 *
 * Customer can:
 *
 * - View product
 * - Add product to Cart
 * - Add product to Wishlist
 * - Remove product from Wishlist
 * - See current Wishlist state
 * - See product image
 * - See category
 * - See price
 * - See stock
 *
 * =========================================================
 */

const CustomerProducts = () => {
    const reducedMotion = useReducedMotion();
    const navigate = useNavigate();
    const { user, logout, loyaltySummary } = useAuth();
    const [params] = useSearchParams();
    const initialCategory = params.get("category") || "All";

    const [products, setProducts] = useState([]);

    /*
     * =====================================================
     * WISHLIST STATE
     * =====================================================
     */

    const [wishlistIds, setWishlistIds] = useState(
        new Set()
    );


    /*
     * =====================================================
     * CART STATE
     * =====================================================
     *
     * Stores product IDs which are already in cart.
     *
     * Example:
     *
     * { 1, 5, 8 }
     *
     * =====================================================
     */

    const [cartIds, setCartIds] = useState(
        new Set()
    );


    const [loading, setLoading] = useState(true);

    const [wishlistLoading, setWishlistLoading] = useState(
        {}
    );


    const [cartLoading, setCartLoading] = useState(
        {}
    );


    const [error, setError] = useState("");
    const [query, setQuery] = useState(() => params.get("search") || "");
    const [brand, setBrand] = useState("All");
    const [availability, setAvailability] = useState("ALL");
    const [minimumPrice, setMinimumPrice] = useState("");
    const [maximumPrice, setMaximumPrice] = useState("");
    const [sort, setSort] = useState("RELEVANCE");
    const [specFilters, setSpecFilters] = useState({});
    const [attributeFilters, setAttributeFilters] = useState({});
    const [theme, setTheme] = useState(() => {
        const saved = window.localStorage.getItem("shivhub_customer_theme");
        return CUSTOMER_THEMES.includes(saved) ? saved : "light";
    });
    const [language, setLanguage] = useState(() => {
        const saved = window.localStorage.getItem("shivhub_customer_language");
        return ["en", "mr", "hi"].includes(saved) ? saved : "en";
    });
    const [categoryCampaigns, setCategoryCampaigns] = useState({ slides: [], tiles: [] });
    const [activeCategorySlide, setActiveCategorySlide] = useState(0);

    useEffect(() => {
        let live = true;
        getActiveOffers("CUSTOMER").then(items => {
            const matchesCategory = item => String(item.targetFilters || "").trim().toLowerCase() === initialCategory.toLowerCase();
            const banners = (Array.isArray(items) ? items : []).filter(item => item.type === "BANNER" && matchesCategory(item));
            const slides = banners.filter(item => ["CUSTOMER_CATEGORY", "CUSTOMER_CATEGORY_HERO"].includes(item.placement) && item.bannerUrl);
            const tiles = banners.filter(item => item.placement === "CUSTOMER_CATEGORY_TILE" && item.bannerUrl).slice(0, 4);
            if (live) { setCategoryCampaigns({ slides, tiles }); setActiveCategorySlide(0); }
        }).catch(() => { if (live) setCategoryCampaigns({ slides: [], tiles: [] }); });
        return () => { live = false; };
    }, [initialCategory]);

    useEffect(() => {
        if (categoryCampaigns.slides.length < 2) return undefined;
        const timer = window.setInterval(() => setActiveCategorySlide(current => (current + 1) % categoryCampaigns.slides.length), 5200);
        return () => window.clearInterval(timer);
    }, [categoryCampaigns.slides.length]);

    useEffect(() => { saveWorkspacePreference("customer", "theme", theme); applyWorkspaceAppearance("customer"); }, [theme]);
    useEffect(() => { saveWorkspacePreference("customer", "language", language); applyWorkspaceAppearance("customer"); }, [language]);

    const categories = useMemo(() => ["All", ...Array.from(new Set(products.map(item => item.categoryEntity?.name || item.category).filter(Boolean)))], [products]);
    const headerCategories = useMemo(() => categories.map(name => ({ name })), [categories]);
    const brands = useMemo(() => Array.from(new Set(products.map(item => item.brand).filter(Boolean))).sort(), [products]);
    const categoryProducts = useMemo(() => products.filter(item => initialCategory === "All" || (item.categoryEntity?.name || item.category) === initialCategory || item.subCategory?.name === initialCategory), [products, initialCategory]);
    const specificationGroups = useMemo(() => {
        const aliases = ["RAM", "Internal storage", "Network type", "Battery capacity", "Processor brand"];
        return aliases.map(label => [label, Array.from(new Set(categoryProducts.map(item => specificationValue(item, label)).filter(Boolean).map(String)))])
            .filter(([, values]) => values.length > 0);
    }, [categoryProducts]);
    const filteredProducts = useMemo(() => {
        const text = query.trim().toLowerCase();
        const list = categoryProducts.filter(item => {
            const price = Number(item.finalSellingPrice ?? item.price ?? 0);
            return (!text || [item.name, item.brand, item.model, item.category].filter(Boolean).some(value => String(value).toLowerCase().includes(text)))
                && (brand === "All" || item.brand === brand)
                && (availability === "ALL" || (availability === "IN_STOCK" ? Number(item.availableStock ?? item.stock ?? 0) > 0 : Number(item.offerPercentage || 0) > 0))
                && (!minimumPrice || price >= Number(minimumPrice)) && (!maximumPrice || price <= Number(maximumPrice))
                && specificationGroups.every(([label]) => !specFilters[label]?.length || specFilters[label].includes(String(specificationValue(item, label) || "")))
                && matchesAttributeFilters(item, attributeFilters);
        });
        return [...list].sort((a, b) => sort === "PRICE_LOW" ? Number(a.finalSellingPrice ?? a.price ?? 0) - Number(b.finalSellingPrice ?? b.price ?? 0) : sort === "PRICE_HIGH" ? Number(b.finalSellingPrice ?? b.price ?? 0) - Number(a.finalSellingPrice ?? a.price ?? 0) : sort === "NEWEST" ? Number(b.id || 0) - Number(a.id || 0) : Number(b.offerPercentage || 0) - Number(a.offerPercentage || 0));
    }, [categoryProducts, query, brand, availability, minimumPrice, maximumPrice, sort, specificationGroups, specFilters, attributeFilters]);


    /*
     * =====================================================
     * GET JWT TOKEN
     * =====================================================
     */

    


    /*
     * =====================================================
     * AUTH CONFIG
     * =====================================================
     */

    


    /*
     * =====================================================
     * LOAD PRODUCTS
     * =====================================================
     */

    useEffect(() => {

        let cancelled = false;


        const fetchProducts = async () => {

            try {

                const response = await api.get(
                    "/api/products"
                );


                if (!cancelled) {

                    setProducts(
                        Array.isArray(response.data)
                            ? response.data
                            : []
                    );

                    setError("");
                }


            } catch (error) {

                console.error(
                    "Failed to load products:",
                    error
                );


                if (!cancelled) {

                    setError(
                        error.response?.data?.message ||
                        "Unable to load products."
                    );
                }


            } finally {

                if (!cancelled) {

                    setLoading(false);
                }
            }
        };


        fetchProducts();


        return () => {

            cancelled = true;
        };

    }, []);


    /*
     * =====================================================
     * LOAD CUSTOMER WISHLIST
     * =====================================================
     */

    useEffect(() => {

        const fetchWishlist = async () => {

            const token = getToken();


            if (!token) {

                return;
            }


            try {

                const response = await api.get(
                    "/api/wishlist",
                    getAuthConfig()
                );


                const wishlistData =
                    Array.isArray(response.data)
                        ? response.data
                        : [];


                /*
                 * Support both:
                 *
                 * {
                 *     productId: 1
                 * }
                 *
                 * and
                 *
                 * {
                 *     product: {
                 *         id: 1
                 *     }
                 * }
                 */

                const ids =
                    wishlistData
                        .map(
                            (item) =>
                                Number(
                                    item.productId ??
                                    item.product?.id
                                )
                        )
                        .filter(
                            (id) =>
                                !Number.isNaN(id)
                        );


                setWishlistIds(
                    new Set(ids)
                );


            } catch (error) {

                console.error(
                    "Failed to load wishlist:",
                    error
                );
            }
        };


        fetchWishlist();

    }, []);


    /*
     * =====================================================
     * LOAD CUSTOMER CART
     * =====================================================
     *
     * GET /api/cart
     *
     * We only store product IDs locally because we only
     * need to know whether a product is already in cart.
     *
     * =====================================================
     */

    useEffect(() => {

        const fetchCart = async () => {

            const token = getToken();


            if (!token) {

                return;
            }


            try {

                const response = await api.get(
                    "/api/cart",
                    getAuthConfig()
                );


                const cartData =
                    Array.isArray(response.data)
                        ? response.data
                        : [];


                const ids =
                    cartData
                        .map(
                            (item) =>
                                Number(
                                    item.productId ??
                                    item.product?.id
                                )
                        )
                        .filter(
                            (id) =>
                                !Number.isNaN(id)
                        );


                setCartIds(
                    new Set(ids)
                );


            } catch (error) {

                /*
                 * Cart failure should not stop the
                 * customer from browsing products.
                 */

                console.error(
                    "Failed to load cart:",
                    error
                );
            }
        };


        fetchCart();

    }, []);


    /*
     * =====================================================
     * ADD / REMOVE WISHLIST
     * =====================================================
     */

    const handleWishlistToggle = async (
        productId
    ) => {

        const token = getToken();


        if (!token) {

            alert(
                "Please login to use Wishlist."
            );

            return;
        }


        if (wishlistLoading[productId]) {

            return;
        }


        const alreadyInWishlist =
            wishlistIds.has(
                Number(productId)
            );


        setWishlistLoading(
            (previous) => ({
                ...previous,
                [productId]: true
            })
        );


        try {

            /*
             * =================================================
             * REMOVE FROM WISHLIST
             * =================================================
             */

            if (alreadyInWishlist) {

                await api.delete(
                    `/api/wishlist/${productId}`,
                    getAuthConfig()
                );


                setWishlistIds(
                    (previous) => {

                        const updated =
                            new Set(previous);

                        updated.delete(
                            Number(productId)
                        );

                        return updated;
                    }
                );


            } else {

                /*
                 * =================================================
                 * ADD TO WISHLIST
                 * =================================================
                 */

                await api.post(
                    `/api/wishlist/${productId}`,
                    {},
                    getAuthConfig()
                );


                setWishlistIds(
                    (previous) => {

                        const updated =
                            new Set(previous);

                        updated.add(
                            Number(productId)
                        );

                        return updated;
                    }
                );
            }


        } catch (error) {

            console.error(
                "Wishlist operation failed:",
                error
            );


            if (
                error.response?.status === 409
            ) {

                setWishlistIds(
                    (previous) => {

                        const updated =
                            new Set(previous);

                        updated.add(
                            Number(productId)
                        );

                        return updated;
                    }
                );


                alert(
                    "Product is already in your wishlist."
                );


            } else if (
                error.response?.status === 401 ||
                error.response?.status === 403
            ) {

                alert(
                    "Your login session has expired. Please login again."
                );


            } else {

                alert(
                    error.response?.data?.message ||
                    "Unable to update wishlist."
                );
            }


        } finally {

            setWishlistLoading(
                (previous) => ({
                    ...previous,
                    [productId]: false
                })
            );
        }
    };


    /*
     * =====================================================
     * ADD TO CART
     * =====================================================
     *
     * POST /api/cart
     *
     * Request:
     *
     * {
     *     productId: 1,
     *     quantity: 1
     * }
     *
     * =====================================================
     */

    const handleAddToCart = async (
        product
    ) => {
        if (product?.variantsEnabled) { window.location.href = `/product/${product.id}`; return; }

        const token = getToken();


        /*
         * =================================================
         * LOGIN CHECK
         * =================================================
         */

        if (!token) {

            alert(
                "Please login to add products to cart."
            );

            return;
        }


        const productId =
            Number(product.id);


        /*
         * =================================================
         * STOCK CHECK
         * =================================================
         */

        if (
            !product.stock ||
            Number(product.stock) <= 0
        ) {

            alert(
                "This product is currently out of stock."
            );

            return;
        }


        /*
         * Prevent multiple clicks.
         */

        if (cartLoading[productId]) {

            return;
        }


        setCartLoading(
            (previous) => ({
                ...previous,
                [productId]: true
            })
        );


        try {

            /*
             * =================================================
             * ADD PRODUCT
             * =================================================
             */

            await api.post(
                "/api/cart",
                {
                    productId: productId,
                    quantity: 1
                },
                getAuthConfig()
            );


            /*
             * Update local cart state.
             */

            setCartIds(
                (previous) => {

                    const updated =
                        new Set(previous);

                    updated.add(
                        productId
                    );

                    return updated;
                }
            );


            alert(
                `${product.name} added to cart 🛒`
            );


        } catch (error) {

            console.error(
                "Failed to add product to cart:",
                error
            );


            /*
             * =================================================
             * AUTH ERROR
             * =================================================
             */

            if (
                error.response?.status === 401 ||
                error.response?.status === 403
            ) {

                alert(
                    "Your login session has expired. Please login again."
                );


            } else {

                alert(
                    error.response?.data?.message ||
                    "Unable to add product to cart."
                );
            }


        } finally {

            setCartLoading(
                (previous) => ({
                    ...previous,
                    [productId]: false
                })
            );
        }
    };


    /*
     * =====================================================
     * OPEN CART
     * =====================================================
     */

    const handleOpenCart = () => {

        window.location.href =
            "/customer/cart";
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

    const handleCompareAdd = product => {
        const storageKey = "shivhub_product_compare";
        try {
            const existing = JSON.parse(localStorage.getItem(storageKey) || "[]");
            const items = Array.isArray(existing) ? existing.filter(item => item?.id) : [];
            const sameCategory = item => (item.categoryEntity?.id || item.category) === (product.categoryEntity?.id || product.category)
                && (!item.subCategory?.id || !product.subCategory?.id || item.subCategory.id === product.subCategory.id);
            if (!items.every(sameCategory)) { alert("Compare products from the same category or subcategory only."); return; }
            if (items.some(item => Number(item.id) === Number(product.id))) { alert("This product is already in Compare."); return; }
            if (items.length >= 3) { alert("You can compare up to 3 products."); return; }
            localStorage.setItem(storageKey, JSON.stringify([...items, product]));
            alert("Added to Compare. Open a product page to compare selected products.");
        } catch { alert("Could not add this product to Compare."); }
    };

    const selectHeaderCategory = category => {
        const name = typeof category === "string" ? category : category?.name;
        navigate(`/customer/products${name && name !== "All" ? `?category=${encodeURIComponent(name)}` : ""}`);
    };
    const clearHeaderFilters = () => { setQuery(""); setBrand("All"); setAvailability("ALL"); setMinimumPrice(""); setMaximumPrice(""); setSort("RELEVANCE"); setSpecFilters({}); setAttributeFilters({}); };
    const t = key => customerText(language, key);


    /*
     * =====================================================
     * RETRY
     * =====================================================
     */

    const handleRetry = () => {

        window.location.reload();
    };


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    if (loading) {

        return (

            <div className="customer-products-page">

                <div className="customer-products-loading">

                    <div className="customer-products-spinner">
                    </div>

                    <p>
                        Loading products...
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

            <div className="customer-products-page">

                <div className="customer-products-error">

                    <p>
                        ⚠ {error}
                    </p>

                    <button
                        onClick={handleRetry}
                    >
                        Try Again
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
        <>
        <CustomerHeader
            user={user}
            searchQuery={query}
            onSearchChange={setQuery}
            onSearchSubmit={() => document.getElementById("customer-category-results")?.scrollIntoView({ behavior: "smooth", block: "start" })}
            onSuggestionSelect={suggestion => { if (suggestion?.type === "PRODUCT" && suggestion.id != null) navigate(`/product/${suggestion.id}`); else setQuery(suggestion?.label || ""); }}
            onClearSearch={() => setQuery("")}
            categories={headerCategories}
            activeCategory={initialCategory}
            onCategoryChange={selectHeaderCategory}
            availabilityFilter={availability}
            onAvailabilityChange={setAvailability}
            sortOrder={sort}
            onSortChange={value => setSort(value === "BEST_OFFERS" ? "RELEVANCE" : value)}
            brands={brands}
            brandFilter={brand}
            onBrandChange={setBrand}
            ramOptions={(specificationGroups.find(([label]) => label === "RAM")?.[1] || [])}
            ramFilter={specFilters.RAM?.[0] || "ALL"}
            onRamChange={value => setSpecFilters(current => ({ ...current, RAM: value === "ALL" ? [] : [value] }))}
            storageOptions={(specificationGroups.find(([label]) => label === "Internal storage")?.[1] || [])}
            storageFilter={specFilters["Internal storage"]?.[0] || "ALL"}
            onStorageChange={value => setSpecFilters(current => ({ ...current, "Internal storage": value === "ALL" ? [] : [value] }))}
            minimumPrice={minimumPrice}
            maximumPrice={maximumPrice}
            onMinimumPriceChange={setMinimumPrice}
            onMaximumPriceChange={setMaximumPrice}
            products={products}
            attributeFilters={attributeFilters}
            onAttributeFiltersChange={setAttributeFilters}
            onClearFilters={clearHeaderFilters}
            onHome={() => navigate("/customer/dashboard")}
            onCart={handleOpenCart}
            cartItemCount={cartIds.size}
            onWishlist={() => navigate("/customer/wishlist")}
            onOffers={() => navigate("/customer/dashboard#customer-offers")}
            theme={theme}
            onThemeChange={setTheme}
            language={language}
            onLanguageChange={setLanguage}
            t={t}
            onProfile={() => navigate("/customer/profile")}
            onOrders={() => navigate("/customer/orders")}
            onAfterSales={() => navigate("/customer/after-sales")}
            onSecondHandMarket={() => navigate("/customer/second-hand-market")}
            loyaltySummary={loyaltySummary}
            onRewards={() => navigate("/customer/rewards")}
            onLogout={() => { logout(); navigate("/login"); }}
        />
        <div className="customer-products-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <section className="customer-products-header">

                <div>

                    <span>
                        SHIVHUB STORE
                    </span>

                    <h1>
                        {initialCategory === "All" ? "Explore Products" : `${initialCategory} picks`}
                    </h1>

                    <p>
                        Discover products from
                        ShivHub sellers.
                    </p>

                </div>


                {/* =================================================
                    CART HEADER BUTTON
                ================================================= */}

                <div className="customer-products-header-actions">

                    <div className="customer-product-count">

                        <strong>
                            {filteredProducts.length}
                        </strong>

                        <small>
                            Products
                        </small>

                    </div>


                    <button
                        type="button"
                        className="customer-cart-header-button"
                        onClick={
                            handleOpenCart
                        }
                    >
                        🛒 Cart
                    </button>

                </div>

            </section>


            {/* =================================================
                PRODUCTS
            ================================================= */}

            {categoryProducts.length > 0 && <>
                {categoryCampaigns.slides.length > 0 && <section className="category-admin-banner category-banner-slider" aria-label={`${initialCategory} offers`}>
                    {categoryCampaigns.slides.map((slide, index) => <article key={slide.id} className={index === activeCategorySlide ? "is-active" : ""} aria-hidden={index !== activeCategorySlide}><img src={assetUrl(slide.bannerUrl)} alt={slide.title || `${initialCategory} offer`} onError={event => { event.currentTarget.style.display = "none"; }} /><div><span>SHIVHUB CATEGORY OFFER</span><h2>{slide.title}</h2>{slide.description && <p>{slide.description}</p>}</div></article>)}
                    {categoryCampaigns.slides.length > 1 && <div className="category-banner-dots">{categoryCampaigns.slides.map((slide, index) => <button type="button" key={slide.id} className={index === activeCategorySlide ? "active" : ""} aria-label={`Show offer ${index + 1}`} onClick={() => setActiveCategorySlide(index)} />)}</div>}
                </section>}
                <section className="category-product-showcase" aria-label={`${initialCategory} highlights`}>
                    <div className="category-showcase-heading"><p><TrendingUp size={15} /> {initialCategory === "All" ? "TRENDING CATALOGUE" : `${initialCategory.toUpperCase()} HIGHLIGHTS`}</p><h2>Popular picks to explore first</h2><span>Real active ShivHub products, prices and stock.</span></div>
                    <div className="category-spotlight-rail">{(categoryCampaigns.tiles.length ? categoryCampaigns.tiles : categoryProducts.slice(0, 4)).map(item => item.placement === "CUSTOMER_CATEGORY_TILE" ? <article key={item.id} className="category-admin-tile"><img src={assetUrl(item.bannerUrl)} alt={item.title || `${initialCategory} highlight`} /><span>NEW LAUNCH &amp; OFFERS</span><strong>{item.title}</strong>{item.description && <small>{item.description}</small>}</article> : <button type="button" key={item.id} onClick={() => handleViewProduct(item.id)}><img src={item.imageUrl || ""} alt="" onError={event => { event.currentTarget.style.display = "none"; }} /><span>{item.brand || item.category}</span><strong>{item.name}</strong><b>₹{Number(item.finalSellingPrice ?? item.price ?? 0).toLocaleString("en-IN")}</b></button>)}</div>
                </section>
                <section className="category-trending-rail"><div><span>NEW &amp; OFFERS</span><h2>Browse the latest in {initialCategory}</h2></div><div>{categoryProducts.slice(0, 10).map(product => <button type="button" key={product.id} onClick={() => handleViewProduct(product.id)}>{product.imageUrl && <img src={product.imageUrl} alt="" />}<strong>{product.name}</strong><small>₹{Number(product.finalSellingPrice ?? product.price ?? 0).toLocaleString("en-IN")}</small></button>)}</div></section>
            </>}
            <div className="category-results-workspace">
            <aside className="category-filter-sidebar" aria-label="Product filters"><div><Filter size={20} /><h2>Filters</h2></div><label><Search size={15} /> Search<input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search products" /></label><label>Category<select value={initialCategory} disabled><option>{initialCategory}</option></select></label><label>Availability<select value={availability} onChange={event => setAvailability(event.target.value)}><option value="ALL">All products</option><option value="IN_STOCK">In stock</option><option value="OFFERS">Offers only</option></select></label><label>Brand<select value={brand} onChange={event => setBrand(event.target.value)}><option>All</option>{brands.map(value => <option key={value}>{value}</option>)}</select></label>{specificationGroups.map(([label, values]) => <details key={label} open={label === "RAM"}><summary>{label}</summary><div className="category-checkbox-list">{values.map(value => <label key={value}><input type="checkbox" checked={specFilters[label]?.includes(value) || false} onChange={event => setSpecFilters(current => ({ ...current, [label]: event.target.checked ? [...(current[label] || []), value] : (current[label] || []).filter(item => item !== value) }))} />{value}</label>)}</div></details>)}<label>Minimum price<input type="number" min="0" value={minimumPrice} onChange={event => setMinimumPrice(event.target.value)} /></label><label>Maximum price<input type="number" min="0" value={maximumPrice} onChange={event => setMaximumPrice(event.target.value)} /></label><label><SlidersHorizontal size={15} /> Sort<select value={sort} onChange={event => setSort(event.target.value)}><option value="RELEVANCE">Recommended</option><option value="NEWEST">Newest</option><option value="PRICE_LOW">Price: low to high</option><option value="PRICE_HIGH">Price: high to low</option></select></label><button type="button" onClick={() => { setQuery(""); setBrand("All"); setAvailability("ALL"); setMinimumPrice(""); setMaximumPrice(""); setSort("RELEVANCE"); setSpecFilters({}); }}>Clear filters</button></aside>

            <section id="customer-category-results" className="category-catalogue-results"><div className="category-results-heading"><span>{filteredProducts.length} results</span><p>Filtered using real catalogue data</p></div>
            {filteredProducts.length === 0 ? (

                <div className="customer-empty-products">

                    <div className="empty-products-icon">
                        📦
                    </div>

                    <h2>
                        No Products Available
                    </h2>

                    <p>
                        There are no active products
                        available right now.
                    </p>

                </div>

            ) : (

                <div className="customer-product-grid category-product-list">

                    {filteredProducts.map(
                        (product, index) => {

                            const productId =
                                Number(product.id);


                            const isWishlisted =
                                wishlistIds.has(
                                    productId
                                );


                            const isWishlistLoading =
                                wishlistLoading[
                                    productId
                                ];


                            const isInCart =
                                cartIds.has(
                                    productId
                                );


                            const isCartLoading =
                                cartLoading[
                                    productId
                                ];


                            const isOutOfStock =
                                Number(
                                    product.stock || 0
                                ) <= 0;
                            const productSpecs = savedSpecifications(product);


                            return (

                                <motion.div
                                    className="customer-product-card"
                                    key={product.id}
                                    initial={reducedMotion || index > 11 ? false : { opacity: 0, y: 8 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    whileHover={reducedMotion ? undefined : { y: -5 }}
                                    whileTap={reducedMotion ? undefined : { scale: 0.995 }}
                                    transition={{ duration: 0.2, delay: Math.min(index, 8) * 0.025 }}
                                >


                                    {/* =================================================
                                        PRODUCT IMAGE
                                    ================================================= */}

                                    <div className="customer-product-image">

                                        {product.imageUrl ? (

                                            <img
                                                src={
                                                    product.imageUrl
                                                }
                                                alt={
                                                    product.name
                                                }
                                            />

                                        ) : (

                                            <div className="customer-no-image">
                                                📦
                                            </div>
                                        )}


                                        {/* =================================================
                                            WISHLIST HEART
                                        ================================================= */}

                                        <button
                                            type="button"
                                            className={
                                                isWishlisted
                                                    ? "customer-wishlist-button customer-wishlist-active"
                                                    : "customer-wishlist-button"
                                            }
                                            onClick={() =>
                                                handleWishlistToggle(
                                                    productId
                                                )
                                            }
                                            disabled={
                                                isWishlistLoading
                                            }
                                            title={
                                                isWishlisted
                                                    ? "Remove from Wishlist"
                                                    : "Add to Wishlist"
                                            }
                                        >

                                            {isWishlistLoading

                                                ? "..."

                                                : isWishlisted
                                                    ? "❤️"
                                                    : "♡"}

                                        </button>

                                    </div>


                                    {/* =================================================
                                        PRODUCT DETAILS
                                    ================================================= */}

                                    <div className="customer-product-details">


                                        <div className="customer-product-top">

                                            <span className="customer-product-category">

                                                {product.category}

                                            </span>


                                            {product.stock > 0 ? (

                                                <span className="customer-in-stock">

                                                    In Stock

                                                </span>

                                            ) : (

                                                <span className="customer-out-stock">

                                                    Out of Stock

                                                </span>
                                            )}

                                        </div>


                                        <h2>
                                            {product.name}
                                        </h2>

                                        {productSpecs.length > 0 && <ul className="category-product-specs">
                                            {productSpecs.map(([key, value]) => <li key={key}><span>{specLabel(key)}:</span> {String(value)}</li>)}
                                        </ul>}

                                        <p className="customer-product-description">

                                            {product.description ||
                                                "No description available."}

                                        </p>


                                        <div className="customer-product-bottom">

                                            <strong>
                                                ₹
                                                {Number(
                                                    product.finalSellingPrice ?? product.price ?? 0
                                                ).toLocaleString(
                                                    "en-IN"
                                                )}
                                            </strong>

                                            {Number(product.offerPercentage || 0) > 0 && Number(product.finalSellingPrice ?? product.price ?? 0) < Number(product.price || 0) && (
                                                <small className="customer-product-offer">
                                                    {product.offerPercentage}% OFF
                                                </small>
                                            )}

                                            <span>
                                                Stock:{" "}
                                                {product.stock}
                                            </span>

                                        </div>


                                        {/* =================================================
                                            ACTION BUTTONS
                                        ================================================= */}

                                        <div className="customer-product-actions">


                                            {/* =================================================
                                                VIEW PRODUCT
                                            ================================================= */}

                                            <button
                                                type="button"
                                                className="customer-view-product"
                                                onClick={() =>
                                                    handleViewProduct(
                                                        product.id
                                                    )
                                                }
                                            >
                                                View Product
                                            </button>

                                            <button type="button" className="customer-compare-action" onClick={() => handleCompareAdd(product)}>Add to Compare</button>

                                            {/* =================================================
                                                ADD TO CART
                                            ================================================= */}

                                            <button
                                                type="button"
                                                className={
                                                    isInCart
                                                        ? "customer-cart-action customer-cart-action-added"
                                                        : "customer-cart-action"
                                                }
                                                onClick={() => {

                                                    if (isInCart) {

                                                        handleOpenCart();

                                                    } else {

                                                        handleAddToCart(
                                                            product
                                                        );
                                                    }

                                                }}
                                                disabled={
                                                    isCartLoading ||
                                                    isOutOfStock
                                                }
                                            >

                                                {isCartLoading

                                                    ? "Adding..."

                                                    : isOutOfStock

                                                        ? "Out of Stock"

                                                        : isInCart

                                                            ? "🛒 Go to Cart"

                                                            : "🛒 Add to Cart"}

                                            </button>


                                            {/* =================================================
                                                WISHLIST
                                            ================================================= */}

                                            <button
                                                type="button"
                                                className={
                                                    isWishlisted
                                                        ? "customer-wishlist-action customer-wishlist-action-active"
                                                        : "customer-wishlist-action"
                                                }
                                                onClick={() =>
                                                    handleWishlistToggle(
                                                        productId
                                                    )
                                                }
                                                disabled={
                                                    isWishlistLoading
                                                }
                                            >

                                                {isWishlistLoading

                                                    ? "Updating..."

                                                    : isWishlisted

                                                        ? "❤️ Wishlisted"

                                                        : "♡ Add to Wishlist"}

                                            </button>

                                        </div>

                                    </div>

                                </motion.div>
                            );
                        }
                    )}

                </div>
            )}</section>
            </div>

        </div>
        </>
    );
};


export default CustomerProducts;
