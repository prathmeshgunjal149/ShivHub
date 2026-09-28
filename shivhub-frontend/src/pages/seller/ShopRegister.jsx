import { useCallback, useEffect, useState } from "react";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./ShopOperations.css";

const today = () => new Date().toISOString().slice(0, 10);
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const initialEntry = () => ({ entryType: "CASH_SALE", paymentMode: "CASH", category: "ACCESSORY", itemName: "", amount: "", entryAt: `${today()}T${new Date().toTimeString().slice(0, 5)}`, customerName: "", customerMobile: "", whatsappConsent: false, notes: "", reason: "" });

export default function ShopRegister() {
    const [date, setDate] = useState(today());
    const [range, setRange] = useState({ from: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10), to: today() });
    const [summary, setSummary] = useState(null);
    const [entries, setEntries] = useState([]);
    const [history, setHistory] = useState([]);
    const [entry, setEntry] = useState(initialEntry);
    const [openingCash, setOpeningCash] = useState("");
    const [query, setQuery] = useState("");
    const [editing, setEditing] = useState(null);
    const [audit, setAudit] = useState([]);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const load = useCallback(async () => {
        try {
            setError("");
            const [daily, entryRows, historyRows] = await Promise.all([
                api.get("/api/seller/shop-register/summary", { params: { date } }),
                api.get("/api/seller/shop-register/entries", { params: { from: date, to: date, q: query || undefined } }),
                api.get("/api/seller/shop-register/history", { params: range })
            ]);
            setSummary(daily.data); setOpeningCash(daily.data?.openingCash ?? ""); setEntries(entryRows.data || []); setHistory(historyRows.data || []);
        } catch (requestError) { setError(requestError.response?.data?.message || "Could not load the non-GST shop register."); }
    }, [date, query, range]);

    useEffect(() => { void load(); }, [load]);
    const change = (key, value) => setEntry(current => ({ ...current, [key]: value }));

    const saveOpening = async event => {
        event.preventDefault();
        try { setBusy(true); setError(""); await api.post("/api/seller/shop-register/opening-cash", { businessDate: date, openingCash: Number(openingCash || 0), reason: "Opening cash set from shop register" }); setNotice("Opening cash saved."); await load(); }
        catch (requestError) { setError(requestError.response?.data?.message || "Could not save opening cash."); }
        finally { setBusy(false); }
    };
    const saveEntry = async event => {
        event.preventDefault();
        try {
            setBusy(true); setError("");
            const payload = { ...entry, amount: Number(entry.amount), entryAt: entry.entryAt || null };
            if (editing) await api.put(`/api/seller/shop-register/entries/${editing}`, payload); else await api.post("/api/seller/shop-register/entries", payload);
            setNotice(editing ? "Shop entry updated and audit saved." : "Non-GST shop entry saved."); setEntry(initialEntry()); setEditing(null); await load();
        } catch (requestError) { setError(requestError.response?.data?.message || "Could not save the shop entry."); }
        finally { setBusy(false); }
    };
    const edit = row => { setEditing(row.id); setEntry({ ...row, entryAt: row.entryAt?.slice(0, 16) || "", amount: row.amount ?? "", reason: "" }); window.scrollTo({ top: 0, behavior: "smooth" }); };
    const remove = async row => {
        const reason = window.prompt(`Delete ${row.itemName || row.category} entry? Enter the audit reason:`);
        if (!reason?.trim()) return;
        try { setBusy(true); await api.delete(`/api/seller/shop-register/entries/${row.id}`, { params: { reason } }); setNotice("Entry deleted safely; its audit record is retained."); await load(); }
        catch (requestError) { setError(requestError.response?.data?.message || "Could not delete the shop entry."); }
        finally { setBusy(false); }
    };
    const showAudit = async id => { try { setAudit((await api.get(`/api/seller/shop-register/entries/${id}/audits`)).data || []); } catch { setError("Could not load entry audit history."); } };
    const downloadCsv = async () => {
        try { const response = await api.get("/api/seller/shop-register/report.csv", { params: { ...range, q: query || undefined }, responseType: "blob" }); const href = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = href; link.download = "non-gst-shop-register.csv"; link.click(); URL.revokeObjectURL(href); }
        catch { setError("Could not export the shop report."); }
    };
    const cards = [["Opening cash", summary?.openingCash, "navy"], ["Cash in hand", summary?.expectedClosingCash, "green"], ["Online collection", summary?.onlineCollection, "blue"], ["Today’s expense", summary?.totalExpense, "red"]];

    return <div className="seller-data-page shop-operations-page"><SellerSidebar /><main className="seller-data-main"><header className="shop-operations-header"><div><span>NON-GST SHOP REGISTER</span><h1>Daily cashbook</h1><p>Quick counter entries only. Nothing on this page changes stock, GST, purchases, online orders or product approvals.</p></div><label>Business date<input type="date" value={date} onChange={event => setDate(event.target.value)} /></label></header>
        {error && <p className="shop-message error">{error}</p>}{notice && <p className="shop-message success">{notice}</p>}
        <section className="shop-summary-grid">{cards.map(([label, amount, tone]) => <article className={tone} key={label}><span>{label}</span><strong>{money(amount)}</strong>{label === "Cash in hand" && <small>Expected closing cash</small>}</article>)}</section>
        <section className="shop-split-grid"><form className="shop-card opening-cash-form" onSubmit={saveOpening}><div><span className="shop-eyebrow">DAY START</span><h2>Opening cash</h2><p>Set the cash physically present when the shop opens.</p></div><label>Cash amount<input type="number" min="0" step="0.01" required value={openingCash} onChange={event => setOpeningCash(event.target.value)} /></label><button disabled={busy}>Save opening cash</button></form>
            <section className="shop-card shop-breakdown"><span className="shop-eyebrow">TODAY’S COLLECTION</span><div><span>Cash sales</span><strong>{money(summary?.cashSales)}</strong></div><div><span>UPI</span><strong>{money(summary?.upiCollection)}</strong></div><div><span>Card / bank</span><strong>{money(Number(summary?.cardCollection || 0) + Number(summary?.bankCollection || 0))}</strong></div><div><span>Withdrawals</span><strong>{money(summary?.cashWithdrawals)}</strong></div></section></section>
        <section className="shop-card"><div className="shop-card-head"><div><span className="shop-eyebrow">QUICK ENTRY</span><h2>{editing ? "Edit shop entry" : "Add shop entry"}</h2></div>{editing && <button type="button" className="shop-plain-button" onClick={() => { setEditing(null); setEntry(initialEntry()); }}>Cancel edit</button>}</div><form className="shop-entry-form" onSubmit={saveEntry}><label>Entry type<select value={entry.entryType} onChange={event => change("entryType", event.target.value)}>{["CASH_SALE", "ONLINE_SALE", "EXPENSE", "CASH_DEPOSIT", "CASH_WITHDRAWAL", "ADJUSTMENT"].map(value => <option key={value}>{value.replaceAll("_", " ")}</option>)}</select></label><label>Payment mode<select value={entry.paymentMode} onChange={event => change("paymentMode", event.target.value)}>{["CASH", "UPI", "CARD", "BANK_TRANSFER", "OTHER"].map(value => <option key={value}>{value.replaceAll("_", " ")}</option>)}</select></label><label>Category<input required value={entry.category} onChange={event => change("category", event.target.value)} placeholder="Recharge, glass, repair…" /></label><label>Item / service<input value={entry.itemName || ""} onChange={event => change("itemName", event.target.value)} placeholder="Tempered glass" /></label><label>Amount<input required type="number" step="0.01" value={entry.amount} onChange={event => change("amount", event.target.value)} /></label><label>Entry time<input required type="datetime-local" value={entry.entryAt} onChange={event => change("entryAt", event.target.value)} /></label><label>Customer name<input value={entry.customerName || ""} onChange={event => change("customerName", event.target.value)} /></label><label>Customer mobile<input inputMode="numeric" maxLength="10" value={entry.customerMobile || ""} onChange={event => change("customerMobile", event.target.value)} /></label><label className="shop-wide">Notes<textarea value={entry.notes || ""} onChange={event => change("notes", event.target.value)} placeholder="Optional counter note" /></label>{editing && <label className="shop-wide">Edit reason (required)<input required value={entry.reason || ""} onChange={event => change("reason", event.target.value)} /></label>}<label className="shop-check"><input type="checkbox" checked={entry.whatsappConsent} onChange={event => change("whatsappConsent", event.target.checked)} /> Customer agreed to WhatsApp offers</label><button className="shop-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save audited edit" : "Save non-GST entry"}</button></form></section>
        <section className="shop-card"><div className="shop-card-head"><div><span className="shop-eyebrow">DAILY HISTORY</span><h2>Cash history and search</h2></div><button type="button" className="shop-plain-button" onClick={downloadCsv}>Download CSV</button></div><div className="shop-report-filters"><label>From<input type="date" value={range.from} onChange={event => setRange(current => ({ ...current, from: event.target.value }))} /></label><label>To<input type="date" value={range.to} onChange={event => setRange(current => ({ ...current, to: event.target.value }))} /></label><label className="shop-search">Search selected day<input value={query} onChange={event => setQuery(event.target.value)} placeholder="Item, customer, amount, staff…" /></label></div><div className="shop-table-wrap"><table><thead><tr><th>Date</th><th>Opening</th><th>Cash closing</th><th>Cash sale</th><th>Online</th><th>Expense</th><th>Entries</th></tr></thead><tbody>{history.map(row => <tr key={row.businessDate}><td>{row.businessDate}</td><td>{money(row.openingCash)}</td><td><strong>{money(row.expectedClosingCash)}</strong></td><td>{money(row.cashSales)}</td><td>{money(row.onlineCollection)}</td><td>{money(row.totalExpense)}</td><td>{row.entryCount}</td></tr>)}{!history.length && <tr><td colSpan="7">No saved shop history for this period.</td></tr>}</tbody></table></div></section>
        <section className="shop-card"><div className="shop-card-head"><div><span className="shop-eyebrow">{date}</span><h2>Saved entries</h2></div></div><div className="shop-table-wrap"><table><thead><tr><th>Time / type</th><th>Item</th><th>Amount</th><th>Customer</th><th>Entered by</th><th /></tr></thead><tbody>{entries.map(row => <tr key={row.id}><td>{new Date(row.entryAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}<small>{row.entryType.replaceAll("_", " ")} · {row.paymentMode.replaceAll("_", " ")}</small></td><td><strong>{row.itemName || row.category}</strong><small>{row.category}{row.notes ? ` · ${row.notes}` : ""}</small></td><td>{money(row.amount)}</td><td>{row.customerName || "—"}<small>{row.customerMobile || ""}</small></td><td>{row.createdByName}</td><td className="shop-actions"><button type="button" onClick={() => edit(row)}>Edit</button><button type="button" onClick={() => void showAudit(row.id)}>Audit</button><button type="button" className="danger" onClick={() => void remove(row)}>Delete</button></td></tr>)}{!entries.length && <tr><td colSpan="6">No counter entries saved for this day.</td></tr>}</tbody></table></div>{audit.length > 0 && <div className="shop-audit"><h3>Entry audit history</h3>{audit.map(row => <p key={row.id}><strong>{row.action.replaceAll("_", " ")}</strong> by {row.actionByName} · {new Date(row.createdAt).toLocaleString("en-IN")}{row.reason ? ` · ${row.reason}` : ""}</p>)}</div>}</section>
    </main></div>;
}
