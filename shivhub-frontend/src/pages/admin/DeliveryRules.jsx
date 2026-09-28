import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "./AdminTools.css";
import "./DeliveryRules.css";

const blank = {
    name: "", productScope: "MOBILE_ONLY", minimumDistanceKm: "0", maximumDistanceKm: "",
    estimatedDeliveryText: "", estimatedMinutes: "", deliveryCharge: "0", priority: 0,
    active: true, serviceAvailable: true,
};

export default function DeliveryRules() {
    const [rules, setRules] = useState([]);
    const [form, setForm] = useState(blank);
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [distance, setDistance] = useState("");
    const [preview, setPreview] = useState(null);

    const load = async () => {
        const { data } = await api.get("/api/admin/delivery-rules");
        setRules(Array.isArray(data) ? data : []);
    };

    useEffect(() => {
        let active = true;
        api.get("/api/admin/delivery-rules")
            .then(({ data }) => { if (active) setRules(Array.isArray(data) ? data : []); })
            .catch(result => { if (active) setError(result.response?.data?.message || "Unable to load delivery rules."); });
        return () => { active = false; };
    }, []);

    const change = (key, value) => setForm(current => ({ ...current, [key]: value }));
    const asPayload = () => ({
        ...form,
        minimumDistanceKm: Number(form.minimumDistanceKm),
        maximumDistanceKm: form.maximumDistanceKm === "" ? null : Number(form.maximumDistanceKm),
        estimatedMinutes: form.estimatedMinutes === "" ? null : Number(form.estimatedMinutes),
        deliveryCharge: Number(form.deliveryCharge || 0), priority: Number(form.priority),
    });

    const save = async event => {
        event.preventDefault(); setSaving(true); setError("");
        try {
            await api[form.id ? "put" : "post"](`/api/admin/delivery-rules${form.id ? `/${form.id}` : ""}`, asPayload());
            setForm(blank); await load();
        } catch (result) { setError(result.response?.data?.message || "Delivery rule could not be saved."); }
        finally { setSaving(false); }
    };

    const toggle = async rule => {
        try { setError(""); await api.patch(`/api/admin/delivery-rules/${rule.id}/status`, { active: !rule.active }); await load(); }
        catch (result) { setError(result.response?.data?.message || "Could not change rule status."); }
    };
    const remove = async rule => {
        if (!window.confirm("Deactivate this rule? Existing order estimates will be preserved.")) return;
        try { setError(""); await api.delete(`/api/admin/delivery-rules/${rule.id}`); await load(); }
        catch (result) { setError(result.response?.data?.message || "Could not deactivate rule."); }
    };
    const test = async event => {
        event.preventDefault(); setError("");
        try { const { data } = await api.post("/api/admin/delivery-rules/preview", { distanceKm: Number(distance) }); setPreview(data || { estimatedDeliveryText: "No active rule matches this distance." }); }
        catch (result) { setError(result.response?.data?.message || "Preview failed."); }
    };
    const edit = rule => setForm({ ...rule, maximumDistanceKm: rule.maximumDistanceKm ?? "", estimatedMinutes: rule.estimatedMinutes ?? "" });

    return (
        <main className="admin-tools delivery-rules-page">
            <header>
                <div><p className="eyebrow">FULFILMENT</p><h1>Delivery Time & Distance Rules</h1><p>Set the customer-facing ETA by distance. Minimum is inclusive and maximum is exclusive.</p></div>
                <Link to="/admin/dashboard">Dashboard</Link>
            </header>
            {error && <p className="error" role="alert">{error}</p>}

            <form className="delivery-rule-form" onSubmit={save}>
                <section>
                    <div className="delivery-rule-section-heading"><div><h2>{form.id ? "Edit delivery rule" : "Add delivery rule"}</h2><p>Example: 5–15 km → Delivery within 1 hour.</p></div>{form.id && <button type="button" className="delivery-rule-link-button" onClick={() => setForm(blank)}>Create new rule</button>}</div>
                    <div className="delivery-rule-grid">
                        <label>Rule name<input required value={form.name} onChange={event => change("name", event.target.value)} placeholder="Pune local delivery" /></label>
                        <label>Minimum distance (km)<input required type="number" step="0.001" min="0" value={form.minimumDistanceKm} onChange={event => change("minimumDistanceKm", event.target.value)} /></label>
                        <label>Maximum distance (km)<input type="number" step="0.001" min="0" value={form.maximumDistanceKm} onChange={event => change("maximumDistanceKm", event.target.value)} placeholder="Leave empty for above range" /></label>
                        <label>Customer delivery message<input required value={form.estimatedDeliveryText} onChange={event => change("estimatedDeliveryText", event.target.value)} placeholder="Delivery within 1 hour" /></label>
                        <label>Estimated minutes<input required={form.serviceAvailable} type="number" min="0" value={form.estimatedMinutes} onChange={event => change("estimatedMinutes", event.target.value)} placeholder="60" /></label>
                        <label>Delivery charge (₹)<input required type="number" step="0.01" min="0" value={form.deliveryCharge} onChange={event => change("deliveryCharge", event.target.value)} /></label>
                        <label>Priority<input required type="number" min="0" value={form.priority} onChange={event => change("priority", event.target.value)} /></label>
                    </div>
                    <div className="delivery-rule-options"><label><input type="checkbox" checked={form.active} onChange={event => change("active", event.target.checked)} />Active</label><label><input type="checkbox" checked={form.serviceAvailable} onChange={event => change("serviceAvailable", event.target.checked)} />Delivery available</label></div>
                    <div className="delivery-rule-form-actions"><button disabled={saving}>{saving ? "Saving…" : "Save rule"}</button><button type="button" className="secondary" onClick={() => setForm(blank)}>Reset</button></div>
                </section>
            </form>

            <section className="delivery-rule-list-section">
                <div className="delivery-rule-section-heading"><div><h2>Configured rules</h2><p>Only active, non-overlapping rules are used for new delivery estimates.</p></div></div>
                <div className="delivery-rule-table-wrap"><table><thead><tr><th>Rule</th><th>Distance (km)</th><th>ETA</th><th>Available</th><th>Charge</th><th>Status</th><th>Actions</th></tr></thead><tbody>
                    {rules.length === 0 ? <tr><td className="delivery-rule-empty" colSpan="7">No delivery rules configured yet.</td></tr> : rules.map(rule => <tr key={rule.id}>
                        <td>{rule.name}</td><td>{rule.minimumDistanceKm}–{rule.maximumDistanceKm ?? "∞"}</td><td>{rule.estimatedDeliveryText}</td><td>{rule.serviceAvailable ? "Yes" : "No"}</td><td>₹{rule.deliveryCharge}</td>
                        <td><span className={rule.active ? "delivery-rule-status active" : "delivery-rule-status inactive"}>{rule.active ? "Active" : "Inactive"}</span></td>
                        <td><div className="delivery-rule-actions"><button type="button" onClick={() => edit(rule)}>Edit</button><button type="button" className="secondary" onClick={() => toggle(rule)}>{rule.active ? "Deactivate" : "Activate"}</button><button type="button" className="danger" onClick={() => remove(rule)}>Delete</button></div></td>
                    </tr>)}
                </tbody></table></div>
            </section>

            <form className="delivery-rule-preview" onSubmit={test}><section>
                <div className="delivery-rule-section-heading"><div><h2>Distance preview</h2><p>Confirm what a customer will see before using the rule.</p></div></div>
                <div className="delivery-rule-preview-row"><label>Distance in km<input type="number" step="0.001" min="0" required value={distance} onChange={event => { setDistance(event.target.value); setPreview(null); }} /></label><button>Check ETA</button></div>
                {preview && <p className="delivery-rule-preview-result" role="status">{preview.estimatedDeliveryText} {preview.serviceAvailable === false && "(Not available)"}</p>}
            </section></form>
        </main>
    );
}
