import { useEffect, useState } from "react";
import { Heart, Menu, Package, Search, ShoppingCart, UserRound, X } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";
import { websiteContentApi } from "../services/websiteContentApi";
import "./CustomerPublicHeader.css";

/** Lightweight public-facing customer navigation for support and policy pages. */
export default function CustomerPublicHeader() {
    const navigate = useNavigate();
    const { isAuthenticated, user } = useAuth();
    const [menuOpen, setMenuOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [brand, setBrand] = useState("ShivHub");

    useEffect(() => {
        let active = true;
        websiteContentApi.getSettings().then(settings => {
            if (active && settings?.brandName) setBrand(settings.brandName);
        }).catch(() => {});
        return () => { active = false; };
    }, []);

    const customerPath = path => isAuthenticated ? path : "/login";
    const search = event => {
        event.preventDefault();
        navigate(isAuthenticated ? `/customer/products${query.trim() ? `?search=${encodeURIComponent(query.trim())}` : ""}` : "/login");
    };

    return <header className="customer-public-header">
        <div className="customer-public-header-inner">
            <Link to="/" className="customer-public-brand" aria-label={`${brand} home`}>
                <span className="customer-public-brand-mark"><ShoppingCart size={21} aria-hidden="true" /></span>
                <strong>{brand.slice(0, Math.max(1, brand.length - 3))}<b>{brand.slice(-3)}</b><small>SHOP SMART. LIVE BETTER.</small></strong>
            </Link>
            <form className="customer-public-search" onSubmit={search}>
                <Search size={18} aria-hidden="true" />
                <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search for mobiles, accessories, electronics…" aria-label="Search products" />
                <button aria-label="Search"><Search size={18} aria-hidden="true" /></button>
            </form>
            <button className="customer-public-menu" type="button" aria-label="Open navigation" aria-expanded={menuOpen} onClick={() => setMenuOpen(value => !value)}>{menuOpen ? <X /> : <Menu />}</button>
            <nav className={menuOpen ? "open" : ""} aria-label="Customer navigation">
                <Link to="/"><Package size={18} aria-hidden="true" /><span>Categories</span></Link>
                <Link to={customerPath("/customer/orders")}><Package size={18} aria-hidden="true" /><span>Orders</span></Link>
                <Link to={customerPath("/customer/wishlist")}><Heart size={18} aria-hidden="true" /><span>Wishlist</span></Link>
                <Link to={customerPath("/customer/cart")}><ShoppingCart size={18} aria-hidden="true" /><span>Cart</span></Link>
                <Link className="customer-public-profile" to={isAuthenticated ? "/customer/profile" : "/login"}><UserRound size={20} aria-hidden="true" /><span>{isAuthenticated ? (user?.name || "My account") : "Login / Register"}<small>{isAuthenticated ? "Customer" : "Customer"}</small></span></Link>
            </nav>
        </div>
    </header>;
}
