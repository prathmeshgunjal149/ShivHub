import { useEffect, useEffectEvent, useState } from "react";
import { getOfflinePaymentSummary } from "../../services/paymentService";
import PaymentStatusBadge from "./PaymentStatusBadge";
import "./PaymentComponents.css";

export default function PaymentHistoryModal({ billId, open, onClose, onSummary }) {
    const [summary, setSummary] = useState(null);
    const [error, setError] = useState("");

    const reportSummary = useEffectEvent(value => onSummary?.(value));

    useEffect(() => {
        if (!open || !billId) return;
        setError("");
        getOfflinePaymentSummary(billId)
            .then(value => { setSummary(value); reportSummary(value); })
            .catch(() => setError("Could not load payment history. Please try again."));
    }, [billId, open]);

    if (!open) return null;
    return <div className="payment-modal-backdrop" role="presentation" onMouseDown={onClose}>
        <section className="payment-modal" role="dialog" aria-modal="true" aria-label="Payment history" onMouseDown={event => event.stopPropagation()}>
            <header><div><small>OFFLINE BILL</small><h3>Payment history</h3></div><button type="button" onClick={onClose} aria-label="Close payment history">×</button></header>
            {error && <p className="payment-error">{error}</p>}
            {!summary && !error && <p className="payment-muted">Loading payment details…</p>}
            {summary && <>
                <div className="payment-summary-grid">
                    <span>Total <b>₹{Number(summary.billTotal || 0).toFixed(2)}</b></span>
                    <span>Paid <b>₹{Number(summary.paidAmount || 0).toFixed(2)}</b></span>
                    <span>Due <b>₹{Number(summary.remainingAmount || 0).toFixed(2)}</b></span>
                </div>
                <PaymentStatusBadge status={summary.paymentStatus} />
                <div className="payment-history-list">
                    {(summary.payments || []).length === 0 ? <p className="payment-muted">No payments recorded yet.</p> : summary.payments.map(payment => <article key={payment.id || payment.paymentReference}>
                        <div><strong>{String(payment.paymentMethod).replaceAll("_", " ")}</strong><small>{payment.paymentReference}{payment.transactionReference ? ` · ${payment.transactionReference}` : ""}</small></div>
                        <div><b>₹{Number(payment.amount || 0).toFixed(2)}</b><PaymentStatusBadge status={payment.paymentStatus} /></div>
                    </article>)}
                </div>
            </>}
        </section>
    </div>;
}
