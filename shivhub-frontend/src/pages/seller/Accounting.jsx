import { useCallback, useEffect, useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import {
    closeFinancialYear,
    createFinancialYear,
    createLedgerAccount,
    listFinancialYears,
    listJournalEntries,
    listLedgerAccounts,
    listOpeningBalances,
    saveOpeningBalance,
    seedLedgerAccounts
} from "../../services/accountingService";
import { getSellerProducts } from "../../services/purchaseService";
import "./Accounting.css";

const fyDefault = () => {
    const now = new Date();
    const startYear = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
    return {
        yearCode: `${startYear}-${String(startYear + 1).slice(-2)}`,
        startDate: `${startYear}-04-01`,
        endDate: `${startYear + 1}-03-31`
    };
};

export default function Accounting() {
    const [years, setYears] = useState([]);
    const [accounts, setAccounts] = useState([]);
    const [journals, setJournals] = useState([]);
    const [products, setProducts] = useState([]);
    const [opening, setOpening] = useState([]);
    const [fyForm, setFyForm] = useState(fyDefault());
    const [accountForm, setAccountForm] = useState({ code: "", name: "", accountType: "ASSET" });
    const [openingForm, setOpeningForm] = useState({ financialYearId: "", balanceType: "CASH", amount: "", quantity: "", unitValue: "", productId: "", notes: "" });
    const [error, setError] = useState("");
    const [saving, setSaving] = useState("");

    const activeYear = useMemo(() => years.find(y => String(y.id) === String(openingForm.financialYearId)), [years, openingForm.financialYearId]);

    const load = useCallback(async () => {
        try {
            setError("");
            const [fy, ledger, journal, productRows] = await Promise.all([
                listFinancialYears(),
                listLedgerAccounts(),
                listJournalEntries(),
                getSellerProducts()
            ]);
            setYears(Array.isArray(fy) ? fy : []);
            setAccounts(Array.isArray(ledger) ? ledger : []);
            setJournals(Array.isArray(journal) ? journal : []);
            setProducts(Array.isArray(productRows) ? productRows : []);
            if (Array.isArray(fy) && fy[0] ) {
                setOpeningForm(prev => ({ ...prev, financialYearId: prev.financialYearId || fy[0].id }));
            }
        } catch (err) {
            setError(err.response?.data?.message || "Could not load accounting data.");
        }
    }, []);

    useEffect(() => { void load(); }, [load]);
    useEffect(() => {
        if (!openingForm.financialYearId) return;
        listOpeningBalances(openingForm.financialYearId).then(data => setOpening(Array.isArray(data) ? data : [])).catch(() => setOpening([]));
    }, [openingForm.financialYearId]);

    const submitFy = async e => {
        e.preventDefault();
        await act("fy", async () => { await createFinancialYear(fyForm); setFyForm(fyDefault()); await load(); });
    };

    const submitAccount = async e => {
        e.preventDefault();
        await act("account", async () => { await createLedgerAccount(accountForm); setAccountForm({ code: "", name: "", accountType: "ASSET" }); await load(); });
    };

    const submitOpening = async e => {
        e.preventDefault();
        await act("opening", async () => {
            await saveOpeningBalance({
                ...openingForm,
                amount: Number(openingForm.amount || 0),
                quantity: openingForm.quantity ? Number(openingForm.quantity) : null,
                unitValue: openingForm.unitValue ? Number(openingForm.unitValue) : null,
                productId: openingForm.productId || null
            });
            setOpeningForm(prev => ({ ...prev, amount: "", quantity: "", unitValue: "", productId: "", notes: "" }));
            setOpening(await listOpeningBalances(openingForm.financialYearId));
        });
    };

    const act = async (key, fn) => {
        try {
            setSaving(key);
            setError("");
            await fn();
        } catch (err) {
            setError(err.response?.data?.message || "Action failed.");
        } finally {
            setSaving("");
        }
    };

    return <div className="accounting-page">
        <SellerSidebar />
        <main className="accounting-main">
            <header>
                <div><span>ACCOUNTING</span><h1>Ledger setup</h1><p>Financial years, ledger accounts and opening balances for CA-grade reports.</p></div>
                <button type="button" onClick={() => act("seed", async () => { await seedLedgerAccounts(); await load(); })} disabled={saving === "seed"}>
                    {saving === "seed" ? "Seeding..." : "Seed default accounts"}
                </button>
            </header>

            {error && <div className="accounting-error">{error}</div>}

            <section className="accounting-grid">
                <article className="accounting-card">
                    <h2>Financial year</h2>
                    <form onSubmit={submitFy} className="accounting-form">
                        <label>Code<input value={fyForm.yearCode} onChange={e => setFyForm({ ...fyForm, yearCode: e.target.value })} /></label>
                        <label>Start<input type="date" value={fyForm.startDate} onChange={e => setFyForm({ ...fyForm, startDate: e.target.value })} /></label>
                        <label>End<input type="date" value={fyForm.endDate} onChange={e => setFyForm({ ...fyForm, endDate: e.target.value })} /></label>
                        <button disabled={saving === "fy"}>{saving === "fy" ? "Saving..." : "Create FY"}</button>
                    </form>
                    <div className="accounting-list">
                        {years.map(y => <div key={y.id}><span>{y.yearCode}</span><small>{y.startDate} to {y.endDate}</small><button type="button" disabled={y.closed} onClick={() => act("close", async () => { await closeFinancialYear(y.id); await load(); })}>{y.closed ? "Closed" : "Close"}</button></div>)}
                    </div>
                </article>

                <article className="accounting-card">
                    <h2>Ledger account</h2>
                    <form onSubmit={submitAccount} className="accounting-form">
                        <label>Code<input required value={accountForm.code} onChange={e => setAccountForm({ ...accountForm, code: e.target.value })} /></label>
                        <label>Name<input required value={accountForm.name} onChange={e => setAccountForm({ ...accountForm, name: e.target.value })} /></label>
                        <label>Type<select value={accountForm.accountType} onChange={e => setAccountForm({ ...accountForm, accountType: e.target.value })}>{["ASSET","LIABILITY","EQUITY","INCOME","EXPENSE"].map(x => <option key={x}>{x}</option>)}</select></label>
                        <button disabled={saving === "account"}>{saving === "account" ? "Saving..." : "Add account"}</button>
                    </form>
                    <div className="accounting-list compact">
                        {accounts.slice(0, 8).map(a => <div key={a.id}><span>{a.code} - {a.name}</span><small>{a.accountType}</small></div>)}
                    </div>
                </article>
            </section>

            <section className="accounting-card full">
                <h2>Opening balances</h2>
                <form onSubmit={submitOpening} className="accounting-form opening">
                    <label>FY<select value={openingForm.financialYearId} onChange={e => setOpeningForm({ ...openingForm, financialYearId: e.target.value })}>{years.map(y => <option key={y.id} value={y.id}>{y.yearCode}</option>)}</select></label>
                    <label>Type<select value={openingForm.balanceType} onChange={e => setOpeningForm({ ...openingForm, balanceType: e.target.value })}>{["CASH","BANK","CUSTOMER_RECEIVABLE","DISTRIBUTOR_PAYABLE","STOCK_VALUE","ASSET","LOAN","CAPITAL"].map(x => <option key={x}>{x}</option>)}</select></label>
                    <label>Amount<input type="number" min="0" value={openingForm.amount} onChange={e => setOpeningForm({ ...openingForm, amount: e.target.value })} /></label>
                    <label>Product<select value={openingForm.productId} onChange={e => setOpeningForm({ ...openingForm, productId: e.target.value })}><option value="">None</option>{products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
                    <label>Qty<input type="number" min="0" value={openingForm.quantity} onChange={e => setOpeningForm({ ...openingForm, quantity: e.target.value })} /></label>
                    <label>Unit value<input type="number" min="0" value={openingForm.unitValue} onChange={e => setOpeningForm({ ...openingForm, unitValue: e.target.value })} /></label>
                    <label>Notes<input value={openingForm.notes} onChange={e => setOpeningForm({ ...openingForm, notes: e.target.value })} /></label>
                    <button disabled={!activeYear || saving === "opening"}>{saving === "opening" ? "Saving..." : "Save opening"}</button>
                </form>
                <table><thead><tr><th>Type</th><th>Amount</th><th>Qty</th><th>Product</th><th>Status</th></tr></thead><tbody>{opening.map(row => <tr key={row.id}><td>{row.balanceType}</td><td>{Number(row.amount || 0).toFixed(2)}</td><td>{row.quantity || "-"}</td><td>{row.product?.name || "-"}</td><td>{row.posted ? "Posted" : "Draft"}</td></tr>)}</tbody></table>
            </section>

            <section className="accounting-card full">
                <h2>Recent journal entries</h2>
                <table><thead><tr><th>Date</th><th>Source</th><th>Event</th><th>Status</th><th>Description</th></tr></thead><tbody>{journals.length === 0 ? <tr><td colSpan="5">No journal entries posted yet.</td></tr> : journals.slice(0, 20).map(j => <tr key={j.id}><td>{j.entryDate}</td><td>{j.sourceType} #{j.sourceId}</td><td>{j.eventType}</td><td>{j.status}</td><td>{j.description || "-"}</td></tr>)}</tbody></table>
            </section>
        </main>
    </div>;
}

