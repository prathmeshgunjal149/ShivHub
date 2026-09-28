import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  downloadAdminOrderInvoice,
  downloadAdminSellerOfflineInvoice,
  getAdminSeller,
  getAdminSellerCustomers,
  getAdminSellerSales,
  suspendAdminSeller,
  updateAdminSellerProduct
} from "../../services/adminManagementService";
import "./AdminGrowth.css";

const money = value => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));
const date = value => value ? new Date(value).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "—";
const cleanParams = source => Object.fromEntries(Object.entries(source).filter(([, value]) => value !== "" && value != null));

export default function AdminSellerDetails() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [tab, setTab] = useState("overview");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const [sales, setSales] = useState([]);
  const [salesFilters, setSalesFilters] = useState({ from: "", to: "" });
  const [customerResult, setCustomerResult] = useState({ content: [], page: 0, totalPages: 0 });
  const [customerFilters, setCustomerFilters] = useState({ search: "", from: "", to: "", minimumAmount: "", maximumAmount: "", page: 0, size: 20 });

  const load = useCallback(async () => {
    try {
      const response = await getAdminSeller(id);
      setData(response);
      setSales(response.sales || []);
      setCustomerResult(current => current.content.length ? current : { content: response.customers || [], page: 0, totalPages: 1 });
      setError("");
    } catch {
      setError("Seller profile could not be loaded. Please sign in as an administrator and try again.");
    }
  }, [id]);
  useEffect(() => { load(); }, [load]);

  const loadSales = async next => {
    const filters = { ...salesFilters, ...next };
    setSalesFilters(filters);
    setBusy("sales");
    try {
      setSales(await getAdminSellerSales(id, cleanParams(filters)));
      setError("");
    } catch {
      setError("Sales records could not be loaded.");
    } finally {
      setBusy("");
    }
  };
  const loadCustomers = async next => {
    const filters = { ...customerFilters, ...next };
    setCustomerFilters(filters);
    setBusy("customers");
    try {
      setCustomerResult(await getAdminSellerCustomers(id, cleanParams(filters)));
      setError("");
    } catch {
      setError("Seller customer records could not be loaded.");
    } finally {
      setBusy("");
    }
  };
  const productStatus = async (productId, status) => {
    setBusy(`${productId}-${status}`);
    try {
      await updateAdminSellerProduct(id, productId, status);
      await load();
    } catch {
      setError("Product status could not be updated.");
    } finally {
      setBusy("");
    }
  };
  const suspend = async () => {
    if (!window.confirm("Suspend this seller? The shop owner will no longer be able to sign in.")) return;
    setBusy("suspend");
    try {
      await suspendAdminSeller(id);
      await load();
    } catch {
      setError("Seller could not be suspended.");
    } finally {
      setBusy("");
    }
  };
  const downloadInvoice = async sale => {
    setBusy(`invoice-${sale.type}-${sale.id}`);
    try {
      const file = sale.type === "OFFLINE_BILL"
        ? await downloadAdminSellerOfflineInvoice(id, sale.id)
        : await downloadAdminOrderInvoice(sale.orderId);
      const url = URL.createObjectURL(file);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${sale.reference || "shivhub-invoice"}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setError("Invoice could not be downloaded.");
    } finally {
      setBusy("");
    }
  };

  if (!data && !error) return <main className="growth-page"><p>Loading seller profile…</p></main>;
  const seller = data?.seller;
  const profile = data?.profile || {};
  const financial = data?.financial || {};
  const tabs = [["overview", "Overview"], ["products", `Products (${data?.products?.length || 0})`], ["sales", "Sales & bills"], ["customers", "Customers"], ["payments", "Payments"], ["staff", "Staff"]];

  return <main className="growth-page">
    {error && <p className="growth-error">{error}</p>}
    <Link className="back-link" to="/admin/sellers">← Seller Management</Link>
    {seller && <>
      <header className="profile-hero">
        <div className="avatar-large">{seller.shopLogoUrl ? <img src={seller.shopLogoUrl} alt="Shop" /> : seller.ownerName?.slice(0, 1)}</div>
        <div><p className="eyebrow">Seller profile · #{seller.id}</p><h1>{seller.shopName || seller.ownerName}</h1><p>{seller.ownerName} · {seller.email} · {seller.mobile || "No mobile"}</p><span className={`pill ${seller.approvalStatus?.toLowerCase()}`}>{seller.approvalStatus}</span></div>
        <button className="danger-button" disabled={busy === "suspend" || seller.approvalStatus === "SUSPENDED"} onClick={suspend}>{busy === "suspend" ? "Suspending…" : "Suspend seller"}</button>
      </header>
      <section className="metric-grid compact"><article><span>Total turnover</span><strong>{money(seller.totalRevenue)}</strong></article><article><span>Stock value</span><strong>{money(seller.stockValue)}</strong></article><article><span>Customer due</span><strong>{money(seller.customerReceivable)}</strong></article><article><span>Distributor due</span><strong>{money(seller.pendingDistributorPayment)}</strong></article></section>
      <nav className="tab-list">{tabs.map(([value, label]) => <button key={value} className={tab === value ? "selected" : ""} onClick={() => { setTab(value); if (value === "sales") loadSales(); if (value === "customers") loadCustomers(); }}>{label}</button>)}</nav>

      {tab === "overview" && <section className="detail-columns"><article className="growth-card"><h2>Owner & shop details</h2><dl className="detail-list"><dt>Owner</dt><dd>{profile.ownerName}</dd><dt>GSTIN</dt><dd>{profile.gstin || "Not provided"}</dd><dt>Address</dt><dd>{profile.address || "Not provided"}</dd><dt>Website</dt><dd>{profile.websiteUrl || "Not provided"}</dd><dt>Social</dt><dd>{[profile.instagramUrl, profile.facebookUrl, profile.whatsappUrl, profile.youtubeUrl].filter(Boolean).join(" · ") || "Not provided"}</dd><dt>Registered</dt><dd>{date(profile.registrationDate)}</dd><dt>Last login</dt><dd>{date(profile.lastLoginAt)}</dd></dl></article><article className="growth-card"><h2>Business performance</h2><dl className="detail-list"><dt>Offline sales</dt><dd>{money(financial.offlineSales)}</dd><dt>Online sales</dt><dd>{money(financial.onlineSales)}</dd><dt>Profit estimate</dt><dd>{money(financial.profitEstimate)}</dd><dt>Products</dt><dd>{financial.totalProducts}</dd><dt>Stock value</dt><dd>{money(financial.stockValue)}</dd><dt>Customers</dt><dd>{financial.customers}</dd><dt>Customer receivables</dt><dd>{money(financial.customerReceivable)}</dd><dt>Distributor payable</dt><dd>{money(financial.pendingDistributorPayment)}</dd></dl></article></section>}

      {tab === "products" && <section className="growth-card table-scroll"><h2>Products & stock</h2><table><thead><tr><th>Product</th><th>Category</th><th>Price</th><th>Available stock</th><th>Review status</th><th>Admin action</th></tr></thead><tbody>{data.products.map(product => <tr key={product.id} className={product.outOfStock ? "out-stock" : product.lowStock ? "low-stock" : ""}><td className="person-cell">{product.imageUrl && <img src={product.imageUrl} alt="" />}<div><strong>{product.name}</strong><small>{product.outOfStock ? "Out of stock" : product.lowStock ? "Low stock" : "In stock"}</small></div></td><td>{product.category || "—"}</td><td>{money(product.sellingPrice)}</td><td>{product.stock}</td><td><span className={`pill ${product.status?.toLowerCase()}`}>{product.status}</span></td><td className="inline-actions"><button disabled={!!busy} onClick={() => productStatus(product.id, "APPROVED")}>Approve</button><button disabled={!!busy} onClick={() => productStatus(product.id, "REJECTED")}>Reject</button><button className="danger-text" disabled={!!busy} onClick={() => productStatus(product.id, "SUSPENDED")}>Suspend</button></td></tr>)}</tbody></table>{!data.products.length && <p className="empty-state">No products added by this seller.</p>}</section>}

      {tab === "sales" && <section className="growth-card table-scroll"><div className="section-heading"><div><h2>Offline bills & online orders</h2><p>Filter by billing date and download the original invoice.</p></div><form className="inline-filter" onSubmit={event => { event.preventDefault(); loadSales(); }}><label>From<input type="date" value={salesFilters.from} onChange={event => setSalesFilters(current => ({ ...current, from: event.target.value }))} /></label><label>To<input type="date" value={salesFilters.to} onChange={event => setSalesFilters(current => ({ ...current, to: event.target.value }))} /></label><button className="secondary-button" disabled={busy === "sales"}>{busy === "sales" ? "Loading…" : "Apply"}</button></form></div><table><thead><tr><th>Reference</th><th>Date</th><th>Customer</th><th>Type</th><th>GST</th><th>Payment</th><th>Amount</th><th /></tr></thead><tbody>{sales.map(sale => <tr key={`${sale.type}-${sale.id}`}><td><strong>{sale.reference}</strong><small>{sale.productName}</small></td><td>{date(sale.date)}</td><td>{sale.customerName || "Walk-in customer"}<small>{sale.customerMobile}</small></td><td><span className="pill neutral">{sale.type}</span></td><td>{money(sale.gst)}</td><td>{sale.paymentStatus}</td><td><strong>{money(sale.amount)}</strong></td><td><button className="text-button" disabled={busy.startsWith("invoice-")} onClick={() => downloadInvoice(sale)}>Invoice</button></td></tr>)}</tbody></table>{!sales.length && <p className="empty-state">No sales match this date range.</p>}</section>}

      {tab === "customers" && <section className="growth-card table-scroll"><div className="section-heading"><div><h2>Seller customers</h2><p>Registered online buyers and walk-in customers, restricted to this seller.</p></div><form className="inline-filter" onSubmit={event => { event.preventDefault(); loadCustomers({ page: 0 }); }}><input value={customerFilters.search} onChange={event => setCustomerFilters(current => ({ ...current, search: event.target.value }))} placeholder="Name, mobile or email" /><label>From<input type="date" value={customerFilters.from} onChange={event => setCustomerFilters(current => ({ ...current, from: event.target.value }))} /></label><label>To<input type="date" value={customerFilters.to} onChange={event => setCustomerFilters(current => ({ ...current, to: event.target.value }))} /></label><input type="number" min="0" value={customerFilters.minimumAmount} onChange={event => setCustomerFilters(current => ({ ...current, minimumAmount: event.target.value }))} placeholder="Min ₹" /><input type="number" min="0" value={customerFilters.maximumAmount} onChange={event => setCustomerFilters(current => ({ ...current, maximumAmount: event.target.value }))} placeholder="Max ₹" /><button className="secondary-button" disabled={busy === "customers"}>{busy === "customers" ? "Loading…" : "Apply"}</button></form></div><table><thead><tr><th>Customer</th><th>Type</th><th>Orders / bills</th><th>Total spent</th><th>Pending credit</th><th>Last purchase</th></tr></thead><tbody>{customerResult.content.map(customer => <tr key={customer.id}><td><Link className="text-button" to={`/admin/customers/${customer.id}`}>{customer.name}</Link><small>{customer.mobile || customer.email || "No contact"}</small></td><td><span className="pill neutral">{customer.customerType}</span></td><td>{customer.totalOrdersAndBills}</td><td>{money(customer.totalPurchaseAmount)}</td><td>{money(customer.pendingReceivable)}</td><td>{date(customer.lastPurchaseDate)}</td></tr>)}</tbody></table>{!customerResult.content.length && <p className="empty-state">No customers match these filters.</p>}<nav className="pagination"><button disabled={customerResult.page <= 0 || busy === "customers"} onClick={() => loadCustomers({ page: customerResult.page - 1 })}>Previous</button><span>Page {customerResult.page + 1} of {Math.max(customerResult.totalPages, 1)}</span><button disabled={customerResult.page + 1 >= customerResult.totalPages || busy === "customers"} onClick={() => loadCustomers({ page: customerResult.page + 1 })}>Next</button></nav></section>}

      {tab === "payments" && <section className="detail-columns"><article className="growth-card table-scroll"><h2>Distributor payments</h2><table><thead><tr><th>Invoice</th><th>Distributor</th><th>Amount</th><th>Paid</th><th>Pending</th></tr></thead><tbody>{data.payments.distributorPayments.map(payment => <tr key={payment.purchaseId}><td>{payment.invoiceNumber}</td><td>{payment.distributor}</td><td>{money(payment.amount)}</td><td>{money(payment.paid)}</td><td>{money(payment.pending)}</td></tr>)}</tbody></table></article><article className="growth-card table-scroll"><h2>Customer receivables</h2><table><thead><tr><th>Customer</th><th>Due date</th><th>Pending</th></tr></thead><tbody>{data.payments.customerReceivables.map(due => <tr key={due.id} className={due.overdue ? "overdue" : ""}><td>{due.customerName}<small>{due.mobile}</small></td><td>{due.dueDate || "—"}</td><td>{money(due.pending)}</td></tr>)}</tbody></table></article></section>}

      {tab === "staff" && <section className="growth-card table-scroll"><h2>Staff performance</h2><table><thead><tr><th>Staff</th><th>Shop / access</th><th>Bills</th><th>Sales</th><th>Customers handled</th></tr></thead><tbody>{data.staff.map(member => <tr key={member.id}><td><strong>{member.name}</strong><small>{member.email}</small></td><td>{member.shop}<small>{member.role}</small></td><td>{member.bills}</td><td>{money(member.sales)}</td><td>{member.customersHandled}</td></tr>)}</tbody></table>{!data.staff.length && <p className="empty-state">No active staff assignments.</p>}</section>}
    </>}
  </main>;
}
