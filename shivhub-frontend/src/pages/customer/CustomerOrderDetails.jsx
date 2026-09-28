import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { AlertCircle, Box, Home, Package, ShoppingBag, ShoppingCart, Trash2, X } from "lucide-react";

import api, { API_BASE_URL } from "../../services/api";
import RazorpayCheckout from "../../components/payments/RazorpayCheckout";
import PaymentRetryButton from "../../components/payments/PaymentRetryButton";
import { createOnlineRazorpayOrder, verifyOnlineRazorpayPayment } from "../../services/paymentService";

import "./CustomerOrderDetails.css";
import "./CustomerOrderDetailsTheme.css";


/*
 * =========================================================
 * CUSTOMER ORDER DETAILS
 * =========================================================
 *
 * Displays:
 *
 * 1. Order number
 * 2. Order date
 * 3. Order status
 * 4. Status timeline
 * 5. Products
 * 6. Quantity
 * 7. Price
 * 8. Shipping address
 * 9. Payment status
 * 10. Price summary
 *
 * =========================================================
 */


const STATUS_STEPS = [
    {
        key: "PENDING",
        title: "Order Placed",
        description: "Your order has been received."
    },
    {
        key: "CONFIRMED",
        title: "Confirmed",
        description: "Your order has been confirmed."
    },
    {
        key: "PROCESSING",
        title: "Processing",
        description: "Your order is being prepared."
    },
    {
        key: "PACKED",
        title: "Packed",
        description: "Your order has been packed and is ready for dispatch."
    },
    {
        key: "SHIPPED",
        title: "Shipped",
        description: "Your order is on the way."
    },
    {
        key: "OUT_FOR_DELIVERY",
        title: "Out for Delivery",
        description: "Your order is out for delivery."
    },
    {
        key: "DELIVERED",
        title: "Delivered",
        description: "Your order has been delivered."
    }
];
const CANCELLATION_REASONS = [
    ["ORDERED_BY_MISTAKE", "Ordered by mistake"], ["CHANGE_OF_MIND", "Changed my mind"],
    ["FOUND_BETTER_PRICE", "Found a better price"], ["DELIVERY_TIME_TOO_LONG", "Delivery time is too long"],
    ["WANT_TO_CHANGE_ADDRESS", "Want to change the delivery address"], ["WANT_TO_CHANGE_PRODUCT", "Want to change the product"], ["OTHER", "Other"]
];
const orderAssetUrl = value => !value || /^https?:\/\//i.test(value) ? value : `${API_BASE_URL}/${String(value).replace(/^\/+/, "")}`;


const CustomerOrderDetails = () => {


    /*
     * =========================================================
     * ROUTE PARAMETER
     * =========================================================
     */

    const { id } = useParams();


    /*
     * =========================================================
     * NAVIGATION
     * =========================================================
     */

    const navigate = useNavigate();


    /*
     * =========================================================
     * STATE
     * =========================================================
     */

    const [order, setOrder] =
        useState(null);


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");

    const [downloadingInvoice, setDownloadingInvoice] =
        useState(false);

    const [razorpayOrder, setRazorpayOrder] = useState(null);

    const [retryingPayment, setRetryingPayment] = useState(false);
    const [cancelOpen, setCancelOpen] = useState(false);
    const [cancelReason, setCancelReason] = useState("");
    const [cancelComment, setCancelComment] = useState("");
    const [cancelling, setCancelling] = useState(false);

    // Relative delivery text is driven by state so render stays pure.
    const [currentTimeMs, setCurrentTimeMs] = useState(null);

    useEffect(() => {
        const refreshCurrentTime = () => setCurrentTimeMs(new Date().getTime());
        refreshCurrentTime();
        const intervalId = window.setInterval(refreshCurrentTime, 60 * 1000);
        return () => window.clearInterval(intervalId);
    }, []);


    /*
     * =========================================================
     * LOAD ORDER
     * =========================================================
     */

    const loadOrder = useCallback(async () => {

        try {

            setLoading(true);

            setError("");


            /*
             * Axios interceptor automatically sends:
             *
             * Authorization: Bearer <JWT>
             */

            const response =
                await api.get(
                    `/api/orders/${id}`
                );


            setOrder(
                response.data
            );


        } catch (requestError) {

            console.error(
                "Failed to load order:",
                requestError
            );


            /*
             * =================================================
             * UNAUTHORIZED
             * =================================================
             */

            if (
                requestError.response?.status === 401 ||
                requestError.response?.status === 403
            ) {

                setError(
                    "Your login session has expired. Please login again."
                );

                return;
            }


            /*
             * =================================================
             * ORDER NOT FOUND
             * =================================================
             */

            if (
                requestError.response?.status === 404
            ) {

                setError(
                    "Order not found."
                );

                return;
            }


            /*
             * =================================================
             * OTHER ERROR
             * =================================================
             */

            setError(
                requestError.response?.data?.message ||
                "Unable to load order details."
            );


        } finally {

            setLoading(false);
        }
    }, [id]);


    /*
     * =========================================================
     * LOAD WHEN PAGE OPENS
     * =========================================================
     */

    useEffect(() => {

        if (id) {

            loadOrder();
        }

    }, [id, loadOrder]);


    /*
     * =========================================================
     * FORMAT DATE
     * =========================================================
     */

    const formatDate = (dateValue) => {

        if (!dateValue) {
            return "-";
        }


        const date =
            new Date(dateValue);


        if (Number.isNaN(
            date.getTime()
        )) {

            return "-";
        }


        return date.toLocaleDateString(
            "en-IN",
            {
                day: "2-digit",
                month: "short",
                year: "numeric"
            }
        );
    };


    /*
     * =========================================================
     * FORMAT MONEY
     * =========================================================
     */

    const formatMoney = (value) => {

        const amount =
            Number(value || 0);


        return amount.toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );
    };


    /*
     * =========================================================
     * STATUS LABEL
     * =========================================================
     */

    const formatStatus = (status) => {

        if (!status) {
            return "UNKNOWN";
        }


        return status
            .replaceAll("_", " ");
    };

    const downloadInvoice = async () => {
        if (!order?.orderId) return;

        try {
            setDownloadingInvoice(true);

            const response = await api.get(
                `/api/orders/${order.orderId}/invoice`,
                { responseType: "blob" }
            );

            const url = window.URL.createObjectURL(
                new Blob([response.data], { type: "application/pdf" })
            );

            const link = document.createElement("a");
            link.href = url;
            link.download = `ShivHub-Invoice-${order.orderNumber || order.orderId}.pdf`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (requestError) {
            alert(
                requestError.response?.data?.message ||
                "Invoice download failed. Please try again."
            );
        } finally {
            setDownloadingInvoice(false);
        }
    };

    const canCancel = order && !["PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"].includes(order.orderStatus);
    const submitCancellation = async event => {
        event.preventDefault();
        if (!cancelReason || (cancelReason === "OTHER" && !cancelComment.trim())) return;
        try {
            setCancelling(true); setError("");
            const response = await api.post(`/api/orders/${order.orderId}/cancel`, { reason: cancelReason, comment: cancelComment.trim() || null });
            setOrder(response.data); setCancelOpen(false); setCancelReason(""); setCancelComment("");
        } catch (requestError) { setError(requestError.response?.data?.message || "This order could not be cancelled."); }
        finally { setCancelling(false); }
    };

    const retryRazorpayPayment = async () => {
        if (!order?.orderId) return;
        try {
            setRetryingPayment(true);
            setError("");
            setRazorpayOrder(await createOnlineRazorpayOrder(order.orderId));
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to restart secure payment.");
        } finally {
            setRetryingPayment(false);
        }
    };

    const handleRazorpaySuccess = async payload => {
        try {
            const verified = await verifyOnlineRazorpayPayment(payload);
            if (!verified.success) throw new Error("Payment verification failed.");
            setRazorpayOrder(null);
            await loadOrder();
        } catch (requestError) {
            setRazorpayOrder(null);
            setError(requestError.response?.data?.message || requestError.message || "Payment was received but could not be verified. Please contact support with the payment ID.");
        }
    };


    /*
     * =========================================================
     * GET STATUS INDEX
     * =========================================================
     */

    const getStatusIndex = () => {

        if (!order?.orderStatus) {
            return -1;
        }


        return STATUS_STEPS.findIndex(
            step =>
                step.key ===
                order.orderStatus
        );
    };


    /*
     * =========================================================
     * CURRENT STATUS INDEX
     * =========================================================
     */

    const currentStatusIndex =
        getStatusIndex();


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (loading) {

        return (

            <main className="customer-order-details-page">

                <div className="order-details-loading">

                    <div className="order-details-spinner" />

                    <p>
                        Loading order details...
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

    if (error || !order) {

        return (

            <main className="customer-order-details-page">

                <div className="order-details-error">

                    <div className="order-error-icon">
                        ⚠
                    </div>

                    <h2>
                        Unable to Load Order
                    </h2>

                    <p>
                        {error ||
                            "Order details are unavailable."
                        }
                    </p>


                    <div className="order-error-actions">

                        <button
                            type="button"
                            onClick={() =>
                                navigate(
                                    "/customer/orders"
                                )
                            }
                        >
                            ← Back to My Orders
                        </button>


                        <button
                            type="button"
                            onClick={loadOrder}
                        >
                            Try Again
                        </button>

                    </div>

                </div>

            </main>
        );
    }


    /*
     * =========================================================
     * CANCELLED ORDER
     * =========================================================
     */

    const isCancelled =
        order.orderStatus === "CANCELLED";

    const deliveryExpectations = Array.isArray(order.deliveryExpectations)
        ? order.deliveryExpectations
        : [];

    const formatExpectedDelivery = (value) => {
        if (!value) return "Delivery time will be confirmed by the seller.";
        const target = new Date(value);
        if (Number.isNaN(target.getTime())) return "Delivery time will be confirmed by the seller.";
        const remainingHours = currentTimeMs == null
            ? null
            : Math.ceil((target.getTime() - currentTimeMs) / 3600000);
        const date = target.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
        const time = target.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
        if (remainingHours != null && remainingHours > 0 && remainingHours <= 24) {
            return `Expected within ${remainingHours} hour${remainingHours === 1 ? "" : "s"} · by ${time}`;
        }
        return `Expected by ${date}, ${time}`;
    };


    /*
     * =========================================================
     * MAIN UI
     * =========================================================
     */

    return (

        <main className="customer-order-details-page">

            <nav className="customer-order-nav" aria-label="Customer navigation"><button type="button" className="customer-order-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingBag size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/orders")}><Package size={18} /> Orders</button><button type="button" onClick={() => navigate("/customer/cart")}><ShoppingCart size={18} /> Cart</button></div></nav>

            {razorpayOrder && <RazorpayCheckout payment={razorpayOrder} internalId={order?.orderId} onSuccess={handleRazorpaySuccess} onDismiss={() => setRazorpayOrder(null)} onError={requestError => { setRazorpayOrder(null); setError(requestError?.message || "Payment could not be completed."); }} />}


            {/* =================================================
                HEADER
            ================================================= */}

            <header className="order-details-header">

                <button
                    type="button"
                    className="order-back-button"
                    onClick={() =>
                        navigate(
                            "/customer/orders"
                        )
                    }
                >
                    ← Back to My Orders
                </button>


                <div className="order-header-center">

                    <span>
                        SHIVHUB
                    </span>

                    <h1>
                        Order Details
                    </h1>

                </div>

                <button
                    type="button"
                    className="order-shopping-button"
                    onClick={() =>
                        navigate(
                            "/customer/products"
                        )
                    }
                >
                    Continue Shopping
                </button>

            </header>


            {/* =================================================
                CONTENT
            ================================================= */}

            <section className="order-details-container">


                {/* =================================================
                    ORDER HERO
                ================================================= */}

                <div className="order-details-hero">

                    <div>

                        <span className="order-eyebrow">
                            ORDER
                        </span>


                        <h2>
                            {order.orderNumber}
                        </h2>


                        <p>
                            Placed on{" "}
                            {formatDate(
                                order.createdAt
                            )}
                        </p>

                    </div>


                    <div className="order-hero-status">

                        <span
                            className={`order-status-badge ${String(
                                order.orderStatus || ""
                            ).toLowerCase()}`}
                        >
                            {formatStatus(
                                order.orderStatus
                            )}
                        </span>


                        <span className="order-payment-status">

                            Method:{" "}

                            <strong>
                                {formatStatus(
                                    order.paymentMethod || "CASH"
                                )}
                            </strong>

                        </span>

                        {order.orderStatus === "DELIVERED" && (
                            <button
                                type="button"
                                className="download-invoice-button"
                                onClick={downloadInvoice}
                                disabled={downloadingInvoice}
                            >
                                {downloadingInvoice
                                    ? "Preparing invoice..."
                                    : "Download Invoice"}
                            </button>
                        )}

                        {canCancel && <button type="button" className="order-cancel-trigger" onClick={() => setCancelOpen(true)}><Trash2 size={17} /> Cancel order</button>}

                    </div>

                </div>


                {!isCancelled && deliveryExpectations.length > 0 && (
                    <section className="order-delivery-expectations" aria-label="Expected delivery time">
                        <div className="order-delivery-expectations-heading">
                            <span>DELIVERY PROMISE</span>
                            <h2>When your order should arrive</h2>
                            <p>Times are updated by the assigned seller and ShivHub team.</p>
                        </div>
                        <div className="order-delivery-expectations-list">
                            {deliveryExpectations.map(expectation => (
                                <article key={expectation.sellerId} className="order-delivery-expectation-card">
                                    <div className="delivery-expectation-icon" aria-hidden="true">◷</div>
                                    <div>
                                        <small>FROM {expectation.sellerName || "SHIVHUB SELLER"}</small>
                                        <strong>{formatExpectedDelivery(expectation.expectedDeliveryAt)}</strong>
                                        {expectation.customerMessage && <p>{expectation.customerMessage}</p>}
                                    </div>
                                </article>
                            ))}
                        </div>
                    </section>
                )}


                {/* =================================================
                    CANCELLED MESSAGE
                ================================================= */}

                {isCancelled && (

                    <div className="cancelled-order-banner">

                        <div className="cancelled-icon">
                            ×
                        </div>

                        <div>

                            <strong>
                                Order Cancelled
                            </strong>

                            <p>
                                This order has been cancelled.
                            </p>

                        </div>

                    </div>

                )}


                {/* =================================================
                    STATUS TIMELINE
                ================================================= */}

                {!isCancelled && (

                    <section className="order-section">

                        <div className="section-heading">

                            <span>
                                TRACKING
                            </span>

                            <h2>
                                Order Status
                            </h2>

                        </div>


                        <div className="order-timeline">

                            {STATUS_STEPS.map(
                                (step, index) => {

                                    const completed =
                                        currentStatusIndex >=
                                        index;


                                    const current =
                                        currentStatusIndex ===
                                        index;


                                    return (

                                        <div
                                            key={step.key}
                                            className={`timeline-item ${
                                                completed
                                                    ? "completed"
                                                    : ""
                                            } ${
                                                current
                                                    ? "current"
                                                    : ""
                                            }`}
                                        >

                                            <div className="timeline-marker">

                                                {completed
                                                    ? "✓"
                                                    : index + 1
                                                }

                                            </div>


                                            <div className="timeline-content">

                                                <h3>
                                                    {step.title}
                                                </h3>

                                                <p>
                                                    {current
                                                        ? "Current status"
                                                        : step.description
                                                    }
                                                </p>

                                            </div>

                                        </div>

                                    );
                                }
                            )}

                        </div>

                    </section>
                )}


                {/* =================================================
                    PRODUCTS
                ================================================= */}

                <section className="order-section">

                    <div className="section-heading">

                        <span>
                            ITEMS
                        </span>

                        <h2>
                            Ordered Products
                        </h2>

                    </div>


                    <div className="order-products-list">

                        {order.items?.map(
                            (item) => (

                                <div
                                    className="order-product-row"
                                    key={item.id}
                                >

                                    <div className="order-product-icon">{item.imageUrl ? <img src={orderAssetUrl(item.imageUrl)} alt={item.productName || "Product"} onError={event => { event.currentTarget.style.display = "none"; }} /> : <Box size={28} />}</div>


                                    <div className="order-product-info">

                                        <h3>
                                            {item.productName}
                                        </h3>

                                        <p>
                                            Quantity:{" "}
                                            {item.quantity}
                                        </p>

                                    </div>


                                    <div className="order-product-price">

                                        <span>
                                            ₹
                                            {formatMoney(
                                                item.unitPrice
                                            )}
                                        </span>

                                        <small>
                                            ₹
                                            {formatMoney(
                                                item.totalPrice
                                            )}{" "}
                                            total
                                        </small>

                                    </div>

                                </div>

                            )
                        )}

                    </div>

                </section>


                {/* =================================================
                    TWO COLUMN AREA
                ================================================= */}

                <div className="order-details-grid">


                    {/* =================================================
                        SHIPPING
                    ================================================= */}

                    <section className="order-section">

                        <div className="section-heading">

                            <span>
                                DELIVERY
                            </span>

                            <h2>
                                Shipping Address
                            </h2>

                        </div>


                        <div className="shipping-address-card">

                            <div className="address-icon">
                                📍
                            </div>

                            <p>
                                {order.shippingAddress}
                            </p>

                        </div>

                    </section>


                    {/* =================================================
                        PAYMENT
                    ================================================= */}

                    <section className="order-section">

                        <div className="section-heading">

                            <span>
                                PAYMENT
                            </span>

                            <h2>
                                Payment Information
                            </h2>

                        </div>


                        <div className="payment-info-card">

                            <div>

                                <span>
                                    Payment Method
                                </span>

                                <strong>
                                    {formatStatus(
                                        order.paymentMethod || "CASH"
                                    )}
                                </strong>

                            </div>


                            <div>

                                <span>
                                    Payment Status
                                </span>

                                <strong>
                                    {formatStatus(
                                        order.paymentStatus
                                    )}
                                </strong>

                            </div>


                            <div>

                                <span>
                                    Order Status
                                </span>

                                <strong>
                                    {formatStatus(
                                        order.orderStatus
                                    )}
                                </strong>

                            </div>

                            {order.paymentMethod === "RAZORPAY" && order.paymentStatus !== "PAID" && order.orderStatus !== "CANCELLED" && (
                                <div className="payment-retry-row">
                                    <span>Complete payment to confirm this order</span>
                                    <PaymentRetryButton onClick={retryRazorpayPayment} disabled={retryingPayment} />
                                </div>
                            )}

                        </div>

                    </section>

                </div>


                {/* =================================================
                    PRICE SUMMARY
                ================================================= */}

                <section className="order-section">

                    <div className="section-heading">

                        <span>
                            SUMMARY
                        </span>

                        <h2>
                            Price Details
                        </h2>

                    </div>


                    <div className="order-price-summary">

                        <div>

                            <span>
                                Subtotal
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    order.subtotal
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Discount
                            </span>

                            <strong className="discount-value">
                                − ₹
                                {formatMoney(
                                    order.discount
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Tax
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    order.tax
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Delivery
                            </span>

                            <strong className="free-delivery">
                                {Number(
                                    order.deliveryCharge || 0
                                ) === 0
                                    ? "FREE"
                                    : `₹${formatMoney(
                                        order.deliveryCharge
                                    )}`
                                }
                            </strong>

                        </div>


                        <div className="summary-total">

                            <span>
                                Grand Total
                            </span>

                            <strong>
                                ₹
                                {formatMoney(
                                    order.grandTotal
                                )}
                            </strong>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    FOOTER ACTIONS
                ================================================= */}

                <div className="order-details-actions">

                    <button
                        type="button"
                        className="secondary-order-button"
                        onClick={() =>
                            navigate(
                                "/customer/orders"
                            )
                        }
                    >
                        ← My Orders
                    </button>


                    <button
                        type="button"
                        className="primary-order-button"
                        onClick={() =>
                            navigate(
                                "/customer/products"
                            )
                        }
                    >
                        Continue Shopping
                    </button>

                    {canCancel && <button type="button" className="order-cancel-trigger order-cancel-bottom" onClick={() => setCancelOpen(true)}><Trash2 size={17} /> Cancel order</button>}

                </div>


            </section>

            {cancelOpen && <section className="order-cancel-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-order-title"><form onSubmit={submitCancellation}><button type="button" className="order-cancel-close" onClick={() => setCancelOpen(false)} aria-label="Close cancellation form"><X size={20} /></button><span><AlertCircle size={23} /> ORDER CANCELLATION</span><h2 id="cancel-order-title">Why do you want to cancel?</h2><p>This order can be cancelled before it is packed. After cancellation is confirmed, ShivHub sends a confirmation email to your registered email address.</p><label>Cancellation reason<select value={cancelReason} onChange={event => setCancelReason(event.target.value)} required><option value="">Select a reason</option>{CANCELLATION_REASONS.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></label>{cancelReason === "OTHER" && <label>Tell us more<textarea value={cancelComment} onChange={event => setCancelComment(event.target.value)} minLength="3" maxLength="1000" required placeholder="Enter cancellation reason" /></label>}<div><button type="button" className="order-cancel-keep" onClick={() => setCancelOpen(false)}>Keep order</button><button type="submit" className="order-cancel-confirm" disabled={cancelling || !cancelReason || (cancelReason === "OTHER" && !cancelComment.trim())}>{cancelling ? "Cancelling…" : "Confirm cancellation"}</button></div></form></section>}

        </main>
    );
};


export default CustomerOrderDetails;
