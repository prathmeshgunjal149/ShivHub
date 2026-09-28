import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    listMarketing,
    marketingHistory,
    removeMarketing,
    sendMarketingEmail
} from "../../../services/marketingService";
import "./MarketingDashboard.css";

const pluralType = value => value?.replace(/s$/, "").toUpperCase() || "OFFER";
const nice = value => value ? value[0] + value.slice(1).toLowerCase() : "Marketing";

export default function MarketingList({ fixedType }) {
    const { type } = useParams();
    const kind = fixedType || pluralType(type);
    const nav = useNavigate();
    const [items, setItems] = useState([]);
    const [error, setError] = useState("");
    const [working, setWorking] = useState(null);
    const [result, setResult] = useState(null);
    const [historyFor, setHistoryFor] = useState(null);
    const [history, setHistory] = useState([]);

    const load = useCallback(() => {
        setError("");
        listMarketing(kind)
            .then(setItems)
            .catch(() => setError("Could not load marketing data."));
    }, [kind]);

    useEffect(() => {
        const requestTimer = window.setTimeout(load, 0);
        return () => window.clearTimeout(requestTimer);
    }, [load]);

    const activeTitle = useMemo(
        () => items.find(item => item.id === historyFor)?.title,
        [items, historyFor]
    );

    const remove = async id => {
        if (!window.confirm("Delete this marketing item?")) return;
        setWorking(id);
        try {
            await removeMarketing(id);
            load();
        } catch {
            setError("Could not delete this item.");
        } finally {
            setWorking(null);
        }
    };

    const email = async id => {
        setWorking(id);
        setResult(null);
        setError("");
        try {
            const sendResult = await sendMarketingEmail(id);
            setResult(sendResult);
            await showHistory(id);
        } catch {
            setError("Email could not be sent. Check mail configuration.");
        } finally {
            setWorking(null);
        }
    };

    const showHistory = async id => {
        setHistoryFor(id);
        try {
            setHistory(await marketingHistory(id));
        } catch {
            setError("Could not load delivery history.");
        }
    };

    return (
        <main className="marketing">
            <Link to="/admin/marketing" className="back-link">← Marketing</Link>
            <header>
                <div>
                    <p className="eyebrow">{kind}</p>
                    <h1>{nice(kind)}s</h1>
                    <p>Preview, send and audit campaign delivery with opt-out safety.</p>
                </div>
                <button className="primary" onClick={() => nav(`/admin/marketing/${kind.toLowerCase()}s/new`)}>
                    + Create
                </button>
            </header>

            {error && <p className="marketing-error">{error}</p>}
            {result && (
                <section className="marketing-send-result">
                    <article><span>Sent</span><strong>{result.sent || 0}</strong></article>
                    <article><span>Skipped / opted out</span><strong>{result.skipped || 0}</strong></article>
                    <article><span>Failed</span><strong>{result.failed || 0}</strong></article>
                </section>
            )}

            <section className="marketing-table">
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Audience</th>
                            <th>Code / Discount</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.map(item => (
                            <tr key={item.id}>
                                <td><strong>{item.title}</strong><small>{item.description}</small></td>
                                <td>{item.audience}</td>
                                <td>{item.couponCode || "—"} {item.discountPercent ? `(${item.discountPercent}%)` : ""}</td>
                                <td><span className={item.active ? "active-pill" : "off-pill"}>{item.active ? "Active" : "Inactive"}</span></td>
                                <td className="actions">
                                    <button onClick={() => nav(`/admin/marketing/${kind.toLowerCase()}s/${item.id}`)}>Edit</button>
                                    <button disabled={working === item.id} onClick={() => email(item.id)}>
                                        {working === item.id ? "Sending..." : "Email"}
                                    </button>
                                    <button onClick={() => showHistory(item.id)}>History</button>
                                    <button className="danger" disabled={working === item.id} onClick={() => remove(item.id)}>Delete</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {!items.length && <p className="empty">No {kind.toLowerCase()}s yet.</p>}
            </section>

            {historyFor && (
                <section className="marketing-history-panel">
                    <div className="marketing-history-head">
                        <div>
                            <p className="eyebrow">Delivery history</p>
                            <h2>{activeTitle || "Campaign"}</h2>
                        </div>
                        <button onClick={() => setHistoryFor(null)}>Close</button>
                    </div>
                    <div className="marketing-history-list">
                        {history.map(row => (
                            <article key={row.id} className={`delivery-${String(row.status || "").toLowerCase()}`}>
                                <strong>{row.recipientName || row.recipientEmail}</strong>
                                <span>{row.recipientEmail}</span>
                                <b>{row.status}</b>
                                <small>{row.message || "—"} · {String(row.createdAt || "").replace("T", " ").slice(0, 16)}</small>
                            </article>
                        ))}
                        {!history.length && <p className="empty">No delivery records yet.</p>}
                    </div>
                </section>
            )}
        </main>
    );
}
