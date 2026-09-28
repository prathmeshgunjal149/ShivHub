import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ArrowRight, Camera, Gamepad2, Grid2X2, Headphones, Heart, House, Laptop, Monitor, MoreHorizontal, Package, Search, ShieldCheck, ShoppingBag, ShoppingCart, Smartphone, Store, Tag, Truck, Watch } from "lucide-react";
import api from "../../services/api";
import { imageUrl, money, stars } from "../common/product-details/productUtils";
import "./PublicHomePage.css";


const sellerModules = [
    ["Billing", "Create professional POS invoices and customer bills."],
    ["IMEI inventory", "Track serialized mobile stock through purchase and sale."],
    ["Distributor management", "Manage distributor requests, payments and adjustments."],
    ["CA reports", "Export sales, purchase, stock and accounting reports."]
];

const topCategories = [
    ["All", Grid2X2], ["Mobiles", Smartphone], ["Laptops", Laptop], ["Smartwatches", Watch], ["Headphones", Headphones], ["Cameras", Camera], ["TV & Home", Monitor], ["Gaming", Gamepad2], ["Accessories", ShoppingBag], ["Smart Home", House], ["More", MoreHorizontal]
];

const platformJourneys = [
    {
        tone: "customer",
        tag: "Customer",
        title: "Everything you need to shop with confidence.",
        text: "Discover verified products, compare real specifications and keep every purchase in one protected account.",
        points: ["Product search, filters & comparisons", "Wishlist, cart, checkout & order tracking", "Invoices, delivery updates, returns & warranty"],
        action: "Login as customer",
        to: "/login"
    },
    {
        tone: "seller",
        tag: "Seller",
        title: "Run your shop beyond the counter.",
        text: "A connected operating system for local electronics sellers—from stock to bills to business reports.",
        points: ["POS billing, offline customers & professional invoices", "IMEI inventory, variants, purchase & distributor control", "Offers, service requests, payments & CA-ready reports"],
        action: "Become a seller",
        to: "/register"
    },
    {
        tone: "market",
        tag: "Community",
        title: "Support that continues after checkout.",
        text: "Use one ShivHub account for value that keeps growing after you receive your product.",
        points: ["Second-hand listings and local marketplace discovery", "Repair, replacement, refund & warranty request tracking", "Credit points, referrals, reviews and customer support"],
        action: "Create your account",
        to: "/register"
    }
];

export default function PublicHomePage() {
    const navigate = useNavigate();
    const [params, setParams] = useSearchParams();
    const [products, setProducts] = useState([]);
    const [settings, setSettings] = useState(null);
    const [query, setQuery] = useState(params.get("q") || "");
    const [category, setCategory] = useState("All");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [menuOpen, setMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        let active = true;
        setLoading(true);
        Promise.all([
            api.get("/api/products").catch(() => ({ data: [] })),
            api.get("/api/public/site-settings").catch(() => ({ data: null }))
        ]).then(([productResponse, settingResponse]) => {
            if (!active) return;
            setProducts(Array.isArray(productResponse.data) ? productResponse.data : []);
            setSettings(settingResponse.data);
            setError("");
        }).catch(err => {
            if (active) setError(err.response?.data?.message || "Could not load ShivHub catalogue.");
        }).finally(() => {
            if (active) setLoading(false);
        });
        return () => { active = false; };
    }, []);

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 18);
        onScroll();
        window.addEventListener("scroll", onScroll, { passive: true });
        return () => window.removeEventListener("scroll", onScroll);
    }, []);

    const categories = useMemo(() => {
        const names = Array.from(new Set(products.map(product => product.categoryEntity?.name || product.category).filter(Boolean))).sort();
        return ["All", ...names];
    }, [products]);

    const filtered = useMemo(() => {
        const text = query.trim().toLowerCase();
        return products.filter(product => {
            const productCategory = product.categoryEntity?.name || product.category;
            const categoryMatch = category === "All" || productCategory === category;
            const searchMatch = !text || [product.name, product.brand, product.model, product.description, productCategory]
                .filter(Boolean)
                .some(value => String(value).toLowerCase().includes(text));
            return categoryMatch && searchMatch;
        });
    }, [products, query, category]);

    const featured = filtered.slice(0, 8);
    const latest = [...filtered].sort((a, b) => Number(b.id || 0) - Number(a.id || 0)).slice(0, 8);
    const offers = filtered.filter(product => Number(product.offerPercentage || 0) > 0).slice(0, 8);

    const submitSearch = event => {
        event.preventDefault();
        setParams(query ? { q: query } : {});
        document.getElementById("public-products")?.scrollIntoView({ behavior: "smooth" });
    };

    const productImage = product => product.images?.find(image => image.imageUrl)?.imageUrl || product.imageUrl;

    const productCard = product => {
        const price = product.finalSellingPrice ?? product.price;
        const ratingAvailable = product.averageRating !== undefined && product.averageRating !== null;

        return (
            <article className="public-product-card" key={product.id}>
                <Link to={`/product/${product.id}`} className="public-product-media" aria-label={`Open ${product.name}`}>
                    {productImage(product)
                        ? <img src={imageUrl(productImage(product))} alt={product.name} loading="lazy" />
                        : <span>No image</span>}
                    {Number(product.offerPercentage || 0) > 0 && <em>{product.offerPercentage}% off</em>}
                </Link>
                <div className="public-product-body">
                    <p>{product.brand || product.category || "Product"}</p>
                    <Link to={`/product/${product.id}`}>{product.name}</Link>
                    <small>{[product.ram, product.storage].filter(Boolean).join(" • ") || "View specifications"}</small>
                    <div className="public-product-foot">
                        <strong>{money(price)}</strong>
                        {ratingAvailable
                            ? <span className="public-product-rating">{stars(product.averageRating || 0)}</span>
                            : <span className="public-product-rating muted">No reviews yet</span>}
                    </div>
                    <Link className="product-card-action" to={`/product/${product.id}`}>View product</Link>
                </div>
            </article>
        );
    };

    return (
        <main className="public-home">
            <header className={`public-header ${scrolled ? "scrolled" : ""}`}>
                <div className="public-header-main">
                    <Link className="public-logo" to="/"><span className="public-logo-mark"><ShoppingBag size={20} /></span>Shiv<span>Hub</span></Link>
                    <form className="public-search" onSubmit={submitSearch}><Search size={19} aria-hidden="true" /><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search mobiles, accessories, brands, electronics..." aria-label="Search products" /><button>Search</button></form>
                    <button className="public-menu-button" aria-expanded={menuOpen} onClick={() => setMenuOpen(value => !value)}>Menu</button>
                    <nav className={menuOpen ? "open" : ""}><a href="#public-categories"><Grid2X2 size={18} />Categories</a><a href="#public-products"><Tag size={18} />Products</a><a href="#seller-section"><Store size={18} />For sellers</a><span className="public-nav-divider" aria-hidden="true" /><Link className="nav-icon-link wishlist-link" to="/login" title="Login to view wishlist" aria-label="Login to view wishlist"><Heart size={21} /></Link><Link className="nav-icon-link cart-link" to="/login" title="Login to view cart" aria-label="Login to view cart"><ShoppingCart size={21} /><b>3</b></Link><Link className="login-link" to="/login">Login</Link><Link className="register" to="/register">Register</Link></nav>
                </div>
            </header>
            <nav className="public-category-navigation" aria-label="Marketplace categories">{topCategories.map(([name, Icon]) => <button key={name} className={name === "All" || category === name ? "active" : ""} onClick={() => { setCategory(categories.includes(name) ? name : "All"); document.getElementById("public-categories")?.scrollIntoView({ behavior: "smooth" }); }}><Icon size={20} /><span>{name}</span></button>)}</nav>

            <section className="public-hero">
                <span className="hero-grid" aria-hidden="true" />
                <div className="hero-copy">
                    <p className="eyebrow">Premium electronics marketplace</p>
                    <h1>Your complete electronics world, in one <strong>secure place.</strong></h1>
                    <span className="hero-lede">Shop verified products, manage a local store, discover the second-hand market and get support after checkout—all through ShivHub.</span>
                    <div className="hero-actions">
                        <button onClick={() => document.getElementById("public-products")?.scrollIntoView({ behavior: "smooth" })}><ShoppingBag size={17} />Explore products <ArrowRight size={17} /></button>
                        <button className="ghost" onClick={() => navigate("/login")}>Login to ShivHub</button>
                    </div>
                    <div className="hero-benefits" aria-label="ShivHub account benefits"><span><ShieldCheck size={23} />100% verified<br />products</span><span><Truck size={23} />Fast &amp; secure<br />delivery</span><span><Package size={23} />Easy returns<br />&amp; support</span><span><Headphones size={23} />Dedicated<br />customer support</span></div>
                </div>
                <aside className="hero-device-stack hero-showcase" aria-label="Featured products">
                    <div className="hero-stat">
                        <strong>{loading ? "—" : products.length}</strong>
                        <span>Live products on ShivHub</span>
                    </div>
                    {featured.slice(0, 2).map((product, index) => (
                        <Link to={`/product/${product.id}`} key={product.id} className={`hero-device-card card-${index + 1}`}>
                            {productImage(product)
                                ? <img src={imageUrl(productImage(product))} alt={product.name} />
                                : <span>{product.name}</span>}
                            <small>{product.brand || "ShivHub product"}</small>
                        </Link>
                    ))}
                    {!featured.length && <strong className="hero-empty-note">Catalogue will appear here after products are approved.</strong>}
                </aside>
            </section>

            <section className="public-intro">
                <article><strong>Browse</strong><span>Search products and open full specifications before login.</span></article>
                <article><strong>Compare</strong><span>See available stock, genuine offers and real review data.</span></article>
                <article><strong>Buy</strong><span>Login only when you want wishlist, cart, checkout or order history.</span></article>
            </section>

            <section className="public-platform-map" aria-label="Explore ShivHub">
                <div className="public-platform-head">
                    <p className="eyebrow">One account, complete access</p>
                    <h2>Made for every step of your electronics journey.</h2>
                    <span>Start as a shopper, grow as a seller, and keep service, rewards and marketplace access close whenever you need them.</span>
                </div>
                <div className="public-platform-grid">
                    {platformJourneys.map(journey => <article className={`platform-journey ${journey.tone}`} key={journey.tag}>
                        <p className="journey-tag">{journey.tag}</p>
                        <h3>{journey.title}</h3>
                        <span className="journey-copy">{journey.text}</span>
                        <ul>{journey.points.map(point => <li key={point}>{point}</li>)}</ul>
                        <Link to={journey.to}>{journey.action} <b aria-hidden="true">→</b></Link>
                    </article>)}
                </div>
            </section>

            <section className="public-hub-highlights" aria-label="ShivHub highlights">
                <div className="public-hub-copy">
                    <p className="eyebrow">One platform, more possibilities</p>
                    <h2>Shop new. Discover pre-owned. Grow your business.</h2>
                    <span>ShivHub brings customers, local sellers and trusted marketplace tools together—with real listings, clear details and admin-reviewed activity.</span>
                    <div className="public-hub-actions">
                        <button type="button" onClick={() => navigate("/register")}>Join ShivHub</button>
                        <Link to="/login">Already a member? Login</Link>
                    </div>
                </div>
                <div className="public-hub-feature-grid">
                    <article><strong>Second-hand market</strong><span>Mobiles, cars, bikes, farming equipment, animals, property and more.</span></article>
                    <article><strong>Complete product details</strong><span>Specifications, warranty, images and seller information in one view.</span></article>
                    <article><strong>Finance &amp; after-sales</strong><span>EMI support, invoices, service requests and return tracking.</span></article>
                    <article><strong>Built for local business</strong><span>Billing, inventory, reports and customer relationships for sellers.</span></article>
                </div>
            </section>

            <section className="public-section category-section" id="public-categories">
                <div className="public-section-head"><p className="eyebrow">Browse by category</p><h2>Find the right electronics faster</h2></div>
                <div className="public-category-row">
                    {categories.map(item => {
                        const count = item === "All" ? products.length : products.filter(product => (product.categoryEntity?.name || product.category) === item).length;
                        return (
                            <button className={category === item ? "active" : ""} key={item} onClick={() => setCategory(item)}>
                                <strong>{item}</strong>
                                <span>{count}</span>
                            </button>
                        );
                    })}
                </div>
            </section>

            <section className="public-section" id="public-products">
                <div className="public-section-head"><p className="eyebrow">Featured products</p><h2>Live ShivHub catalogue</h2></div>
                {loading && <div className="public-skeleton-grid">{Array.from({ length: 4 }).map((_, index) => <span key={index} />)}</div>}
                {error && <div className="public-empty"><h3>Something went wrong</h3><p>{error}</p><button onClick={() => window.location.reload()}>Retry</button></div>}
                {!loading && !error && !featured.length && <div className="public-empty"><h3>No approved products yet</h3><p>Once sellers/admin approve products, they will appear here automatically.</p></div>}
                {!!featured.length && <div className="public-product-grid">{featured.map(productCard)}</div>}
            </section>

            {!!offers.length && (
                <section className="public-section offer-section">
                    <div className="public-section-head"><p className="eyebrow">Active offers</p><h2>Genuine live discounts</h2></div>
                    <div className="public-product-row">{offers.map(productCard)}</div>
                </section>
            )}

            {!!latest.length && (
                <section className="public-section latest-section">
                    <div className="public-section-head"><p className="eyebrow">Latest arrivals</p><h2>Recently added products</h2></div>
                    <div className="public-product-row">{latest.map(productCard)}</div>
                </section>
            )}

            <section className="seller-intro" id="seller-section">
                <div>
                    <p className="eyebrow">For sellers</p>
                    <h2>Run your mobile shop with ShivHub tools.</h2>
                    <span>Seller onboarding uses the existing registration, OTP verification and admin approval flow.</span>
                    <button onClick={() => navigate("/register")}>Start seller onboarding</button>
                </div>
                <div className="seller-module-grid">
                    {sellerModules.map(([title, text]) => <article key={title}><strong>{title}</strong><span>{text}</span></article>)}
                </div>
            </section>

            <section className="public-account-cta">
                <div><p className="eyebrow">Ready when you are</p><h2>One ShivHub login unlocks the whole experience.</h2><span>Save products, manage orders, collect reward points, request service or turn your own shop into a connected business.</span></div>
                <div className="public-account-actions"><Link to="/login">Login securely</Link><Link to="/register">Create free account</Link></div>
            </section>

            {settings?.demoData && <p className="public-demo-note">Demo data — replace business information in Admin Settings before going live.</p>}
        </main>
    );
}
