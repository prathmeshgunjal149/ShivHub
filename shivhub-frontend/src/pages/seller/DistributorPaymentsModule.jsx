import { useEffect, useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import api from "../../services/api";
import { addPurchasePayment, getPurchasePaymentSummary } from "../../services/purchaseService";
import PaymentMetadataFields from "./components/PaymentMetadataFields";
import "./PaymentModule.css";
import "./PremiumPolish.css";

const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
const toPaymentDateTime = date => date ? `${date}T12:00:00` : null;
const safeNotes = input => [input.upiId ? `UPI ID: ${input.upiId}` : "", input.notes || ""].filter(Boolean).join("\n") || null;

export default function DistributorPaymentsModule() {
    const [bills, setBills] = useState([]);
    const [inputs, setInputs] = useState({});
    const [search, setSearch] = useState("");
    const [status, setStatus] = useState("ALL");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const load = async () => {
        try {
            setError("");
            const response = await api.get("/api/purchase-payments/payable-bills");
            const rows = await Promise.all((response.data || []).map(async bill => {
                try { return { ...bill, ...await getPurchasePaymentSummary(bill.id) }; }
                catch { return { ...bill, purchaseTotal: bill.grandTotal, totalPaid: 0, remainingAmount: bill.grandTotal, paymentStatus: "UNPAID" }; }
            }));
            setBills(rows);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to load distributor bills.");
        }
    };

    useEffect(() => { void load(); }, []);

    const pay = async (event, bill) => {
        event.preventDefault();
        const input = inputs[bill.id] || {};
        const amount = Number(input.amount);
        if (!Number.isFinite(amount) || amount <= 0 || amount > Number(bill.remainingAmount)) {
            setError("Enter a positive amount that does not exceed the remaining payable balance.");
            return;
        }
        try {
            setError(""); setSuccess("");
            await addPurchasePayment({
                purchaseId: bill.id,
                amount,
                paymentMethod: input.paymentMethod || "CASH",
                transactionReference: input.reference || null,
                paymentDate: toPaymentDateTime(input.paymentDate),
                notes: safeNotes(input)
            });
            setInputs({});
            setSuccess(`Payment for ${bill.invoiceNumber || "the distributor bill"} was recorded.`);
            await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Distributor payment could not be saved.");
        }
    };

    const shown = useMemo(() => bills.filter(bill => {
        const term = search.toLowerCase();
        return (!term || [bill.invoiceNumber, bill.distributorName, bill.id]
            .some(value => String(value || "").toLowerCase().includes(term)))
            && (status === "ALL" || bill.paymentStatus === status);
    }), [bills, search, status]);

    return <div className="payment-page"><SellerSidebar /><main className="payment-main">
        <header><div><span>PAYABLES</span><h1>Distributor Payments</h1><p>Bill-wise payable view with safe partial-payment history.</p></div><button type="button" onClick={load}>Refresh</button></header>
        {error && <div className="payment-error" role="alert">{error}</div>}
        {success && <div className="payment-success" role="status">{success}</div>}
        <section className="payment-filter"><input placeholder="Search original bill, distributor or purchase ID" value={search} onChange={event => setSearch(event.target.value)} /><select value={status} onChange={event => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="UNPAID">Unpaid</option><option value="PARTIALLY_PAID">Partial</option><option value="PAID">Paid</option></select></section>
        <section className="payment-table"><table><thead><tr><th>Original Bill</th><th>Distributor</th><th>Bill Total</th><th>Already Paid</th><th>Remaining</th><th>Status</th><th>New Payment</th></tr></thead><tbody>{shown.length ? shown.map(bill => <tr key={bill.id}>
            <td><b>{bill.invoiceNumber}</b><small>{String(bill.purchaseDate || "").slice(0, 10)}</small></td><td>{bill.distributorName || "Distributor"}</td><td>{money(bill.purchaseTotal || bill.grandTotal)}</td><td>{money(bill.totalPaid)}</td><td><b>{money(bill.remainingAmount)}</b></td><td>{bill.paymentStatus}</td>
            <td>{Number(bill.remainingAmount) > 0 && <form className="inline-pay" onSubmit={event => pay(event, bill)}><input required type="number" min="0.01" max={bill.remainingAmount} placeholder="Amount" value={inputs[bill.id]?.amount || ""} onChange={event => setInputs({ ...inputs, [bill.id]: { ...inputs[bill.id], amount: event.target.value } })} /><PaymentMetadataFields value={inputs[bill.id]} onChange={value => setInputs({ ...inputs, [bill.id]: value })} /><button>Save payment</button></form>}</td>
        </tr>) : <tr><td colSpan="7">No distributor bill matches this filter.</td></tr>}</tbody></table></section>
    </main></div>;
}
