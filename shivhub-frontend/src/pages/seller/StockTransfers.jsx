import { useCallback, useEffect, useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import { getActiveSellerDistributors, getSellerProducts } from "../../services/purchaseService";
import {
    confirmStockTransferAdjustment,
    confirmStockTransferHandover,
    createStockTransfer,
    getAvailableImeis,
    listStockTransfers
} from "../../services/stockTransferService";
import "./StockTransfers.css";

const blankTransfer = {
    sellerDistributorId: "",
    productId: "",
    serialId: "",
    receivingShopName: "",
    receivingShopContact: "",
    receivingShopAddress: "",
    adjustmentAmount: "",
    distributorReference: "",
    instructionAttachmentUrl: "",
    notes: ""
};

const serialLabel = item => [item.imei1, item.serialNumber, item.imei2].filter(Boolean).join(" / ") || `Serial #${item.serialId || item.id}`;

export default function StockTransfers() {
    const [transfers, setTransfers] = useState([]);
    const [distributors, setDistributors] = useState([]);
    const [products, setProducts] = useState([]);
    const [serials, setSerials] = useState([]);
    const [status, setStatus] = useState("ALL");
    const [form, setForm] = useState(blankTransfer);
    const [adjust, setAdjust] = useState({});
    const [saving, setSaving] = useState("");
    const [error, setError] = useState("");

    const selectedProduct = useMemo(() => products.find(p => String(p.id) === String(form.productId)), [products, form.productId]);

    const load = useCallback(async () => {
        try {
            setError("");
            const [transferRows, distributorRows, productRows] = await Promise.all([
                listStockTransfers(status),
                getActiveSellerDistributors(),
                getSellerProducts()
            ]);
            setTransfers(Array.isArray(transferRows) ? transferRows : []);
            setDistributors(Array.isArray(distributorRows) ? distributorRows : []);
            setProducts(Array.isArray(productRows) ? productRows : []);
        } catch (err) {
            setError(err.response?.data?.message || "Could not load stock transfers.");
        }
    }, [status]);

    useEffect(() => { void load(); }, [load]);
    useEffect(() => {
        if (!form.productId) {
            setSerials([]);
            return;
        }
        getAvailableImeis(form.productId).then(data => setSerials(Array.isArray(data) ? data : [])).catch(() => setSerials([]));
    }, [form.productId]);

    const submit = async e => {
        e.preventDefault();
        await act("create", async () => {
            await createStockTransfer({
                ...form,
                sellerDistributorId: Number(form.sellerDistributorId),
                serialId: Number(form.serialId),
                adjustmentAmount: Number(form.adjustmentAmount)
            });
            setForm(blankTransfer);
            setSerials([]);
            await load();
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

    return <div className="transfer-page">
        <SellerSidebar />
        <main className="transfer-main">
            <header>
                <div><span>STOCK TRANSFER</span><h1>Distributor adjustments</h1><p>Move an actual IMEI out of stock and settle the distributor credit note separately.</p></div>
                <select value={status} onChange={e => setStatus(e.target.value)}>
                    <option value="ALL">All transfers</option>
                    <option value="DRAFT">Draft</option>
                    <option value="HANDOVER_CONFIRMED">Adjustment pending</option>
                    <option value="ADJUSTMENT_CONFIRMED">Settled</option>
                </select>
            </header>

            {error && <div className="transfer-error">{error}</div>}

            <section className="transfer-card">
                <h2>Create transfer</h2>
                <form onSubmit={submit} className="transfer-form">
                    <label>Distributor<select required value={form.sellerDistributorId} onChange={e => setForm({ ...form, sellerDistributorId: e.target.value })}><option value="">Select distributor</option>{distributors.map(d => <option key={d.id} value={d.id}>{d.distributorName || d.businessName || `Distributor #${d.id}`}</option>)}</select></label>
                    <label>Product<select required value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value, serialId: "" })}><option value="">Select product</option>{products.map(p => <option key={p.id} value={p.id}>{p.name} ({p.stock || 0})</option>)}</select></label>
                    <label>IMEI / Serial<select required value={form.serialId} onChange={e => setForm({ ...form, serialId: e.target.value })}><option value="">{selectedProduct ? "Select available unit" : "Select product first"}</option>{serials.map(s => <option key={s.serialId || s.id} value={s.serialId || s.id}>{serialLabel(s)}</option>)}</select></label>
                    <label>Receiving shop<input required value={form.receivingShopName} onChange={e => setForm({ ...form, receivingShopName: e.target.value })} /></label>
                    <label>Contact<input value={form.receivingShopContact} onChange={e => setForm({ ...form, receivingShopContact: e.target.value })} /></label>
                    <label>Adjustment amount<input required type="number" min="0.01" value={form.adjustmentAmount} onChange={e => setForm({ ...form, adjustmentAmount: e.target.value })} /></label>
                    <label>Distributor ref<input value={form.distributorReference} onChange={e => setForm({ ...form, distributorReference: e.target.value })} /></label>
                    <label>Address<input value={form.receivingShopAddress} onChange={e => setForm({ ...form, receivingShopAddress: e.target.value })} /></label>
                    <label>Notes<input value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></label>
                    <button disabled={saving === "create"}>{saving === "create" ? "Saving..." : "Create entry"}</button>
                </form>
            </section>

            <section className="transfer-card">
                <div className="transfer-title"><h2>Transfer entries</h2><button type="button" onClick={load}>Refresh</button></div>
                <div className="transfer-table-wrap">
                    <table>
                        <thead><tr><th>Status</th><th>IMEI</th><th>Product</th><th>Distributor</th><th>Receiving shop</th><th>Amount</th><th>Reference</th><th>Actions</th></tr></thead>
                        <tbody>{transfers.length === 0 ? <tr><td colSpan="8">No stock transfer entries found.</td></tr> : transfers.map(row => <tr key={row.id}>
                            <td><span className={`transfer-status ${String(row.status || "").toLowerCase()}`}>{row.status}</span></td>
                            <td>{row.imeiOrSerial}</td>
                            <td>{row.productName}</td>
                            <td>{row.distributorName}</td>
                            <td>{row.receivingShopName}<small>{row.receivingShopContact || ""}</small></td>
                            <td>Rs. {Number(row.adjustmentAmount || 0).toFixed(2)}</td>
                            <td>{row.distributorReference || "-"}</td>
                            <td className="transfer-actions">
                                {row.status === "DRAFT" && <button type="button" onClick={() => act(`handover-${row.id}`, async () => { await confirmStockTransferHandover(row.id); await load(); })}>Confirm handover</button>}
                                {row.status === "HANDOVER_CONFIRMED" && <>
                                    <input placeholder="Credit note no." value={adjust[row.id]?.creditNoteNumber || ""} onChange={e => setAdjust({ ...adjust, [row.id]: { ...(adjust[row.id] || {}), creditNoteNumber: e.target.value } })} />
                                    <button type="button" onClick={() => act(`adjust-${row.id}`, async () => { await confirmStockTransferAdjustment(row.id, adjust[row.id] || {}); await load(); })}>Confirm adjustment</button>
                                </>}
                                {row.status === "ADJUSTMENT_CONFIRMED" && <span>Credit note {row.creditNoteNumber || row.creditNoteId}</span>}
                            </td>
                        </tr>)}</tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>;
}

