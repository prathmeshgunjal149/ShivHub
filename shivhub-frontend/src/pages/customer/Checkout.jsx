import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { BadgeCheck, ChevronLeft, CreditCard, Home, Landmark, LockKeyhole, PackageCheck, Search, ShoppingCart, WalletCards, Zap } from "lucide-react";
import api from "../../services/api";
import { validateCoupon } from "../../services/couponService";
import { createAddress, getAddresses, updateAddress } from "../../services/addressService";
import AddressForm from "../../components/customer/AddressForm";
import { emptyAddress } from "../../components/customer/addressDefaults.js";
import { formatAddress } from "../../components/customer/addressUtils.js";
import RazorpayCheckout from "../../components/payments/RazorpayCheckout";
import { createOnlineRazorpayOrder, verifyOnlineRazorpayPayment } from "../../services/paymentService";
import { getLoyaltySummary, notifyLoyaltyChanged } from "../../services/loyaltyService";
import { showCustomerFeedback } from "../../utils/customerFeedback";

import "./Checkout.css";
import MobileDeliveryEstimate from "../../components/products/MobileDeliveryEstimate";
import { variantSummary } from "../../components/products/productCommonFields";

const PAYMENT_METHODS = [
    { value: "RAZORPAY", Icon: Zap, title: "Razorpay Online Payment", description: "Pay securely using UPI, card or net banking." },
    { value: "CASH", Icon: WalletCards, title: "Cash on Delivery", description: "Pay when the order is delivered." },
    { value: "UPI", Icon: CreditCard, title: "UPI", description: "Choose UPI as your preferred payment method." },
    { value: "CARD", Icon: CreditCard, title: "Credit / Debit Card", description: "Card details are collected only by a secure payment gateway." },
    { value: "BANK_TRANSFER", Icon: Landmark, title: "Bank Transfer", description: "Use your bank transfer preference for this order." },
    { value: "CHEQUE", Icon: BadgeCheck, title: "Cheque", description: "Choose this only when agreed with the seller." }
];

const isMobileProduct = (product) => {
    const category = product?.categoryEntity?.name
        || product?.categoryName
        || (typeof product?.category === "object" ? product.category?.name : product?.category)
        || "";
    const normalized = String(category).trim().toLowerCase();
    return normalized === "mobile" || normalized === "mobiles";
};

function DeliveryAddressEstimate({ hasMobileProducts, estimates }) {
    if (!hasMobileProducts) {
        return (
            <div className="checkout-address-estimate checkout-address-estimate--confirmation" role="status">
                <span>DELIVERY ESTIMATE</span>
                <strong>Expected delivery date and time will be confirmed by the seller.</strong>
                <p>Fast distance-based delivery is shown for eligible mobile products only.</p>
            </div>
        );
    }

    if (estimates.length === 0) {
        return (
            <div className="checkout-address-estimate" role="status">
                <span>DELIVERY ESTIMATE</span>
                <strong>Checking mobile delivery time...</strong>
                <p>We are calculating it from your selected delivery address and the seller's shop.</p>
            </div>
        );
    }

    return (
        <div className="checkout-address-estimate" aria-live="polite">
            <span>DELIVERY ESTIMATE</span>
            {estimates.map((estimate, index) => (
                <div className="checkout-address-estimate-row" key={`${estimate.sellerId || "seller"}-${estimate.ruleId || index}`}>
                    <strong className={estimate.serviceAvailable === false ? "unavailable" : ""}>
                        {estimate.estimatedDeliveryText || estimate.message || "Delivery time will be confirmed by the seller."}
                    </strong>
                    {estimate.sellerShopName && <p>Delivered from: {estimate.sellerShopName}</p>}
                    {estimate.locationAvailable && estimate.distanceKm !== null && estimate.distanceKm !== undefined && (
                        <p>Approximate distance: {estimate.distanceKm} km{estimate.estimated ? " (estimated)" : ""}</p>
                    )}
                    {estimate.serviceAvailable === false && <p className="checkout-estimate-warning">Delivery is not available for this address.</p>}
                </div>
            ))}
        </div>
    );
}

/*
 * =========================================================
 * CHECKOUT PAGE
 * =========================================================
 *
 * Handles:
 *
 * 1. Load customer cart
 * 2. Load customer profile
 * 3. Delivery address
 * 4. Order summary
 * 5. COD payment
 * 6. Place order
 *
 * IMPORTANT:
 *
 * Customer ID is NOT sent from frontend.
 *
 * Backend gets the logged-in customer from JWT.
 *
 * Request sent to backend:
 *
 * {
 *     shippingAddress: "...",
 *     items: [
 *         {
 *             productId: 1,
 *             quantity: 2
 *         }
 *     ]
 * }
 *
 * =========================================================
 */

export default function Checkout() {

    const navigate = useNavigate();


    /*
     * =========================================================
     * STATE
     * =========================================================
     */

    const [cartItems, setCartItems] = useState([]);

    const [loading, setLoading] = useState(true);

    const [placingOrder, setPlacingOrder] = useState(false);

    const [error, setError] = useState("");

    const [success, setSuccess] = useState("");

    const [couponCode, setCouponCode] = useState("");
    const [appliedCoupon, setAppliedCoupon] = useState(null);
    const [couponMessage, setCouponMessage] = useState("");
    const [validatingCoupon, setValidatingCoupon] = useState(false);

    // This is a preference. The backend keeps the payment status pending until verified.
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [pendingOnlineOrder, setPendingOnlineOrder] = useState(null);
    const [razorpayOrder, setRazorpayOrder] = useState(null);
    const [headerSearch, setHeaderSearch] = useState("");


    /*
     * =========================================================
     * CUSTOMER DETAILS
     * =========================================================
     */

    const [address, setAddress] = useState(emptyAddress);
    const [savedAddresses, setSavedAddresses] = useState([]);
    const [selectedAddress, setSelectedAddress] = useState(null);
    const [editingAddress, setEditingAddress] = useState(false);
    const [deliveryEstimates,setDeliveryEstimates]=useState({});
    const [savingAddress, setSavingAddress] = useState(false);
    const [loyaltySummary, setLoyaltySummary] = useState(null);
    const [useLoyaltyPoints, setUseLoyaltyPoints] = useState(false);
    const [loyaltyPointsInput, setLoyaltyPointsInput] = useState("");


    /*
     * =========================================================
     * LOAD CART
     * =========================================================
     */

    useEffect(() => {

        const loadCart = async () => {

            try {

                setLoading(true);

                setError("");


                /*
                 * =================================================
                 * GET CUSTOMER CART
                 * =================================================
                 */

                const response =
                    await api.get("/api/cart");


                const data =
                    Array.isArray(response.data)
                        ? response.data
                        : [];


                setCartItems(data);


                /*
                 * =================================================
                 * IF CART IS EMPTY
                 * =================================================
                 */

                if (data.length === 0) {

                    navigate(
                        "/customer/cart",
                        {
                            replace: true
                        }
                    );

                    return;
                }


                /*
                 * =================================================
                 * GET CUSTOMER PROFILE
                 * =================================================
                 */

                try {

                    const profileResponse =
                        await api.get(
                            "/api/customer/profile"
                        );


                    setAddress(previous => ({ ...previous, recipientName: profileResponse.data?.name || "", mobileNumber: profileResponse.data?.mobile || "" }));


                } catch (profileError) {

                    console.warn(
                        "Unable to load customer profile:",
                        profileError
                    );
                }

                try {
                    const addresses = await getAddresses();
                    setSavedAddresses(addresses);
                    setSelectedAddress(addresses.find(item => item.isDefault) || addresses[0] || null);
                } catch (addressError) {
                    console.warn("Unable to load saved addresses:", addressError);
                }

                try {
                    setLoyaltySummary(await getLoyaltySummary());
                } catch (loyaltyError) {
                    // Loyalty is optional at checkout. A temporary summary failure must not block an order.
                    console.warn("Unable to load loyalty balance:", loyaltyError);
                }


            } catch (requestError) {

                console.error(
                    "Failed to load checkout cart:",
                    requestError
                );


                setError(

                    requestError.response
                        ?.data
                        ?.message ||

                    "Unable to load your cart."

                );


            } finally {

                setLoading(false);

            }

        };


        loadCart();

    }, [navigate]);


    const handleRazorpaySuccess = useCallback(async payload => {
        const verified = await verifyOnlineRazorpayPayment(payload);
        if (!verified.success) throw new Error("Payment verification failed.");
        setRazorpayOrder(null);
        setPendingOnlineOrder(null);
        notifyLoyaltyChanged();
        setSuccess("Payment verified. Your order is confirmed.");
        showCustomerFeedback({ kind: "rewards", title: "Order confirmed", message: "Your ShivHub rewards balance is being refreshed." });
        window.setTimeout(() => navigate("/customer/orders"), 900);
    }, [navigate]);

    const handleRazorpayDismiss = useCallback(() => {
        setRazorpayOrder(null);
        setError("Payment is pending. You can safely retry without creating another order.");
    }, []);

    const handleRazorpayError = useCallback(paymentError => {
        setRazorpayOrder(null);
        setError(paymentError?.response?.data?.message || paymentError?.message || "Payment could not be completed. Retry when ready.");
    }, []);


    /*
     * =========================================================
     * SUBTOTAL
     * =========================================================
     */

    const subtotal = useMemo(() => {

        return cartItems.reduce(

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

    }, [cartItems]);


    /*
     * =========================================================
     * DISCOUNT
     * =========================================================
     *
     * Actual discount will eventually come from backend
     * offer calculation.
     *
     * Currently no fake discount is sent.
     */

    const discount = Number(appliedCoupon?.discountAmount || 0);


    /*
     * =========================================================
     * DELIVERY
     * =========================================================
     */

    const activeEstimates=Object.values(deliveryEstimates).filter(estimate=>estimate&&estimate.addressId===selectedAddress?.id&&!editingAddress);
    const addressEstimates = [...new Map(activeEstimates.map((estimate, index) => [estimate.sellerId || `product-${index}`, estimate])).values()];
    const hasMobileProducts = cartItems.some(item => isMobileProduct(item?.product));
    const deliveryCharge = [...new Map(activeEstimates.map(estimate=>[estimate.sellerId,Number(estimate.deliveryCharge||0)])).values()].reduce((sum,charge)=>sum+charge,0);
    const availableLoyaltyPoints = Math.max(0, Number(loyaltySummary?.availablePoints || 0));
    const amountEligibleForLoyalty = Math.max(0, subtotal - discount + deliveryCharge);
    const maximumLoyaltyPoints = Math.min(
        Math.floor(availableLoyaltyPoints / 3) * 3,
        Math.floor(amountEligibleForLoyalty * 3)
    );
    const requestedLoyaltyPoints = useLoyaltyPoints
        ? Math.min(Math.floor(Math.max(0, Number(loyaltyPointsInput || 0)) / 3) * 3, maximumLoyaltyPoints)
        : 0;
    const loyaltyDiscount = requestedLoyaltyPoints / 3;


    /*
     * =========================================================
     * GRAND TOTAL
     * =========================================================
     */

    const grandTotal =
        subtotal -
        discount +
        deliveryCharge -
        loyaltyDiscount;

    const applyCoupon = async () => {
        const code = couponCode.trim();
        if (!code) {
            setAppliedCoupon(null);
            setCouponMessage("Enter a coupon code first.");
            return;
        }
        try {
            setValidatingCoupon(true);
            const coupon = await validateCoupon(code, subtotal);
            setAppliedCoupon(coupon);
            setCouponCode(coupon.code);
            setCouponMessage(`Coupon applied — you save ₹${Number(coupon.discountAmount).toLocaleString("en-IN", { maximumFractionDigits: 2 })}.`);
        } catch (couponError) {
            setAppliedCoupon(null);
            setCouponMessage(couponError.response?.data?.message || "This coupon cannot be applied.");
        } finally {
            setValidatingCoupon(false);
        }
    };

    useEffect(() => {
        setAppliedCoupon(null);
        setCouponMessage("");
    }, [subtotal]);


    /*
     * =========================================================
     * FORMAT MONEY
     * =========================================================
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
     * =========================================================
     * VALIDATE CHECKOUT
     * =========================================================
     */

    const validateCheckout = () => {


        /*
         * FULL NAME
         */

        if (!selectedAddress || editingAddress) if (!address.recipientName.trim()) {

            return "Please enter your full name.";

        }


        /*
         * MOBILE
         */

        if (!selectedAddress || editingAddress) if (!address.mobileNumber.trim()) {

            return "Please enter your mobile number.";

        }


        if (
            (!selectedAddress || editingAddress) && !/^[6-9][0-9]{9}$/.test(
                address.mobileNumber.trim()
            )
        ) {

            return (
                "Please enter a valid 10 digit mobile number."
            );

        }


        /*
         * ADDRESS
         */

        if ((!selectedAddress || editingAddress) && (!address.addressLine1.trim() || !address.addressLine2.trim())) {

            return "Please enter your delivery address.";

        }


        /*
         * CITY
         */

        if ((!selectedAddress || editingAddress) && (!address.city.trim() || address.city === "__OTHER__" || !address.district.trim())) {

            return "Please enter your city.";

        }


        /*
         * STATE
         */

        if ((!selectedAddress || editingAddress) && !address.state.trim()) {

            return "Please enter your state.";

        }


        /*
         * PINCODE
         */

        if (
            (!selectedAddress || editingAddress) && !/^[0-9]{6}$/.test(
                address.pincode.trim()
            )
        ) {

            return "Please enter a valid 6 digit pincode.";

        }


        /*
         * CART
         */

        if (cartItems.length === 0) {

            return "Your cart is empty.";

        }


        /*
         * VALIDATE CART ITEMS
         */

        for (const item of cartItems) {

            const productId =
                item.product?.id;


            const quantity =
                Number(item.quantity);


            if (!productId) {

                return (
                    "One of the products in your cart is invalid."
                );

            }


            if (
                !Number.isInteger(quantity) ||
                quantity <= 0
            ) {

                return (
                    "One of the products has an invalid quantity."
                );

            }

        }


        return "";

    };

    const validateAddressForSave = (candidate) => {
        if (!candidate.recipientName?.trim()) return "Please enter your full name.";
        if (!/^[6-9][0-9]{9}$/.test(candidate.mobileNumber?.trim() || "")) return "Please enter a valid 10 digit mobile number.";
        if (!candidate.addressLine1?.trim() || !candidate.addressLine2?.trim()) return "Please enter your delivery address.";
        if (!candidate.city?.trim() || candidate.city === "__OTHER__" || !candidate.district?.trim()) return "Please select your city and district.";
        if (!candidate.state?.trim()) return "Please select your state.";
        if (!/^[0-9]{6}$/.test(candidate.pincode?.trim() || "")) return "Please enter a valid 6 digit pincode.";
        return "";
    };

    const saveDeliveryAddress = async () => {
        const validationError = validateAddressForSave(address);
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setSavingAddress(true);
            setError("");
            const saved = selectedAddress && editingAddress
                ? await updateAddress(selectedAddress.id, address)
                : await createAddress(address);
            setSavedAddresses(current => {
                const withoutSaved = current.filter(item => item.id !== saved.id);
                return [saved, ...withoutSaved];
            });
            setSelectedAddress(saved);
            setAddress(saved);
            setEditingAddress(false);
            setSuccess("Delivery address saved successfully.");
        } catch (saveError) {
            setError(saveError.response?.data?.message || "Unable to save this delivery address.");
        } finally {
            setSavingAddress(false);
        }
    };

    const startNewAddress = () => {
        const source = selectedAddress || address;
        setAddress({
            ...emptyAddress,
            recipientName: source?.recipientName || "",
            mobileNumber: source?.mobileNumber || ""
        });
        setSelectedAddress(null);
        setEditingAddress(false);
        setError("");
    };


    /*
     * =========================================================
     * PLACE ORDER
     * =========================================================
     */

    const handlePlaceOrder = async (
        event
    ) => {

        event.preventDefault();
        if(activeEstimates.some(estimate=>estimate.serviceAvailable===false)){setError("Mobile delivery is unavailable for the selected address. Please select another address.");return;}


        setError("");

        setSuccess("");


        /*
         * =====================================================
         * VALIDATE CHECKOUT
         * =====================================================
         */

        const validationError =
            validateCheckout();


        if (validationError) {

            setError(
                validationError
            );

            return;

        }

        if (paymentMethod !== "CASH" && pendingOnlineOrder) {
            try {
                setPlacingOrder(true);
                setRazorpayOrder(await createOnlineRazorpayOrder(pendingOnlineOrder.orderId));
            } catch (paymentError) {
                setError(paymentError.response?.data?.message || "Unable to restart the secure payment.");
            } finally {
                setPlacingOrder(false);
            }
            return;
        }


        try {

            setPlacingOrder(true);


            /*
             * =================================================
             * BUILD SHIPPING ADDRESS
             * =================================================
             *
             * Backend CreateOrderRequest expects:
             *
             * shippingAddress
             *
             * =================================================
             */

            let addressForOrder = selectedAddress;
            if (selectedAddress && editingAddress) {
                addressForOrder = await updateAddress(selectedAddress.id, address);
                setSelectedAddress(addressForOrder);
                setSavedAddresses(current => current.map(item => item.id === addressForOrder.id ? addressForOrder : item));
            }
            const snapshot = addressForOrder || address;
            const shippingAddress = `${snapshot.recipientName}, ${snapshot.mobileNumber}, ${formatAddress(snapshot)}`;


            /*
             * =================================================
             * BUILD ORDER ITEMS
             * =================================================
             *
             * IMPORTANT:
             *
             * We send only:
             *
             * productId
             * quantity
             *
             * We DO NOT send:
             *
             * price
             * subtotal
             * customerId
             *
             * Backend calculates these.
             *
             * =================================================
             */

            const items =
                cartItems.map(
                    (item) => ({

                        productId:
                            item.product?.id,
                        variantId: item.variantId,

                        quantity:
                            Number(
                                item.quantity
                            )

                    })
                );


            /*
             * =================================================
             * FINAL ORDER REQUEST
             * =================================================
             *
             * Matches CreateOrderRequest.java
             *
             * {
             *
             *     shippingAddress: "...",
             *
             *     items: [
             *
             *         {
             *             productId: 1,
             *             quantity: 2
             *         }
             *
             *     ]
             *
             * }
             *
             * =================================================
             */

            const requestData = {

                shippingAddress:

                    shippingAddress,

                deliveryAddressId: addressForOrder?.id,
                deliveryAddress: addressForOrder ? undefined : snapshot,

                items:

                    items,

                couponCode: appliedCoupon?.code || undefined,

                loyaltyPointsToRedeem: requestedLoyaltyPoints || undefined,

                paymentMethod: paymentMethod === "CASH" ? "CASH" : "RAZORPAY"

            };


            /*
             * =================================================
             * DEBUG
             * =================================================
             *
             * Check browser console during development.
             *
             * =================================================
             */

            console.log(
                "PLACE ORDER REQUEST:",
                requestData
            );


            /*
             * =================================================
             * PLACE ORDER API
             * =================================================
             */

            const response =
                await api.post(

                    "/api/orders",

                    requestData

                );


            /*
             * =================================================
             * ORDER SUCCESS
             * =================================================
             */

            console.log(
                "Order created:",
                response.data
            );

            if (requestedLoyaltyPoints > 0) notifyLoyaltyChanged();

            if (paymentMethod !== "CASH") {
                setPendingOnlineOrder(response.data);
                setRazorpayOrder(await createOnlineRazorpayOrder(response.data.orderId));
                setSuccess("Order reserved. Complete the secure Razorpay payment to confirm it.");
                return;
            }


            setSuccess(
                "Your order has been placed successfully."
            );
            showCustomerFeedback({ kind: "order", title: "Order placed successfully", message: "You can track its status from My Orders." });


            /*
             * =================================================
             * REDIRECT TO MY ORDERS
             * =================================================
             */

            setTimeout(() => {

                navigate(
                    "/customer/orders"
                );

            }, 900);


        } catch (requestError) {

            console.error(
                "Failed to place order:",
                requestError
            );


            /*
             * =================================================
             * AUTHENTICATION ERROR
             * =================================================
             */

            if (
                requestError.response?.status === 401 ||
                requestError.response?.status === 403
            ) {

                setError(
                    "Your login session has expired. Please login again."
                );


            } else {

                /*
                 * =================================================
                 * BACKEND ERROR
                 * =================================================
                 */

                const backendMessage =

                    requestError.response
                        ?.data
                        ?.message ||


                    requestError.response
                        ?.data
                        ?.error ||


                    (
                        typeof requestError.response
                            ?.data === "string"

                            ? requestError.response.data

                            : null
                    ) ||


                    "Unable to place your order.";


                setError(
                    backendMessage
                );

            }


        } finally {

            setPlacingOrder(false);

        }

    };


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (loading) {

        return (

            <main className="checkout-page">

                <div className="checkout-loading">

                    <div className="checkout-spinner" />

                    <p>
                        Preparing your checkout...
                    </p>

                </div>

            </main>

        );

    }


    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    if (
        error &&
        cartItems.length === 0
    ) {

        return (

            <main className="checkout-page">

                <div className="checkout-error">

                    <div>
                        ⚠️
                    </div>


                    <h2>
                        Unable to Load Checkout
                    </h2>


                    <p>
                        {error}
                    </p>


                    <button
                        type="button"
                        onClick={() =>
                            navigate(
                                "/customer/cart"
                            )
                        }
                    >

                        Back to Cart

                    </button>

                </div>

            </main>

        );

    }


    /*
     * =========================================================
     * MAIN CHECKOUT
     * =========================================================
     */

    return (

        <main className="checkout-page">

            {razorpayOrder && <RazorpayCheckout payment={razorpayOrder} internalId={pendingOnlineOrder?.orderId} onSuccess={handleRazorpaySuccess} onDismiss={handleRazorpayDismiss} onError={handleRazorpayError} />}


            {/* =================================================
                HEADER
            ================================================= */}

            <header className="checkout-header">
                <div className="checkout-header-main">
                    <Link to="/customer/dashboard" className="checkout-brand" aria-label="ShivHub home">
                        <span className="checkout-brand-mark"><ShoppingCart size={21} aria-hidden="true" /></span>
                        <span><strong>Shiv<span>Hub</span></strong><small>SMART SHOPPING</small></span>
                    </Link>
                    <form className="checkout-header-search" onSubmit={event => { event.preventDefault(); const value = headerSearch.trim(); navigate(`/customer/products${value ? `?search=${encodeURIComponent(value)}` : ""}`); }}>
                        <Search size={18} aria-hidden="true" />
                        <input value={headerSearch} onChange={event => setHeaderSearch(event.target.value)} placeholder="Search mobiles, laptops, accessories and more…" aria-label="Search products" />
                        <button type="submit" aria-label="Search products"><Search size={18} aria-hidden="true" /></button>
                    </form>
                    <nav className="checkout-header-actions" aria-label="Checkout navigation">
                        <Link to="/customer/dashboard"><Home size={18} aria-hidden="true" /><span>Home</span></Link>
                        <Link to="/customer/orders"><PackageCheck size={18} aria-hidden="true" /><span>Orders</span></Link>
                        <Link to="/customer/cart" className="active"><ShoppingCart size={18} aria-hidden="true" /><span>Cart</span><b>{cartItems.length}</b></Link>
                    </nav>
                </div>
                <div className="checkout-progress" aria-label="Checkout progress">
                    <button type="button" className="checkout-back-button" onClick={() => navigate("/customer/cart")}><ChevronLeft size={17} aria-hidden="true" />Back to Cart</button>
                    <div className="checkout-progress-steps"><span className="complete"><BadgeCheck size={17} /> Address</span><i /><span className="active"><CreditCard size={17} /> Payment</span><i /><span><PackageCheck size={17} /> Review</span></div>
                    <span className="checkout-secure"><LockKeyhole size={16} aria-hidden="true" /> Secure checkout</span>
                </div>
            </header>



            {/* =================================================
                CONTENT
            ================================================= */}

            <form
                className="checkout-layout"
                onSubmit={
                    handlePlaceOrder
                }
            >


                {/* =================================================
                    LEFT SIDE
                ================================================= */}

                <section className="checkout-main">


                    {/* =================================================
                        DELIVERY ADDRESS
                    ================================================= */}

                    <div className="checkout-card">

                        <div className="checkout-card-heading">

                            <div>

                                <span>
                                    DELIVERY
                                </span>

                                <h2>
                                    Delivery Address
                                </h2>

                            </div>

                        </div>


                        {selectedAddress && !editingAddress ? <><div className="checkout-saved-address">
                            <div><span>DELIVERING TO</span><strong>{selectedAddress.recipientName} · {selectedAddress.mobileNumber}</strong><p>{formatAddress(selectedAddress)}</p></div>
                            <div><button type="button" onClick={() => { setAddress(selectedAddress); setEditingAddress(true); }}>Edit Address</button><button type="button" onClick={startNewAddress}>Add New Address</button>{savedAddresses.length > 1 && <button type="button" onClick={() => { const next = savedAddresses.find(item => item.id !== selectedAddress.id); setSelectedAddress(next || null); }}>Change Address</button>}</div>
                        </div><DeliveryAddressEstimate hasMobileProducts={hasMobileProducts} estimates={addressEstimates} /></> : <div className="checkout-address-editor"><AddressForm value={address} onChange={setAddress} disabled={placingOrder || savingAddress}/><div className="checkout-address-editor-actions"><button type="button" className="checkout-save-address" onClick={saveDeliveryAddress} disabled={placingOrder || savingAddress}>{savingAddress ? "Saving address..." : selectedAddress && editingAddress ? "Save address changes" : "Save new address"}</button>{savedAddresses.length > 0 && <button type="button" className="checkout-cancel-address" onClick={() => { setSelectedAddress(savedAddresses.find(item => item.isDefault) || savedAddresses[0]); setEditingAddress(false); }}>Use saved address</button>}</div></div>}

                    </div>



                    {/* =================================================
                        PAYMENT
                    ================================================= */}

                    <div className="checkout-card">

                        <div className="checkout-card-heading">

                            <div>

                                <span>
                                    PAYMENT
                                </span>

                                <h2>
                                    Payment Method
                                </h2>

                            </div>

                        </div>


                        <div className="checkout-payment-options" role="radiogroup" aria-label="Payment method">
                            {PAYMENT_METHODS.map(method => (
                                <label
                                    className={`checkout-payment-option ${paymentMethod === method.value ? "selected" : ""}`}
                                    key={method.value}
                                >
                                    <span className="checkout-payment-icon" aria-hidden="true"><method.Icon size={22} /></span>
                                    <span>
                                        <strong>{method.title}</strong>
                                        <p>{method.description}</p>
                                    </span>
                                    <input
                                        type="radio"
                                        name="paymentMethod"
                                        value={method.value}
                                        checked={paymentMethod === method.value}
                                        onChange={() => setPaymentMethod(method.value)}
                                    />
                                </label>
                            ))}
                        </div>
                        <p className="checkout-payment-disclosure">Payment preference is saved with your order. No card number, CVV, or UPI PIN is requested here, and payment stays pending until it is verified.</p>

                    </div>



                    {/* =================================================
                        ERROR / SUCCESS
                    ================================================= */}

                    {
                        error && (

                            <div className="checkout-alert error">

                                ⚠ {error}

                            </div>

                        )
                    }


                    {
                        success && (

                            <div className="checkout-alert success">

                                ✓ {success}

                            </div>

                        )
                    }


                </section>



                {/* =================================================
                    RIGHT SIDE
                ================================================= */}

                <aside className="checkout-sidebar">


                    {/* =================================================
                        ORDER SUMMARY
                    ================================================= */}

                    <div className="checkout-summary-card">

                        <div className="checkout-summary-heading">

                            <h2>
                                Order Summary
                            </h2>


                            <span>

                                {cartItems.length}

                                {" "}

                                item

                                {cartItems.length !== 1
                                    ? "s"
                                    : ""}

                            </span>

                        </div>



                        {/* =================================================
                            ORDER ITEMS
                        ================================================= */}

                        <div className="checkout-items">

                            {
                                cartItems.map(
                                    (item) => {

                                        const product =
                                            item.product;


                                        const price =
                                            Number(
                                                item.unitPrice ?? product?.finalSellingPrice ??
                                                product?.price ??
                                                0
                                            );


                                        const quantity =
                                            Number(
                                                item.quantity ||
                                                0
                                            );


                                        const total =
                                            price *
                                            quantity;


                                        return (

                                            <div
                                                className="checkout-item"
                                                key={
                                                    item.id
                                                }
                                            >


                                                {/* IMAGE */}

                                                <div className="checkout-item-image">

                                                    {
                                                        product?.imageUrl

                                                            ? (

                                                                <img
                                                                    src={
                                                                        product.imageUrl
                                                                    }
                                                                    alt={
                                                                        product.name
                                                                    }
                                                                />

                                                            )

                                                            : (

                                                                <span>
                                                                    📦
                                                                </span>

                                                            )
                                                    }

                                                </div>



                                                {/* INFO */}

                                                <div className="checkout-item-info">
                                                    {item.selectedAttributes&&<p className="selected-attributes">{variantSummary(item.selectedAttributes)}</p>}
                                                    <MobileDeliveryEstimate product={product} addressId={editingAddress?null:selectedAddress?.id||null} display={false} onEstimate={estimate=>setDeliveryEstimates(previous=>({...previous,[product.id]:estimate?{...estimate,addressId:selectedAddress?.id}:null}))} />

                                                    <strong>

                                                        {
                                                            product?.name ||
                                                            "Product"
                                                        }

                                                    </strong>


                                                    <span>

                                                        Qty:

                                                        {" "}

                                                        {quantity}

                                                    </span>

                                                </div>



                                                {/* TOTAL */}

                                                <strong>

                                                    ₹

                                                    {
                                                        formatMoney(
                                                            total
                                                        )
                                                    }

                                                </strong>

                                            </div>

                                        );

                                    }
                                )
                            }

                        </div>



                        {/* =================================================
                            TOTALS
                        ================================================= */}

                        <div className="checkout-coupon-field">
                            <label htmlFor="coupon-code">Coupon code (optional)</label>
                            <div className="checkout-coupon-input-row">
                                <input
                                    id="coupon-code"
                                    value={couponCode}
                                    onChange={(event) => {
                                        setCouponCode(event.target.value.toUpperCase());
                                        setAppliedCoupon(null);
                                        setCouponMessage("");
                                    }}
                                    placeholder="Enter coupon code"
                                    disabled={placingOrder || validatingCoupon}
                                />
                                <button type="button" onClick={applyCoupon} disabled={placingOrder || validatingCoupon || subtotal <= 0}>
                                    {validatingCoupon ? "Checking..." : "Apply"}
                                </button>
                            </div>
                            {couponMessage && <small className={appliedCoupon ? "coupon-success" : "coupon-error"}>{couponMessage}</small>}
                            <small>Coupon eligibility is checked again before your order is placed.</small>
                        </div>

                        <div className="checkout-loyalty-redemption">
                            <div className="checkout-loyalty-heading">
                                <div><span>SHIVHUB REWARDS</span><strong>{availableLoyaltyPoints.toLocaleString("en-IN")} points available</strong></div>
                                <label className="checkout-loyalty-toggle"><input type="checkbox" checked={useLoyaltyPoints} disabled={maximumLoyaltyPoints < 3 || placingOrder} onChange={event => { setUseLoyaltyPoints(event.target.checked); setLoyaltyPointsInput(event.target.checked ? String(maximumLoyaltyPoints) : ""); }} />Use points</label>
                            </div>
                            <p>3 points = â‚¹1. Points are applied as a discount and verified again when the order is placed.</p>
                            {useLoyaltyPoints && <div className="checkout-loyalty-input-row"><label htmlFor="loyalty-points">Points to redeem<input id="loyalty-points" type="number" inputMode="numeric" min="0" step="3" max={maximumLoyaltyPoints} value={loyaltyPointsInput} onChange={event => setLoyaltyPointsInput(event.target.value.replace(/[^0-9]/g, ""))} disabled={placingOrder} /></label><strong>{requestedLoyaltyPoints.toLocaleString("en-IN")} pts = â‚¹{formatMoney(loyaltyDiscount)}</strong></div>}
                            {useLoyaltyPoints && Number(loyaltyPointsInput || 0) % 3 !== 0 && <small>Only complete groups of 3 points can be redeemed; the value is rounded down safely.</small>}
                            {maximumLoyaltyPoints < 3 && <small>You need at least 3 available points and a payable amount of â‚¹1 to redeem rewards.</small>}
                        </div>

                        <div className="checkout-total-lines">


                            {/* SUBTOTAL */}

                            <div>

                                <span>
                                    Subtotal
                                </span>


                                <strong>

                                    ₹

                                    {
                                        formatMoney(
                                            subtotal
                                        )
                                    }

                                </strong>

                            </div>

                            {loyaltyDiscount > 0 && <div className="checkout-loyalty-discount-line"><span>ShivHub points discount</span><strong>- â‚¹{formatMoney(loyaltyDiscount)}</strong></div>}



                            {/* DISCOUNT */}

                            <div>

                                <span>
                                    Discount
                                </span>


                                <strong>

                                    ₹

                                    {
                                        formatMoney(
                                            discount
                                        )
                                    }

                                </strong>

                            </div>



                            {/* DELIVERY */}

                            <div>

                                <span>
                                    Delivery
                                </span>


                                <strong className={deliveryCharge > 0 ? "" : "checkout-free"}>
                                    {deliveryCharge > 0 ? `â‚¹${formatMoney(deliveryCharge)}` : "FREE"}
                                </strong>

                            </div>

                        </div>



                        {/* GRAND TOTAL */}

                        <div className="checkout-grand-total">

                            <span>
                                Grand Total
                            </span>


                            <strong>

                                ₹

                                {
                                    formatMoney(
                                        grandTotal
                                    )
                                }

                            </strong>

                        </div>



                        {/* PLACE ORDER */}

                        <button
                            type="submit"
                            className="checkout-place-order"
                            disabled={
                                placingOrder
                            }
                        >

                            {
                                placingOrder

                                    ? "Placing Order..."

                                    : "Place Order"
                            }

                        </button>


                        <p className="checkout-note">

                            🔒 Your order information is
                            securely processed. Please review our{" "}
                            <Link to="/terms">Terms</Link>,{" "}
                            <Link to="/cancellation-refund">Refund Policy</Link> and{" "}
                            <Link to="/shipping-policy">Shipping Policy</Link>.

                        </p>

                    </div>

                </aside>

            </form>

        </main>

    );
}
