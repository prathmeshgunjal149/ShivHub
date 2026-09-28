import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
    activateMobileSpecification,
    createMobileSpecification,
    deactivateMobileSpecification,
    fetchExternalMobileSpecification,
    getAdminMobileSpecifications,
    updateMobileSpecification
} from "../../services/mobileSpecificationService";
import "./MobileSpecifications.css";

const emptyForm = () => ({
    brand: "",
    modelName: "",
    variantName: "",
    ram: "",
    storage: "",
    colorOptions: "",
    hsnCode: "85171300",
    displayDetails: "",
    processor: "",
    cameraDetails: "",
    batteryDetails: "",
    osDetails: "",
    connectivityDetails: "",
    otherDetails: "",
    indiaVariant: true,
    active: true,
    source: "MANUAL"
});

const text = value => String(value ?? "").trim();

export default function MobileSpecifications() {
    const [specs, setSpecs] = useState([]);
    const [form, setForm] = useState(emptyForm());
    const [editingId, setEditingId] = useState(null);
    const [query, setQuery] = useState("");
    const [saving, setSaving] = useState(false);
    const [fetching, setFetching] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    const loadSpecs = useCallback(async (search = query) => {
        try {
            setError("");
            const data = await getAdminMobileSpecifications(search);
            setSpecs(Array.isArray(data) ? data : []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Mobile specifications could not be loaded.");
        }
    }, [query]);

    useEffect(() => {
        const timer = window.setTimeout(() => {
            void loadSpecs("");
        }, 0);
        return () => window.clearTimeout(timer);
    }, [loadSpecs]);

    const change = event => {
        const { name, value, type, checked } = event.target;
        setForm(current => ({
            ...current,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const reset = () => {
        setForm(emptyForm());
        setEditingId(null);
        setMessage("");
        setError("");
    };

    const save = async event => {
        event.preventDefault();

        if (!text(form.brand) || !text(form.modelName)) {
            setError("Brand and model name are required.");
            return;
        }

        setSaving(true);
        try {
            const payload = {
                ...form,
                brand: text(form.brand),
                modelName: text(form.modelName),
                hsnCode: text(form.hsnCode) || "85171300"
            };

            if (editingId) {
                await updateMobileSpecification(editingId, payload);
                setMessage("Mobile specification updated.");
            } else {
                await createMobileSpecification(payload);
                setMessage("Mobile specification saved.");
            }

            setForm(emptyForm());
            setEditingId(null);
            await loadSpecs("");
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Mobile specification could not be saved.");
        } finally {
            setSaving(false);
        }
    };

    const fetchFromApi = async () => {
        if (!text(form.brand) || !text(form.modelName)) {
            setError("API fetch साठी आधी Brand आणि Model name भरा.");
            return;
        }

        setFetching(true);
        try {
            setError("");
            const spec = await fetchExternalMobileSpecification(form.brand, form.modelName);
            setForm(current => ({
                ...current,
                brand: spec.brand || current.brand,
                modelName: spec.modelName || current.modelName,
                variantName: spec.variantName || current.variantName,
                ram: spec.ram || current.ram,
                storage: spec.storage || current.storage,
                colorOptions: spec.colorOptions || current.colorOptions,
                hsnCode: spec.hsnCode || current.hsnCode || "85171300",
                displayDetails: spec.displayDetails || current.displayDetails,
                processor: spec.processor || current.processor,
                cameraDetails: spec.cameraDetails || current.cameraDetails,
                batteryDetails: spec.batteryDetails || current.batteryDetails,
                osDetails: spec.osDetails || current.osDetails,
                connectivityDetails: spec.connectivityDetails || current.connectivityDetails,
                otherDetails: spec.otherDetails || current.otherDetails,
                source: spec.source || "GSMARENA_PREVIEW"
            }));
            setMessage("API मधून specs preview आले. Verify करून Save करा.");
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Mobile specs API configured नाही किंवा fetch failed.");
        } finally {
            setFetching(false);
        }
    };

    const edit = spec => {
        setEditingId(spec.id);
        setForm({
            brand: spec.brand || "",
            modelName: spec.modelName || "",
            variantName: spec.variantName || "",
            ram: spec.ram || "",
            storage: spec.storage || "",
            colorOptions: spec.colorOptions || "",
            hsnCode: spec.hsnCode || "85171300",
            displayDetails: spec.displayDetails || "",
            processor: spec.processor || "",
            cameraDetails: spec.cameraDetails || "",
            batteryDetails: spec.batteryDetails || "",
            osDetails: spec.osDetails || "",
            connectivityDetails: spec.connectivityDetails || "",
            otherDetails: spec.otherDetails || "",
            indiaVariant: spec.indiaVariant !== false,
            active: spec.active !== false,
            source: spec.source || "MANUAL"
        });
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const toggleStatus = async spec => {
        try {
            if (spec.active) {
                await deactivateMobileSpecification(spec.id);
            } else {
                await activateMobileSpecification(spec.id);
            }
            await loadSpecs(query);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Status could not be changed.");
        }
    };

    const search = event => {
        event.preventDefault();
        void loadSpecs(query);
    };

    return (
        <main className="mobile-spec-page">
            <header className="mobile-spec-hero">
                <div>
                    <p className="mobile-spec-kicker">Mobile catalogue</p>
                    <h1>Model specifications</h1>
                    <p>
                        Save verified India variant details once. Sellers can select the model in purchases and auto-fill
                        RAM, storage, colour, HSN and technical notes.
                    </p>
                </div>
                <Link to="/admin/dashboard">Dashboard</Link>
            </header>

            {error && <p className="mobile-spec-alert error">{error}</p>}
            {message && <p className="mobile-spec-alert success">{message}</p>}

            <section className="mobile-spec-shell">
                <form className="mobile-spec-form" onSubmit={save}>
                    <div className="mobile-spec-form-head">
                        <div>
                            <p className="mobile-spec-kicker">{editingId ? "Edit verified model" : "Add verified model"}</p>
                            <h2>{editingId ? "Update specification" : "New mobile specification"}</h2>
                        </div>
                        {editingId && (
                            <button type="button" className="ghost" onClick={reset}>
                                Cancel edit
                            </button>
                        )}
                    </div>

                    <div className="mobile-spec-api-box">
                        <div>
                            <strong>Fetch from licensed specs API</strong>
                            <span>Brand + model भरून API preview घ्या. Save करण्याआधी admin verification आवश्यक.</span>
                        </div>
                        <button type="button" className="ghost" onClick={fetchFromApi} disabled={fetching}>
                            {fetching ? "Fetching..." : "Fetch specs"}
                        </button>
                    </div>

                    <div className="mobile-spec-grid">
                        <label>Brand *<input name="brand" value={form.brand} onChange={change} placeholder="realme" /></label>
                        <label>Model name *<input name="modelName" value={form.modelName} onChange={change} placeholder="16T 5G" /></label>
                        <label>Variant name<input name="variantName" value={form.variantName} onChange={change} placeholder="Starlight Black (6+128GB)" /></label>
                        <label>RAM<input name="ram" value={form.ram} onChange={change} placeholder="6 GB" /></label>
                        <label>Storage<input name="storage" value={form.storage} onChange={change} placeholder="128 GB" /></label>
                        <label>Colour options<input name="colorOptions" value={form.colorOptions} onChange={change} placeholder="Starlight Black, Glory White" /></label>
                        <label>HSN/SAC<input name="hsnCode" value={form.hsnCode} onChange={change} placeholder="85171300" /></label>
                        <label>Processor<input name="processor" value={form.processor} onChange={change} placeholder="Chipset name" /></label>
                        <label>Operating system<input name="osDetails" value={form.osDetails} onChange={change} placeholder="Android / iOS version" /></label>
                    </div>

                    <div className="mobile-spec-text-grid">
                        <label>Display<textarea name="displayDetails" value={form.displayDetails} onChange={change} placeholder="Size, panel, refresh rate" /></label>
                        <label>Camera<textarea name="cameraDetails" value={form.cameraDetails} onChange={change} placeholder="Rear and front camera" /></label>
                        <label>Battery & charging<textarea name="batteryDetails" value={form.batteryDetails} onChange={change} placeholder="Battery capacity, charging wattage" /></label>
                        <label>Connectivity<textarea name="connectivityDetails" value={form.connectivityDetails} onChange={change} placeholder="5G, Wi-Fi, Bluetooth, SIM" /></label>
                        <label className="wide">Other details<textarea name="otherDetails" value={form.otherDetails} onChange={change} placeholder="Dimensions, weight, IP rating, notes" /></label>
                    </div>

                    <div className="mobile-spec-checks">
                        <label><input type="checkbox" name="indiaVariant" checked={form.indiaVariant} onChange={change} /> India variant verified</label>
                        <label><input type="checkbox" name="active" checked={form.active} onChange={change} /> Active for seller purchase autofill</label>
                    </div>

                    <button className="primary" disabled={saving}>{saving ? "Saving..." : editingId ? "Update specification" : "Save specification"}</button>
                </form>

                <aside className="mobile-spec-help">
                    <h3>How this works</h3>
                    <p>External API data can be copied here after admin verification. Purchase price, stock and IMEI still come from seller purchase entry.</p>
                    <ul>
                        <li>One model can have multiple RAM/storage variants.</li>
                        <li>Inactive specs stay saved but do not show in seller purchase.</li>
                        <li>HSN defaults to 85171300 for mobile phones.</li>
                    </ul>
                </aside>
            </section>

            <section className="mobile-spec-list">
                <form className="mobile-spec-search" onSubmit={search}>
                    <div>
                        <p className="mobile-spec-kicker">Saved catalogue</p>
                        <h2>Verified mobile models</h2>
                    </div>
                    <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search brand or model" />
                    <button>Search</button>
                </form>

                <div className="mobile-spec-cards">
                    {specs.map(spec => (
                        <article className="mobile-spec-card" key={spec.id}>
                            <div>
                                <span className={spec.active ? "pill active" : "pill inactive"}>{spec.active ? "Active" : "Inactive"}</span>
                                <h3>{spec.brand} {spec.modelName}</h3>
                                <p>{spec.variantName || "Standard variant"}</p>
                            </div>
                            <dl>
                                <dt>RAM</dt><dd>{spec.ram || "-"}</dd>
                                <dt>Storage</dt><dd>{spec.storage || "-"}</dd>
                                <dt>Colours</dt><dd>{spec.colorOptions || "-"}</dd>
                                <dt>HSN</dt><dd>{spec.hsnCode || "-"}</dd>
                            </dl>
                            <p className="spec-note">{[spec.displayDetails, spec.processor, spec.batteryDetails].filter(Boolean).join(" • ") || "No extra technical details added."}</p>
                            <div className="mobile-spec-actions">
                                <button type="button" onClick={() => edit(spec)}>Edit</button>
                                <button type="button" className="ghost" onClick={() => toggleStatus(spec)}>{spec.active ? "Deactivate" : "Activate"}</button>
                            </div>
                        </article>
                    ))}
                </div>

                {!specs.length && <p className="mobile-spec-empty">No mobile specifications saved yet.</p>}
            </section>
        </main>
    );
}
