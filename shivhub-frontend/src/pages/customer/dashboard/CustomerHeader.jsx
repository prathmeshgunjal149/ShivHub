import {
    useMemo,
    useState
} from "react";
import {
    BadgePercent, ChevronDown, ChevronUp, Coins, Heart, Languages, LogOut,
    Moon, Package, Recycle, Search, ShoppingBag, ShoppingCart, SlidersHorizontal, Sun,
    UserRound, Wrench, X
} from "lucide-react";
import SearchAutocomplete from "../../../components/common/SearchAutocomplete/SearchAutocomplete";
import { getCustomerSearchSuggestions } from "../../../services/searchSuggestionService";
import DynamicAttributeFilters from "../../../components/products/DynamicAttributeFilters";
import "./CustomerLoyaltyBadge.css";


/*
 * =========================================================
 * CustomerHeader
 * =========================================================
 *
 * ShivHub customer marketplace header.
 *
 * Working actions:
 *
 * - Home
 * - Search
 * - Wishlist
 * - Cart
 * - Profile
 * - Orders
 * - Logout
 *
 * =========================================================
 */


export default function CustomerHeader({

    user,

    searchQuery,

    onSearchChange,

    onSearchSubmit,

    onSuggestionSelect,

    onClearSearch,

    categories = [],

    activeCategory,

    onCategoryChange,

    availabilityFilter,

    onAvailabilityChange,

    sortOrder,

    onSortChange,

    brands = [],

    brandFilter,

    onBrandChange,

    ramOptions = [],

    ramFilter,

    onRamChange,

    storageOptions = [],

    storageFilter,

    onStorageChange,

    minimumPrice,

    maximumPrice,

    onMinimumPriceChange,

    onMaximumPriceChange,

    products = [],

    attributeFilters = {},

    onAttributeFiltersChange,

    onClearFilters,

    onHome,

    onCart,

    cartItemCount = 0,

    onWishlist,

    onOffers,

    theme = "light",

    onThemeChange,

    language = "en",

    onLanguageChange,

    t = value => value,

    onProfile,

    onOrders,

    onAfterSales,

    onSecondHandMarket,

    loyaltySummary,

    onRewards,

    onLogout

}) {


    const [
        profileOpen,
        setProfileOpen
    ] = useState(false);

    const [
        filtersOpen,
        setFiltersOpen
    ] = useState(false);

    const [
        preferencesOpen,
        setPreferencesOpen
    ] = useState(false);



    /*
     * =====================================================
     * SEARCH
     * =====================================================
     */

    /*
     * =====================================================
     * PROFILE MENU
     * =====================================================
     */

    const toggleProfile = () => {

        setProfileOpen(
            previous =>
                !previous
        );

    };



    /*
     * =====================================================
     * PROFILE
     * =====================================================
     */

    const handleProfile = () => {

        setProfileOpen(false);

        if (typeof onProfile === "function") {

            onProfile();

        }

    };



    /*
     * =====================================================
     * ORDERS
     * =====================================================
     */

    const handleOrders = () => {

        setProfileOpen(false);

        if (typeof onOrders === "function") {

            onOrders();

        }

    };

    const handleAfterSales = () => {
        setProfileOpen(false);
        if (typeof onAfterSales === "function") onAfterSales();
    };



    /*
     * =====================================================
     * CART
     * =====================================================
     */

    const handleCart = () => {

        setProfileOpen(false);

        if (typeof onCart === "function") {

            onCart();

        }

    };



    /*
     * =====================================================
     * WISHLIST
     * =====================================================
     */

    const handleWishlist = () => {

        setProfileOpen(false);

        if (typeof onWishlist === "function") {

            onWishlist();

        }

    };



    /*
     * =====================================================
     * LOGOUT
     * =====================================================
     */

    const handleLogout = () => {

        setProfileOpen(false);

        if (typeof onLogout === "function") {

            onLogout();

        }

    };



    /*
     * =====================================================
     * USER DATA
     * =====================================================
     */

    const customerName =
        user?.name ||
        "Customer";


    const customerEmail =
        user?.email ||
        "";


    const avatarLetter =
        customerName
            ?.charAt(0)
            ?.toUpperCase() ||
        "U";

    const subcategories = useMemo(() => {
        const activeName = String(activeCategory || "All");
        if (activeName === "All") return [];
        const found = new Map();
        products.filter(product => {
            const categoryName = product?.categoryEntity?.name || product?.category;
            return categoryName === activeName || product?.subCategory?.name === activeName;
        }).forEach(product => {
            const subcategory = product?.subCategory;
            if (subcategory?.name) found.set(String(subcategory.id || subcategory.name), subcategory.name);
        });
        return [...found.values()].sort((first, second) => first.localeCompare(second));
    }, [activeCategory, products]);



    /*
     * =====================================================
     * PAGE
     * =====================================================
     */

    return (

        <header className="customer-header">


            <div className="customer-header-inner">


                {/* =================================================
                    LOGO
                ================================================= */}

                <button
                    type="button"
                    className="customer-brand"
                    onClick={
                        onHome
                    }
                >

                    <span className="customer-brand-mark" aria-hidden="true">
                        <ShoppingBag size={25} />
                    </span>


                    <span className="customer-brand-name">

                        Shiv<span>Hub</span>

                    </span>

                </button>



                {/* =================================================
                    SEARCH
                ================================================= */}

                <div className="customer-search">


                    <span className="customer-search-icon">

                        <Search size={18} aria-hidden="true" />

                    </span>


                    <SearchAutocomplete
                        value={searchQuery || ""}
                        onChange={onSearchChange}
                        onSelect={suggestion => {
                            if (typeof onSuggestionSelect === "function") {
                                onSuggestionSelect(suggestion);
                                return;
                            }
                            onSearchChange?.(suggestion.label || "");
                            onSearchSubmit?.();
                        }}
                        onEnterWithoutSelection={onSearchSubmit}
                        fetchSuggestions={getCustomerSearchSuggestions}
                        placeholder={t("searchPlaceholder")}
                        className="customer-search-autocomplete"
                        inputProps={{ "aria-label": t("searchPlaceholder") }}
                    />


                    {
                        searchQuery && (

                            <button
                                type="button"
                                className="customer-search-clear"
                                onClick={
                                    onClearSearch
                                }
                                aria-label="Clear search"
                            >

                                <X size={16} aria-hidden="true" />

                            </button>

                        )
                    }

                    <button
                        type="button"
                        className="customer-search-submit"
                        onClick={onSearchSubmit}
                        aria-label="Search products"
                    >
                        <Search size={16} aria-hidden="true" />
                        <span>Search</span>
                    </button>

                    <button
                        type="button"
                        className="customer-filter-toggle"
                        onClick={() => setFiltersOpen(previous => !previous)}
                        aria-expanded={filtersOpen}
                    >
                        <SlidersHorizontal size={16} aria-hidden="true" />
                        <span>{t("filters")}</span>
                    </button>


                </div>



                {/* =================================================
                    HEADER ACTIONS
                ================================================= */}

                <div className="customer-header-actions">

                    {user?.role === "CUSTOMER" && (
                        <button type="button" className="customer-loyalty-badge" title="Available ShivHub Points" onClick={onRewards}>
                            <span className="customer-loyalty-badge-icon" aria-hidden="true"><Coins size={16} /></span>
                            <span>{Number(loyaltySummary?.availablePoints || 0)} <span className="customer-loyalty-badge-label">Points</span></span>
                        </button>
                    )}

                    <button
                        type="button"
                        className="customer-header-action customer-theme-toggle"
                        onClick={() => setPreferencesOpen(previous => !previous)}
                        aria-expanded={preferencesOpen}
                        title={t("appearance")}
                    >
                        <span className="customer-action-icon">{theme === "dark" ? <Sun size={17} /> : <Moon size={17} />}</span>
                        <span>{t("appearance")}</span>
                    </button>

                    {preferencesOpen && (
                        <div className="customer-preferences-menu" aria-label="Customer preferences">
                            <strong>{t("appearance")}</strong>
                            <div className="customer-theme-options">
                                {["light", "dark", "emerald", "violet"].map(option => (
                                    <button
                                        type="button"
                                        key={option}
                                        className={theme === option ? "active" : ""}
                                        onClick={() => onThemeChange?.(option)}
                                    >
                                        <i className={`customer-theme-swatch customer-theme-swatch--${option}`} />
                                        {t(option)}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    <label className="customer-header-language" title={t("language")}>
                        <span><Languages size={16} aria-hidden="true" /></span>
                        <select value={language} onChange={event => onLanguageChange?.(event.target.value)} aria-label={t("language")}>
                            <option value="en">English</option>
                            <option value="mr">मराठी</option>
                            <option value="hi">हिन्दी</option>
                        </select>
                    </label>

                    <button type="button" className="customer-header-action" onClick={onOffers} title={t("offers")}>
                        <span className="customer-action-icon"><BadgePercent size={17} /></span><span>{t("offers")}</span>
                    </button>

                    <button type="button" className="customer-header-action" onClick={handleAfterSales} title="Service & Returns">
                        <span className="customer-action-icon"><Wrench size={17} /></span><span>Service</span>
                    </button>

                    <button
                        type="button"
                        className="customer-header-action"
                        onClick={() => onSecondHandMarket?.()}
                        title="Second-hand Market"
                    >
                        <span className="customer-action-icon"><Recycle size={17} /></span><span>Second-hand</span>
                    </button>


                    {/* =================================================
                        WISHLIST
                    ================================================= */}

                    <button
                        type="button"
                        className="customer-header-action"
                        onClick={
                            handleWishlist
                        }
                        title={t("wishlist")}
                    >

                        <span className="customer-action-icon">

                            <Heart size={17} />

                        </span>


                        <span>

                            {t("wishlist")}

                        </span>

                    </button>



                    {/* =================================================
                        CART
                    ================================================= */}

                    <button
                        type="button"
                        className="customer-header-action"
                        onClick={
                            handleCart
                        }
                        title={t("cart")}
                    >

                        <span className="customer-action-icon">

                            <ShoppingCart size={17} />

                        </span>


                        <span>

                            {t("cart")}

                        </span>

                        {cartItemCount > 0 && <span className="customer-cart-count" aria-label={`${cartItemCount} cart items`}>{cartItemCount > 99 ? "99+" : cartItemCount}</span>}

                    </button>



                    {/* =================================================
                        PROFILE
                    ================================================= */}

                    <div className="customer-profile-wrapper">


                        <button
                            type="button"
                            className="customer-profile-button"
                            onClick={
                                toggleProfile
                            }
                        >


                            <span className="customer-avatar">

                                {avatarLetter}

                            </span>


                            <span className="customer-profile-name">

                                {customerName}

                            </span>


                            <span className="customer-profile-arrow">

                                {
                                    profileOpen ? <ChevronUp size={15} /> : <ChevronDown size={15} />
                                }

                            </span>


                        </button>



                        {/* =================================================
                            PROFILE DROPDOWN
                        ================================================= */}

                        {
                            profileOpen && (

                                <div className="customer-profile-menu">


                                    {/* USER */}

                                    <div className="customer-profile-menu-user">


                                        <span className="customer-avatar customer-avatar-large">

                                            {avatarLetter}

                                        </span>


                                        <div>

                                            <strong>

                                                {customerName}

                                            </strong>


                                            <small>

                                                {customerEmail}

                                            </small>

                                        </div>


                                    </div>



                                    <div className="customer-profile-menu-divider" />



                                    {/* PROFILE */}

                                    <button
                                        type="button"
                                        onClick={
                                            handleProfile
                                        }
                                    >

                                        <span>

                                            <UserRound size={17} />

                                        </span>


                                        {t("profile")}

                                    </button>



                                    {/* ORDERS */}

                                    <button
                                        type="button"
                                        onClick={
                                            handleOrders
                                        }
                                    >

                                        <span>

                                            <Package size={17} />

                                        </span>


                                        {t("orders")}

                                    </button>

                                    <button type="button" onClick={() => { setProfileOpen(false); onSecondHandMarket?.(); }}>
                                        <span><Recycle size={17} /></span>
                                        Second-hand Market
                                    </button>



                                    {/* WISHLIST */}

                                    <button
                                        type="button"
                                        onClick={
                                            handleWishlist
                                        }
                                    >

                                        <span>

                                            <Heart size={17} />

                                        </span>


                                        {t("wishlist")}

                                    </button>



                                    {/* CART */}

                                    <button
                                        type="button"
                                        onClick={
                                            handleCart
                                        }
                                    >

                                        <span>

                                            <ShoppingCart size={17} />

                                        </span>


                                        {t("cart")}

                                    </button>



                                    <div className="customer-profile-menu-divider" />



                                    {/* LOGOUT */}

                                    <button
                                        type="button"
                                        className="customer-logout-button"
                                        onClick={
                                            handleLogout
                                        }
                                    >

                                        <span>

                                            <LogOut size={17} />

                                        </span>


                                        {t("logout")}

                                    </button>


                                </div>

                            )
                        }


                    </div>


                </div>


            </div>

            {subcategories.length > 0 && (
                <nav className="customer-subcategory-nav" aria-label="Subcategories">
                    <span>Browse {activeCategory}</span>
                    {subcategories.map(subcategory => <button type="button" key={subcategory} onClick={() => onCategoryChange?.(subcategory)}>{subcategory}</button>)}
                </nav>
            )}


            {filtersOpen && (
                <div className="customer-filter-panel" aria-label="Product filters">
                    <label>
                        Category
                        <select
                            value={activeCategory || "All"}
                            onChange={event => onCategoryChange?.(event.target.value)}
                        >
                            {categories.map(category => (
                                <option key={category.name} value={category.name}>
                                    {category.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Availability
                        <select
                            value={availabilityFilter || "ALL"}
                            onChange={event => onAvailabilityChange?.(event.target.value)}
                        >
                            <option value="ALL">All products</option>
                            <option value="IN_STOCK">In stock</option>
                            <option value="OFFERS">Offers only</option>
                        </select>
                    </label>
                    <label>
                        Brand
                        <select
                            value={brandFilter || "ALL"}
                            onChange={event => onBrandChange?.(event.target.value)}
                        >
                            <option value="ALL">All brands</option>
                            {brands.map(brand => <option key={brand} value={brand}>{brand}</option>)}
                        </select>
                    </label>
                    {ramOptions.length > 0 && (
                        <label>
                            RAM
                            <select value={ramFilter || "ALL"} onChange={event => onRamChange?.(event.target.value)}>
                                <option value="ALL">Any RAM</option>
                                {ramOptions.map(ram => <option key={ram} value={ram}>{ram}</option>)}
                            </select>
                        </label>
                    )}
                    {storageOptions.length > 0 && (
                        <label>
                            Storage
                            <select value={storageFilter || "ALL"} onChange={event => onStorageChange?.(event.target.value)}>
                                <option value="ALL">Any storage</option>
                                {storageOptions.map(storage => <option key={storage} value={storage}>{storage}</option>)}
                            </select>
                        </label>
                    )}
                    <DynamicAttributeFilters
                        products={products}
                        activeCategory={activeCategory}
                        values={attributeFilters}
                        onChange={onAttributeFiltersChange}
                    />
                    <label>
                        Minimum price
                        <input
                            type="number"
                            min="0"
                            inputMode="numeric"
                            value={minimumPrice || ""}
                            onChange={event => onMinimumPriceChange?.(event.target.value)}
                            placeholder="No minimum"
                        />
                    </label>
                    <label>
                        Maximum price
                        <input
                            type="number"
                            min="0"
                            inputMode="numeric"
                            value={maximumPrice || ""}
                            onChange={event => onMaximumPriceChange?.(event.target.value)}
                            placeholder="No maximum"
                        />
                    </label>
                    <label>
                        Sort by
                        <select
                            value={sortOrder || "RELEVANCE"}
                            onChange={event => onSortChange?.(event.target.value)}
                        >
                            <option value="RELEVANCE">Recommended</option>
                            <option value="NEWEST">Newest first</option>
                            <option value="PRICE_LOW">Price: low to high</option>
                            <option value="PRICE_HIGH">Price: high to low</option>
                            <option value="BEST_OFFERS">Best offers</option>
                        </select>
                    </label>
                    <button type="button" onClick={onClearFilters}>Clear all</button>
                </div>
            )}


        </header>

    );
}
