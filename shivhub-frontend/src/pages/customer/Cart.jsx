import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { AlertCircle, ArrowRight, Box, CheckCircle2, CreditCard, Home, Minus, Package, Plus, RotateCcw, ShoppingCart, Trash2, Truck } from "lucide-react";
import api from "../../services/api";

import "./Cart.css";
import "./CartTheme.css";
import MobileDeliveryEstimate from "../../components/products/MobileDeliveryEstimate";
import { variantSummary } from "../../components/products/productCommonFields";
import { listItem, noTransform } from "../../utils/animationVariants";

const getToken = () => localStorage.getItem("shivhub_token");
const getAuthConfig = () => ({ headers: { Authorization: `Bearer ${getToken()}` } });
const CartNavigation = ({ navigate }) => <><nav className="cart-navigation" aria-label="Customer navigation"><button type="button" className="cart-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingCart size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/products")}><Package size={18} /> Products</button><button type="button" onClick={() => navigate("/customer/orders")}><Box size={18} /> Orders</button><button type="button" className="active" onClick={() => navigate("/customer/cart")}><ShoppingCart size={18} /> Cart</button></div></nav><section className="cart-benefits" aria-label="Shopping benefits"><span><Truck size={22} /><b>Fast Delivery<small>Based on your address</small></b></span><span><CreditCard size={22} /><b>Secure Payments<small>Protected checkout</small></b></span><span><RotateCcw size={22} /><b>Easy Returns<small>When eligible</small></b></span><span><CheckCircle2 size={22} /><b>Genuine Products<small>Verified listings</small></b></span></section></>;

const Cart = () => {

    const navigate = useNavigate();
    const reducedMotion = useReducedMotion();

    const [cartItems, setCartItems] = useState([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [processing, setProcessing] = useState({});

    /* Estimates are keyed by cart line so variants remain separate. Charges
       are later deduplicated by seller, exactly as the backend does. */
    const [deliveryEstimates, setDeliveryEstimates] = useState({});


    /*
     * =====================================================
     * LOAD CART
     * =====================================================
     */

    const fetchCart = useCallback(async () => {

        try {

            setLoading(true);
            setError("");

            const token = getToken();

            if (!token) {

                setError(
                    "Please login to view your cart."
                );

                return;
            }


            const response = await api.get(
                "/api/cart",
                getAuthConfig()
            );


            const data = Array.isArray(response.data)
                ? response.data
                : [];


            setCartItems(data);

        } catch (err) {

            console.error(
                "Failed to load cart:",
                err
            );


            if (
                err.response?.status === 401 ||
                err.response?.status === 403
            ) {

                setError(
                    "Your login session has expired. Please login again."
                );

            } else {

                setError(
                    err.response?.data?.message ||
                    "Unable to load your cart."
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

        fetchCart();

    }, [fetchCart]);

    useEffect(() => {
        const currentIds = new Set(cartItems.map(item => String(item.id)));
        setDeliveryEstimates(previous => {
            const next = Object.fromEntries(
                Object.entries(previous).filter(([itemId]) => currentIds.has(String(itemId)))
            );
            return Object.keys(next).length === Object.keys(previous).length ? previous : next;
        });
    }, [cartItems]);


    /*
     * =====================================================
     * PROCESSING STATE
     * =====================================================
     */

    const setItemProcessing = (
        itemId,
        value
    ) => {

        setProcessing(
            previous => ({
                ...previous,
                [itemId]: value
            })
        );
    };


    /*
     * =====================================================
     * INCREASE
     * =====================================================
     */

    const handleIncrease = async (
        item
    ) => {

        setItemProcessing(
            item.id,
            true
        );


        try {

            const response = await api.post(
                item.variantId ? `/api/cart/lines/${item.id}/increase` : `/api/cart/${item.product.id}/increase`,
                {},
                getAuthConfig()
            );


            setCartItems(
                previous =>
                    previous.map(
                        cartItem =>
                            cartItem.id === item.id
                                ? response.data
                                : cartItem
                    )
            );

        } catch (err) {

            console.error(
                "Increase quantity failed:",
                err
            );


            alert(
                err.response?.data?.message ||
                "Unable to increase quantity."
            );

        } finally {

            setItemProcessing(
                item.id,
                false
            );
        }
    };


    /*
     * =====================================================
     * DECREASE
     * =====================================================
     */

    const handleDecrease = async (
        item
    ) => {

        setItemProcessing(
            item.id,
            true
        );


        try {

            const response = await api.post(
                item.variantId ? `/api/cart/lines/${item.id}/decrease` : `/api/cart/${item.product.id}/decrease`,
                {},
                getAuthConfig()
            );


            /*
             * If backend removes item.
             */

            if (response.status === 204) {

                setCartItems(
                    previous =>
                        previous.filter(
                            cartItem =>
                                cartItem.id !== item.id
                        )
                );

                return;
            }


            /*
             * Otherwise update item.
             */

            setCartItems(
                previous =>
                    previous.map(
                        cartItem =>
                            cartItem.id === item.id
                                ? response.data
                                : cartItem
                    )
            );

        } catch (err) {

            console.error(
                "Decrease quantity failed:",
                err
            );


            alert(
                err.response?.data?.message ||
                "Unable to decrease quantity."
            );

        } finally {

            setItemProcessing(
                item.id,
                false
            );
        }
    };


    /*
     * =====================================================
     * REMOVE ITEM
     * =====================================================
     */

    const handleRemove = async (
        item
    ) => {

        setItemProcessing(
            item.id,
            true
        );


        try {

            await api.delete(
                item.variantId ? `/api/cart/lines/${item.id}` : `/api/cart/${item.product.id}`,
                getAuthConfig()
            );


            setCartItems(
                previous =>
                    previous.filter(
                        cartItem =>
                            cartItem.id !== item.id
                    )
            );

        } catch (err) {

            console.error(
                "Remove cart item failed:",
                err
            );


            alert(
                err.response?.data?.message ||
                "Unable to remove product from cart."
            );

        } finally {

            setItemProcessing(
                item.id,
                false
            );
        }
    };


    /*
     * =====================================================
     * CLEAR CART
     * =====================================================
     */

    const handleClearCart = async () => {

        if (cartItems.length === 0) {
            return;
        }


        const confirmed = window.confirm(
            "Are you sure you want to clear your cart?"
        );


        if (!confirmed) {
            return;
        }


        try {

            setLoading(true);


            await api.delete(
                "/api/cart",
                getAuthConfig()
            );


            setCartItems([]);

        } catch (err) {

            console.error(
                "Clear cart failed:",
                err
            );


            alert(
                err.response?.data?.message ||
                "Unable to clear cart."
            );

        } finally {

            setLoading(false);
        }
    };


    /*
     * =====================================================
     * SUBTOTAL
     * =====================================================
     */

    const subtotal =
        cartItems.reduce(
            (total, item) => {

                const price =
                    Number(
                        item.unitPrice ?? item.product?.finalSellingPrice ?? item.product?.price ?? 0
                    );


                const quantity =
                    Number(
                        item.quantity || 0
                    );


                return (
                    total +
                    price * quantity
                );

            },
            0
        );


    /*
     * =====================================================
     * DISCOUNT
     * =====================================================
     */

    const discount = 0;


    /*
     * =====================================================
     * DELIVERY
     * =====================================================
     */

    const mobileCartItems = cartItems.filter(item =>
        String(item.product?.categoryEntity?.name || item.product?.category || "").toLowerCase() === "mobiles"
    );

    const deliveryCharge = [...new Map(
        Object.values(deliveryEstimates)
            .filter(estimate => estimate?.locationAvailable && estimate.serviceAvailable !== false)
            .map(estimate => [estimate.sellerId, Number(estimate.deliveryCharge || 0)])
    ).values()].reduce((total, charge) => total + charge, 0);

    const needsDeliveryAddressCheck = mobileCartItems.length > 0 &&
        !mobileCartItems.some(item => deliveryEstimates[item.id]?.locationAvailable);


    /*
     * =====================================================
     * GRAND TOTAL
     * =====================================================
     */

    const grandTotal =
        subtotal -
        discount +
        deliveryCharge;


    /*
     * =====================================================
     * FORMAT MONEY
     * =====================================================
     */

    const formatMoney = (
        amount
    ) => {

        return Number(
            amount || 0
        ).toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );
    };


    /*
     * =====================================================
     * VIEW PRODUCT
     * =====================================================
     */

    const handleViewProduct = (
        productId
    ) => {

        navigate(
            `/product/${productId}`
        );
    };


    /*
     * =====================================================
     * CHECKOUT
     * =====================================================
     */

    const handleCheckout = () => {

        if (cartItems.length === 0) {
            return;
        }


        navigate(
            "/customer/checkout"
        );
    };


    /*
     * =====================================================
     * RETRY
     * =====================================================
     */

    const handleRetry = () => {

        fetchCart();
    };


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    if (loading) {

        return (

            <div className="cart-page">

                <CartNavigation navigate={navigate} />

                <div className="cart-loading">

                    <div className="cart-spinner" />

                    <p>
                        Loading your cart...
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

            <div className="cart-page">

                <CartNavigation navigate={navigate} />

                <div className="cart-error">

                    <div className="cart-error-icon"><AlertCircle size={42} /></div>

                    <h2>
                        Unable to Load Cart
                    </h2>

                    <p>
                        {error}
                    </p>

                    <button
                        className="cart-retry-button"
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
     * EMPTY CART
     * =====================================================
     */

    if (cartItems.length === 0) {

        return (

            <div className="cart-page">

                <CartNavigation navigate={navigate} />

                <section className="cart-header">

                    <div>

                        <span>
                            SHIVHUB
                        </span>

                        <h1>My Cart <ShoppingCart size={31} /></h1>

                        <p>
                            Review your selected products
                            before checkout.
                        </p>

                    </div>

                </section>


                <div className="cart-empty">

                    <div className="cart-empty-icon"><ShoppingCart size={55} /></div>

                    <h2>
                        Your Cart is Empty
                    </h2>

                    <p>
                        Looks like you haven't added
                        anything to your cart yet.
                    </p>

                    <button
                        className="cart-shop-button"
                        onClick={() =>
                            navigate(
                                "/customer/dashboard"
                            )
                        }
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

        <div className="cart-page">

            <CartNavigation navigate={navigate} />

            <section className="cart-header">

                <div>

                    <span>
                        SHIVHUB
                    </span>

                    <h1>My Cart <ShoppingCart size={31} /></h1>

                    <p>
                        Review your products before
                        placing the order.
                    </p>

                </div>


                <button
                    className="cart-clear-button"
                    onClick={handleClearCart}
                >
                    <Trash2 size={17} /> Clear Cart
                </button>

            </section>


            <div className="cart-layout">

                <section className="cart-items-section">

                    <div className="cart-items-heading">

                        <h2>
                            Cart Items
                        </h2>

                        <span>
                            {cartItems.length} item
                            {cartItems.length !== 1
                                ? "s"
                                : ""}
                        </span>

                    </div>


                    <div className="cart-items-list">

                        <AnimatePresence initial={false}>

                        {cartItems.map(
                            item => {

                                const product =
                                    item.product;


                                const price =
                                    Number(
                                        item.unitPrice ?? product?.finalSellingPrice ?? product?.price ?? 0
                                    );


                                const quantity =
                                    Number(
                                        item.quantity || 0
                                    );


                                const itemTotal =
                                    price *
                                    quantity;


                                const isProcessing =
                                    processing[item.id];


                                return (

                                    <motion.article
                                        className="cart-item"
                                        key={item.id}
                                        variants={reducedMotion ? noTransform : listItem}
                                        initial="hidden"
                                        animate="visible"
                                        exit="exit"
                                        layout={!reducedMotion}
                                    >

                                        <div className="cart-item-image">

                                            {product?.imageUrl ? (

                                                <img
                                                    src={
                                                        product.imageUrl
                                                    }
                                                    alt={
                                                        product.name
                                                    }
                                                />

                                            ) : (

                                                <div className="cart-no-image"><Box size={42} /></div>
                                            )}

                                        </div>


                                        <div className="cart-item-details">
                                            {item.selectedAttributes && <p className="selected-attributes">{variantSummary(item.selectedAttributes)}</p>}
                                            <MobileDeliveryEstimate
                                                product={product}
                                                onEstimate={estimate => setDeliveryEstimates(previous => {
                                                    const next = { ...previous };
                                                    if (estimate) next[item.id] = estimate;
                                                    else delete next[item.id];
                                                    return next;
                                                })}
                                            />

                                            <div className="cart-item-top">

                                                <span className="cart-item-category">
                                                    {product?.category ||
                                                        "Product"}
                                                </span>


                                                {Number(
                                                    item.availableStock ?? product?.stock ?? 0
                                                ) > 0 ? (

                                                    <span className="cart-stock">
                                                        In Stock
                                                    </span>

                                                ) : (

                                                    <span className="cart-out-stock">
                                                        Out of Stock
                                                    </span>
                                                )}

                                            </div>


                                            <h3>
                                                {product?.name ||
                                                    "Product"}
                                            </h3>


                                            {product?.description && <p className="cart-item-description">{product.description}</p>}


                                            <div className="cart-item-price">

                                                ₹
                                                {formatMoney(
                                                    price
                                                )}

                                            </div>


                                            <div className="cart-item-actions">

                                                <div className="cart-quantity">

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            handleDecrease(
                                                                item
                                                            )
                                                        }
                                                        disabled={
                                                            isProcessing
                                                        }
                                                    ><Minus size={16} /></button>


                                                    <span>
                                                        {quantity}
                                                    </span>


                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            handleIncrease(
                                                                item
                                                            )
                                                        }
                                                        disabled={
                                                            isProcessing ||
                                                            Number(
                                                                item.availableStock ?? product?.stock ?? 0
                                                            ) <=
                                                                quantity
                                                        }
                                                    ><Plus size={16} /></button>

                                                </div>


                                                <button
                                                    type="button"
                                                    className="cart-view-button"
                                                    onClick={() =>
                                                        handleViewProduct(
                                                            product?.id
                                                        )
                                                    }
                                                ><Package size={16} /> View product</button>


                                                <button
                                                    type="button"
                                                    className="cart-remove-button"
                                                    onClick={() =>
                                                        handleRemove(
                                                            item
                                                        )
                                                    }
                                                    disabled={
                                                        isProcessing
                                                    }
                                                ><Trash2 size={16} />{isProcessing ? "Removing…" : "Remove"}</button>

                                            </div>

                                        </div>


                                        <div className="cart-item-total">

                                            <span>
                                                Total
                                            </span>

                                            <strong>
                                                ₹
                                                {formatMoney(
                                                    itemTotal
                                                )}
                                            </strong>

                                        </div>

                                    </motion.article>
                                );
                            }
                        )}

                        </AnimatePresence>

                    </div>

                </section>


                <aside className="cart-summary">

                    <div className="cart-summary-card">

                        <h2><CreditCard size={21} /> Order Summary</h2>


                        <div className="cart-summary-row">

                            <span>
                                Subtotal
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    subtotal
                                )}
                            </strong>

                        </div>


                        <div className="cart-summary-row">

                            <span>
                                Discount
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    discount
                                )}
                            </strong>

                        </div>


                        <div className="cart-summary-row">

                            <span>
                                Delivery
                            </span>

                            <strong className="cart-free">
                                {needsDeliveryAddressCheck
                                    ? "Check address"
                                    : deliveryCharge === 0
                                        ? "FREE"
                                        : `₹${formatMoney(deliveryCharge)}`}
                            </strong>

                        </div>


                        <div className="cart-summary-divider" />


                        <div className="cart-summary-total">

                            <span>
                                Grand Total
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    grandTotal
                                )}
                            </strong>

                        </div>


                        <button
                            className="cart-checkout-button"
                            onClick={
                                handleCheckout
                            }
                        ><CreditCard size={19} /> Proceed to Checkout <ArrowRight size={18} /></button>


                        <button
                            className="cart-continue-button"
                            onClick={() =>
                                navigate(
                                    "/customer/dashboard"
                                )
                            }
                        ><ArrowRight className="continue-arrow" size={17} /> Continue Shopping</button>


                        <p className="cart-secure-text"><CheckCircle2 size={15} /> Secure checkout — payment details are never stored here.</p>

                    </div>

                </aside>

            </div>

        </div>
    );
};


export default Cart;
