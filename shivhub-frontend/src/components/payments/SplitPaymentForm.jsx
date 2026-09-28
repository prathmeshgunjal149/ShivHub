import { useMemo, useState } from "react";
import { recordOfflineManualPayment } from "../../services/paymentService";
import "./PaymentComponents.css";

const emptyRow = () => ({ paymentMethod: "CASH", amount: "", transactionReference: "", notes: "" });

export default function SplitPaymentForm({ billId, remainingAmount, onRecorded }) {
    const [rows, setRows] = useState([emptyRow(), emptyRow()]);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const entered = useMemo(() => rows.reduce((total, row) => total + (Number(row.amount) || 0), 0), [rows]);
    const change = Math.max(0, entered - Number(remainingAmount || 0));

    const changeRow = (index, field, value) => setRows(current => current.map((row, rowIndex) => rowIndex === index ? { ...row, [field]: value } : row));
    const submit = async event => {
        event.preventDefault(); setError("");
        const validRows = rows.filter(row => Number(row.amount) > 0);
        if (!validRows.length) return setError("Enter at least one payment amount.");
        if (entered > Number(remainingAmount || 0) && validRows.some(row => row.paymentMethod !== "CASH")) {
            return setError("Only cash can include change above the outstanding due.");
        }
        setSaving(true);
        try {
            let latest;
            for (const row of validRows) {
                latest = await recordOfflineManualPayment(billId, { ...row, amount: Number(row.amount) });
            }
            onRecorded?.(latest);
            setRows([emptyRow(), emptyRow()]);
        } catch (requestError) {
            setError(requestError?.response?.data?.message || requestError?.message || "Could not record split payment.");
        } finally { setSaving(false); }
    };

    return <form className="split-payment-form" onSubmit={submit}>
        <div className="split-heading"><strong>Split payment</strong><small>Due ₹{Number(remainingAmount || 0).toFixed(2)}</small></div>
        {rows.map((row, index) => <div className="split-row" key={index}>
            <select value={row.paymentMethod} onChange={event => changeRow(index, "paymentMethod", event.target.value)}>
                <option value="CASH">Cash</option><option value="UPI">Manual UPI</option><option value="CARD">Manual Card</option><option value="BANK_TRANSFER">Bank transfer</option><option value="CHEQUE">Cheque</option>
            </select>
            <input type="number" min="0" step="0.01" value={row.amount} onChange={event => changeRow(index, "amount", event.target.value)} placeholder="Amount" />
            <input value={row.transactionReference} onChange={event => changeRow(index, "transactionReference", event.target.value)} placeholder="Reference (optional)" />
            {rows.length > 1 && <button type="button" onClick={() => setRows(current => current.filter((_, rowIndex) => rowIndex !== index))} aria-label="Remove payment row">×</button>}
        </div>)}
        <div className="split-actions"><button type="button" onClick={() => setRows(current => [...current, emptyRow()])}>+ Add method</button><span>Total ₹{entered.toFixed(2)}{change > 0 ? ` · Change ₹${change.toFixed(2)}` : ""}</span><button type="submit" disabled={saving}>{saving ? "Recording…" : "Record split payment"}</button></div>
        {error && <p className="payment-error">{error}</p>}
    </form>;
}
