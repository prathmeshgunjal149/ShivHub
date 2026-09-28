import { useEffect, useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import api from "../../services/api";
import { getPurchases } from "../../services/purchaseService";
import PaymentMetadataFields from "./components/PaymentMetadataFields";
import "./PaymentModule.css";
import "./PremiumPolish.css";

const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
const blankEntry = () => ({ customerName: "", customerMobile: "", invoiceNumber: "", productDetails: "", imei: "", saleAmount: "", dueDate: "" });
const toPaymentDateTime = date => date ? `${date}T12:00:00` : null;
const paymentNotes = input => [input.upiId ? `UPI ID: ${input.upiId}` : "", input.notes || ""].filter(Boolean).join("\n") || null;

export default function PaymentModule() {
    const [rows, setRows] = useState([]);
    const [purchases, setPurchases] = useState([]);
    const [creditNotes, setCreditNotes] = useState([]);
    const [tab, setTab] = useState("receivables");
    const [form, setForm] = useState({});
    const [entry, setEntry] = useState(blankEntry);
    const [credit, setCredit] = useState({ purchaseId: "", creditNoteNumber: "", amount: "", creditNoteDate: new Date().toISOString().slice(0, 10), reason: "", remarks: "" });
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [search, setSearch] = useState("");

    const load = async () => {
        try {
            setError("");
            const [customerData, purchaseData, creditData] = await Promise.all([
                api.get("/api/customer-receivables"), getPurchases(), api.get("/api/distributor-credit-notes")
            ]);
            setRows(customerData.data || []); setPurchases(purchaseData || []); setCreditNotes(creditData.data || []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to load payment details.");
        }
    };

    useEffect(() => { void load(); }, []);

    const add = async event => {
        event.preventDefault();
        const saleAmount = Number(entry.saleAmount);
        if (!Number.isFinite(saleAmount) || saleAmount <= 0) { setError("Due amount must be greater than zero."); return; }
        try {
            setError(""); setSuccess("");
            await api.post("/api/customer-receivables", { ...entry, saleAmount, saleDate: new Date().toISOString().slice(0, 10) });
            setEntry(blankEntry()); setSuccess("Customer receivable was added."); await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Customer due could not be saved.");
        }
    };

    const addCredit = async event => {
        event.preventDefault();
        const amount = Number(credit.amount);
        if (!Number.isFinite(amount) || amount <= 0) { setError("Credit note amount must be greater than zero."); return; }
        try {
            setError(""); setSuccess("");
            await api.post("/api/distributor-credit-notes", { ...credit, purchaseId: Number(credit.purchaseId), amount });
            setCredit({ purchaseId: "", creditNoteNumber: "", amount: "", creditNoteDate: new Date().toISOString().slice(0, 10), reason: "", remarks: "" });
            setSuccess("Distributor credit note was added."); await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Credit note could not be saved.");
        }
    };

    const collect = async (event, row) => {
        event.preventDefault();
        const value = form[row.id] || {};
        const amount = Number(value.amount);
        if (!Number.isFinite(amount) || amount <= 0 || amount > Number(row.remainingAmount)) {
            setError("Received amount must be positive and cannot exceed the remaining due."); return;
        }
        try {
            setError(""); setSuccess("");
            await api.post(`/api/customer-receivables/${row.id}/payments`, {
                amount,
                paymentMethod: value.paymentMethod || "CASH",
                transactionReference: value.reference || null,
                paymentDate: toPaymentDateTime(value.paymentDate),
                notes: paymentNotes(value)
            });
            if (value.promisedDueDate) {
                await api.put(`/api/customer-receivables/${row.id}/due-date`, {
                    dueDate: value.promisedDueDate, notes: "Customer promised the remaining amount on this date."
                });
            }
            setForm({}); setSuccess(`Collection from ${row.customerName || "the customer"} was recorded.`); await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Collection could not be saved.");
        }
    };

    const due = rows.reduce((sum, row) => sum + Number(row.remainingAmount || 0), 0);
    const shown = useMemo(() => rows.filter(row => !search || [row.customerName, row.customerMobile, row.invoiceNumber, row.productDetails, row.imei, row.paymentStatus]
        .some(value => String(value || "").toLowerCase().includes(search.toLowerCase()))), [rows, search]);

    return <div className="payment-page"><SellerSidebar /><main className="payment-main">
        <header><div><span>FINANCE LEDGER</span><h1>Customer Udhar & Collections</h1><p>Record a partial payment and a new promised date for any remaining amount.</p></div><button type="button" onClick={load}>Refresh</button></header>
        {error && <div className="payment-error" role="alert">{error}</div>}{success && <div className="payment-success" role="status">{success}</div>}
        <section className="payment-kpis"><article><small>Total customer due</small><b>{money(due)}</b></article><article><small>Overdue</small><b>{money(rows.filter(row => row.paymentStatus === "OVERDUE").reduce((sum, row) => sum + Number(row.remainingAmount || 0), 0))}</b></article></section>
        <nav className="payment-tabs"><button type="button" className={tab === "receivables" ? "active" : ""} onClick={() => setTab("receivables")}>Customer Receivables</button><button type="button" className={tab === "credit-notes" ? "active" : ""} onClick={() => setTab("credit-notes")}>Distributor Credit Notes</button></nav>
        {tab === "receivables" && <>
            <section className="payment-entry"><form onSubmit={add}><h2>New credit sale / opening due</h2><input required placeholder="Customer name" value={entry.customerName} onChange={event => setEntry({ ...entry, customerName: event.target.value })} /><input placeholder="Mobile number" value={entry.customerMobile} onChange={event => setEntry({ ...entry, customerMobile: event.target.value })} /><input placeholder="Invoice / bill number" value={entry.invoiceNumber} onChange={event => setEntry({ ...entry, invoiceNumber: event.target.value })} /><input placeholder="Product details" value={entry.productDetails} onChange={event => setEntry({ ...entry, productDetails: event.target.value })} /><input placeholder="IMEI" value={entry.imei} onChange={event => setEntry({ ...entry, imei: event.target.value })} /><input required type="number" min="0.01" placeholder="Due amount" value={entry.saleAmount} onChange={event => setEntry({ ...entry, saleAmount: event.target.value })} /><input type="date" title="Promised due date" value={entry.dueDate} onChange={event => setEntry({ ...entry, dueDate: event.target.value })} /><button>Add due</button></form></section>
            <section className="payment-filter"><input placeholder="Search customer, bill, mobile, IMEI or status" value={search} onChange={event => setSearch(event.target.value)} /></section>
            <section className="payment-table"><table><thead><tr><th>Customer / Bill</th><th>Product / IMEI</th><th>Promised date</th><th>Due</th><th>Paid</th><th>Remaining</th><th>Entry when customer pays</th></tr></thead><tbody>{shown.length ? shown.map(row => <tr key={row.id}><td><b>{row.customerName}</b><small>{row.customerMobile} · {row.invoiceNumber || "No bill"}</small></td><td>{row.productDetails || "—"}<small>{row.imei || ""}</small></td><td>{row.dueDate || "—"}</td><td>{money(row.saleAmount)}</td><td>{money(row.paidAmount)}</td><td><b>{money(row.remainingAmount)}</b><small>{row.paymentStatus}</small></td><td>{Number(row.remainingAmount) > 0 && <form className="inline-pay" onSubmit={event => collect(event, row)}><input required type="number" min="0.01" max={row.remainingAmount} placeholder="Received amount" value={form[row.id]?.amount || ""} onChange={event => setForm({ ...form, [row.id]: { ...form[row.id], amount: event.target.value } })} /><PaymentMetadataFields value={form[row.id]} onChange={value => setForm({ ...form, [row.id]: value })} includePromiseDate /><button>Save collection</button><small>For a partial payment, set the new promised date for the remaining amount.</small></form>}</td></tr>) : <tr><td colSpan="7">No customer due matches this search.</td></tr>}</tbody></table></section>
        </>}
        {tab === "credit-notes" && <><section className="payment-entry"><form onSubmit={addCredit}><h2>Distributor Credit Note (CN)</h2><select required value={credit.purchaseId} onChange={event => setCredit({ ...credit, purchaseId: event.target.value })}><option value="">Select original purchase bill</option>{purchases.map(purchase => <option key={purchase.id} value={purchase.id}>{purchase.invoiceNumber} — {purchase.distributor?.distributor?.businessName || "Distributor"} — {money(purchase.grandTotal)}</option>)}</select><input required placeholder="Credit note number" value={credit.creditNoteNumber} onChange={event => setCredit({ ...credit, creditNoteNumber: event.target.value })} /><input required type="number" min="0.01" placeholder="CN amount" value={credit.amount} onChange={event => setCredit({ ...credit, amount: event.target.value })} /><input type="date" value={credit.creditNoteDate} onChange={event => setCredit({ ...credit, creditNoteDate: event.target.value })} /><input placeholder="Reason" value={credit.reason} onChange={event => setCredit({ ...credit, reason: event.target.value })} /><input placeholder="Remarks" value={credit.remarks} onChange={event => setCredit({ ...credit, remarks: event.target.value })} /><button>Save credit note</button></form></section><section className="payment-table"><table><thead><tr><th>CN Number</th><th>Original purchase bill</th><th>Date</th><th>Amount</th><th>Reason</th></tr></thead><tbody>{creditNotes.length ? creditNotes.map(note => <tr key={note.id}><td><b>{note.creditNoteNumber}</b></td><td>{note.purchaseInvoiceNumber}</td><td>{note.creditNoteDate}</td><td>{money(note.amount)}</td><td>{note.reason || "—"}</td></tr>) : <tr><td colSpan="5">No distributor credit notes yet.</td></tr>}</tbody></table></section></>}
    </main></div>;
}
