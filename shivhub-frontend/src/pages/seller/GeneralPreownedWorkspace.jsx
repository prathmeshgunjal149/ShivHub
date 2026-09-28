import { useNavigate } from "react-router-dom";
import SellerSidebar from "./SellerSidebar";
import "./SellerWorkspaces.css";

const workspaceCards = [
    {
        title: "Products",
        note: "Manage general products, used phones and refurbished listings through seller-owned products.",
        path: "/seller/products"
    },
    {
        title: "Purchases",
        note: "Record distributor/shop/individual acquisition in the existing purchase workflow before stock is available.",
        path: "/seller/purchases"
    },
    {
        title: "Stock",
        note: "Use the existing inventory source of truth. IMEI/device units remain protected from double selling.",
        path: "/seller/inventory"
    },
    {
        title: "Billing",
        note: "Sell available units through POS/offline billing with immutable invoice snapshots.",
        path: "/seller/billing"
    },
    {
        title: "Payments & outstanding",
        note: "Track customer payments and distributor outstanding balances using existing ledgers.",
        path: "/seller/payments"
    },
    {
        title: "Returns / cancellations",
        note: "Use order, billing and stock movement modules so reversals happen once.",
        path: "/seller/orders"
    },
    {
        title: "Reports",
        note: "Review sales, stock, CA exports and accounting reports without double counting.",
        path: "/seller/reports"
    }
];

export default function GeneralPreownedWorkspace() {
    const navigate = useNavigate();

    return (
        <div className="seller-workspace-page">
            <SellerSidebar />
            <main className="seller-workspace-main">
                <header className="workspace-hero">
                    <div>
                        <span>GENERAL & PRE-OWNED</span>
                        <h1>General and pre-owned workspace</h1>
                        <p>Separate workspace for ordinary stock, used phones and refurbished phones. Tax treatment and physical condition remain separate decisions.</p>
                    </div>
                    <button type="button" onClick={() => navigate("/seller/add-accessories")}>Add accessory</button>
                </header>

                <section className="workspace-card">
                    <h2>Condition and tax guardrails</h2>
                    <div className="workspace-note-grid">
                        <article><strong>Used / refurbished is not tax-exempt by default.</strong><span>Purchase tax treatment and sale tax treatment must be configured independently.</span></article>
                        <article><strong>Stock must come from purchase/receipt.</strong><span>Creating a listing does not create physical inventory or IMEI units.</span></article>
                        <article><strong>Device-level details stay private.</strong><span>Previous-owner contact and acquisition documents are never public catalogue data.</span></article>
                    </div>
                </section>

                <section className="workspace-link-grid">
                    {workspaceCards.map(card => (
                        <button type="button" key={card.title} onClick={() => navigate(card.path)}>
                            <span>{card.title}</span>
                            <p>{card.note}</p>
                            <b>Open →</b>
                        </button>
                    ))}
                </section>
            </main>
        </div>
    );
}
