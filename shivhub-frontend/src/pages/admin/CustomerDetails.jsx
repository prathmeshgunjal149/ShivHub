import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { API_BASE_URL } from "../../services/api";

import "./CustomerDetails.css";


/*
 * =========================================================
 * SHIVHUB ADMIN - CUSTOMER DETAILS
 * =========================================================
 */

const API_URL = `${API_BASE_URL}/api/admin/customers`;


/*
 * =========================================================
 * GET JWT TOKEN
 * =========================================================
 */

const getToken = () => {

    return localStorage.getItem(
        "shivhub_token"
    );

};


/*
 * =========================================================
 * GET CUSTOMER INITIALS
 * =========================================================
 */

const getInitials = (name) => {

    if (!name) {

        return "U";

    }


    const words =
        name.trim().split(" ");


    if (words.length === 1) {

        return words[0]
            .charAt(0)
            .toUpperCase();

    }


    return (

        words[0].charAt(0) +

        words[
            words.length - 1
        ].charAt(0)

    ).toUpperCase();

};

const formatCurrency = (value) => {
    return Number(value || 0).toLocaleString("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    });
};

const formatDateTime = (value) => {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
};


/*
 * =========================================================
 * CUSTOMER DETAILS COMPONENT
 * =========================================================
 */

function CustomerDetails() {


    const { id } = useParams();

    const navigate = useNavigate();


    /*
     * =====================================================
     * STATE
     * =====================================================
     */

    const [customer, setCustomer] =
        useState(null);


    const [summary, setSummary] =
        useState(null);


    const [purchases, setPurchases] =
        useState([]);


    const [loading, setLoading] =
        useState(true);


    const [error, setError] =
        useState("");


    const [actionLoading, setActionLoading] =
        useState(false);


    const [message, setMessage] =
        useState("");


    /*
     * =====================================================
     * LOAD CUSTOMER
     * =====================================================
     *
     * IMPORTANT:
     *
     * We do NOT call setLoading(true)
     * inside the initial useEffect.
     *
     * Initial loading state is already true.
     *
     * This avoids React cascading-render warning.
     *
     * =====================================================
     */

    useEffect(() => {


        let cancelled = false;


        const loadCustomer = async () => {

            try {

                const token =
                    getToken();


                const response =
                    await fetch(
                        `${API_URL}/${id}/details`,
                        {
                            method: "GET",

                            headers: {
                                Authorization:
                                    `Bearer ${token}`,

                                "Content-Type":
                                    "application/json"
                            }
                        }
                    );


                if (!response.ok) {


                    if (
                        response.status === 401 ||
                        response.status === 403
                    ) {

                        throw new Error(
                            "You are not authorized to view this customer."
                        );

                    }


                    if (
                        response.status === 404
                    ) {

                        throw new Error(
                            "Customer not found."
                        );

                    }


                    throw new Error(
                        "Failed to load customer."
                    );

                }


                const data =
                    await response.json();


                if (!cancelled) {

                    setCustomer(data.customer || data);

                    setSummary(data.summary || null);

                    setPurchases(
                        Array.isArray(data.purchases)
                            ? data.purchases
                            : []
                    );

                    setError("");

                    setLoading(false);

                }


            } catch (err) {


                console.error(
                    "Customer details error:",
                    err
                );


                if (!cancelled) {

                    setError(
                        err.message ||
                        "Unable to load customer."
                    );

                    setLoading(false);

                }

            }

        };


        loadCustomer();


        /*
         * Cleanup
         */

        return () => {

            cancelled = true;

        };


    }, [id]);


    /*
     * =====================================================
     * RETRY CUSTOMER LOAD
     * =====================================================
     */

    const handleRetry = async () => {


        try {

            setLoading(true);

            setError("");


            const token =
                getToken();


            const response =
                await fetch(
                    `${API_URL}/${id}/details`,
                    {
                        method: "GET",

                        headers: {
                            Authorization:
                                `Bearer ${token}`,

                            "Content-Type":
                                "application/json"
                        }
                    }
                );


            if (!response.ok) {

                if (
                    response.status === 401 ||
                    response.status === 403
                ) {

                    throw new Error(
                        "You are not authorized to view this customer."
                    );

                }


                if (
                    response.status === 404
                ) {

                    throw new Error(
                        "Customer not found."
                    );

                }


                throw new Error(
                    "Failed to load customer."
                );

            }


            const data =
                await response.json();


            setCustomer(data.customer || data);

            setSummary(data.summary || null);

            setPurchases(
                Array.isArray(data.purchases)
                    ? data.purchases
                    : []
            );

            setError("");


        } catch (err) {

            console.error(
                "Retry customer error:",
                err
            );


            setError(
                err.message ||
                "Unable to load customer."
            );


        } finally {

            setLoading(false);

        }

    };


    /*
     * =====================================================
     * BLOCK / UNBLOCK CUSTOMER
     * =====================================================
     */

    const handleStatusChange = async () => {


        if (!customer) {

            return;

        }


        try {


            setActionLoading(true);

            setMessage("");

            setError("");


            const token =
                getToken();


            const action =
                customer.enabled
                    ? "block"
                    : "unblock";


            const response =
                await fetch(
                    `${API_URL}/${customer.id}/${action}`,
                    {
                        method: "PUT",

                        headers: {
                            Authorization:
                                `Bearer ${token}`,

                            "Content-Type":
                                "application/json"
                        }
                    }
                );


            if (!response.ok) {

                if (
                    response.status === 401 ||
                    response.status === 403
                ) {

                    throw new Error(
                        "You are not authorized to perform this action."
                    );

                }


                throw new Error(
                    `Unable to ${action} customer.`
                );

            }


            const updatedCustomer =
                await response.json();


            /*
             * Update UI immediately
             */

            setCustomer(
                updatedCustomer
            );


            /*
             * Success message
             */

            setMessage(

                customer.enabled

                    ? "Customer blocked successfully."

                    : "Customer unblocked successfully."

            );


        } catch (err) {


            console.error(
                "Customer status error:",
                err
            );


            setError(
                err.message ||
                "Unable to update customer."
            );


        } finally {

            setActionLoading(false);

        }

    };


    /*
     * =====================================================
     * LOADING SCREEN
     * =====================================================
     */

    if (loading) {

        return (

            <div className="customer-details-page">


                <div className="customer-details-loading">


                    <div className="details-spinner"></div>


                    <p>
                        Loading customer...
                    </p>


                </div>


            </div>

        );

    }


    /*
     * =====================================================
     * ERROR SCREEN
     * =====================================================
     */

    if (error && !customer) {

        return (

            <div className="customer-details-page">


                <button
                    type="button"
                    className="back-customer-button"
                    onClick={() =>
                        navigate(
                            "/admin/customers"
                        )
                    }
                >

                    ← Back to Customers

                </button>


                <div className="customer-details-error">


                    <div className="customer-error-icon">
                        ⚠
                    </div>


                    <h2>
                        Unable to load customer
                    </h2>


                    <p>
                        {error}
                    </p>


                    <button
                        type="button"
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
     * MAIN PAGE
     * =====================================================
     */

    return (

        <div className="customer-details-page">


            {/* =================================================
                TOP NAVIGATION
            ================================================= */}

            <div className="customer-details-top">


                <button
                    type="button"
                    className="back-customer-button"
                    onClick={() =>
                        navigate(
                            "/admin/customers"
                        )
                    }
                >

                    ← Back to Customers

                </button>


            </div>


            {/* =================================================
                SUCCESS MESSAGE
            ================================================= */}

            {message && (

                <div className="customer-details-success">

                    ✓ {message}

                </div>

            )}


            {/* =================================================
                ERROR MESSAGE
            ================================================= */}

            {error && customer && (

                <div className="customer-details-inline-error">

                    ⚠ {error}

                </div>

            )}


            {/* =================================================
                CUSTOMER PROFILE
            ================================================= */}

            <section className="customer-profile-card">


                {/* AVATAR */}

                <div className="customer-details-avatar">

                    {
                        customer.profilePhotoUrl ? (

                            <img
                                src={customer.profilePhotoUrl}
                                alt={`${customer.name || "Customer"} profile`}
                            />

                        ) : (

                            getInitials(
                                customer.name
                            )

                        )
                    }

                </div>


                {/* CUSTOMER BASIC INFORMATION */}

                <div className="customer-profile-main">


                    <span className="customer-profile-kicker">

                        CUSTOMER #{customer.id}

                    </span>


                    <h1>

                        {customer.name || "Unknown Customer"}

                    </h1>


                    <p>

                        {customer.email || "-"}

                    </p>


                    {/* STATUS */}

                    <span
                        className={
                            customer.enabled
                                ? "details-status active"
                                : "details-status blocked"
                        }
                    >

                        <i></i>


                        {
                            customer.enabled
                                ? "ACTIVE"
                                : "BLOCKED"
                        }

                    </span>


                </div>


                {/* ACTION */}

                <div className="customer-profile-actions">


                    <button
                        type="button"
                        className={
                            customer.enabled
                                ? "details-block-button"
                                : "details-unblock-button"
                        }
                        disabled={
                            actionLoading
                        }
                        onClick={
                            handleStatusChange
                        }
                    >

                        {
                            actionLoading

                                ? "Updating..."

                                : customer.enabled

                                    ? "Block Customer"

                                    : "Unblock Customer"
                        }

                    </button>


                </div>

            </section>


            {/* =================================================
                CUSTOMER INFORMATION
            ================================================= */}

            <section className="customer-information-grid">


                {/* FULL NAME */}

                <div className="customer-information-card">


                    <span>
                        FULL NAME
                    </span>


                    <strong>

                        {
                            customer.name ||
                            "-"
                        }

                    </strong>


                </div>


                {/* EMAIL */}

                <div className="customer-information-card">


                    <span>
                        EMAIL ADDRESS
                    </span>


                    <strong>

                        {
                            customer.email ||
                            "-"
                        }

                    </strong>


                </div>


                {/* MOBILE */}

                <div className="customer-information-card">


                    <span>
                        MOBILE NUMBER
                    </span>


                    <strong>

                        {
                            customer.mobile ||
                            "-"
                        }

                    </strong>


                </div>


                {/* TOTAL PURCHASE */}

                <div className="customer-information-card">


                    <span>
                        TOTAL PURCHASE
                    </span>


                    <strong>

                        {
                            formatCurrency(
                                summary?.totalPurchaseAmount
                            )
                        }

                    </strong>


                </div>

            </section>


            {/* =================================================
                ACCOUNT INFORMATION
            ================================================= */}

            <section className="customer-details-card">


                <div className="customer-details-card-header">


                    <div>


                        <h2>
                            Account Information
                        </h2>


                        <p>
                            Customer account details
                            from ShivHub.
                        </p>


                    </div>


                    <div>


                        <span>
                            Account Created
                        </span>


                        <strong>

                            {
                                formatDateTime(
                                    customer.createdAt
                                )
                            }

                        </strong>


                    </div>


                    <div>


                        <span>
                            Last Login
                        </span>


                        <strong>

                            {
                                formatDateTime(
                                    customer.lastLoginAt
                                )
                            }

                        </strong>


                    </div>


                    <div>


                        <span>
                            Last Purchase
                        </span>


                        <strong>

                            {
                                formatDateTime(
                                    summary?.lastPurchaseAt
                                )
                            }

                        </strong>


                    </div>


                    <div>


                        <span>
                            Orders / Bills
                        </span>


                        <strong>

                            {
                                summary?.totalOrdersAndBills ??
                                0
                            }

                        </strong>


                    </div>


                </div>


                <div className="customer-account-grid">


                    {/* CUSTOMER ID */}

                    <div>


                        <span>
                            Customer ID
                        </span>


                        <strong>

                            #{customer.id}

                        </strong>


                    </div>


                    {/* ROLE */}

                    <div>


                        <span>
                            Role
                        </span>


                        <strong>

                            {
                                customer.role ||
                                "CUSTOMER"
                            }

                        </strong>


                    </div>


                    {/* STATUS */}

                    <div>


                        <span>
                            Status
                        </span>


                        <strong>

                            {
                                customer.status ||
                                "-"
                            }

                        </strong>


                    </div>


                    {/* ACCOUNT ACCESS */}

                    <div>


                        <span>
                            Account Access
                        </span>


                        <strong>

                            {
                                customer.enabled
                                    ? "Enabled"
                                    : "Disabled"
                            }

                        </strong>


                    </div>


                </div>

            </section>


            {/* =================================================
                ORDER ACTIVITY
            ================================================= */}

            <section className="customer-details-card">


                <div className="customer-details-card-header">


                    <div>


                        <h2>
                            Order Activity
                        </h2>


                        <p>
                            Online orders, offline bills
                            and walk-in purchases linked
                            with this customer.
                        </p>


                    </div>


                </div>


                <div className="customer-purchase-summary">


                    <div>
                        <span>Online Orders</span>
                        <strong>{summary?.onlineOrders ?? 0}</strong>
                        <small>{formatCurrency(summary?.onlinePurchaseAmount)}</small>
                    </div>


                    <div>
                        <span>Offline / Walk-in Bills</span>
                        <strong>{summary?.offlineBills ?? 0}</strong>
                        <small>{formatCurrency(summary?.offlinePurchaseAmount)}</small>
                    </div>


                    <div>
                        <span>Total Purchase Amount</span>
                        <strong>{formatCurrency(summary?.totalPurchaseAmount)}</strong>
                        <small>Real order + bill data</small>
                    </div>


                </div>


                {
                    purchases.length > 0 && (

                        <div className="customer-purchase-table-wrap">


                            <table className="customer-purchase-table">


                                <thead>

                                    <tr>
                                        <th>Date</th>
                                        <th>Type</th>
                                        <th>Reference</th>
                                        <th>Status</th>
                                        <th>Payment</th>
                                        <th>GST</th>
                                        <th>Discount</th>
                                        <th>Amount</th>
                                    </tr>

                                </thead>


                                <tbody>

                                    {
                                        purchases.map((purchase, index) => (

                                            <tr key={`${purchase.type}-${purchase.id}-${index}`}>
                                                <td>{formatDateTime(purchase.date)}</td>
                                                <td>
                                                    <span className={`purchase-type ${String(purchase.type || "").toLowerCase()}`}>
                                                        {purchase.type || "-"}
                                                    </span>
                                                </td>
                                                <td>{purchase.reference || "-"}</td>
                                                <td>{purchase.status || "-"}</td>
                                                <td>{purchase.paymentStatus || "-"}</td>
                                                <td>{formatCurrency(purchase.tax)}</td>
                                                <td>{formatCurrency(purchase.discount)}</td>
                                                <td>
                                                    <strong>{formatCurrency(purchase.amount)}</strong>
                                                </td>
                                            </tr>

                                        ))
                                    }

                                </tbody>


                            </table>


                        </div>

                    )
                }


                {
                    purchases.length === 0 && (

                <div className="customer-coming-soon">


                    <div>
                        🛍️
                    </div>


                    <h3>
                        Order history coming next
                    </h3>


                    <p>
                        Once the ShivHub order module
                        is connected, this section will
                        show orders, purchases, totals
                        and recent activity.
                    </p>


                </div>

                    )
                }


            </section>


        </div>

    );

}


export default CustomerDetails;
