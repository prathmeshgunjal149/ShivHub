import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./SellerSettingsPage.css";
import "./PremiumPolish.css";

const basicFields = [
    ["businessName", "Shop Name"],
    ["legalBusinessName", "Legal Business Name"],
    ["shopLogoUrl", "Shop Logo URL"],
    ["name", "Owner Name"],
    ["mobile", "Mobile Number"],
    ["alternateMobile", "Alternate Mobile"],
    ["email", "Email", true],
    ["businessCity", "City"],
    ["businessDistrict", "District"],
    ["businessState", "State"],
    ["businessPincode", "Pincode"],
    ["googleMapsUrl", "Google Maps Location URL"],
    ["websiteUrl", "Shop Website URL"],
    ["instagramUrl", "Instagram Link"],
    ["facebookUrl", "Facebook Link"],
    ["whatsappUrl", "WhatsApp Link / Number"],
    ["youtubeUrl", "YouTube Link"],
    ["shopOpeningDate", "Shop Opening Date", false, "date"],
    ["shopType", "Shop Type"]
];

const taxFields = [
    ["gstin", "GSTIN"],
    ["panNumber", "PAN"],
    ["businessRegistrationNumber", "Business Registration Number"],
    ["gstRegistrationType", "GST Registration Type"],
    ["stateCode", "State Code"],
    ["gstRates", "GST Rates (example: 5, 12, 18, 28)"],
    ["invoicePrefix", "Billing Series / Invoice Prefix"]
];

export default function SellerSettingsPage() {
    const [form, setForm] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        api.get("/api/seller/profile")
            .then(({ data }) => setForm(data))
            .catch(err => setError(err?.response?.data?.message || "Unable to load settings."));
    }, []);

    const change = event => {
        const { name, value, type, checked } = event.target;
        setForm(current => ({
            ...current,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const submit = async event => {
        event.preventDefault();
        setSaving(true);
        setError("");
        setMessage("");

        try {
            const profilePayload = { ...form };
            delete profilePayload.email;
            const { data } = await api.put("/api/seller/profile", profilePayload);
            setForm(data);
            setMessage("Shop settings saved. New invoices will use your selected display settings.");
        } catch (err) {
            setError(err?.response?.data?.message || "Unable to save settings.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="seller-settings-page">
            <SellerSidebar />

            <main className="seller-settings-main">
                <p>SHOP SETTINGS</p>
                <h1>Shop, legal & invoice profile</h1>
                <Link className="seller-subscription-settings-link" to="/seller/subscription">Manage Subscription & Plans</Link>

                {error && <div className="settings-error">{error}</div>}
                {message && <div className="settings-success">✓ {message}</div>}

                {!form ? (
                    <div className="settings-loading">Loading settings...</div>
                ) : (
                    <form onSubmit={submit}>
                        <h2 className="settings-wide">Shop basic details</h2>

                        {basicFields.map(([name, label, disabled, type]) => (
                            <label key={name}>
                                {label}
                                <input
                                    name={name}
                                    type={type || "text"}
                                    disabled={disabled}
                                    required={name === "name" || name === "mobile"}
                                    value={form[name] || ""}
                                    onChange={change}
                                />
                            </label>
                        ))}

                        <label className="settings-wide">
                            Complete Address
                            <textarea name="businessAddress" value={form.businessAddress || ""} onChange={change} />
                        </label>

                        <h2 className="settings-wide">Legal & tax details</h2>

                        {taxFields.map(([name, label]) => (
                            <label key={name}>
                                {label}
                                <input name={name} value={form[name] || ""} onChange={change} />
                            </label>
                        ))}

                        <label className="settings-wide">
                            Tax settings
                            <textarea name="taxSettings" placeholder="Inclusive/exclusive tax, interstate tax rules..." value={form.taxSettings || ""} onChange={change} />
                        </label>

                        <label className="settings-wide">
                            HSN / SAC settings
                            <textarea name="hsnSacSettings" placeholder="Default HSN/SAC codes and rules" value={form.hsnSacSettings || ""} onChange={change} />
                        </label>

                        <label className="settings-wide">
                            Invoice terms & conditions
                            <textarea name="invoiceTerms" placeholder="Warranty, return policy, payment terms..." value={form.invoiceTerms || ""} onChange={change} />
                        </label>

                        <fieldset className="settings-wide invoice-display">
                            <legend>Show on new bills</legend>
                            <label><input type="checkbox" name="invoiceShowAddress" checked={form.invoiceShowAddress !== false} onChange={change} /> Shop address</label>
                            <label><input type="checkbox" name="invoiceShowMobile" checked={form.invoiceShowMobile !== false} onChange={change} /> Contact number</label>
                            <label><input type="checkbox" name="invoiceShowGstin" checked={form.invoiceShowGstin !== false} onChange={change} /> GSTIN</label>
                            <label><input type="checkbox" name="invoiceShowWebsite" checked={form.invoiceShowWebsite !== false} onChange={change} /> Website</label>
                            <label><input type="checkbox" name="invoiceShowSocialLinks" checked={form.invoiceShowSocialLinks === true} onChange={change} /> Social media links</label>
                            <label><input type="checkbox" name="invoiceShowCustomerDetails" checked={form.invoiceShowCustomerDetails !== false} onChange={change} /> Customer details</label>
                            <label><input type="checkbox" name="invoiceShowNotes" checked={form.invoiceShowNotes !== false} onChange={change} /> Billing notes</label>
                        </fieldset>

                        <button disabled={saving}>{saving ? "Saving..." : "Save shop settings"}</button>
                    </form>
                )}
            </main>
        </div>
    );
}
