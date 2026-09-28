import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";

import "./MyOrders.css";


const MyOrders = () => {

    const navigate = useNavigate();

    const [orders, setOrders] = useState([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");


    /*
     * =========================================================
     * LOAD MY ORDERS
     * =========================================================
     */

    useEffect(() => {

        loadOrders();

    }, []);


    const loadOrders = async () => {

        try {

            setLoading(true);

            setError("");


            /*
             * JWT is automatically added by api.js
             */

            const response =
                await api.get("/api/orders/my");


            setOrders(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );


        } catch (requestError) {

            console.error(
                "Failed to load orders:",
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
                    "Unable to load your orders."
                );
            }


        } finally {

            setLoading(false);

        }
    };


    /*
     * =========================================================
     * FORMAT DATE
     * =========================================================
     */

    const formatDate = (date) => {

        if (!date) {
            return "-";
        }

        return new Date(date).toLocaleDateString(
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

    const formatMoney = (amount) => {

        return Number(amount || 0).toLocaleString(
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
     * EMPTY STATE
     * =========================================================
     */

    if (!loading && orders.length === 0 && !error) {

        return (

            <main className="my-orders-page">

                <div className="my-orders-header">

                    <button
                        className="back-button"
                        onClick={() =>
                            navigate(
                                "/customer/dashboard"
                            )
                        }
                    >
                        ← Dashboard
                    </button>

                    <div>
                        <span className="eyebrow">
                            SHIVHUB
                        </span>

                        <h1>
                            My Orders
                        </h1>

                        <p>
                            Track and manage your purchases.
                        </p>
                    </div>

                </div>


                <section className="orders-empty">

                    <div className="empty-icon">
                        📦
                    </div>

                    <h2>
                        No orders yet
                    </h2>

                    <p>
                        You haven't placed any orders yet.
                    </p>

                    <button
                        onClick={() =>
                            navigate(
                                "/customer/products"
                            )
                        }
                    >
                        Start Shopping
                    </button>

                </section>

            </main>
        );
    }


    /*
     * =========================================================
     * MAIN PAGE
     * =========================================================
     */

    return (

        <main className="my-orders-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <header className="my-orders-header">

                <button
                    className="back-button"
                    onClick={() =>
                        navigate(
                            "/customer/dashboard"
                        )
                    }
                >
                    ← Dashboard
                </button>


                <div>

                    <span className="eyebrow">
                        SHIVHUB
                    </span>

                    <h1>
                        My Orders
                    </h1>

                    <p>
                        Track and manage your purchases.
                    </p>

                </div>


                <button
                    className="continue-shopping"
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
                LOADING
            ================================================= */}

            {loading && (

                <section className="orders-loading">

                    <div className="spinner" />

                    <p>
                        Loading your orders...
                    </p>

                </section>
            )}


            {/* =================================================
                ERROR
            ================================================= */}

            {!loading && error && (

                <section className="orders-error">

                    <div className="error-icon">
                        ⚠️
                    </div>

                    <h2>
                        Unable to Load Orders
                    </h2>

                    <p>
                        {error}
                    </p>

                    <button
                        onClick={loadOrders}
                    >
                        Try Again
                    </button>

                </section>
            )}


            {/* =================================================
                ORDERS
            ================================================= */}

            {!loading && !error && orders.length > 0 && (

                <section className="orders-container">

                    <div className="orders-count">

                        <strong>
                            {orders.length}
                        </strong>

                        {" "}

                        {orders.length === 1
                            ? "Order"
                            : "Orders"}

                    </div>


                    {orders.map((order) => (

                        <article
                            className="order-card"
                            key={order.orderId}
                        >


                            {/* =================================================
                                ORDER HEADER
                            ================================================= */}

                            <div className="order-card-header">

                                <div>

                                    <span className="order-label">
                                        ORDER
                                    </span>

                                    <h2>
                                        {order.orderNumber}
                                    </h2>

                                </div>


                                <span
                                    className={`order-status ${getStatusClass(
                                        order.orderStatus
                                    )}`}
                                >
                                    {order.orderStatus
                                        ?.replaceAll(
                                            "_",
                                            " "
                                        )}
                                </span>

                            </div>


                            {/* =================================================
                                ORDER META
                            ================================================= */}

                            <div className="order-meta">

                                <div>

                                    <span>
                                        Order Date
                                    </span>

                                    <strong>
                                        {formatDate(
                                            order.createdAt
                                        )}
                                    </strong>

                                </div>


                                <div>

                                    <span>
                                        Payment
                                    </span>

                                    <strong>
                                        {order.paymentStatus
                                            ?.replaceAll(
                                                "_",
                                                " "
                                            ) || "-"}
                                    </strong>

                                </div>


                                <div>

                                    <span>
                                        Total
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
                                PRODUCTS
                            ================================================= */}

                            <div className="order-products">

                                {order.items?.map(
                                    (item) => (

                                        <div
                                            className="order-product"
                                            key={item.id}
                                        >

                                            <div className="product-placeholder">
                                                📦
                                            </div>


                                            <div className="product-info">

                                                <strong>
                                                    {item.productName}
                                                </strong>

                                                <span>
                                                    Qty:{" "}
                                                    {item.quantity}
                                                </span>

                                            </div>


                                            <strong>
                                                ₹
                                                {formatMoney(
                                                    item.totalPrice
                                                )}
                                            </strong>

                                        </div>

                                    )
                                )}

                            </div>


                            {/* =================================================
                                FOOTER
                            ================================================= */}

                            <div className="order-card-footer">

                                <span>

                                    {order.items?.length || 0}

                                    {" "}

                                    {order.items?.length === 1
                                        ? "item"
                                        : "items"}

                                </span>


                                <button
                                    onClick={() =>
                                        navigate(
                                            `/customer/orders/${order.orderId}`
                                        )
                                    }
                                >
                                    View Details →
                                </button>

                            </div>

                        </article>

                    ))}

                </section>

            )}

        </main>
    );
};


export default MyOrders;
