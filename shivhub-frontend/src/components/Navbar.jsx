import { useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";

const Navbar = () => {

    const navigate = useNavigate();

    const { user, logout } = useAuth();

    const [search, setSearch] = useState("");

    const handleSearch = (event) => {

        event.preventDefault();

        if (!search.trim()) {
            return;
        }

        navigate(
            `/search?q=${encodeURIComponent(search.trim())}`
        );
    };


    const handleLogout = () => {

        logout();

        navigate("/login");
    };


    return (
        <>
            {/* =========================
                TOP NAVBAR
            ========================== */}

            <header className="shivhub-navbar">

                {/* =========================
                    SHIVHUB BRAND
                ========================== */}

                <div
                    className="shivhub-brand"
                    onClick={() =>
                        navigate("/customer/dashboard")
                    }
                >

                    <div className="brand-logo">

                        <span className="logo-letter">
                            S
                        </span>

                        <span className="logo-shine"></span>

                    </div>


                    <div className="brand-text">

                        <h1>
                            Shiv<span>Hub</span>
                        </h1>

                        <p>
                            Shop Smart. Live Better.
                        </p>

                    </div>

                </div>


                {/* =========================
                    SEARCH BAR
                ========================== */}

                <form
                    className="navbar-search"
                    onSubmit={handleSearch}
                >

                    <div className="search-icon">
                        🔍
                    </div>


                    <input
                        type="text"
                        placeholder="Search mobiles, electronics, accessories..."
                        value={search}
                        onChange={(event) =>
                            setSearch(event.target.value)
                        }
                    />


                    {search && (
                        <button
                            type="button"
                            className="clear-search"
                            onClick={() =>
                                setSearch("")
                            }
                        >
                            ×
                        </button>
                    )}


                    <button
                        type="submit"
                        className="search-button"
                    >
                        Search
                    </button>

                </form>


                {/* =========================
                    NAVBAR ACTIONS
                ========================== */}

                <div className="navbar-actions">


                    {/* Wishlist */}

                    <button
                        className="nav-action"
                        onClick={() =>
                            navigate("/customer/wishlist")
                        }
                    >

                        <span className="nav-icon">
                            ♡
                        </span>

                        <small>
                            Wishlist
                        </small>

                    </button>


                    {/* Cart */}

                    <button
                        className="nav-action cart-action"
                        onClick={() =>
                            navigate("/customer/cart")
                        }
                    >

                        <span className="nav-icon">
                            🛒
                        </span>

                        <span className="cart-badge">
                            0
                        </span>

                        <small>
                            Cart
                        </small>

                    </button>


                    {/* Profile */}

                    <button
                        className="profile-action"
                        onClick={() =>
                            navigate("/customer/profile")
                        }
                    >

                        <span className="profile-avatar">

                            {
                                user?.name
                                    ?.charAt(0)
                                    ?.toUpperCase() || "U"
                            }

                        </span>


                        <div className="profile-info">

                            <small>
                                Hello,
                            </small>

                            <strong>
                                {user?.name || "User"}
                            </strong>

                        </div>

                        <span className="profile-arrow">
                            ▾
                        </span>

                    </button>


                    {/* Logout */}

                    <button
                        className="logout-button"
                        onClick={handleLogout}
                    >

                        <span>
                            ↪
                        </span>

                        Logout

                    </button>

                </div>

            </header>


            {/* =========================
                OFFER STRIP
            ========================== */}

            <div className="navbar-offer-strip">

                <div className="offer-message">

                    <span className="offer-fire">
                        🔥
                    </span>

                    <span>
                        ShivHub Special Deals
                    </span>

                    <strong>
                        Up to 50% OFF
                    </strong>

                </div>


                <div className="offer-right">

                    <span>
                        🚚 Free delivery on selected products
                    </span>

                    <span className="offer-divider">
                        |
                    </span>

                    <span>
                        🔒 Secure Payments
                    </span>

                </div>

            </div>

        </>
    );
};

export default Navbar;