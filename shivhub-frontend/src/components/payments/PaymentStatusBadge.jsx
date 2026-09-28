export default function PaymentStatusBadge({ status = "PENDING" }) {
    return <span className={`payment-status-badge payment-status-${String(status).toLowerCase()}`}>{String(status).replaceAll("_", " ")}</span>;
}
