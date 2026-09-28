import { useEffect, useRef } from "react";

const SCRIPT_URL = "https://checkout.razorpay.com/v1/checkout.js";

const loadScript = () => new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const existing = document.querySelector(`script[src="${SCRIPT_URL}"]`);
    if (existing) { existing.addEventListener("load", resolve, { once: true }); existing.addEventListener("error", reject, { once: true }); return; }
    const script = document.createElement("script"); script.src = SCRIPT_URL; script.async = true;
    script.onload = resolve; script.onerror = () => reject(new Error("Razorpay checkout could not be loaded.")); document.body.appendChild(script);
});

/** Opens Standard Checkout for a backend-created Razorpay Order only. */
export default function RazorpayCheckout({ payment, internalId, onSuccess, onDismiss, onError }) {
    const opened = useRef(false);
    useEffect(() => {
        if (!payment || opened.current) return undefined;
        let checkout;
        const open = async () => {
            try {
                await loadScript();
                if (!window.Razorpay) throw new Error("Razorpay checkout is unavailable.");
                opened.current = true;
                checkout = new window.Razorpay({
                    key: payment.keyId,
                    amount: payment.amount,
                    currency: payment.currency,
                    name: "ShivHub",
                    description: payment.description,
                    order_id: payment.razorpayOrderId,
                    prefill: { name: payment.customerName || "", email: payment.customerEmail || "", contact: payment.customerMobile || "" },
                    theme: { color: "#2563eb" },
                    modal: { ondismiss: () => onDismiss?.() },
                    handler: async response => {
                        try { await onSuccess?.({ internalOrderId: internalId, razorpayOrderId: response.razorpay_order_id, razorpayPaymentId: response.razorpay_payment_id, razorpaySignature: response.razorpay_signature }); }
                        catch (error) { onError?.(error); }
                    }
                });
                checkout.on("payment.failed", response => onError?.(new Error(response?.error?.description || "Razorpay payment failed.")));
                checkout.open();
            } catch (error) { onError?.(error); }
        };
        open();
        return () => checkout?.close?.();
    }, [payment, internalId, onSuccess, onDismiss, onError]);
    return null;
}
