import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "./SellerGSTReports.css";

const REPORTS = [
  ["GSTR1", "GSTR-1 (Sales)", "Completed POS and online sales with approved credit-note adjustment."],
  ["GSTR2", "GSTR-2 (Purchase)", "Completed supplier purchase invoices and input tax."],
  ["GSTR3B", "GSTR-3B summary", "Internal output tax and input tax-credit summary."],
  ["SALES_HSN", "GST Sales with HSN", "Sales tax rows grouped by HSN where a snapshot is available."],
  ["PURCHASE_HSN", "GST Purchase with HSN", "Purchase item tax rows and supplier HSN."],
  ["HSN_SALES", "HSN-wise sales summary", "Summarised taxable value and tax by HSN."],
  ["TDS_PAYABLE", "TDS Payable", "Recorded withholding transactions only."],
  ["TDS_RECEIVABLE", "TDS Receivable", "Recorded withholding transactions only."],
  ["TCS_PAYABLE", "TCS Payable", "Recorded collection transactions only."],
  ["TCS_RECEIVABLE", "TCS Receivable", "Recorded collection transactions only."],
  ["ITEM_PARTIES", "Party report by item", "Customers who purchased a POS item."],
  ["OUTSTANDING", "Party-wise outstanding", "Customer receivables, paid values and balance due."],
  ["LEDGER", "Party statement / ledger", "Invoice-level party statement from existing receivables."],
];
const amount = value => Number(value || 0).toLocaleString("en-IN", { style: "currency", currency: "INR" });
const iso = value => value ? String(value).slice(0, 10) : "—";
const currentMonth = () => new Date().toISOString().slice(0, 7) + "-01";
const today = () => new Date().toISOString().slice(0, 10);

export default function SellerGSTReports() {
  const [type, setType] = useState("GSTR1");
  const [filters, setFilters] = useState({ from: currentMonth(), to: today(), q: "", party: "", gstin: "" });
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const { data: report } = await api.get("/api/seller/gst-reports", { params: { ...filters, type, page, size: 25 } });
      setData(report);
    } catch (err) { setError(err.response?.data?.message || "Could not load the GST report."); }
    finally { setLoading(false); }
  }, [filters, type, page]);
  useEffect(() => { void load(); }, [load]);
  const choose = value => { setType(value); setPage(0); };
  const change = (key, value) => { setPage(0); setFilters(previous => ({ ...previous, [key]: value })); };
  const download = async format => {
    setDownloading(format); setError("");
    try {
      const response = await api.get("/api/seller/gst-reports/download", { params: { ...filters, type, format }, responseType: "blob" });
      const ext = format === "XLSX" ? "xls" : format.toLowerCase();
      const link = document.createElement("a"); link.href = URL.createObjectURL(response.data); link.download = `shivhub-${type.toLowerCase()}.${ext}`; link.click(); URL.revokeObjectURL(link.href);
    } catch (err) { setError(err.response?.data?.message || "Download could not be prepared."); }
    finally { setDownloading(""); }
  };
  const total = data?.totals || {};
  const selected = REPORTS.find(item => item[0] === type);
  return <main className="seller-gst-page">
    <header className="gst-hero"><div><p className="gst-eyebrow">TAX & PARTY REPORTING</p><h1>GST reports</h1><p>Seller-only reports from completed transactions. These exports assist review; they do not file a GST return.</p></div><Link to="/seller/reports">All reports</Link></header>
    <section className="gst-dashboard" aria-label="GST dashboard">
      <Metric label="Sales tax" value={amount(total.tax)} />
      <Metric label="Purchase ITC" value={type === "GSTR2" ? amount(total.tax) : "Open GSTR-2"} />
      <Metric label="Net GST payable" value={amount(total.netGstPayable)} />
      <Metric label="Taxable value" value={amount(total.taxable)} />
      <Metric label="Invoice total" value={amount(total.total)} />
      <Metric label="Outstanding" value={amount(total.due)} />
    </section>
    <section className="gst-layout"><aside className="gst-report-list"><h2>Reports</h2>{REPORTS.map(([value, label, help]) => <button className={type === value ? "active" : ""} key={value} onClick={() => choose(value)}><strong>{label}</strong><small>{help}</small></button>)}</aside>
      <section className="gst-content"><div className="gst-content-title"><div><h2>{selected?.[1]}</h2><p>{selected?.[2]}</p></div><div className="gst-downloads">{["CSV", "XLSX", "PDF"].map(format => <button key={format} onClick={() => void download(format)} disabled={!!downloading}>{downloading === format ? "Preparing…" : `Download ${format}`}</button>)}</div></div>
        <div className="gst-filters"><label>From<input type="date" value={filters.from} onChange={e => change("from", e.target.value)} /></label><label>To<input type="date" value={filters.to} onChange={e => change("to", e.target.value)} /></label><label>Search<input placeholder="Invoice, product or HSN" value={filters.q} onChange={e => change("q", e.target.value)} /></label><label>Party<input placeholder="Customer / supplier" value={filters.party} onChange={e => change("party", e.target.value)} /></label><label>GSTIN<input placeholder="GSTIN" value={filters.gstin} onChange={e => change("gstin", e.target.value)} /></label></div>
        {error && <p className="gst-message error">{error}</p>}
        {type.startsWith("TDS") || type.startsWith("TCS") ? <p className="gst-message">No TDS/TCS transactions are recorded in the current accounting data. Amounts are never estimated.</p> : null}
        <div className="gst-totals"><Metric label="Taxable" value={amount(total.taxable)} /><Metric label="CGST" value={amount(total.cgst)} /><Metric label="SGST" value={amount(total.sgst)} /><Metric label="IGST" value={amount(total.igst)} /><Metric label="Discount" value={amount(total.discount)} /><Metric label="Total tax" value={amount(total.tax)} /></div>
        <div className="gst-table-wrap"><table><thead><tr>{["Date","Source","Reference","Party","GSTIN","Product / HSN","Taxable","CGST","SGST","IGST","Total","Paid","Due","Status"].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{loading ? <tr><td colSpan="14">Loading report…</td></tr> : !data?.rows?.length ? <tr><td colSpan="14">No transactions match these filters.</td></tr> : data.rows.map((row, index) => <tr key={`${row.reference}-${index}`}><td>{iso(row.date)}</td><td>{row.source || "—"}</td><td>{row.reference || "—"}</td><td>{row.party || "—"}</td><td>{row.gstin || "B2C"}</td><td>{row.product || "—"}{row.hsn ? <small>HSN {row.hsn}</small> : null}</td><td>{amount(row.taxable)}</td><td>{amount(row.cgst)}</td><td>{amount(row.sgst)}</td><td>{amount(row.igst)}</td><td>{amount(row.total)}</td><td>{amount(row.paid)}</td><td>{amount(row.due)}</td><td><span className="gst-status">{row.status || "—"}</span></td></tr>)}</tbody></table></div>
        <div className="gst-pagination"><span>{data ? `${data.totalRows} row(s)` : ""}</span><button disabled={page === 0 || loading} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {page + 1}</span><button disabled={!data || page + 1 >= data.totalPages || loading} onClick={() => setPage(value => value + 1)}>Next</button></div>
      </section></section>
  </main>;
}
function Metric({ label, value }) { return <article className="gst-metric"><span>{label}</span><strong>{value}</strong></article>; }
