import { useEffect, useMemo, useState } from "react";
import { downloadAdminInvoice, getAdminOrders } from "../../services/adminService";
import "./AdminTools.css";

const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
export default function Invoices() {
  const [orders, setOrders] = useState([]), [category, setCategory] = useState("ALL"), [error, setError] = useState("");
  useEffect(() => { getAdminOrders().then(setOrders).catch(() => setError("Could not load invoices.")); }, []);
  const invoices = useMemo(() => orders.filter(order => category === "ALL" || order.orderStatus === category), [orders, category]);
  const download = async order => { try { setError(""); const response = await downloadAdminInvoice(order.orderId); const url = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = url; link.download = `ShivHub-Invoice-${order.orderNumber}.pdf`; link.click(); URL.revokeObjectURL(url); } catch { setError("Invoice PDF could not be downloaded."); } };
  return <main className="admin-tools"><header><div><p className="eyebrow">ACCOUNTING</p><h1>Invoices</h1></div></header>{error && <p className="error">{error}</p>}<label>Invoice category <select value={category} onChange={event => setCategory(event.target.value)}><option value="ALL">All invoices</option><option value="PENDING">Pending</option><option value="DELIVERED">Delivered</option><option value="CANCELLED">Cancelled / credit note</option></select></label><table><thead><tr><th>Invoice / Order</th><th>Date</th><th>Tax</th><th>Total</th><th>Status</th><th /></tr></thead><tbody>{invoices.map(order => <tr key={order.orderId}><td><strong>{order.orderNumber}</strong></td><td>{order.createdAt ? new Date(order.createdAt).toLocaleDateString("en-IN") : "—"}</td><td>{money(order.tax)}</td><td>{money(order.grandTotal)}</td><td>{order.orderStatus}</td><td><button className="secondary" onClick={() => download(order)}>Download PDF</button></td></tr>)}</tbody></table>{!invoices.length && <p className="empty">No invoices in this category.</p>}</main>;
}
