const getToken = () => {

        const keys = [
            "token",
            "jwtToken",
            "accessToken",
            "shivhub_token",
            "authToken",
        ];

        for (const key of keys) {

            const value =
                localStorage.getItem(key);

            if (value) {
                return value;
            }
        }

        return null;
    };
import { useCallback, useEffect, useMemo, useState } from "react";
import { API_BASE_URL } from "../../services/api";
import "./Orders.css";

const ORDER_STATUS = [
    "PENDING",
    "CONFIRMED",
    "PROCESSING",
    "PACKED",
    "SHIPPED",
    "OUT_FOR_DELIVERY",
    "DELIVERED",
    "CANCELLED",
];

const PAYMENT_STATUS = [
    "PENDING",
    "PAID",
    "FAILED",
    "REFUNDED",
];

const formatDate = (value) => {
    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    return date.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
};

const formatTime = (value) => {
    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    return date.toLocaleTimeString("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
    });
};

const formatCurrency = (value) => {
    const amount = Number(value || 0);

    return amount.toLocaleString("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2,
    });
};

const getStatusClass = (status) => {
    if (!status) return "pending";

    return status
        .toLowerCase()
        .replaceAll("_", "-");
};

const formatStatus = (status) => {
    if (!status) return "PENDING";

    return status.replaceAll("_", " ");
};

const getCustomerName = (order) => {
    return (
        order.customerName ||
        order.customer?.name ||
        order.userName ||
        order.name ||
        "Customer"
    );
};

const getCustomerEmail = (order) => {
    return (
        order.customerEmail ||
        order.customer?.email ||
        order.userEmail ||
        order.email ||
        "-"
    );
};

const getCustomerId = (order) => {
    return (
        order.customerId ||
        order.customer?.id ||
        "-"
    );
};

const getOrderItems = (order) => {
    return (
        order.items ||
        order.orderItems ||
        []
    );
};

const toDateTimeInput = (value) => {
    const date = value ? new Date(value) : new Date(Date.now() + (2 * 60 * 60 * 1000));
    const safeDate = Number.isNaN(date.getTime())
        ? new Date(Date.now() + (2 * 60 * 60 * 1000))
        : date;
    const pad = number => String(number).padStart(2, "0");
    return `${safeDate.getFullYear()}-${pad(safeDate.getMonth() + 1)}-${pad(safeDate.getDate())}T${pad(safeDate.getHours())}:${pad(safeDate.getMinutes())}`;
};

const formatExpectedDelivery = (value) => {
    if (!value) return "Delivery time needs confirmation";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "Delivery time needs confirmation";
    const minutes = Math.round((date.getTime() - Date.now()) / 60000);
    if (minutes > 0 && minutes <= 24 * 60) {
        const hours = Math.max(1, Math.ceil(minutes / 60));
        return `Expected within ${hours} ${hours === 1 ? "hour" : "hours"} · ${formatTime(value)}`;
    }
    return `Expected by ${formatDate(value)} · ${formatTime(value)}`;
};

function Orders() {

    const [orders, setOrders] = useState([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const [search, setSearch] =
        useState("");

    const [statusFilter, setStatusFilter] =
        useState("ALL");

    const [paymentFilter, setPaymentFilter] =
        useState("ALL");

    const [selectedOrder, setSelectedOrder] =
        useState(null);

    const [showDetails, setShowDetails] =
        useState(false);

    const [pendingStatus, setPendingStatus] =
        useState("PENDING");

    const [updatingOrderId, setUpdatingOrderId] =
        useState(null);

    const [deliveryPlan, setDeliveryPlan] =
        useState(null);


    /* =========================================================
       GET TOKEN
    ========================================================= */

    


    /* =========================================================
       FETCH ALL ORDERS
    ========================================================= */

    const fetchOrders = useCallback(async () => {

        try {

            setLoading(true);
            setError("");

            const token = getToken();

            if (!token) {
                throw new Error(
                    "Admin login token not found. Please login again."
                );
            }

            const response = await fetch(
                `${API_BASE_URL}/api/admin/orders`,
                {
                    method: "GET",

                    headers: {
                        Authorization:
                            `Bearer ${token}`,

                        Accept:
                            "application/json",
                    },
                }
            );


            if (response.status === 401) {
                throw new Error(
                    "Admin session expired. Please login again."
                );
            }


            if (response.status === 403) {
                throw new Error(
                    "You are not authorized to view orders."
                );
            }


            if (!response.ok) {

                const text =
                    await response.text();

                throw new Error(
                    text ||
                    `Failed to load orders (${response.status})`
                );
            }


            const data =
                await response.json();


            if (Array.isArray(data)) {

                setOrders(data);

            } else if (
                Array.isArray(data.content)
            ) {

                setOrders(data.content);

            } else if (
                Array.isArray(data.orders)
            ) {

                setOrders(data.orders);

            } else {

                setOrders([]);
            }

        } catch (err) {

            console.error(
                "Admin Orders Error:",
                err
            );

            setError(
                err.message ||
                "Unable to load orders."
            );

        } finally {

            setLoading(false);
        }
    }, []);


    /* =========================================================
       INITIAL LOAD
    ========================================================= */

    useEffect(() => {

        fetchOrders();

    }, [fetchOrders]);


    /* =========================================================
       STATISTICS
    ========================================================= */

    const counts = useMemo(() => {

        const total =
            orders.length;


        const pending =
            orders.filter(
                order =>
                    order.orderStatus ===
                    "PENDING"
            ).length;


        const confirmed =
            orders.filter(
                order =>
                    order.orderStatus ===
                    "CONFIRMED"
            ).length;


        const processing =
            orders.filter(
                order =>
                    order.orderStatus ===
                    "PROCESSING"
            ).length;


        const shipped =
            orders.filter(
                order =>
                    order.orderStatus ===
                        "SHIPPED" ||
                    order.orderStatus ===
                        "OUT_FOR_DELIVERY"
            ).length;


        const delivered =
            orders.filter(
                order =>
                    order.orderStatus ===
                    "DELIVERED"
            ).length;


        const cancelled =
            orders.filter(
                order =>
                    order.orderStatus ===
                    "CANCELLED"
            ).length;


        const revenue =
            orders
                .filter(
                    order =>
                        order.orderStatus !==
                        "CANCELLED"
                )
                .reduce(
                    (sum, order) =>
                        sum +
                        Number(
                            order.grandTotal ||
                            order.totalAmount ||
                            order.total ||
                            0
                        ),
                    0
                );


        return {
            total,
            pending,
            confirmed,
            processing,
            shipped,
            delivered,
            cancelled,
            revenue,
        };

    }, [orders]);


    /* =========================================================
       FILTER
    ========================================================= */

    const filteredOrders = useMemo(() => {

        const searchValue =
            search
                .trim()
                .toLowerCase();


        return orders.filter(order => {

            const orderNumber =
                String(
                    order.orderNumber ||
                    order.orderNo ||
                    order.orderId ||
                    ""
                ).toLowerCase();


            const customerName =
                getCustomerName(order)
                    .toLowerCase();


            const customerEmail =
                getCustomerEmail(order)
                    .toLowerCase();


            const matchesSearch =
                !searchValue ||
                orderNumber.includes(searchValue) ||
                customerName.includes(searchValue) ||
                customerEmail.includes(searchValue);


            const matchesStatus =
                statusFilter === "ALL" ||
                order.orderStatus ===
                    statusFilter;


            const matchesPayment =
                paymentFilter === "ALL" ||
                order.paymentStatus ===
                    paymentFilter;


            return (
                matchesSearch &&
                matchesStatus &&
                matchesPayment
            );
        });

    }, [
        orders,
        search,
        statusFilter,
        paymentFilter,
    ]);


    /* =========================================================
       VIEW ORDER
    ========================================================= */

    const handleViewOrder = (order) => {

        setSelectedOrder(order);

        setPendingStatus(
            order.orderStatus ||
            "PENDING"
        );

        setError("");

        setSuccessMessage("");

        setShowDetails(true);
    };


    /* =========================================================
       UPDATE ORDER STATUS
    ========================================================= */

    const handleStatusUpdate = async (
        orderId,
        newStatus
    ) => {

        if (!newStatus) {
            return;
        }


        if (
            selectedOrder &&
            selectedOrder.orderStatus ===
            newStatus
        ) {
            return;
        }


        try {

            setUpdatingOrderId(orderId);

            setError("");

            setSuccessMessage("");


            const token = getToken();


            if (!token) {

                throw new Error(
                    "Admin token not found. Please login again."
                );
            }


            console.log(
                "Updating order:",
                orderId,
                "to:",
                newStatus
            );


            const response =
                await fetch(
                    `${API_BASE_URL}/api/admin/orders/${orderId}/status`,
                    {
                        method: "PUT",

                        headers: {
                            Authorization:
                                `Bearer ${token}`,

                            "Content-Type":
                                "application/json",

                            Accept:
                                "application/json",
                        },

                        body: JSON.stringify({
                            orderStatus:
                                newStatus,
                        }),
                    }
                );


            if (response.status === 401) {

                throw new Error(
                    "Admin session expired. Please login again."
                );
            }


            if (response.status === 403) {

                throw new Error(
                    "You are not authorized to update this order."
                );
            }


            if (!response.ok) {

                const errorText =
                    await response.text();

                throw new Error(
                    errorText ||
                    `Order status update failed (${response.status})`
                );
            }


            const updatedOrder =
                await response.json();


            console.log(
                "Updated order:",
                updatedOrder
            );


            /* -------------------------------------------------
               UPDATE ORDER LIST
            ------------------------------------------------- */

            setOrders(
                previousOrders =>
                    previousOrders.map(order =>
                        order.orderId === orderId
                            ? updatedOrder
                            : order
                    )
            );


            /* -------------------------------------------------
               UPDATE SELECTED ORDER
            ------------------------------------------------- */

            setSelectedOrder(
                updatedOrder
            );


            setPendingStatus(
                updatedOrder.orderStatus ||
                newStatus
            );


            setSuccessMessage(
                `Order status changed to ${formatStatus(
                    updatedOrder.orderStatus ||
                    newStatus
                )}.`
            );


            setTimeout(() => {

                setSuccessMessage("");

            }, 4000);


        } catch (err) {

            console.error(
                "Update Order Status Error:",
                err
            );

            setError(
                err.message ||
                "Unable to update order status."
            );

        } finally {

            setUpdatingOrderId(null);
        }
    };


    /* =========================================================
       REFRESH
    ========================================================= */

    const handleRefresh = () => {

        fetchOrders();
    };


    /* =========================================================
       CLOSE MODAL
    ========================================================= */

    const closeDetails = () => {

        setShowDetails(false);

        setSelectedOrder(null);

        setDeliveryPlan(null);

        setError("");

        setSuccessMessage("");
    };


    /* =========================================================
       RENDER
    ========================================================= */

    const approveSellerRequest = async (order) => {
        try {
            setUpdatingOrderId(order.orderId); setError(""); setSuccessMessage("");
            const token = getToken(); if (!token) throw new Error("Admin token not found. Please login again.");
            const response = await fetch(`${API_BASE_URL}/api/admin/orders/${order.orderId}/approve-seller-status`, { method: "PUT", headers: { Authorization: `Bearer ${token}`, Accept: "application/json" } });
            if (!response.ok) throw new Error((await response.text()) || "Could not approve seller request.");
            const updated = await response.json();
            setOrders(current => current.map(item => item.orderId === updated.orderId ? updated : item));
            setSuccessMessage(`Approved ${formatStatus(updated.orderStatus)}. Customer email has been sent.`);
        } catch (err) { setError(err.message || "Could not approve seller request."); }
        finally { setUpdatingOrderId(null); }
    };

    const handleDeliveryExpectationUpdate = async () => {
        const orderId = selectedOrder?.orderId;
        const sellerId = deliveryPlan?.sellerId;

        if (!orderId || !sellerId || !deliveryPlan.expectedDeliveryAt) {
            setError("Choose a future delivery date and time first.");
            return;
        }

        try {
            setUpdatingOrderId(`delivery-${orderId}-${sellerId}`);
            setError("");
            setSuccessMessage("");

            const token = getToken();
            if (!token) throw new Error("Admin token not found. Please login again.");

            const response = await fetch(
                `${API_BASE_URL}/api/admin/orders/${orderId}/delivery-expectations/${sellerId}`,
                {
                    method: "PUT",
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                        Accept: "application/json",
                    },
                    body: JSON.stringify({
                        expectedDeliveryAt: deliveryPlan.expectedDeliveryAt,
                        customerMessage: deliveryPlan.customerMessage?.trim() || null,
                    }),
                }
            );

            const updated = await response.json().catch(() => null);
            if (!response.ok) {
                throw new Error(updated?.message || updated?.error || "Unable to save the expected delivery time.");
            }

            const applyExpectation = order => ({
                ...order,
                deliveryExpectations: [
                    ...(Array.isArray(order.deliveryExpectations) ? order.deliveryExpectations : [])
                        .filter(item => item.sellerId !== updated.sellerId),
                    updated,
                ].sort((left, right) => String(left.expectedDeliveryAt || "").localeCompare(String(right.expectedDeliveryAt || ""))),
            });

            setSelectedOrder(current => applyExpectation(current));
            setOrders(current => current.map(order => order.orderId === orderId ? applyExpectation(order) : order));
            setDeliveryPlan(null);
            setSuccessMessage("Expected delivery time was updated for the customer.");
            setTimeout(() => setSuccessMessage(""), 4000);
        } catch (err) {
            setError(err.message || "Unable to save the expected delivery time.");
        } finally {
            setUpdatingOrderId(null);
        }
    };

    return (

        <div className="orders-page">


            {/* =================================================
               HEADER
            ================================================= */}

            <div className="orders-header">

                <div>

                    <p className="page-kicker">
                        SHIVHUB ADMIN
                    </p>

                    <h1>
                        Orders Management
                    </h1>

                    <p className="page-description">
                        Monitor customer orders,
                        payments and delivery status
                        from one place.
                    </p>

                </div>


                <button
                    type="button"
                    className="refresh-button"
                    onClick={handleRefresh}
                    disabled={loading}
                >
                    {loading
                        ? "Refreshing..."
                        : "↻ Refresh Orders"}
                </button>

            </div>


            {/* =================================================
               MESSAGES
            ================================================= */}

            {error && (

                <div className="order-message error">

                    {error}

                </div>

            )}


            {successMessage && (

                <div className="order-message success">

                    {successMessage}

                </div>

            )}

            {orders.some(order => order.sellerRequestedStatus) && (
                <section className="seller-approval-section">
                    <div className="seller-approval-title"><div><span>SELLER ACTION REQUIRED</span><h2>Delivery status approval queue</h2><p>Approve the seller’s request to update the order and notify the customer.</p></div><b>{orders.filter(order => order.sellerRequestedStatus).length} pending</b></div>
                    <div className="seller-approval-grid">{orders.filter(order => order.sellerRequestedStatus).map(order => <article key={order.orderId}><div><strong>{order.orderNumber}</strong><small>Current: {formatStatus(order.orderStatus)} → Requested: {formatStatus(order.sellerRequestedStatus)}</small></div>{order.sellerRequestedStatus === "OUT_FOR_DELIVERY" && <p><b>Delivery partner:</b> {order.deliveryPersonName || "Not provided"} · {order.deliveryPersonMobile || "Not provided"}</p>}<button type="button" disabled={updatingOrderId === order.orderId} onClick={() => approveSellerRequest(order)}>{updatingOrderId === order.orderId ? "Approving…" : "Approve & notify customer"}</button></article>)}</div>
                </section>
            )}


            {/* =================================================
               STATISTICS
            ================================================= */}

            <div className="order-stats">


                <div className="order-stat-card">

                    <div className="order-stat-icon">
                        🛒
                    </div>

                    <div>

                        <span>
                            Total Orders
                        </span>

                        <strong>
                            {counts.total}
                        </strong>

                    </div>

                </div>


                <div className="order-stat-card pending">

                    <div className="order-stat-icon">
                        ◷
                    </div>

                    <div>

                        <span>
                            Pending
                        </span>

                        <strong>
                            {counts.pending}
                        </strong>

                    </div>

                </div>


                <div className="order-stat-card confirmed">

                    <div className="order-stat-icon">
                        ✓
                    </div>

                    <div>

                        <span>
                            Confirmed
                        </span>

                        <strong>
                            {counts.confirmed}
                        </strong>

                    </div>

                </div>


                <div className="order-stat-card processing">

                    <div className="order-stat-icon">
                        ⚙
                    </div>

                    <div>

                        <span>
                            Processing
                        </span>

                        <strong>
                            {counts.processing}
                        </strong>

                    </div>

                </div>


                <div className="order-stat-card delivered">

                    <div className="order-stat-icon">
                        ✓
                    </div>

                    <div>

                        <span>
                            Delivered
                        </span>

                        <strong>
                            {counts.delivered}
                        </strong>

                    </div>

                </div>


                <div className="order-stat-card revenue">

                    <div className="order-stat-icon">
                        ₹
                    </div>

                    <div>

                        <span>
                            Revenue
                        </span>

                        <strong>
                            {formatCurrency(
                                counts.revenue
                            )}
                        </strong>

                    </div>

                </div>

            </div>


            {/* =================================================
               ORDERS CARD
            ================================================= */}

            <div className="orders-card">


                <div className="orders-card-header">

                    <div>

                        <h2>
                            Order History
                        </h2>

                        <p>
                            View and manage every
                            customer order.
                        </p>

                    </div>


                    <div className="order-search">

                        <input
                            type="text"
                            value={search}
                            onChange={event =>
                                setSearch(
                                    event.target.value
                                )
                            }
                            placeholder="Search order, customer or email..."
                        />

                    </div>

                </div>


                {/* =================================================
                   FILTERS
                ================================================= */}

                <div className="order-filters">


                    <div className="filter-group">

                        <label>
                            Order Status
                        </label>

                        <select
                            value={statusFilter}
                            onChange={event =>
                                setStatusFilter(
                                    event.target.value
                                )
                            }
                        >

                            <option value="ALL">
                                All Status
                            </option>

                            {ORDER_STATUS.map(
                                status => (

                                    <option
                                        key={status}
                                        value={status}
                                    >
                                        {formatStatus(status)}
                                    </option>

                                )
                            )}

                        </select>

                    </div>


                    <div className="filter-group">

                        <label>
                            Payment
                        </label>

                        <select
                            value={paymentFilter}
                            onChange={event =>
                                setPaymentFilter(
                                    event.target.value
                                )
                            }
                        >

                            <option value="ALL">
                                All Payments
                            </option>

                            {PAYMENT_STATUS.map(
                                status => (

                                    <option
                                        key={status}
                                        value={status}
                                    >
                                        {status}
                                    </option>

                                )
                            )}

                        </select>

                    </div>


                    <div className="filter-result">

                        Showing{" "}

                        <strong>
                            {filteredOrders.length}
                        </strong>

                        {" "}of{" "}

                        <strong>
                            {orders.length}
                        </strong>

                        {" "}orders

                    </div>

                </div>


                {/* =================================================
                   LOADING
                ================================================= */}

                {loading ? (

                    <div className="orders-loading">

                        <div className="loading-spinner">
                        </div>

                        <h3>
                            Loading orders...
                        </h3>

                        <p>
                            Please wait while we
                            fetch the latest orders.
                        </p>

                    </div>

                ) : filteredOrders.length === 0 ? (

                    <div className="empty-orders">

                        <div className="empty-order-icon">
                            🛒
                        </div>

                        <h3>
                            No orders found
                        </h3>

                        <p>
                            Try changing your search
                            or filter.
                        </p>

                    </div>

                ) : (

                    /* =================================================
                       TABLE
                    ================================================= */

                    <div className="orders-table-wrapper">

                        <table className="orders-table">

                            <thead>

                                <tr>

                                    <th>
                                        ORDER
                                    </th>

                                    <th>
                                        CUSTOMER
                                    </th>

                                    <th>
                                        AMOUNT
                                    </th>

                                    <th>
                                        PAYMENT
                                    </th>

                                    <th>
                                        STATUS
                                    </th>

                                    <th>
                                        DATE
                                    </th>

                                    <th>
                                        ACTION
                                    </th>

                                </tr>

                            </thead>


                            <tbody>

                                {filteredOrders.map(
                                    order => (

                                        <tr
                                            key={order.orderId}
                                        >

                                            {/* ORDER */}

                                            <td>

                                                <div className="order-number-cell">

                                                    <div className="order-small-icon">
                                                        🛍
                                                    </div>

                                                    <div>

                                                        <strong>
                                                            {
                                                                order.orderNumber ||
                                                                order.orderNo ||
                                                                `#${order.orderId}`
                                                            }
                                                        </strong>

                                                        <small>
                                                            Order ID:{" "}
                                                            {order.orderId}
                                                        </small>

                                                    </div>

                                                </div>

                                            </td>


                                            {/* CUSTOMER */}

                                            <td>

                                                <div className="customer-cell">

                                                    <div className="customer-avatar">

                                                        {getCustomerName(
                                                            order
                                                        )
                                                            .charAt(0)
                                                            .toUpperCase()}

                                                    </div>

                                                    <div>

                                                        <strong>
                                                            {
                                                                getCustomerName(
                                                                    order
                                                                )
                                                            }
                                                        </strong>

                                                        <small>
                                                            {
                                                                getCustomerEmail(
                                                                    order
                                                                )
                                                            }
                                                        </small>

                                                    </div>

                                                </div>

                                            </td>


                                            {/* AMOUNT */}

                                            <td>

                                                <strong className="order-amount">

                                                    {formatCurrency(
                                                        order.grandTotal ||
                                                        order.totalAmount ||
                                                        order.total
                                                    )}

                                                </strong>

                                                <small className="order-subtotal">

                                                    Subtotal{" "}

                                                    {formatCurrency(
                                                        order.subtotal
                                                    )}

                                                </small>

                                            </td>


                                            {/* PAYMENT */}

                                            <td>

                                                <span
                                                    className={
                                                        `payment-badge ${
                                                            getStatusClass(
                                                                order.paymentStatus
                                                            )
                                                        }`
                                                    }
                                                >

                                                    {
                                                        order.paymentStatus ||
                                                        "PENDING"
                                                    }

                                                </span>

                                            </td>


                                            {/* STATUS */}

                                            <td>

                                                <span
                                                    className={
                                                        `order-status-badge ${
                                                            getStatusClass(
                                                                order.orderStatus
                                                            )
                                                        }`
                                                    }
                                                >

                                                    {formatStatus(
                                                        order.orderStatus
                                                    )}

                                                </span>

                                            </td>


                                            {/* DATE */}

                                            <td>

                                                <div className="date-cell">

                                                    <strong>
                                                        {formatDate(
                                                            order.createdAt
                                                        )}
                                                    </strong>

                                                    <small>
                                                        {formatTime(
                                                            order.createdAt
                                                        )}
                                                    </small>

                                                </div>

                                            </td>


                                            {/* ACTION */}

                                            <td>

                                                <button
                                                    type="button"
                                                    className="view-order-button"
                                                    onClick={() =>
                                                        handleViewOrder(
                                                            order
                                                        )
                                                    }
                                                >
                                                    View
                                                </button>

                                            </td>

                                        </tr>

                                    )
                                )}

                            </tbody>

                        </table>

                    </div>

                )}

            </div>


            {/* =================================================
               ORDER DETAILS MODAL
            ================================================= */}

            {showDetails &&
                selectedOrder && (

                    <div
                        className="order-modal-overlay"
                        onClick={closeDetails}
                    >

                        <div
                            className="order-modal"
                            onClick={event =>
                                event.stopPropagation()
                            }
                        >


                            {/* MODAL HEADER */}

                            <div className="order-modal-header">

                                <div>

                                    <p>
                                        ORDER DETAILS
                                    </p>

                                    <h2>
                                        {
                                            selectedOrder.orderNumber ||
                                            selectedOrder.orderNo ||
                                            `#${selectedOrder.orderId}`
                                        }
                                    </h2>

                                </div>


                                <button
                                    type="button"
                                    className="modal-close-button"
                                    onClick={closeDetails}
                                >
                                    ×
                                </button>

                            </div>


                            {/* CUSTOMER */}

                            <div className="detail-section">

                                <div className="section-heading">
                                    Customer
                                </div>


                                <div className="customer-detail-box">

                                    <div className="large-avatar">

                                        {getCustomerName(
                                            selectedOrder
                                        )
                                            .charAt(0)
                                            .toUpperCase()}

                                    </div>


                                    <div>

                                        <strong>
                                            {
                                                getCustomerName(
                                                    selectedOrder
                                                )
                                            }
                                        </strong>

                                        <span>
                                            {
                                                getCustomerEmail(
                                                    selectedOrder
                                                )
                                            }
                                        </span>

                                        <small>
                                            Customer ID:{" "}
                                            {getCustomerId(
                                                selectedOrder
                                            )}
                                        </small>

                                    </div>

                                </div>

                            </div>


                            {/* PRODUCTS */}

                            <div className="detail-section admin-delivery-expectations">

                                <div className="section-heading">
                                    Customer Delivery Time
                                </div>

                                {(() => {
                                    const sellers = new Map();
                                    getOrderItems(selectedOrder).forEach(item => {
                                        if (item.sellerId) {
                                            sellers.set(item.sellerId, {
                                                sellerId: item.sellerId,
                                                sellerName: item.sellerName || "Assigned seller",
                                            });
                                        }
                                    });
                                    const savedPlans = Array.isArray(selectedOrder.deliveryExpectations)
                                        ? selectedOrder.deliveryExpectations
                                        : [];
                                    savedPlans.forEach(plan => {
                                        if (plan.sellerId && !sellers.has(plan.sellerId)) {
                                            sellers.set(plan.sellerId, {
                                                sellerId: plan.sellerId,
                                                sellerName: plan.sellerName || "Assigned seller",
                                            });
                                        }
                                    });

                                    const rows = [...sellers.values()].map(seller => ({
                                        ...seller,
                                        ...(savedPlans.find(plan => plan.sellerId === seller.sellerId) || {}),
                                    }));

                                    if (rows.length === 0) {
                                        return <p className="delivery-time-empty">Seller allocation is not available for this older order.</p>;
                                    }

                                    return rows.map(plan => {
                                        const editing = deliveryPlan?.sellerId === plan.sellerId;
                                        const locked = ["CANCELLED", "DELIVERED"].includes(selectedOrder.orderStatus);
                                        const saving = updatingOrderId === `delivery-${selectedOrder.orderId}-${plan.sellerId}`;

                                        return (
                                            <article className="admin-delivery-time-card" key={plan.sellerId}>
                                                <div className="admin-delivery-time-summary">
                                                    <div>
                                                        <small>FROM {plan.sellerName || "ASSIGNED SELLER"}</small>
                                                        <strong>{formatExpectedDelivery(plan.expectedDeliveryAt)}</strong>
                                                        {plan.customerMessage && <span>{plan.customerMessage}</span>}
                                                    </div>
                                                    <button
                                                        type="button"
                                                        disabled={locked || saving}
                                                        onClick={() => setDeliveryPlan({
                                                            sellerId: plan.sellerId,
                                                            expectedDeliveryAt: toDateTimeInput(plan.expectedDeliveryAt),
                                                            customerMessage: plan.customerMessage || "",
                                                        })}
                                                    >
                                                        {plan.expectedDeliveryAt ? "Update time" : "Set time"}
                                                    </button>
                                                </div>

                                                {editing && (
                                                    <div className="admin-delivery-time-form">
                                                        <label>
                                                            Expected delivery date & time
                                                            <input
                                                                type="datetime-local"
                                                                min={toDateTimeInput()}
                                                                value={deliveryPlan.expectedDeliveryAt}
                                                                onChange={event => setDeliveryPlan(current => ({ ...current, expectedDeliveryAt: event.target.value }))}
                                                            />
                                                        </label>
                                                        <label>
                                                            Customer message (optional)
                                                            <input
                                                                maxLength="500"
                                                                placeholder="Example: Delivery partner will call before arrival."
                                                                value={deliveryPlan.customerMessage}
                                                                onChange={event => setDeliveryPlan(current => ({ ...current, customerMessage: event.target.value }))}
                                                            />
                                                        </label>
                                                        <div className="admin-delivery-time-actions">
                                                            <button type="button" onClick={handleDeliveryExpectationUpdate} disabled={saving}>
                                                                {saving ? "Saving..." : "Save delivery time"}
                                                            </button>
                                                            <button type="button" className="secondary" onClick={() => setDeliveryPlan(null)} disabled={saving}>
                                                                Cancel
                                                            </button>
                                                        </div>
                                                    </div>
                                                )}
                                            </article>
                                        );
                                    });
                                })()}

                            </div>

                            <div className="detail-section">

                                <div className="section-heading">
                                    Products
                                </div>


                                <div className="order-items-list">

                                    {getOrderItems(
                                        selectedOrder
                                    ).length === 0 ? (

                                        <div className="no-items">
                                            No item details available.
                                        </div>

                                    ) : (

                                        getOrderItems(
                                            selectedOrder
                                        ).map(
                                            (item, index) => (

                                                <div
                                                    className="order-item"
                                                    key={
                                                        item.id ||
                                                        index
                                                    }
                                                >

                                                    <div>

                                                        <strong>
                                                            {
                                                                item.productName ||
                                                                item.name ||
                                                                "Product"
                                                            }
                                                        </strong>

                                                        <span>
                                                            Qty:{" "}
                                                            {
                                                                item.quantity ||
                                                                1
                                                            }
                                                        </span>

                                                    </div>


                                                    <strong>

                                                        {formatCurrency(
                                                            item.totalPrice ||
                                                            item.total ||
                                                            (
                                                                Number(
                                                                    item.unitPrice ||
                                                                    item.price ||
                                                                    0
                                                                ) *
                                                                Number(
                                                                    item.quantity ||
                                                                    1
                                                                )
                                                            )
                                                        )}

                                                    </strong>

                                                </div>

                                            )
                                        )

                                    )}

                                </div>

                            </div>


                            {/* PAYMENT SUMMARY */}

                            <div className="detail-section">

                                <div className="section-heading">
                                    Payment Summary
                                </div>


                                <div className="summary-row">

                                    <span>
                                        Subtotal
                                    </span>

                                    <strong>
                                        {formatCurrency(
                                            selectedOrder.subtotal
                                        )}
                                    </strong>

                                </div>


                                <div className="summary-row">

                                    <span>
                                        Discount
                                    </span>

                                    <strong>
                                        {formatCurrency(
                                            selectedOrder.discount ||
                                            selectedOrder.discountAmount
                                        )}
                                    </strong>

                                </div>


                                <div className="summary-row">

                                    <span>
                                        Tax
                                    </span>

                                    <strong>
                                        {formatCurrency(
                                            selectedOrder.tax ||
                                            selectedOrder.taxAmount
                                        )}
                                    </strong>

                                </div>


                                <div className="summary-row">

                                    <span>
                                        Delivery
                                    </span>

                                    <strong>
                                        {formatCurrency(
                                            selectedOrder.deliveryCharge
                                        )}
                                    </strong>

                                </div>


                                <div className="summary-total">

                                    <span>
                                        Grand Total
                                    </span>

                                    <strong>
                                        {formatCurrency(
                                            selectedOrder.grandTotal ||
                                            selectedOrder.totalAmount ||
                                            selectedOrder.total
                                        )}
                                    </strong>

                                </div>

                            </div>


                            {/* SHIPPING */}

                            <div className="detail-section">

                                <div className="section-heading">
                                    Shipping Address
                                </div>

                                <div className="shipping-box">

                                    {
                                        selectedOrder.shippingAddress ||
                                        "No shipping address available."
                                    }

                                </div>

                            </div>


                            {/* STATUS TIMELINE */}

                            <div className="detail-section">

                                <div className="section-heading">
                                    Order Status
                                </div>


                                <div className="order-status-timeline">

                                    {ORDER_STATUS
                                        .filter(
                                            status =>
                                                status !==
                                                "CANCELLED"
                                        )
                                        .map(
                                            (
                                                status,
                                                index
                                            ) => {

                                                const currentStatus =
                                                    selectedOrder.orderStatus ||
                                                    "PENDING";


                                                const timelineStatuses =
                                                    ORDER_STATUS.filter(
                                                        item =>
                                                            item !==
                                                            "CANCELLED"
                                                    );


                                                const currentIndex =
                                                    timelineStatuses.indexOf(
                                                        currentStatus
                                                    );


                                                const isCancelled =
                                                    currentStatus ===
                                                    "CANCELLED";


                                                const isCompleted =
                                                    !isCancelled &&
                                                    index <
                                                    currentIndex;


                                                const isCurrent =
                                                    !isCancelled &&
                                                    index ===
                                                    currentIndex;


                                                return (

                                                    <div
                                                        className={
                                                            `timeline-step ${
                                                                isCompleted
                                                                    ? "completed"
                                                                    : ""
                                                            } ${
                                                                isCurrent
                                                                    ? "current"
                                                                    : ""
                                                            }`
                                                        }
                                                        key={
                                                            status
                                                        }
                                                    >

                                                        <div className="timeline-dot">

                                                            {isCompleted
                                                                ? "✓"
                                                                : index + 1}

                                                        </div>


                                                        <div className="timeline-content">

                                                            <strong>
                                                                {formatStatus(
                                                                    status
                                                                )}
                                                            </strong>


                                                            {isCurrent && (

                                                                <span>
                                                                    Current status
                                                                </span>

                                                            )}

                                                        </div>


                                                        {index <
                                                            timelineStatuses.length - 1 && (

                                                            <div className="timeline-line" />

                                                        )}

                                                    </div>

                                                );
                                            }
                                        )}


                                    {selectedOrder.orderStatus ===
                                        "CANCELLED" && (

                                        <div className="timeline-step cancelled current">

                                            <div className="timeline-dot">
                                                ×
                                            </div>

                                            <div className="timeline-content">

                                                <strong>
                                                    Cancelled
                                                </strong>

                                                <span>
                                                    Current status
                                                </span>

                                            </div>

                                        </div>

                                    )}

                                </div>

                            </div>


                            {/* =================================================
                               UPDATE STATUS
                            ================================================= */}

                            <div className="detail-section status-section">

                                <div className="section-heading">
                                    Update Order Status
                                </div>


                                <div className="status-update-box">


                                    <div className="status-current">

                                        <span>
                                            Current Status
                                        </span>

                                        <strong
                                            className={
                                                `order-status-badge ${
                                                    getStatusClass(
                                                        selectedOrder.orderStatus
                                                    )
                                                }`
                                            }
                                        >
                                            {formatStatus(
                                                selectedOrder.orderStatus
                                            )}
                                        </strong>

                                    </div>


                                    <div className="status-update-row">

                                        <select
                                            value={
                                                pendingStatus
                                            }
                                            disabled={
                                                updatingOrderId ===
                                                selectedOrder.orderId
                                            }
                                            onChange={event =>
                                                setPendingStatus(
                                                    event.target.value
                                                )
                                            }
                                        >

                                            {ORDER_STATUS.map(
                                                status => (

                                                    <option
                                                        key={
                                                            status
                                                        }
                                                        value={
                                                            status
                                                        }
                                                    >
                                                        {formatStatus(
                                                            status
                                                        )}
                                                    </option>

                                                )
                                            )}

                                        </select>


                                        <button
                                            type="button"
                                            className="update-status-button"
                                            disabled={
                                                updatingOrderId ===
                                                    selectedOrder.orderId ||
                                                pendingStatus ===
                                                    selectedOrder.orderStatus
                                            }
                                            onClick={() =>
                                                handleStatusUpdate(
                                                    selectedOrder.orderId,
                                                    pendingStatus
                                                )
                                            }
                                        >

                                            {updatingOrderId ===
                                                selectedOrder.orderId
                                                ? "Updating..."
                                                : "Update Status"}

                                        </button>

                                    </div>

                                </div>

                            </div>


                            {/* FOOTER */}

                            <div className="order-modal-footer">

                                <div>

                                    <span>
                                        Payment Status
                                    </span>

                                    <strong
                                        className={
                                            `payment-badge ${
                                                getStatusClass(
                                                    selectedOrder.paymentStatus
                                                )
                                            }`
                                        }
                                    >
                                        {
                                            selectedOrder.paymentStatus ||
                                            "PENDING"
                                        }
                                    </strong>

                                </div>


                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={closeDetails}
                                >
                                    Close
                                </button>

                            </div>

                        </div>

                    </div>

                )}

        </div>
    );
}

export default Orders;
