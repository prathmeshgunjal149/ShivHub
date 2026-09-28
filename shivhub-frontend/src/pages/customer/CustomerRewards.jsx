import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import { getLoyaltyTransactions } from "../../services/loyaltyService";
import "./CustomerRewards.css";

const formatReference = transaction => transaction.sourceId ? `${transaction.sourceType?.replaceAll("_", " ") || "Reference"} #${transaction.sourceId}` : transaction.sourceType?.replaceAll("_", " ") || "Manual adjustment";

export default function CustomerRewards() {
    const navigate = useNavigate();
    const { loyaltySummary, refreshLoyalty } = useAuth();
    const [history, setHistory] = useState({ content: [], number: 0, totalPages: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const load = useCallback(async page => {
        try {
            setLoading(true); setError("");
            const response = await getLoyaltyTransactions(page, 20);
            setHistory(response);
            await refreshLoyalty();
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to load rewards history.");
        } finally { setLoading(false); }
    }, [refreshLoyalty]);

    useEffect(() => { load(0); }, [load]); // Load once; subsequent pages are explicit user actions.
    const summary = loyaltySummary || { availablePoints: 0, lifetimeEarnedPoints: 0, lifetimeRedeemedPoints: 0, expiringPoints: 0 };

    return <main className="customer-rewards-page">
        <div className="customer-rewards-topbar"><button type="button" onClick={() => navigate("/customer/dashboard")}>← Dashboard</button><h1>My Rewards</h1></div>
        <section className="customer-rewards-cards" aria-label="Loyalty summary">
            <article><span>Available ShivHub Points</span><strong>{summary.availablePoints}</strong></article>
            <article><span>Lifetime earned</span><strong>{summary.lifetimeEarnedPoints}</strong></article>
            <article><span>Lifetime redeemed</span><strong>{summary.lifetimeRedeemedPoints}</strong></article>
        </section>
        <section className="customer-rewards-history">
            <div><h2>Points history</h2><p>Actual points earned, redeemed, reversed, and adjusted for your account.</p></div>
            {error && <p className="customer-rewards-error">{error}</p>}
            {loading ? <p>Loading points history…</p> : history.content?.length ? <div className="customer-rewards-table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Reference</th><th>Points</th><th>Balance</th><th>Remarks</th></tr></thead><tbody>{history.content.map(transaction => <tr key={transaction.id}><td>{transaction.createdAt ? new Date(transaction.createdAt).toLocaleString() : "—"}</td><td>{transaction.transactionType}</td><td>{formatReference(transaction)}</td><td className={transaction.points >= 0 ? "points-positive" : "points-negative"}>{transaction.points >= 0 ? "+" : ""}{transaction.points}</td><td>{transaction.balanceAfter}</td><td>{transaction.remarks || "—"}</td></tr>)}</tbody></table></div> : <p>No loyalty transactions yet.</p>}
            <div className="customer-rewards-pagination"><button type="button" disabled={!history.number || loading} onClick={() => load(history.number - 1)}>Previous</button><span>Page {(history.number || 0) + 1} of {Math.max(1, history.totalPages || 1)}</span><button type="button" disabled={loading || history.number >= history.totalPages - 1} onClick={() => load(history.number + 1)}>Next</button></div>
        </section>
    </main>;
}
