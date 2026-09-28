import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./SellerOrders.css";
import "./PremiumPolish.css";

const statuses = ["PROCESSING", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED"];
const paymentStatuses = ["PENDING", "PAID", "REFUNDED"];
const label = value => String(value || "PENDING").replaceAll("_", " ");
const money = value =>
    `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
const localDateTime = value => {
    const date = value ? new Date(value) : new Date(Date.now() + 2 * 60 * 60 * 1000);
    if (Number.isNaN(date.getTime())) return "";
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};
const expectedLabel = value => {
    if (!value) return "Set expected date & time";
    const target = new Date(value);
    if (Number.isNaN(target.getTime())) return "Delivery time unavailable";
    const hours = Math.ceil((target.getTime() - Date.now()) / 3600000);
    const dateText = target.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
    const timeText = target.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
    return hours > 0 && hours <= 24 ? `Expected within ${hours} hour${hours === 1 ? "" : "s"} · ${timeText}` : `Expected by ${dateText}, ${timeText}`;
};

const serialOptions = item => {
    const map = new Map();
    [...(item.selectedSerials || []), ...(item.availableSerials || [])].forEach(serial => {
        if (serial?.serialId) map.set(String(serial.serialId), serial);
    });
    return [...map.values()];
};

export default function SellerOrders() {
    const [orders, setOrders] = useState([]);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [saving, setSaving] = useState("");
    const [action, setAction] = useState(null);
    const [deliveryPlan, setDeliveryPlan] = useState(null);

    const load = () =>
        api.get("/api/seller/orders")
            .then(({ data }) => setOrders(Array.isArray(data) ? data : []))
            .catch(err => setError(err?.response?.data?.message || "Unable to load orders."));

    useEffect(() => {
        void load();
    }, []);

    const pending = useMemo(
        () => orders.filter(order => order.sellerRequestedStatus).length,
        [orders]
    );

    const startAction = (order, status) => {
        const serials = {};
        (order.items || []).forEach(item => {
            serials[item.orderItemId] = (item.selectedSerials || [])
                .map(serial => String(serial.serialId));
        });
        setAction({
            orderId: order.id,
            status,
            deliveryPersonName: order.deliveryPersonName || "",
            deliveryPersonMobile: order.deliveryPersonMobile || "",
            serials
        });
        setError("");
        setMessage("");
    };

    const updateSerials = (itemId, selectedValues) => {
        setAction(previous => ({
            ...previous,
            serials: {
                ...previous.serials,
                [itemId]: selectedValues
            }
        }));
    };

    const buildSerialSelections = order => {
        return (order.items || [])
            .filter(item => serialOptions(item).length > 0)
            .map(item => ({
                orderItemId: item.orderItemId,
                serialIds: (action?.serials?.[item.orderItemId] || []).map(Number)
            }));
    };

    const validateAction = order => {
        if (!action?.status) return "Select order status.";

        if (["PACKED", "SHIPPED", "OUT_FOR_DELIVERY"].includes(action.status)) {
            for (const item of order.items || []) {
                const options = serialOptions(item);
                if (!options.length) continue;
                const selected = action.serials?.[item.orderItemId] || [];
                if (selected.length !== Number(item.quantity || 0)) {
                    return `Select exactly ${item.quantity} IMEI/serial for ${item.productName}.`;
                }
            }
        }

        if (action.status === "OUT_FOR_DELIVERY") {
            if (!action.deliveryPersonName.trim()) return "Delivery boy name is required.";
            if (!/^[6-9][0-9]{9}$/.test(action.deliveryPersonMobile.trim())) {
                return "Enter valid 10-digit delivery boy mobile number.";
            }
        }

        return "";
    };

    const sendRequest = async order => {
        const validation = validateAction(order);
        if (validation) {
            setError(validation);
            return;
        }

        try {
            setSaving(order.id);
            setError("");
            setMessage("");
            await api.put(`/api/seller/orders/${order.id}/status-request`, {
                orderStatus: action.status,
                deliveryPersonName: action.deliveryPersonName,
                deliveryPersonMobile: action.deliveryPersonMobile,
                serialSelections: buildSerialSelections(order)
            });
            setAction(null);
            setMessage("Status request sent to admin. Customer mail goes only after admin approval.");
            await load();
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to send request.");
        } finally {
            setSaving("");
        }
    };

    const downloadInvoice = async order => {
        try {
            setSaving(`invoice-${order.id}`);
            const { data } = await api.get(`/api/seller/orders/${order.id}/invoice`, {
                responseType: "blob"
            });
            const url = URL.createObjectURL(new Blob([data], { type: "application/pdf" }));
            const link = document.createElement("a");
            link.href = url;
            link.download = `${order.orderNumber || `order-${order.id}`}-invoice.pdf`;
            link.click();
            window.setTimeout(() => URL.revokeObjectURL(url), 30000);
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to download invoice.");
        } finally {
            setSaving("");
        }
    };

    const updatePaymentStatus = async (order, paymentStatus) => {
        if (!order?.id || !paymentStatus || paymentStatus === order.paymentStatus) return;

        try {
            setSaving(`payment-${order.id}`);
            setError("");
            setMessage("");

            await api.put(`/api/seller/orders/${order.id}/payment-status`, {
                paymentStatus
            });

            setMessage(`Payment status updated to ${label(paymentStatus)}.`);
            await load();
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to update payment status.");
        } finally {
            setSaving("");
        }
    };

    const startDeliveryPlan = order => {
        setDeliveryPlan({
            orderId: order.id,
            expectedDeliveryAt: localDateTime(order.expectedDeliveryAt),
            customerMessage: order.expectedDeliveryMessage || ""
        });
        setError("");
        setMessage("");
    };

    const saveDeliveryPlan = async order => {
        if (!deliveryPlan?.expectedDeliveryAt) {
            setError("Select the expected delivery date and time.");
            return;
        }
        try {
            setSaving(`eta-${order.id}`);
            setError("");
            await api.put(`/api/seller/orders/${order.id}/delivery-expectation`, {
                expectedDeliveryAt: deliveryPlan.expectedDeliveryAt,
                customerMessage: deliveryPlan.customerMessage.trim() || null
            });
            setDeliveryPlan(null);
            setMessage("Expected delivery time updated. The customer can see the new promise immediately.");
            await load();
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to update expected delivery time.");
        } finally {
            setSaving("");
        }
    };

    return (
        <div className="seller-orders-page">
            <SellerSidebar />
            <main className="seller-orders-main">
                <header className="seller-orders-header">
                    <div>
                        <span>FULFILMENT CENTRE</span>
                        <h1>Online order packing & dispatch</h1>
                        <p>Select IMEI, prepare bill, add delivery boy details and request admin approval.</p>
                    </div>
                    <button type="button" onClick={load}>Refresh</button>
                </header>

                {error && <div className="order-alert">{error}</div>}
                {message && <div className="order-success">{message}</div>}

                <section className="order-kpis">
                    <article><small>Assigned orders</small><strong>{orders.length}</strong></article>
                    <article><small>Waiting for approval</small><strong>{pending}</strong></article>
                    <article><small>IMEI + Bill flow</small><strong>Ready</strong></article>
                </section>

                <section className="seller-order-list">
                    {orders.length === 0 ? (
                        <div className="order-empty">No online orders are currently assigned to your shop.</div>
                    ) : orders.map(order => {
                        const active = action?.orderId === order.id;
                        const planOpen = deliveryPlan?.orderId === order.id;
                        const locked = Boolean(order.sellerRequestedStatus)
                            || ["DELIVERED", "CANCELLED"].includes(order.orderStatus);
                        const finalized = ["DELIVERED", "CANCELLED"].includes(order.orderStatus);

                        return (
                            <article className="seller-order-card" key={order.id}>
                                <div className="order-card-top">
                                    <div>
                                        <span className="order-number">{order.orderNumber}</span>
                                        <small>{new Date(order.createdAt).toLocaleString("en-IN")}</small>
                                    </div>
                                    <span className={`order-pill ${String(order.orderStatus).toLowerCase()}`}>
                                        {label(order.orderStatus)}
                                    </span>
                                </div>

                                <div className="order-card-data">
                                    <span><small>Customer</small><b>{order.customerName || "Customer"}</b></span>
                                    <span><small>Mobile</small><b>{order.customerMobile || "—"}</b></span>
                                    <span><small>Payment</small><b>{label(order.paymentStatus)}</b></span>
                                    <span><small>Items</small><b>{order.itemCount}</b></span>
                                    <span><small>Seller value</small><b>{money(order.sellerTotal)}</b></span>
                                </div>

                                <section className="order-delivery-plan" aria-label="Expected delivery time">
                                    <div>
                                        <small>CUSTOMER DELIVERY PROMISE</small>
                                        <strong>{expectedLabel(order.expectedDeliveryAt)}</strong>
                                        <span>{order.expectedDeliveryMessage || "Set a realistic time or date for this customer."}</span>
                                    </div>
                                    <button
                                        type="button"
                                        disabled={finalized || saving === `eta-${order.id}`}
                                        onClick={() => startDeliveryPlan(order)}
                                    >
                                        {order.expectedDeliveryAt ? "Update time" : "Set time"}
                                    </button>
                                </section>

                                {planOpen && (
                                    <form className="delivery-plan-form" onSubmit={event => { event.preventDefault(); void saveDeliveryPlan(order); }}>
                                        <label>Expected delivery date & time
                                            <input required type="datetime-local" min={localDateTime()} value={deliveryPlan.expectedDeliveryAt}
                                                onChange={event => setDeliveryPlan(current => ({ ...current, expectedDeliveryAt: event.target.value }))} />
                                        </label>
                                        <label>Customer message (optional)
                                            <input maxLength="500" placeholder="Example: Delivery after 6 PM due to route delay"
                                                value={deliveryPlan.customerMessage}
                                                onChange={event => setDeliveryPlan(current => ({ ...current, customerMessage: event.target.value }))} />
                                        </label>
                                        <div><button disabled={saving === `eta-${order.id}`}>Save expected time</button><button type="button" className="cancel" onClick={() => setDeliveryPlan(null)}>Cancel</button></div>
                                    </form>
                                )}

                                <div className="payment-update-panel">
                                    <div>
                                        <small>PAYMENT RECEIVED?</small>
                                        <strong>Update payment status after money is received</strong>
                                        <span>Use PAID for cash/UPI received. REFUNDED only for cancelled orders.</span>
                                    </div>
                                    <select
                                        value={order.paymentStatus || "PENDING"}
                                        disabled={saving === `payment-${order.id}`}
                                        onChange={event => updatePaymentStatus(order, event.target.value)}
                                    >
                                        {paymentStatuses.map(status => (
                                            <option value={status} key={status}>
                                                {label(status)}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {order.shippingAddress && (
                                    <div className="order-address">
                                        <small>Delivery address</small>
                                        <p>{order.shippingAddress}</p>
                                    </div>
                                )}

                                <div className="seller-order-items">
                                    {(order.items || []).map(item => {
                                        const options = serialOptions(item);
                                        const selected = action?.serials?.[item.orderItemId] || [];

                                        return (
                                            <div className="seller-order-item" key={item.orderItemId}>
                                                <div>
                                                    <strong>{item.productName}</strong>
                                                    <span>Qty {item.quantity} × {money(item.unitPrice)} = {money(item.totalPrice)}</span>
                                                </div>

                                                {item.assignedSerialSummary && (
                                                    <p className="selected-imei">
                                                        Selected IMEI/Serial: {item.assignedSerialSummary}
                                                    </p>
                                                )}

                                                {active && options.length > 0 && ["PACKED", "SHIPPED", "OUT_FOR_DELIVERY"].includes(action.status) && (
                                                    <label className="imei-select">
                                                        Select IMEI / Serial
                                                        <select
                                                            multiple
                                                            size={Math.min(5, Math.max(2, options.length))}
                                                            value={selected}
                                                            onChange={event =>
                                                                updateSerials(
                                                                    item.orderItemId,
                                                                    [...event.target.selectedOptions].map(option => option.value)
                                                                )
                                                            }
                                                        >
                                                            {options.map(serial => (
                                                                <option key={serial.serialId} value={serial.serialId}>
                                                                    {serial.display} {serial.status ? `(${serial.status})` : ""}
                                                                </option>
                                                            ))}
                                                        </select>
                                                        <small>Select {item.quantity} unit(s). Hold Ctrl to select multiple.</small>
                                                    </label>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>

                                {order.sellerRequestedStatus ? (
                                    <div className="approval-wait">
                                        <strong>Requested: {label(order.sellerRequestedStatus)}</strong>
                                        <span>Waiting for admin approval. Customer notification should be sent after admin approval.</span>
                                    </div>
                                ) : active ? (
                                    <form className="delivery-form" onSubmit={event => { event.preventDefault(); void sendRequest(order); }}>
                                        <strong>{label(action.status)} request</strong>

                                        {action.status === "OUT_FOR_DELIVERY" && (
                                            <>
                                                <input
                                                    required
                                                    placeholder="Delivery boy name"
                                                    value={action.deliveryPersonName}
                                                    onChange={event => setAction({ ...action, deliveryPersonName: event.target.value })}
                                                />
                                                <input
                                                    required
                                                    pattern="[6-9][0-9]{9}"
                                                    placeholder="Delivery boy mobile"
                                                    value={action.deliveryPersonMobile}
                                                    onChange={event => setAction({ ...action, deliveryPersonMobile: event.target.value })}
                                                />
                                            </>
                                        )}

                                        <button disabled={saving === order.id}>Send to admin approval</button>
                                        <button type="button" className="cancel" onClick={() => setAction(null)}>Cancel</button>
                                    </form>
                                ) : (
                                    <div className="order-actions">
                                        <select
                                            defaultValue=""
                                            disabled={locked || saving === order.id}
                                            onChange={event => {
                                                const status = event.target.value;
                                                if (status) startAction(order, status);
                                                event.target.value = "";
                                            }}
                                        >
                                            <option value="">Select next action…</option>
                                            {statuses
                                                .filter(status => status !== order.orderStatus)
                                                .map(status => <option value={status} key={status}>{label(status)}</option>)}
                                        </select>
                                        <button
                                            type="button"
                                            className="invoice-button"
                                            disabled={!order.invoiceAvailable || saving === `invoice-${order.id}`}
                                            onClick={() => downloadInvoice(order)}
                                        >
                                            Download bill / invoice
                                        </button>
                                        <small>Admin approval required before customer status mail.</small>
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </section>
            </main>
        </div>
    );
}
