import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { variantSummary } from "../../components/products/productCommonFields";
import api from "../../services/api";
import { notifyLoyaltyChanged } from "../../services/loyaltyService";

import "./OrderDetails.css";


const OrderDetails = () => {

    const navigate = useNavigate();

    const { id } = useParams();

    const [order, setOrder] = useState(null);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [cancellationReason, setCancellationReason] = useState("");

    const [cancellationComment, setCancellationComment] = useState("");

    const [cancelling, setCancelling] = useState(false);


    /*
     * =========================================================
     * LOAD ORDER DETAILS
     * =========================================================
     */




    const loadOrder = useCallback(async () => {

        try {

            setLoading(true);

            setError("");

            const response =
                await api.get(
                    `/api/orders/${id}`
                );

            setOrder(response.data);

        } catch (requestError) {

            console.error(
                "Failed to load order:",
                requestError
            );

            if (
                requestError.response?.status === 401 ||
                requestError.response?.status === 403
            ) {

                setError(
                    "Your login session has expired. Please login again."
                );

            } else {

                setError(
                    requestError.response?.data?.message ||
                    "Unable to load order details."
                );
            }

        } finally {

            setLoading(false);

        }
    }, [id]);


    /*
     * =========================================================
     * FORMAT DATE
     * =========================================================
     */

    useEffect(() => {

        loadOrder();

    }, [loadOrder]);
    const formatDate = (date) => {

        if (!date) {
            return "-";
        }

        return new Date(date).toLocaleString(
            "en-IN",
            {
                day: "2-digit",
                month: "short",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            }
        );
    };


    /*
     * =========================================================
     * FORMAT MONEY
     * =========================================================
     */

    const formatMoney = (amount) => {

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
     * STATUS CLASS
     * =========================================================
     */

    const getStatusClass = (status) => {

        if (!status) {
            return "status-default";
        }

        return status
            .toLowerCase()
            .replaceAll("_", "-");
    };


    /*
     * =========================================================
     * STATUS LABEL
     * =========================================================
     */

    const getStatusLabel = (status) => {

        if (!status) {
            return "Unknown";
        }

        return status
            .replaceAll("_", " ");
    };


    /*
     * =========================================================
     * STATUS TIMELINE
     * =========================================================
     */

    const statuses = [

        "PENDING",

        "CONFIRMED",

        "PROCESSING",

        "PACKED",

        "SHIPPED",

        "OUT_FOR_DELIVERY",

        "DELIVERED"

    ];


    const getStatusIndex = () => {

        if (!order?.orderStatus) {
            return -1;
        }

        return statuses.indexOf(
            order.orderStatus
        );
    };

    const canCancel = ["PENDING", "CONFIRMED", "PROCESSING"]
        .includes(order?.orderStatus);

    const cancelOrder = async () => {
        if (!cancellationReason) {
            setError("Please select a reason for cancellation.");
            return;
        }
        if (cancellationReason === "OTHER" && !cancellationComment.trim()) {
            setError("Please tell us why you want to cancel this order.");
            return;
        }

        try {
            setCancelling(true);
            setError("");
            const response = await api.post(`/api/orders/${order.orderId}/cancel`, {
                reason: cancellationReason,
                comment: cancellationComment.trim() || null,
            });
            setOrder(response.data);
            notifyLoyaltyChanged();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to cancel this order.");
        } finally {
            setCancelling(false);
        }
    };


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (loading) {

        return (

            <main className="order-details-page">

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

            <main className="order-details-page">

                <section className="order-details-error">

                    <div className="order-error-icon">
                        ⚠️
                    </div>

                    <h2>
                        Unable to Load Order
                    </h2>

                    <p>
                        {error ||
                            "Order not found."}
                    </p>

                    <button
                        onClick={() =>
                            navigate(
                                "/customer/orders"
                            )
                        }
                    >
                        ← Back to My Orders
                    </button>

                </section>

            </main>

        );
    }


    const currentStatusIndex =
        getStatusIndex();


    return (

        <main className="order-details-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <header className="order-details-header">

                <button
                    className="order-back-button"
                    onClick={() =>
                        navigate(
                            "/customer/orders"
                        )
                    }
                >
                    ← My Orders
                </button>


                <div className="order-details-heading">

                    <span>
                        SHIVHUB
                    </span>

                    <h1>
                        Order Details
                    </h1>

                </div>

            </header>



            {/* =================================================
                ORDER TOP CARD
            ================================================= */}

            <section className="order-top-card">

                <div>

                    <span>
                        ORDER NUMBER
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


                <div className="order-top-status">

                    <span>
                        ORDER STATUS
                    </span>

                    <strong
                        className={`detail-status ${getStatusClass(
                            order.orderStatus
                        )}`}
                    >
                        {getStatusLabel(
                            order.orderStatus
                        )}
                    </strong>

                </div>

            </section>



            {/* =================================================
                STATUS TIMELINE
            ================================================= */}

            {order.orderStatus !== "CANCELLED" && (

                <section className="status-timeline-card">

                    <div className="section-title">

                        <span>
                            ORDER TRACKING
                        </span>

                        <h2>
                            Order Status
                        </h2>

                    </div>


                    <div className="status-timeline">

                        {statuses.map(
                            (status, index) => {

                                const completed =
                                    currentStatusIndex >= index;

                                const active =
                                    order.orderStatus === status;


                                return (

                                    <div
                                        className={`timeline-step ${
                                            completed
                                                ? "completed"
                                                : ""
                                        } ${
                                            active
                                                ? "active"
                                                : ""
                                        }`}
                                        key={status}
                                    >

                                        <div className="timeline-dot">

                                            {completed
                                                ? "✓"
                                                : index + 1}

                                        </div>


                                        <div className="timeline-label">

                                            {getStatusLabel(
                                                status
                                            )}

                                        </div>

                                    </div>

                                );

                            }
                        )}

                    </div>

                </section>

            )}



            {/* =================================================
                CANCELLED STATUS
            ================================================= */}

            {order.orderStatus === "CANCELLED" && (

                <section className="cancelled-card">

                    <div>
                        ✕
                    </div>

                    <div>

                        <strong>
                            Order Cancelled
                        </strong>

                        <p>
                            This order has been cancelled.
                        </p>

                    </div>

                </section>

            )}

            {canCancel && (
                <section className="cancel-order-card">
                    <div>
                        <span>NEED TO CANCEL?</span>
                        <h2>Cancel this order</h2>
                        <p>You can cancel until the order is packed. Your cancellation will be recorded and confirmed by email.</p>
                    </div>
                    <div className="cancel-order-controls">
                        <select value={cancellationReason} onChange={(event) => setCancellationReason(event.target.value)} disabled={cancelling}>
                            <option value="">Select a reason</option>
                            <option value="ORDERED_BY_MISTAKE">Ordered by mistake</option>
                            <option value="CHANGE_OF_MIND">Changed my mind</option>
                            <option value="FOUND_BETTER_PRICE">Found a better price</option>
                            <option value="DELIVERY_TIME_TOO_LONG">Delivery time is too long</option>
                            <option value="WANT_TO_CHANGE_ADDRESS">Want to change delivery address</option>
                            <option value="WANT_TO_CHANGE_PRODUCT">Want to change product or quantity</option>
                            <option value="OTHER">Other reason</option>
                        </select>
                        {cancellationReason === "OTHER" && (
                            <textarea value={cancellationComment} onChange={(event) => setCancellationComment(event.target.value)} placeholder="Please describe the reason" disabled={cancelling} />
                        )}
                        <button type="button" onClick={cancelOrder} disabled={cancelling}>
                            {cancelling ? "Cancelling..." : "Cancel order"}
                        </button>
                    </div>
                </section>
            )}



            {/* =================================================
                MAIN CONTENT
            ================================================= */}

            <div className="order-details-grid">


                {/* =================================================
                    LEFT
                ================================================= */}

                <section className="order-details-main">


                    {/* =================================================
                        PRODUCTS
                    ================================================= */}

                    <div className="details-card">

                        <div className="section-title">

                            <span>
                                PURCHASE
                            </span>

                            <h2>
                                Items in this Order
                            </h2>

                        </div>


                        <div className="details-products">

                            {order.items?.map(
                                (item) => (

                                    <div
                                        className="details-product"
                                        key={item.id}
                                    >

                                        <div className="details-product-image">

                                            📦

                                        </div>


                                        <div className="details-product-info">

                                            <strong>
                                                {item.productName}
                                                {item.selectedAttributes&&<small style={{display:"block",fontWeight:400}}>{variantSummary(item.selectedAttributes)}</small>}
                                            </strong>

                                            <span>
                                                Quantity:{" "}
                                                {item.quantity}
                                            </span>

                                            <span>
                                                Unit Price: ₹
                                                {formatMoney(
                                                    item.unitPrice
                                                )}
                                            </span>

                                        </div>


                                        <strong className="details-product-total">

                                            ₹
                                            {formatMoney(
                                                item.totalPrice
                                            )}

                                        </strong>

                                    </div>

                                )
                            )}

                        </div>

                    </div>



                    {/* =================================================
                        DELIVERY ADDRESS
                    ================================================= */}

                    <div className="details-card">

                        <div className="section-title">

                            <span>
                                DELIVERY
                            </span>

                            <h2>
                                Delivery Address
                            </h2>

                        </div>


                        <div className="delivery-address">

                            <div className="address-icon">
                                📍
                            </div>


                            <p>
                                {order.shippingAddress ||
                                    "Delivery address not available."}
                            </p>

                        </div>

                    </div>



                    {/* =================================================
                        PAYMENT
                    ================================================= */}

                    <div className="details-card">

                        <div className="section-title">

                            <span>
                                PAYMENT
                            </span>

                            <h2>
                                Payment Information
                            </h2>

                        </div>


                        <div className="payment-information">

                            <div>

                                <span>
                                    Payment Method
                                </span>

                                <strong>
                                    Cash on Delivery
                                </strong>

                            </div>


                            <div>

                                <span>
                                    Payment Status
                                </span>

                                <strong
                                    className={
                                        order.paymentStatus ===
                                        "PAID"
                                            ? "paid"
                                            : "payment-pending"
                                    }
                                >
                                    {getStatusLabel(
                                        order.paymentStatus
                                    )}
                                </strong>

                            </div>

                        </div>

                    </div>

                </section>



                {/* =================================================
                    RIGHT
                ================================================= */}

                <aside className="order-details-sidebar">


                    {/* =================================================
                        PRICE SUMMARY
                    ================================================= */}

                    <div className="details-card price-summary">

                        <div className="section-title">

                            <span>
                                BILLING
                            </span>

                            <h2>
                                Price Summary
                            </h2>

                        </div>


                        <div className="price-lines">


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

                                <strong>
                                    ₹
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

                                <strong className="free">
                                    {Number(
                                        order.deliveryCharge || 0
                                    ) === 0
                                        ? "FREE"
                                        : `₹${formatMoney(
                                            order.deliveryCharge
                                        )}`}
                                </strong>

                            </div>

                        </div>


                        <div className="grand-total">

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



                    {/* =================================================
                        CUSTOMER INFORMATION
                    ================================================= */}

                    <div className="details-card customer-information">

                        <div className="section-title">

                            <span>
                                CUSTOMER
                            </span>

                            <h2>
                                Customer Information
                            </h2>

                        </div>


                        <div className="customer-row">

                            <span>
                                Name
                            </span>

                            <strong>
                                {order.customerName ||
                                    "-"}
                            </strong>

                        </div>


                        <div className="customer-row">

                            <span>
                                Email
                            </span>

                            <strong>
                                {order.customerEmail ||
                                    "-"}
                            </strong>

                        </div>

                    </div>



                    {/* =================================================
                        ACTIONS
                    ================================================= */}

                    <button
                        className="back-orders-button"
                        onClick={() =>
                            navigate(
                                "/customer/orders"
                            )
                        }
                    >
                        ← Back to My Orders
                    </button>

                </aside>

            </div>

        </main>
    );
};


export default OrderDetails;
