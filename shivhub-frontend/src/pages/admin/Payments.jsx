import { useCallback, useEffect, useMemo, useState } from "react";
import { getAdminOrders, updateOrderPaymentStatus } from "../../services/adminService";
import "./AdminTools.css";

const statuses = ["PENDING", "PAID", "FAILED", "REFUNDED"];
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;

export default function Payments() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      setOrders(await getAdminOrders());
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Could not load payments. Please confirm you are logged in as an admin.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const totals = useMemo(() => statuses.reduce((result, status) => ({
    ...result,
    [status]: orders.filter(order => order.paymentStatus === status)
      .reduce((sum, order) => sum + Number(order.grandTotal || 0), 0)
  }), {}), [orders]);

  const change = async (id, paymentStatus) => {
    try {
      setSaving(id);
      setError("");
      const updated = await updateOrderPaymentStatus(id, paymentStatus);
      setOrders(current => current.map(order => order.orderId === id ? updated : order));
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Payment status could not be updated.");
    } finally {
      setSaving(null);
    }
  };

  return <main className="admin-tools">
    <header><div><p className="eyebrow">FINANCE</p><h1>Payments</h1><p>Review order payments and reconcile their status.</p></div><button className="secondary" onClick={load} disabled={loading}>Refresh</button></header>
    {error && <p className="error">{error}</p>}
    <section className="settings-grid">{statuses.map(status => <article className="settings-card" key={status}><p className="eyebrow">{status}</p><h2>{money(totals[status])}</h2></article>)}</section>
    {loading ? <p className="empty">Loading payments…</p> : <table><thead><tr><th>Order</th><th>Customer</th><th>Amount</th><th>Payment status</th><th>Update</th></tr></thead><tbody>{orders.map(order => <tr key={order.orderId}><td><strong>{order.orderNumber || `Order #${order.orderId}`}</strong></td><td>{order.customerName || "—"}</td><td>{money(order.grandTotal)}</td><td>{order.paymentStatus || "PENDING"}</td><td><select value={order.paymentStatus || "PENDING"} disabled={saving === order.orderId} onChange={event => change(order.orderId, event.target.value)}>{statuses.map(status => <option value={status} key={status}>{status}</option>)}</select></td></tr>)}</tbody></table>}
    {!loading && !orders.length && <p className="empty">No orders are available yet, so there are no payments to reconcile.</p>}
  </main>;
}
