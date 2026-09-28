import {
    useCallback,
    useDeferredValue,
    useEffect,
    useMemo,
    useState
} from "react";
import { ArrowRight, HeartHandshake, PackageSearch, ReceiptText, Recycle } from "lucide-react";

import {
    useNavigate
} from "react-router-dom";

import api from "../../services/api";

import useAuth from "../../hooks/useAuth";

import CustomerBenefits
    from "./dashboard/CustomerBenefits";

import CategoryNavigation
    from "./dashboard/CategoryNavigation";

import CustomerHeader
    from "./dashboard/CustomerHeader";

import CustomerHero
    from "./dashboard/CustomerHero";

import CustomerProductGrid
    from "./dashboard/CustomerProductGrid";

import CustomerProductState
    from "./dashboard/CustomerProductState";

import CustomerChatbot
    from "./dashboard/CustomerChatbot";

import CustomerOfferCarousel
    from "./dashboard/CustomerOfferCarousel";

import CustomerCategoryCarousel
    from "./dashboard/CustomerCategoryCarousel";

import CustomerProductSections
    from "./dashboard/CustomerProductSections";

import CustomerJourneyPromos
    from "./dashboard/CustomerJourneyPromos";

import { matchesAttributeFilters }
    from "../../components/products/productAttributeFilters";

import {
    CUSTOMER_THEMES,
    customerText
} from "./dashboard/customerI18n";

import {
    applyWorkspaceAppearance,
    saveWorkspacePreference
} from "../../utils/workspacePreferences";

import {
    MARKETPLACE_CATEGORIES
} from "./dashboard/constants";

import "./CustomerDashboard.css";

const label = value => {
    if (typeof value === "string") return value.trim();
    if (value && typeof value === "object") return String(value.name || value.title || "").trim();
    return "";
};



/*
 * =========================================================
 * CUSTOMER DASHBOARD
 * =========================================================
 *
 * Main customer marketplace page.
 *
 * Working navigation:
 *
 * - Home
 * - Search
 * - Products
 * - Cart
 * - Wishlist
 * - Profile
 * - Orders
 * - Logout
 *
 * =========================================================
 */


export default function CustomerDashboard() {


    const navigate = useNavigate();


    const {
        user,
        logout,
        loyaltySummary
    } = useAuth();


    const [
        products,
        setProducts
    ] = useState([]);

    const [cartItemCount, setCartItemCount] = useState(0);

    const [theme, setTheme] = useState(() => {
        const savedTheme = window.localStorage.getItem("shivhub_customer_theme");
        return CUSTOMER_THEMES.includes(savedTheme) ? savedTheme : "light";
    });

    const [language, setLanguage] = useState(() => {
        const savedLanguage = window.localStorage.getItem("shivhub_customer_language");
        return ["en", "mr", "hi"].includes(savedLanguage) ? savedLanguage : "en";
    });

    const t = useCallback(key => customerText(language, key), [language]);


    const [
        loading,
        setLoading
    ] = useState(true);


    const [
        error,
        setError
    ] = useState("");


    const [
        searchQuery,
        setSearchQuery
    ] = useState("");


    const [
        activeCategory,
        setActiveCategory
    ] = useState("All");

    const [
        availabilityFilter,
        setAvailabilityFilter
    ] = useState("ALL");

    const [
        sortOrder,
        setSortOrder
    ] = useState("RELEVANCE");

    const [brandFilter, setBrandFilter] = useState("ALL");
    const [ramFilter, setRamFilter] = useState("ALL");
    const [storageFilter, setStorageFilter] = useState("ALL");
    const [minimumPrice, setMinimumPrice] = useState("");
    const [maximumPrice, setMaximumPrice] = useState("");
    const [attributeFilters, setAttributeFilters] = useState({});

    // Keeps typing responsive while the product list is being filtered locally.
    const deferredSearchQuery = useDeferredValue(searchQuery);


    const greeting = useMemo(() => {
        const hour = new Date().getHours();
        return t(hour < 12 ? "goodMorning" : hour < 17 ? "goodAfternoon" : "goodEvening");
    }, [t]);



    /*
     * =====================================================
     * LOAD PRODUCTS
     * =====================================================
     */

    const loadProducts = useCallback(
        async () => {

            try {

                setLoading(true);

                setError("");


                const {
                    data
                } = await api.get(
                    "/api/products"
                );


                setProducts(
                    Array.isArray(data)
                        ? data
                        : []
                );


            } catch (requestError) {

                console.error(
                    "Customer products error:",
                    requestError
                );


                setError(
                    requestError
                        .response
                        ?.data
                        ?.message ||
                    "Unable to load products."
                );


            } finally {

                setLoading(false);

            }

        },
        []
    );

    const loadCartCount = useCallback(async () => {
        if (!user) {
            setCartItemCount(0);
            return;
        }
        try {
            const response = await api.get("/api/cart/count");
            setCartItemCount(Number(response.data || 0));
        } catch {
            // A cart badge must never block the marketplace from loading.
            setCartItemCount(0);
        }
    }, [user]);



    /*
     * =====================================================
     * INITIAL LOAD
     * =====================================================
     */

    useEffect(
        () => {

            const requestTimer =
                window.setTimeout(
                    () => {
                        void loadProducts();
                    },
                    0
                );


            return () => {

                window.clearTimeout(
                    requestTimer
                );

            };

        },
        [loadProducts]
    );

    useEffect(() => { void loadCartCount(); }, [loadCartCount]);

    useEffect(() => {
        saveWorkspacePreference("customer", "theme", theme);
        applyWorkspaceAppearance("customer");
    }, [theme]);

    useEffect(() => {
        saveWorkspacePreference("customer", "language", language);
        applyWorkspaceAppearance("customer");
    }, [language]);



    /*
     * =====================================================
     * CATEGORIES
     * =====================================================
     */

    const categories = useMemo(
        () => {

            const existing =
                new Set(
                    MARKETPLACE_CATEGORIES.map(
                        (category) =>
                            category.name.toLowerCase()
                    )
                );


            const sellerCategories =
                Array.from(
                    new Set(
                        products
                            .flatMap((product) => [
                                label(product.categoryEntity) || label(product.category),
                                label(product.subCategory)
                            ])
                            .filter(Boolean)
                    )
                )
                    .filter(
                        (name) =>
                            !existing.has(
                                name.toLowerCase()
                            )
                    )
                    .sort()
                    .map(
                        (name) => ({
                            name,
                            icon: "•"
                        })
                    );


            return [

                {
                    name: "All",
                    icon: "⌂"
                },

                ...MARKETPLACE_CATEGORIES,

                ...sellerCategories

            ];

        },
        [products]
    );

    const productFilterOptions = useMemo(() => {
        const values = (field) => Array.from(new Set(products
            .map(product => String(product[field] || "").trim())
            .filter(Boolean)))
            .sort((first, second) => first.localeCompare(second, undefined, { numeric: true }));

        return {
            brands: values("brand"),
            ram: values("ram"),
            storage: values("storage")
        };
    }, [products]);



    /*
     * =====================================================
     * FILTER PRODUCTS
     * =====================================================
     */

    const visibleProducts = useMemo(
        () => {

            const query =
                deferredSearchQuery
                    .trim()
                    .toLowerCase();


            const matchedProducts = products.filter(
                (product) => {

                    const productCategory =
                        label(product.categoryEntity) ||
                        label(product.category);

                    const stock = Number(
                        product.stock ??
                        product.availableStock ??
                        0
                    );

                    const productSubCategory = label(product.subCategory);
                    const price = Number(product.finalPrice ?? product.finalSellingPrice ?? product.price ?? 0);
                    const categoryMatch =
                        activeCategory === "All" ||
                        productCategory === activeCategory ||
                        productSubCategory === activeCategory;

                    const availabilityMatch =
                        availabilityFilter === "ALL" ||
                        (availabilityFilter === "IN_STOCK" && stock > 0) ||
                        (availabilityFilter === "OFFERS" &&
                            Number(product.offerPercentage || 0) > 0);


                    const searchMatch =
                        !query ||
                        [
                            product.name,
                            product.productName,
                            product.title,
                            product.description,
                            product.shortDescription,
                            productCategory,
                            productSubCategory,
                            product.brand,
                            product.model,
                            product.sku,
                            product.hsnCode,
                            product.specifications && Object.values(product.specifications).join(" ")
                        ]
                            .filter(Boolean)
                            .some(value => String(value).toLowerCase().includes(query));


                    const brandMatch = brandFilter === "ALL" || product.brand === brandFilter;
                    const ramMatch = ramFilter === "ALL" || product.ram === ramFilter;
                    const storageMatch = storageFilter === "ALL" || product.storage === storageFilter;
                    const minimumMatch = !minimumPrice || price >= Number(minimumPrice);
                    const maximumMatch = !maximumPrice || price <= Number(maximumPrice);
                    const attributeMatch = matchesAttributeFilters(product, attributeFilters);

                    return (
                        categoryMatch &&
                        availabilityMatch &&
                        searchMatch &&
                        brandMatch &&
                        ramMatch &&
                        storageMatch &&
                        minimumMatch &&
                        maximumMatch &&
                        attributeMatch
                    );

                }
            );

            if (sortOrder === "PRICE_LOW") {
                return [...matchedProducts].sort((first, second) =>
                    Number(first.finalPrice ?? first.finalSellingPrice ?? first.price ?? 0) -
                    Number(second.finalPrice ?? second.finalSellingPrice ?? second.price ?? 0)
                );
            }

            if (sortOrder === "PRICE_HIGH") {
                return [...matchedProducts].sort((first, second) =>
                    Number(second.finalPrice ?? second.finalSellingPrice ?? second.price ?? 0) -
                    Number(first.finalPrice ?? first.finalSellingPrice ?? first.price ?? 0)
                );
            }

            if (sortOrder === "NEWEST") {
                return [...matchedProducts].sort((first, second) =>
                    Number(second.id || 0) - Number(first.id || 0)
                );
            }

            if (sortOrder === "BEST_OFFERS") {
                return [...matchedProducts].sort((first, second) =>
                    Number(second.offerPercentage || 0) - Number(first.offerPercentage || 0)
                );
            }

            return matchedProducts;

        },
        [
            activeCategory,
            attributeFilters,
            availabilityFilter,
            brandFilter,
            deferredSearchQuery,
            maximumPrice,
            minimumPrice,
            products,
            ramFilter,
            sortOrder,
            storageFilter
        ]
    );



    /*
     * =====================================================
     * SCROLL PRODUCTS
     * =====================================================
     */

    const showProducts = () => {

        document
            .getElementById(
                "customer-products"
            )
            ?.scrollIntoView({
                behavior: "smooth"
            });

    };



    /*
     * =====================================================
     * CLEAR FILTERS
     * =====================================================
     */

    const clearFilters = () => {

        setSearchQuery("");

        setActiveCategory("All");

        setAvailabilityFilter("ALL");

        setSortOrder("RELEVANCE");

        setBrandFilter("ALL");

        setRamFilter("ALL");

        setStorageFilter("ALL");

        setMinimumPrice("");

        setMaximumPrice("");
        setAttributeFilters({});

    };

    const handleMarketplaceSearch = (value) => {
        setSearchQuery(value);

        // Search is global catalogue search, not restricted by a prior category chip.
        if (String(value || "").trim()) {
            setActiveCategory("All");
        }
    };

    const handleMarketplaceSuggestion = suggestion => {
        if (!suggestion) return;

        if (suggestion.type === "PRODUCT" && suggestion.id != null) {
            navigate(`/product/${suggestion.id}`);
            return;
        }

        setSearchQuery("");
        if (suggestion.type === "BRAND") {
            setActiveCategory("All");
            setBrandFilter(suggestion.metadata?.brand || suggestion.label || "ALL");
        } else if (suggestion.type === "CATEGORY" || suggestion.type === "SUBCATEGORY") {
            setActiveCategory(suggestion.label || "All");
        } else {
            setSearchQuery(suggestion.label || "");
        }
        showProducts();
    };



    /*
     * =====================================================
     * HOME
     * =====================================================
     */

    const handleHome = () => {

        navigate(
            "/customer/dashboard"
        );

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });

    };



    /*
     * =====================================================
     * CART
     * =====================================================
     */

    const handleOpenCart = () => {

        navigate(
            "/customer/cart"
        );

    };



    /*
     * =====================================================
     * WISHLIST
     * =====================================================
     */

    const handleOpenWishlist = () => {

        navigate(
            "/customer/wishlist"
        );

    };

    const handleOpenOffers = () => {
        document.getElementById("customer-offers")?.scrollIntoView({ behavior: "smooth" });
    };



    /*
     * =====================================================
     * PROFILE
     * =====================================================
     *
     * IMPORTANT:
     *
     * CustomerProfile route must exist before enabling
     * this navigation.
     *
     * =====================================================
     */

    const handleOpenProfile = () => {

        navigate(
            "/customer/profile"
        );

    };



    /*
     * =====================================================
     * ORDERS
     * =====================================================
     *
     * Orders page will be added in customer module.
     *
     * =====================================================
     */

    const handleOpenOrders = () => {

        navigate(
            "/customer/orders"
        );

    };

    const handleOpenAfterSales = () => {
        navigate("/customer/after-sales");
    };



    /*
     * =====================================================
     * LOGOUT
     * =====================================================
     */

    const handleLogout = () => {

        logout();

        navigate(
            "/login"
        );

    };



    /*
     * =====================================================
     * CATEGORY SELECT
     * =====================================================
     */

    const handleCategorySelect = (
        category
    ) => {
        if (category && category !== "All") {
            navigate(`/customer/products?category=${encodeURIComponent(category)}`);
            return;
        }
        setActiveCategory("All");
        showProducts();

    };



    /*
     * =====================================================
     * PRODUCT DETAILS
     * =====================================================
     */

    const handleOpenProduct = (
        productId
    ) => {

        navigate(
            `/product/${productId}`
        );

    };



    /*
     * =====================================================
     * PAGE
     * =====================================================
     */

    return (

        <main className={`marketplace-page marketplace-page--${theme}`}>


            {/* =================================================
                HEADER
            ================================================= */}

            <CustomerHeader

                user={user}

                searchQuery={
                    searchQuery
                }

                onSearchChange={handleMarketplaceSearch}

                onClearSearch={() =>
                    setSearchQuery("")
                }

                onSearchSubmit={showProducts}

                onSuggestionSelect={handleMarketplaceSuggestion}

                categories={categories}

                activeCategory={activeCategory}

                onCategoryChange={handleCategorySelect}

                availabilityFilter={availabilityFilter}

                onAvailabilityChange={setAvailabilityFilter}

                sortOrder={sortOrder}

                onSortChange={setSortOrder}

                brands={productFilterOptions.brands}

                brandFilter={brandFilter}

                onBrandChange={setBrandFilter}

                ramOptions={productFilterOptions.ram}

                ramFilter={ramFilter}

                onRamChange={setRamFilter}

                storageOptions={productFilterOptions.storage}

                storageFilter={storageFilter}

                onStorageChange={setStorageFilter}

                minimumPrice={minimumPrice}

                maximumPrice={maximumPrice}

                onMinimumPriceChange={setMinimumPrice}

                onMaximumPriceChange={setMaximumPrice}

                products={products}

                attributeFilters={attributeFilters}

                onAttributeFiltersChange={setAttributeFilters}

                onClearFilters={clearFilters}

                onHome={
                    handleHome
                }

                onCart={
                    handleOpenCart
                }

                cartItemCount={cartItemCount}

                onWishlist={
                    handleOpenWishlist
                }

                onOffers={handleOpenOffers}

                theme={theme}

                onThemeChange={setTheme}

                language={language}

                onLanguageChange={setLanguage}

                t={t}

                onProfile={
                    handleOpenProfile
                }

                onOrders={
                    handleOpenOrders
                }

                onAfterSales={handleOpenAfterSales}

                onSecondHandMarket={() => navigate("/customer/second-hand-market")}

                loyaltySummary={loyaltySummary}

                onRewards={() => navigate("/customer/rewards")}

                onLogout={
                    handleLogout
                }

            />



            {/* =================================================
                CATEGORY NAVIGATION
            ================================================= */}

            <CategoryNavigation

                categories={
                    categories
                }

                activeCategory={
                    activeCategory
                }

                onSelect={
                    handleCategorySelect
                }

            />

            {/* =================================================
                HERO
            ================================================= */}

            <CustomerHero

                greeting={
                    greeting
                }

                customerName={
                    user?.name
                        ?.split(" ")[0] ||
                    "Shopper"
                }

                productCount={
                    products.length
                }

                onExplore={
                    showProducts
                }

                t={t}

            />

            <section className="marketplace-command-center" aria-label="ShivHub quick access">
                <div className="marketplace-command-heading">
                    <div>
                        <span>YOUR SHIVHUB HUB</span>
                        <h2>Everything you need, in one place</h2>
                        <p>Shop new products, discover nearby pre-owned deals and keep every purchase service in view.</p>
                    </div>
                    <span className="marketplace-command-badge">{user?.name ? `Made for ${user.name.split(" ")[0]}` : "Your personal hub"}</span>
                </div>
                <div className="marketplace-command-grid">
                    <button type="button" className="marketplace-command-card marketplace-command-card--shop" onClick={showProducts}>
                        <span className="marketplace-command-icon"><PackageSearch size={22} aria-hidden="true" /></span>
                        <span><strong>Shop new products</strong><small>Explore approved products from ShivHub sellers</small></span>
                        <b>Browse <ArrowRight size={15} aria-hidden="true" /></b>
                    </button>
                    <button type="button" className="marketplace-command-card marketplace-command-card--market" onClick={() => navigate("/customer/second-hand-market")}>
                        <span className="marketplace-command-icon"><Recycle size={22} aria-hidden="true" /></span>
                        <span><strong>Second-hand Market</strong><small>Buy or list mobiles, vehicles, farm items, property and more</small></span>
                        <b>Open market <ArrowRight size={15} aria-hidden="true" /></b>
                    </button>
                    <button type="button" className="marketplace-command-card marketplace-command-card--orders" onClick={handleOpenOrders}>
                        <span className="marketplace-command-icon"><ReceiptText size={22} aria-hidden="true" /></span>
                        <span><strong>Orders &amp; EMI</strong><small>Track deliveries, invoices and finance purchase details</small></span>
                        <b>View orders <ArrowRight size={15} aria-hidden="true" /></b>
                    </button>
                    <button type="button" className="marketplace-command-card marketplace-command-card--care" onClick={handleOpenAfterSales}>
                        <span className="marketplace-command-icon"><HeartHandshake size={22} aria-hidden="true" /></span>
                        <span><strong>Service &amp; Returns</strong><small>Warranty support, repair updates and return requests</small></span>
                        <b>Get support <ArrowRight size={15} aria-hidden="true" /></b>
                    </button>
                </div>
            </section>


            <CustomerJourneyPromos
                onMarket={() => navigate("/customer/second-hand-market")}
                onReferral={() => navigate("/customer/referral")}
            />



            {/* =================================================
                CUSTOMER BENEFITS
            ================================================= */}

            <CustomerBenefits t={t} />

            <CustomerProductSections
                products={products}
                categories={categories}
                onOpenProduct={handleOpenProduct}
                onSelectCategory={handleCategorySelect}
                t={t}
                showCategoryPicks={false}
            />

            <div id="customer-offers">
                <CustomerOfferCarousel onExplore={showProducts} t={t} />
            </div>

            <CustomerCategoryCarousel
                categories={categories}
                products={products}
                activeCategory={activeCategory}
                onSelect={handleCategorySelect}
                t={t}
            />

            <CustomerProductSections
                products={products}
                categories={categories}
                onOpenProduct={handleOpenProduct}
                onSelectCategory={handleCategorySelect}
                t={t}
                showTrending={false}
            />



            {/* =================================================
                PRODUCTS
            ================================================= */}

            <section
                id="customer-products"
                className="marketplace-products"
            >


                {/* SECTION HEADER */}

                <div className="marketplace-section-heading">


                    <div>

                        <span>

                            {
                                activeCategory === "All"
                                    ? "ALL PRODUCTS"
                                    : activeCategory.toUpperCase()
                            }

                        </span>


                        <h2>

                            {
                                searchQuery
                                    ? "Search Results"
                                    : "Shop Products"
                            }

                        </h2>


                        {
                            !loading &&
                            !error && (

                                <p>

                                    {
                                        visibleProducts.length
                                    }

                                    {" "}

                                    item
                                    {
                                        visibleProducts.length === 1
                                            ? ""
                                            : "s"
                                    }

                                    {" "}found

                                </p>

                            )
                        }

                    </div>



                    <button
                        type="button"
                        className="marketplace-refresh"
                        onClick={
                            loadProducts
                        }
                        disabled={
                            loading
                        }
                    >

                        {
                            loading
                                ? "Loading..."
                                : "Refresh"
                        }

                    </button>


                </div>



                {/* ERROR */}

                {
                    error && (

                        <CustomerProductState
                            type="error"
                            message={
                                error
                            }
                            onAction={
                                loadProducts
                            }
                        />

                    )
                }



                {/* LOADING */}

                {
                    !error &&
                    loading && (

                        <CustomerProductState
                            type="loading"
                            message="Loading live products..."
                        />

                    )
                }



                {/* EMPTY */}

                {
                    !error &&
                    !loading &&
                    products.length === 0 && (

                        <CustomerProductState
                            type="empty"
                            message="Products will show here after a seller adds them and an admin approves them."
                        />

                    )
                }



                {/* NO SEARCH RESULT */}

                {
                    !error &&
                    !loading &&
                    products.length > 0 &&
                    visibleProducts.length === 0 && (

                        <CustomerProductState
                            type="no-results"
                            message="Try a different search or category."
                            onAction={
                                clearFilters
                            }
                        />

                    )
                }



                {/* PRODUCTS */}

                {
                    !error &&
                    !loading &&
                    visibleProducts.length > 0 && (

                        <CustomerProductGrid

                            products={
                                visibleProducts
                            }

                            onOpenProduct={
                                handleOpenProduct
                            }

                            onCart={handleOpenCart}

                            onCartUpdated={loadCartCount}

                        />

                    )
                }


            </section>

            <CustomerChatbot />


        </main>
    );
}
