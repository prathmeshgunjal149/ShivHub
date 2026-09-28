export default function PaymentRetryButton({ onClick, disabled = false }) {
    return <button type="button" className="payment-retry-button" onClick={onClick} disabled={disabled}>Retry secure payment</button>;
}
