import { useEffect, useState } from "react";
import api from "../../services/api";
import { variantSummary } from "./productCommonFields";

export default function ReturnedVariantStock({ requestId }) {
    const [info, setInfo] = useState(null);
    const [quantity, setQuantity] = useState("1");
    const [disposition, setDisposition] = useState("DEFECTIVE");
    const [remarks, setRemarks] = useState("");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    useEffect(() => {
        let active = true;
        setInfo(null); setError(""); setQuantity("1"); setDisposition("DEFECTIVE"); setRemarks("");
        api.get(`/api/seller/after-sales/requests/${requestId}/variant-disposition`)
            .then(({ data }) => { if (active) setInfo(data); })
            .catch(e => { if (active) setError(e.response?.data?.message || "Could not check the original variant return stock."); });
        return () => { active = false; };
    }, [requestId]);
    const save = async () => {
        setBusy(true); setError("");
        try {
            await api.post(`/api/seller/after-sales/requests/${requestId}/variant-disposition`, { disposition, quantity: Number(quantity), remarks });
            const { data } = await api.get(`/api/seller/after-sales/requests/${requestId}/variant-disposition`);
            setInfo(data);
        } catch (e) { setError(e.response?.data?.message || "Returned stock could not be saved."); }
        finally { setBusy(false); }
    };
    if (info && !info.variantProduct) return null;
    return <details><summary>Returned variant stock</summary>
        {error && <p role="alert" className="after-sales-alert error">{error}</p>}
        {!info ? <p>Checking original sale variant…</p> : <>
            <p>{variantSummary(info.selectedAttributes)}</p>
            {info.disposition ? <p role="status">Recorded: {info.disposition.replaceAll("_", " ")} · {info.stockRestored ? "Stock restored to the original variant" : "Kept out of sellable stock"}.</p> : <>
                <p>Inspect the received product first. Only AVAILABLE restores the exact original variant. Refund and stock disposition are separate audited actions.</p>
                <label>Returned quantity<input type="number" min="1" max={info.remainingQuantity} step="1" value={quantity} onChange={event => setQuantity(event.target.value)} /></label>
                <label>Final condition<select value={disposition} onChange={event => setDisposition(event.target.value)}>{["AVAILABLE", "DEFECTIVE", "SERVICE_CENTER", "SCRAP", "RETURNED_TO_DISTRIBUTOR"].map(value => <option key={value}>{value}</option>)}</select></label>
                <label>Inspection/disposition remarks<textarea maxLength={5000} value={remarks} onChange={event => setRemarks(event.target.value)} /></label>
                <button type="button" disabled={busy || info.remainingQuantity < 1} onClick={save}>{busy ? "Saving…" : "Record inspected return stock"}</button>
            </>}
        </>}
    </details>;
}
