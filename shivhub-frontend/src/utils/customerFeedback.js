/**
 * A small application-wide customer feedback channel. Events are emitted only
 * after the existing API action succeeds, so the animation never claims an
 * order, cart change, or reward that was not actually completed.
 */
export const showCustomerFeedback = detail => {
    if (typeof window === "undefined") return;
    window.dispatchEvent(new CustomEvent("shivhub:customer-feedback", { detail }));
};
