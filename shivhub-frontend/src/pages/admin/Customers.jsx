import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../services/api";
import SearchAutocomplete from "../../components/common/SearchAutocomplete/SearchAutocomplete";
import { getAdminSearchSuggestions } from "../../services/searchSuggestionService";

import "./Customers.css";

const API_URL = `${API_BASE_URL}/api/admin/customers`;

const getToken = () => {
    return localStorage.getItem("shivhub_token");
};

const getInitials = (name) => {
    if (!name) {
        return "U";
    }

    const words = name.trim().split(" ");

    if (words.length === 1) {
        return words[0].charAt(0).toUpperCase();
    }

    return (
        words[0].charAt(0) +
        words[words.length - 1].charAt(0)
    ).toUpperCase();
};

function Customers() {

    const navigate = useNavigate();

    const [customers, setCustomers] = useState([]);

    const [search, setSearch] = useState("");

    const [filter, setFilter] = useState("ALL");

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [actionLoading, setActionLoading] = useState(null);

    const [message, setMessage] = useState("");


    /*
     * =========================================================
     * LOAD CUSTOMERS
     * =========================================================
     */

    const loadCustomers = async (showLoader = false) => {

        try {

            if (showLoader) {
                setLoading(true);
            }

            setError("");

            const token = getToken();

            const response = await fetch(
                API_URL,
                {
                    method: "GET",

                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                }
            );

            if (!response.ok) {

                if (
                    response.status === 401 ||
                    response.status === 403
                ) {
                    throw new Error(
                        "You are not authorized to view customers."
                    );
                }

                throw new Error(
                    "Failed to load customers."
                );
            }

            const data = await response.json();

            setCustomers(
                Array.isArray(data)
                    ? data
                    : []
            );

        } catch (err) {

            console.error(
                "Customer loading error:",
                err
            );

            setError(
                err.message ||
                "Unable to load customers."
            );

        } finally {

            if (showLoader) {
                setLoading(false);
            }
        }
    };


    /*
     * =========================================================
     * INITIAL LOAD
     * =========================================================
     *
     * setLoading() is NOT called synchronously here.
     *
     * Initial state is already true.
     *
     * =========================================================
     */

    useEffect(() => {

        let cancelled = false;

        const loadInitialCustomers = async () => {

            try {

                const token = getToken();

                const response = await fetch(
                    API_URL,
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
                            "You are not authorized to view customers."
                        );
                    }

                    throw new Error(
                        "Failed to load customers."
                    );
                }


                const data =
                    await response.json();


                if (!cancelled) {

                    setCustomers(
                        Array.isArray(data)
                            ? data
                            : []
                    );

                    setLoading(false);
                }

            } catch (err) {

                if (!cancelled) {

                    console.error(
                        "Customer loading error:",
                        err
                    );

                    setError(
                        err.message ||
                        "Unable to load customers."
                    );

                    setLoading(false);
                }
            }
        };


        loadInitialCustomers();


        return () => {
            cancelled = true;
        };

    }, []);


    /*
     * =========================================================
     * BLOCK / UNBLOCK
     * =========================================================
     */

    const handleStatusChange = async (customer) => {

        try {

            setActionLoading(customer.id);

            setMessage("");

            setError("");

            const token = getToken();

            const action =
                customer.enabled
                    ? "block"
                    : "unblock";


            const response = await fetch(
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

                throw new Error(
                    `Unable to ${action} customer.`
                );
            }


            const updatedCustomer =
                await response.json();


            setCustomers(previous =>
                previous.map(item =>
                    item.id === customer.id
                        ? updatedCustomer
                        : item
                )
            );


            setMessage(
                customer.enabled
                    ? `${customer.name} has been blocked successfully.`
                    : `${customer.name} has been unblocked successfully.`
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

            setActionLoading(null);
        }
    };


    /*
     * =========================================================
     * CUSTOMER COUNTS
     * =========================================================
     */

    const counts = useMemo(() => {

        const total =
            customers.length;


        const active =
            customers.filter(
                customer =>
                    customer.enabled === true
            ).length;


        const blocked =
            customers.filter(
                customer =>
                    customer.enabled === false
            ).length;


        return {
            total,
            active,
            blocked
        };

    }, [customers]);


    /*
     * =========================================================
     * SEARCH + FILTER
     * =========================================================
     */

    const filteredCustomers = useMemo(() => {

        const searchValue =
            search.trim().toLowerCase();


        return customers.filter(
            customer => {

                const matchesSearch =
                    !searchValue ||
                    customer.name
                        ?.toLowerCase()
                        .includes(searchValue) ||
                    customer.email
                        ?.toLowerCase()
                        .includes(searchValue) ||
                    customer.mobile
                        ?.toLowerCase()
                        .includes(searchValue);


                const matchesFilter =
                    filter === "ALL" ||
                    (
                        filter === "ACTIVE" &&
                        customer.enabled === true
                    ) ||
                    (
                        filter === "BLOCKED" &&
                        customer.enabled === false
                    );


                return (
                    matchesSearch &&
                    matchesFilter
                );
            }
        );

    }, [
        customers,
        search,
        filter
    ]);


    return (

        <div className="customers-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <div className="customers-header">

                <div>

                    <span className="customers-kicker">
                        SHIVHUB ADMIN
                    </span>

                    <h1>
                        Customer Management
                    </h1>

                    <p>
                        Manage registered customers
                        and their accounts.
                    </p>

                </div>


                <button
                    className="refresh-customers-button"
                    onClick={() =>
                        loadCustomers(true)
                    }
                    disabled={loading}
                >
                    ↻ Refresh
                </button>

            </div>


            {/* =================================================
                SUCCESS MESSAGE
            ================================================= */}

            {message && (

                <div className="customer-success-message">

                    ✓ {message}

                </div>

            )}


            {/* =================================================
                ERROR MESSAGE
            ================================================= */}

            {error && (

                <div className="customer-error-message">

                    <span>
                        ⚠ {error}
                    </span>

                    <button
                        onClick={() =>
                            setError("")
                        }
                    >
                        ×
                    </button>

                </div>

            )}


            {/* =================================================
                STATISTICS
            ================================================= */}

            <div className="customer-stats">


                <div className="customer-stat-card">

                    <div className="customer-stat-icon">
                        👥
                    </div>

                    <div>

                        <span>
                            Total Customers
                        </span>

                        <strong>
                            {counts.total}
                        </strong>

                    </div>

                </div>


                <div className="customer-stat-card active">

                    <div className="customer-stat-icon">
                        ✓
                    </div>

                    <div>

                        <span>
                            Active Customers
                        </span>

                        <strong>
                            {counts.active}
                        </strong>

                    </div>

                </div>


                <div className="customer-stat-card blocked">

                    <div className="customer-stat-icon">
                        !
                    </div>

                    <div>

                        <span>
                            Blocked Customers
                        </span>

                        <strong>
                            {counts.blocked}
                        </strong>

                    </div>

                </div>

            </div>


            {/* =================================================
                CUSTOMER TABLE CARD
            ================================================= */}

            <div className="customers-card">


                <div className="customers-card-header">

                    <div>

                        <h2>
                            All Customers
                        </h2>

                        <p>
                            Customers registered
                            on ShivHub.
                        </p>

                    </div>


                    <div className="customer-search">

                        <span>
                            🔍
                        </span>

                        <SearchAutocomplete
                            value={search}
                            onChange={setSearch}
                            onSelect={suggestion => setSearch(suggestion.label || "")}
                            onEnterWithoutSelection={() => {}}
                            fetchSuggestions={query => getAdminSearchSuggestions("CUSTOMER", query)}
                            placeholder="Search name, email or mobile..."
                            className="admin-customer-autocomplete"
                        />


                        {search && (

                            <button
                                type="button"
                                onClick={() =>
                                    setSearch("")
                                }
                            >
                                ×
                            </button>

                        )}

                    </div>

                </div>


                {/* =================================================
                    FILTER TABS
                ================================================= */}

                <div className="customer-tabs">


                    <button
                        type="button"
                        className={
                            filter === "ALL"
                                ? "customer-tab active"
                                : "customer-tab"
                        }
                        onClick={() =>
                            setFilter("ALL")
                        }
                    >
                        All

                        <span>
                            {counts.total}
                        </span>

                    </button>


                    <button
                        type="button"
                        className={
                            filter === "ACTIVE"
                                ? "customer-tab active"
                                : "customer-tab"
                        }
                        onClick={() =>
                            setFilter("ACTIVE")
                        }
                    >
                        Active

                        <span>
                            {counts.active}
                        </span>

                    </button>


                    <button
                        type="button"
                        className={
                            filter === "BLOCKED"
                                ? "customer-tab active"
                                : "customer-tab"
                        }
                        onClick={() =>
                            setFilter("BLOCKED")
                        }
                    >
                        Blocked

                        <span>
                            {counts.blocked}
                        </span>

                    </button>

                </div>


                {/* =================================================
                    LOADING
                ================================================= */}

                {loading && (

                    <div className="customers-loading">

                        <div className="loading-spinner"></div>

                        <p>
                            Loading customers...
                        </p>

                    </div>

                )}


                {/* =================================================
                    TABLE
                ================================================= */}

                {!loading &&
                    filteredCustomers.length > 0 && (

                        <div className="customers-table-wrapper">

                            <table className="customers-table">

                                <thead>

                                    <tr>

                                        <th>
                                            CUSTOMER
                                        </th>

                                        <th>
                                            EMAIL
                                        </th>

                                        <th>
                                            MOBILE
                                        </th>

                                        <th>
                                            STATUS
                                        </th>

                                        <th>
                                            ACTIONS
                                        </th>

                                    </tr>

                                </thead>


                                <tbody>

                                    {filteredCustomers.map(
                                        customer => (

                                            <tr
                                                key={
                                                    customer.id
                                                }
                                            >

                                                <td>

                                                    <div className="customer-profile-cell">

                                                        <div className="customer-avatar">

                                                            {
                                                                getInitials(
                                                                    customer.name
                                                                )
                                                            }

                                                        </div>


                                                        <div>

                                                            <strong>
                                                                {
                                                                    customer.name
                                                                }
                                                            </strong>

                                                            <small>
                                                                Customer #
                                                                {
                                                                    customer.id
                                                                }
                                                            </small>

                                                        </div>

                                                    </div>

                                                </td>


                                                <td>

                                                    <span className="customer-email">
                                                        {
                                                            customer.email
                                                        }
                                                    </span>

                                                </td>


                                                <td>

                                                    <span className="customer-mobile">
                                                        {
                                                            customer.mobile ||
                                                            "-"
                                                        }
                                                    </span>

                                                </td>


                                                <td>

                                                    <span
                                                        className={
                                                            customer.enabled
                                                                ? "customer-status active"
                                                                : "customer-status blocked"
                                                        }
                                                    >

                                                        <i></i>

                                                        {
                                                            customer.enabled
                                                                ? "ACTIVE"
                                                                : "BLOCKED"
                                                        }

                                                    </span>

                                                </td>


                                                <td>

                                                    <div className="customer-actions">

                                                        <button
                                                            type="button"
                                                            className="view-customer-button"
                                                            onClick={() =>
                                                                navigate(
                                                                    `/admin/customers/${customer.id}`
                                                                )
                                                            }
                                                        >
                                                            View
                                                        </button>


                                                        <button
                                                            type="button"
                                                            className={
                                                                customer.enabled
                                                                    ? "block-customer-button"
                                                                    : "unblock-customer-button"
                                                            }
                                                            disabled={
                                                                actionLoading ===
                                                                customer.id
                                                            }
                                                            onClick={() =>
                                                                handleStatusChange(
                                                                    customer
                                                                )
                                                            }
                                                        >

                                                            {
                                                                actionLoading ===
                                                                customer.id
                                                                    ? "..."
                                                                    : customer.enabled
                                                                        ? "Block"
                                                                        : "Unblock"
                                                            }

                                                        </button>

                                                    </div>

                                                </td>

                                            </tr>

                                        )
                                    )}

                                </tbody>

                            </table>

                        </div>

                    )}


                {/* =================================================
                    EMPTY
                ================================================= */}

                {!loading &&
                    filteredCustomers.length === 0 && (

                        <div className="customers-empty">

                            <div className="customers-empty-icon">
                                👥
                            </div>

                            <h3>
                                No customers found
                            </h3>

                            <p>
                                No customers match
                                your current search
                                or filter.
                            </p>

                        </div>

                    )}

            </div>


            {/* =================================================
                INFO
            ================================================= */}

            <div className="customers-info-box">

                <div className="customers-info-icon">
                    💡
                </div>

                <div>

                    <h3>
                        Customer Management
                    </h3>

                    <p>
                        Customer data is loaded directly
                        from the ShivHub backend and
                        MySQL users table.
                    </p>

                </div>

            </div>


        </div>
    );
}

export default Customers;
