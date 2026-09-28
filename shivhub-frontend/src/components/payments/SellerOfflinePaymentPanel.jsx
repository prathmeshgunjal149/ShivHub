import { useState } from "react";
import { createOfflineRazorpayOrder, createOfflineRazorpayPaymentLink, recordOfflineManualPayment } from "../../services/paymentService";
import PaymentStatusBadge from "./PaymentStatusBadge";
import SplitPaymentForm from "./SplitPaymentForm";
import PaymentHistoryModal from "./PaymentHistoryModal";
import "./PaymentComponents.css";

export default function SellerOfflinePaymentPanel({ bill, onRazorpay, onUpdated }) {
    const [mode, setMode] = useState("CASH");
    const [amount, setAmount] = useState("");
    const [reference, setReference] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [historyOpen, setHistoryOpen] = useState(false);
    const [paymentLink, setPaymentLink] = useState(null);
    const total = Number(bill?.grandTotal || 0);
    const paid = Number(bill?.paymentAmount || 0);
    const due = Math.max(0, total - paid);
    if (!bill?.billId) return null;

    const recordManual = async event => {
        event.preventDefault(); setError("");
        if (!(Number(amount) > 0)) return setError("Enter a valid received amount.");
        setSaving(true);
        try {
            const summary = await recordOfflineManualPayment(bill.billId, { paymentMethod: mode, amount: Number(amount), transactionReference: reference || undefined });
            onUpdated?.(summary); setAmount(""); setReference("");
        } catch (requestError) { setError(requestError?.response?.data?.message || requestError?.message || "Could not record payment."); }
        finally { setSaving(false); }
    };
    const openRazorpay = async () => {
        setError(""); setSaving(true);
        try { onRazorpay?.(await createOfflineRazorpayOrder(bill.billId)); }
        catch (requestError) { setError(requestError?.response?.data?.message || requestError?.message || "Could not start Razorpay collection."); }
        finally { setSaving(false); }
    };
    const createLink = async () => {
        setError(""); setSaving(true);
        try { setPaymentLink(await createOfflineRazorpayPaymentLink(bill.billId)); }
        catch (requestError) { setError(requestError?.response?.data?.message || requestError?.message || "Could not create Razorpay payment link."); }
        finally { setSaving(false); }
    };

    return <section className="seller-payment-panel">
        <div className="seller-payment-heading"><div><small>PAYMENT CONTROL</small><h3>Collect balance</h3></div><PaymentStatusBadge status={bill.paymentStatus || "PENDING"} /></div>
        <div className="payment-summary-grid"><span>Total <b>₹{total.toFixed(2)}</b></span><span>Paid <b>₹{paid.toFixed(2)}</b></span><span>Due <b>₹{due.toFixed(2)}</b></span></div>
        {due > 0 && <div className="payment-mode-tabs"><button className={mode === "CASH" ? "active" : ""} type="button" onClick={() => setMode("CASH")}>Cash</button><button className={mode === "UPI" ? "active" : ""} type="button" onClick={() => setMode("UPI")}>Manual UPI</button><button className={mode === "CARD" ? "active" : ""} type="button" onClick={() => setMode("CARD")}>Manual Card</button><button className={mode === "RAZORPAY" ? "active" : ""} type="button" onClick={() => setMode("RAZORPAY")}>Razorpay</button><button className={mode === "SPLIT" ? "active" : ""} type="button" onClick={() => setMode("SPLIT")}>Split</button></div>}
        {due > 0 && mode === "RAZORPAY" && <div className="razorpay-collection"><p>Use secure checkout on this device, or create a Razorpay link to send to the customer.</p><div><button type="button" onClick={openRazorpay} disabled={saving}>{saving ? "Starting…" : "Open secure checkout"}</button><button type="button" onClick={createLink} disabled={saving}>Create payment link</button></div>{paymentLink?.paymentLinkUrl && <p className="payment-link"><a href={paymentLink.paymentLinkUrl} target="_blank" rel="noreferrer">Open customer payment link ↗</a><button type="button" onClick={() => navigator.clipboard?.writeText(paymentLink.paymentLinkUrl)}>Copy link</button></p>}</div>}
        {due > 0 && mode === "SPLIT" && <SplitPaymentForm billId={bill.billId} remainingAmount={due} onRecorded={onUpdated} />}
        {due > 0 && !["RAZORPAY", "SPLIT"].includes(mode) && <form className="manual-payment-form" onSubmit={recordManual}><input type="number" min="0" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} placeholder="Received amount" /><input value={reference} onChange={event => setReference(event.target.value)} placeholder="Transaction reference (optional)" /><button type="submit" disabled={saving}>{saving ? "Recording…" : "Record payment"}</button></form>}
        <button className="payment-history-button" type="button" onClick={() => setHistoryOpen(true)}>View payment history</button>
        {error && <p className="payment-error">{error}</p>}
        <PaymentHistoryModal billId={bill.billId} open={historyOpen} onClose={() => setHistoryOpen(false)} onSummary={onUpdated} />
    </section>;
}
