// Informational service promises only; delivery availability remains checkout-specific.
export default function CustomerBenefits({ t = value => value }) {
    const services = [
        [BadgeCheck, "verifiedSellers", "verifiedSellersNote"],
        [Truck, "deliveryOffers", "deliveryOffersNote"],
        [CreditCard, "securePayments", "securePaymentsNote"],
        [Clock3, "anytimeAccess", "anytimeAccessNote"]
    ];

    return <section className="marketplace-promises marketplace-service-promises" aria-label="ShivHub services">
        {services.map(([Icon, title, note]) => <article key={title}>
            <span className="marketplace-service-icon"><Icon size={22} aria-hidden="true" /></span>
            <div><strong>{t(title)}</strong><small>{t(note)}</small></div>
        </article>)}
    </section>;
}
import { BadgeCheck, Clock3, CreditCard, Truck } from "lucide-react";
