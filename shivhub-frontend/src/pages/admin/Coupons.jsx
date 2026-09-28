import { useCallback, useEffect, useState } from "react";
import { createCoupon, deleteCoupon, listCoupons, updateCoupon } from "../../services/couponService";
import AnimatedModal from "../../components/common/animations/AnimatedModal";
import "./AdminTools.css";

const emptyCoupon = {
    code: "", title: "", description: "", discountType: "PERCENTAGE", discountValue: "",
    minimumOrderAmount: "0", maximumDiscount: "", usageLimit: "", perCustomerLimit: "1",
    active: true, startsAt: "", endsAt: ""
};

const toDateTimeInput = value => value ? new Date(value).toISOString().slice(0, 16) : "";
const money = value => `₹${Number(value || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;

export default function Coupons() {
    const [items, setItems] = useState([]);
    const [modal, setModal] = useState(null);
    const [form, setForm] = useState(emptyCoupon);
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const load = useCallback(async () => {
        try {
            setItems(await listCoupons());
        } catch {
            setError("Coupons could not be loaded.");
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    const open = coupon => {
        setError("");
        setModal(coupon || {});
        setForm(coupon ? {
            ...emptyCoupon,
            ...coupon,
            minimumOrderAmount: coupon.minimumOrderAmount ?? "0",
            maximumDiscount: coupon.maximumDiscount ?? "",
            usageLimit: coupon.usageLimit ?? "",
            perCustomerLimit: coupon.perCustomerLimit ?? "1",
            startsAt: toDateTimeInput(coupon.startsAt),
            endsAt: toDateTimeInput(coupon.endsAt)
        } : emptyCoupon);
    };

    const change = event => {
        const { name, value, type, checked } = event.target;
        setForm(current => ({ ...current, [name]: type === "checkbox" ? checked : value }));
    };

    const save = async event => {
        event.preventDefault();
        const payload = {
            ...form,
            code: form.code.trim().toUpperCase(),
            title: form.title.trim(),
            discountValue: Number(form.discountValue),
            minimumOrderAmount: Number(form.minimumOrderAmount || 0),
            maximumDiscount: form.maximumDiscount === "" ? null : Number(form.maximumDiscount),
            usageLimit: form.usageLimit === "" ? null : Number(form.usageLimit),
            perCustomerLimit: form.perCustomerLimit === "" ? null : Number(form.perCustomerLimit),
            startsAt: form.startsAt || null,
            endsAt: form.endsAt || null
        };
        try {
            setSaving(true);
            if (modal?.id) await updateCoupon(modal.id, payload);
            else await createCoupon(payload);
            setModal(null);
            await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Coupon could not be saved.");
        } finally {
            setSaving(false);
        }
    };

    const remove = async coupon => {
        if (!window.confirm(`Delete coupon ${coupon.code}?`)) return;
        try {
            await deleteCoupon(coupon.id);
            await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Coupon could not be deleted.");
        }
    };

    return (
        <main className="admin-tools">
            <header><div><p className="eyebrow">CHECKOUT</p><h1>Coupons</h1><p>Create discount codes that customers can apply at checkout.</p></div><button onClick={() => open()}>+ Create coupon</button></header>
            {error && <p className="error">{error}</p>}
            <table>
                <thead><tr><th>Coupon</th><th>Discount</th><th>Rules</th><th>Usage</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>{items.map(coupon => <tr key={coupon.id}>
                    <td><strong>{coupon.code}</strong><small>{coupon.title}</small></td>
                    <td>{coupon.discountType === "PERCENTAGE" ? `${coupon.discountValue}%` : money(coupon.discountValue)}{coupon.maximumDiscount && <small>Max {money(coupon.maximumDiscount)}</small>}</td>
                    <td><small>Min. order {money(coupon.minimumOrderAmount)}</small><small>Per customer: {coupon.perCustomerLimit ?? "Unlimited"}</small></td>
                    <td>{coupon.usedCount || 0} / {coupon.usageLimit ?? "∞"}</td>
                    <td className={coupon.active ? "active" : "inactive"}>{coupon.active ? "Active" : "Inactive"}</td>
                    <td className="actions"><button className="secondary" onClick={() => open(coupon)}>Edit</button><button className="secondary" onClick={() => remove(coupon)}>Delete</button></td>
                </tr>)}</tbody>
            </table>
            {!items.length && <p className="empty">No coupons created yet.</p>}
            <AnimatedModal open={modal !== null} onBackdropMouseDown={() => !saving && setModal(null)} panelClassName="dialog coupon-dialog" ariaLabel={modal?.id ? "Edit coupon" : "Create coupon"}>
                <form onSubmit={save}>
                    <h2>{modal?.id ? "Edit coupon" : "Create coupon"}</h2>
                    <label>Coupon code<input autoFocus required name="code" maxLength="80" value={form.code} onChange={change} placeholder="WELCOME10" /></label>
                    <label>Title<input required name="title" maxLength="150" value={form.title} onChange={change} placeholder="Welcome discount" /></label>
                    <label>Description<textarea name="description" value={form.description || ""} onChange={change} placeholder="Optional customer-facing description" /></label>
                    <div className="coupon-form-grid"><label>Discount type<select name="discountType" value={form.discountType} onChange={change}><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed amount</option></select></label><label>Discount value<input required type="number" name="discountValue" min="0.01" step="0.01" value={form.discountValue} onChange={change} /></label></div>
                    <div className="coupon-form-grid"><label>Minimum order amount<input type="number" name="minimumOrderAmount" min="0" step="0.01" value={form.minimumOrderAmount} onChange={change} /></label><label>Maximum discount<input type="number" name="maximumDiscount" min="0.01" step="0.01" value={form.maximumDiscount} onChange={change} placeholder="Optional" /></label></div>
                    <div className="coupon-form-grid"><label>Total usage limit<input type="number" name="usageLimit" min="1" step="1" value={form.usageLimit} onChange={change} placeholder="Unlimited" /></label><label>Per-customer limit<input type="number" name="perCustomerLimit" min="1" step="1" value={form.perCustomerLimit} onChange={change} placeholder="Unlimited" /></label></div>
                    <div className="coupon-form-grid"><label>Starts at<input type="datetime-local" name="startsAt" value={form.startsAt} onChange={change} /></label><label>Ends at<input type="datetime-local" name="endsAt" value={form.endsAt} onChange={change} /></label></div>
                    <label className="coupon-active"><input type="checkbox" name="active" checked={form.active} onChange={change} /> Active and available at checkout</label>
                    <div className="actions"><button type="button" className="secondary" disabled={saving} onClick={() => setModal(null)}>Cancel</button><button disabled={saving}>{saving ? "Saving…" : "Save coupon"}</button></div>
                </form>
            </AnimatedModal>
        </main>
    );
}
