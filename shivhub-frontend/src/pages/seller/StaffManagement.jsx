import { useEffect, useState } from "react";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./StaffManagement.css";
import "./PremiumPolish.css";

const money = value =>
    `₹${Number(value || 0).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;

export default function StaffManagement() {
    const [shops, setShops] = useState([]);
    const [summary, setSummary] = useState([]);
    const [form, setForm] = useState({
        shopId: "",
        name: "",
        email: "",
        mobile: "",
        temporaryPassword: "",
        accessRole: "SALES_EXECUTIVE"
    });
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const loadSummary = () =>
        api.get("/api/offline-billing/salesperson-summary")
            .then(({ data }) => setSummary(data || []))
            .catch(() => setSummary([]));

    useEffect(() => {
        api.get("/api/seller/shops")
            .then(({ data }) => {
                setShops(data || []);
                if (data?.[0]) {
                    setForm(current => ({
                        ...current,
                        shopId: String(data[0].id)
                    }));
                }
            })
            .catch(() => setError("Unable to load your branches."));

        loadSummary();
    }, []);

    const change = event =>
        setForm(current => ({
            ...current,
            [event.target.name]: event.target.value
        }));

    const submit = async event => {
        event.preventDefault();
        setSaving(true);
        setError("");
        setMessage("");

        try {
            const { shopId, ...staff } = form;
            const cleanStaff = {
                ...staff,
                name: staff.name.trim(),
                email: staff.email.trim().toLowerCase(),
                mobile: staff.mobile.trim()
            };
            await api.post(`/api/seller/shops/${shopId}/staff`, cleanStaff);
            setMessage("Staff account created and assignment email sent.");
            setForm(current => ({
                ...current,
                name: "",
                email: "",
                mobile: "",
                temporaryPassword: ""
            }));
            loadSummary();
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to create staff account.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="staff-page">
            <SellerSidebar />
            <main className="staff-main">
                <header>
                    <p>TEAM MANAGEMENT</p>
                    <h1>Add branch staff</h1>
                    <span>Create staff logins and track who sold how many mobiles.</span>
                </header>

                <section className="staff-card staff-sales-card">
                    <div className="staff-sales-heading">
                        <div>
                            <h2>Staff progress report</h2>
                            <p>POS bills, mobile units, discounts, GST and net sales by staff member.</p>
                        </div>
                        <button type="button" onClick={loadSummary}>Refresh</button>
                    </div>

                    {summary.length ? (
                        <div className="staff-sales-grid">
                            {summary.map(item => (
                                <article key={item.salespersonId}>
                                    <strong>{item.salespersonName}</strong>
                                    <span>{item.billCount} bills · {item.mobileUnitsSold || 0} units sold</span>
                                    <b>{money(item.salesTotal)}</b>
                                    <small>Discount {money(item.discountTotal)} · GST {money(item.gstTotal)}</small>
                                    <small>Net {money(item.netTotal)} · Margin {Number(item.marginPercent || 0).toFixed(2)}%</small>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <p className="staff-empty">No bill data yet. Select a salesperson while generating an offline bill.</p>
                    )}
                </section>

                <section className="staff-card">
                    {error && <div className="staff-error">{error}</div>}
                    {message && <div className="staff-success">✓ {message}</div>}

                    {shops.length === 0 ? (
                        <div className="staff-empty">
                            <h2>Create a branch first</h2>
                            <p>A staff member must be assigned to a branch.</p>
                        </div>
                    ) : (
                        <form onSubmit={submit}>
                            <label>Branch<select name="shopId" value={form.shopId} onChange={change}>{shops.map(shop => <option value={shop.id} key={shop.id}>{shop.name} · {shop.shopCode}</option>)}</select></label>
                            <label>Full name<input required name="name" value={form.name} onChange={change} /></label>
                            <label>Email<input required type="email" name="email" value={form.email} onChange={change} /></label>
                            <label>Mobile<input required name="mobile" pattern="[6-9][0-9]{9}" value={form.mobile} onChange={change} /></label>
                            <label>Temporary password<input required minLength="8" type="password" name="temporaryPassword" value={form.temporaryPassword} onChange={change} /></label>
                            <label>Access role<select name="accessRole" value={form.accessRole} onChange={change}><option value="SALES_EXECUTIVE">Sales executive</option><option value="MANAGER">Manager</option><option value="OWNER">Owner</option></select></label>
                            <button disabled={saving}>{saving ? "Creating..." : "Create staff account"}</button>
                        </form>
                    )}
                </section>
            </main>
        </div>
    );
}
