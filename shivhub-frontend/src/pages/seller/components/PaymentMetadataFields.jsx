const PAYMENT_METHODS = ["CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE", "OTHER"];

const referenceLabel = method => ({
    UPI: "UPI transaction / UTR",
    CARD: "Card last 4 digits",
    BANK_TRANSFER: "Bank reference / UTR",
    CHEQUE: "Cheque number",
    OTHER: "Transaction reference"
}[method] || "Transaction reference");

/**
 * Shared non-sensitive payment fields for collection and distributor-payment forms.
 * Full card numbers are deliberately never accepted or stored.
 */
export default function PaymentMetadataFields({ value = {}, onChange, includePromiseDate = false }) {
    const method = value.paymentMethod || "CASH";
    const update = patch => onChange({ ...value, ...patch });
    const isReferenceMethod = method !== "CASH";

    return (
        <>
            <select
                aria-label="Payment method"
                value={method}
                onChange={event => update({ paymentMethod: event.target.value, reference: "" })}
            >
                {PAYMENT_METHODS.map(paymentMethod => (
                    <option key={paymentMethod} value={paymentMethod}>
                        {paymentMethod.replaceAll("_", " ")}
                    </option>
                ))}
            </select>

            {isReferenceMethod && (
                <input
                    aria-label={referenceLabel(method)}
                    placeholder={referenceLabel(method)}
                    value={value.reference || ""}
                    maxLength={method === "CARD" ? 4 : 100}
                    inputMode={method === "CARD" ? "numeric" : "text"}
                    onChange={event => update({
                        reference: method === "CARD"
                            ? event.target.value.replace(/\D/g, "").slice(0, 4)
                            : event.target.value
                    })}
                />
            )}

            {method === "UPI" && (
                <input
                    aria-label="UPI ID"
                    placeholder="UPI ID (optional)"
                    value={value.upiId || ""}
                    maxLength="100"
                    onChange={event => update({ upiId: event.target.value })}
                />
            )}

            <input
                type="date"
                aria-label={method === "CHEQUE" ? "Cheque date" : "Payment date"}
                title={method === "CHEQUE" ? "Cheque date" : "Payment date"}
                value={value.paymentDate || ""}
                onChange={event => update({ paymentDate: event.target.value })}
            />

            {includePromiseDate && (
                <input
                    type="date"
                    aria-label="New promised due date"
                    title="New promised due date for any remaining amount"
                    value={value.promisedDueDate || ""}
                    onChange={event => update({ promisedDueDate: event.target.value })}
                />
            )}

            <input
                aria-label="Payment notes"
                placeholder="Payment notes (optional)"
                value={value.notes || ""}
                maxLength="500"
                onChange={event => update({ notes: event.target.value })}
            />
        </>
    );
}
