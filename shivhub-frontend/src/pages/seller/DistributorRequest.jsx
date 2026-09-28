import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import SellerSidebar from "./SellerSidebar";
import "./DistributorRequest.css";

const blank = {
    businessName: "",
    contactPerson: "",
    distributorCode: "",
    businessType: "",
    mobile: "",
    alternateMobile: "",
    whatsappNumber: "",
    email: "",
    website: "",
    status: "PENDING",
    address: "",
    city: "",
    district: "",
    state: "",
    pincode: "",
    gstRegistrationStatus: "UNREGISTERED",
    gstin: "",
    pan: "",
    billingAddress: "",
    warehouseAddress: "",
    stateCode: "",
    accountsEmail: "",
    brands: "",
    productCategories: "Mobile",
    distributorType: "",
    authorizationDetails: "",
    salespersonName: "",
    salespersonMobile: "",
    claimsContact: "",
    accountHolderName: "",
    bankName: "",
    accountNumber: "",
    ifscCode: "",
    branchName: "",
    accountType: "",
    upiId: "",
    paymentQrUrl: "",
    bankDetails: "",
    documentUrls: "",
    returnPolicy: "",
    replacementTerms: "",
    shortageReportingPeriod: "",
    warrantyClaimProcess: "",
    creditNoteTerms: "",
    deliveryTerms: "",
    usualDeliveryTime: "",
    schemeTerms: ""
};

const groups = [
    ["Basic Details", ["businessName", "contactPerson", "businessType", "mobile", "alternateMobile", "whatsappNumber", "email", "website"]],
    ["Address & Tax", ["address", "city", "district", "state", "stateCode", "pincode", "gstRegistrationStatus", "gstin", "pan", "billingAddress", "warehouseAddress", "accountsEmail"]],
    ["Brands & Contacts", ["brands", "productCategories", "distributorType", "authorizationDetails", "salespersonName", "salespersonMobile", "claimsContact"]],
    ["Bank Details", ["accountHolderName", "bankName", "accountNumber", "ifscCode", "branchName", "accountType", "upiId", "paymentQrUrl", "bankDetails"]],
    ["Documents & Policies", ["documentUrls", "returnPolicy", "replacementTerms", "shortageReportingPeriod", "warrantyClaimProcess", "creditNoteTerms", "deliveryTerms", "usualDeliveryTime", "schemeTerms"]]
];

const labels = {
    businessName: "Distributor / Firm Name",
    contactPerson: "Owner / Contact Person",
    businessType: "Business Type",
    mobile: "Mobile Number",
    alternateMobile: "Alternate Mobile",
    whatsappNumber: "WhatsApp Number",
    email: "Email",
    website: "Website",
    address: "Complete Business Address",
    city: "City",
    district: "District",
    state: "State",
    stateCode: "State Code",
    pincode: "Pincode",
    gstRegistrationStatus: "GST Registration Status",
    gstin: "GSTIN",
    pan: "PAN",
    billingAddress: "Billing Address",
    warehouseAddress: "Dispatch / Warehouse Address",
    accountsEmail: "Accounts Email",
    brands: "Supplied Brands",
    productCategories: "Product Categories",
    distributorType: "Distributor Type",
    authorizationDetails: "Authorization Details",
    salespersonName: "Salesperson Name",
    salespersonMobile: "Salesperson Mobile",
    claimsContact: "Service / Claims Contact",
    accountHolderName: "Account Holder Name",
    bankName: "Bank Name",
    accountNumber: "Account Number",
    ifscCode: "IFSC Code",
    branchName: "Branch Name",
    accountType: "Account Type",
    bankDetails: "Bank Details",
    upiId: "UPI ID",
    paymentQrUrl: "Payment QR URL",
    documentUrls: "Document URLs",
    returnPolicy: "Return Policy",
    replacementTerms: "DOA / Replacement Terms",
    shortageReportingPeriod: "Damage / Shortage Reporting Period",
    warrantyClaimProcess: "Warranty Claim Process",
    creditNoteTerms: "Credit Note Terms",
    deliveryTerms: "Delivery Charges / Terms",
    usualDeliveryTime: "Usual Delivery Time",
    schemeTerms: "Price Protection / Scheme Terms"
};

const multilineFields = new Set([
    "address",
    "billingAddress",
    "warehouseAddress",
    "authorizationDetails",
    "bankDetails",
    "documentUrls",
    "returnPolicy",
    "replacementTerms",
    "warrantyClaimProcess",
    "creditNoteTerms",
    "deliveryTerms",
    "schemeTerms"
]);

const requiredFields = new Set(["businessName", "contactPerson", "mobile", "address", "city", "district", "state", "pincode", "brands"]);

const placeholder = key =>
    key === "brands"
        ? "Samsung, Vivo, Oppo or ALL_BRANDS"
        : key === "accountNumber"
          ? "Do not add spaces"
          : key === "ifscCode"
            ? "Example: SBIN0001234"
            : key === "bankDetails"
              ? "Extra bank notes or second account details"
          : key === "documentUrls"
            ? "Paste document links, one per line"
            : "";

const shown = value => value || "Not provided";

const addressLine = item =>
    [item.distributorAddress, item.distributorCity, item.distributorDistrict, item.distributorState, item.distributorPincode]
        .filter(Boolean)
        .join(", ") || "Not provided";

const Detail = ({ label, children }) => (
    <p>
        <span>{label}</span>
        {shown(children)}
    </p>
);

export default function DistributorRequest() {
    const [assigned, setAssigned] = useState([]);
    const [form, setForm] = useState(blank);
    const [search, setSearch] = useState("");
    const [brand, setBrand] = useState("ALL");
    const [requestOpen, setRequestOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [activeRequestTab, setActiveRequestTab] = useState(groups[0][0]);

    const load = async () => {
        try {
            setLoading(true);
            setError("");
            const response = await api.get("/api/seller-distributors/active");
            setAssigned(response.data || []);
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to load your assigned distributors.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void load();
    }, []);

    const brands = useMemo(() => [...new Set(assigned.map(item => item.brand).filter(Boolean))], [assigned]);

    const visible = useMemo(() => {
        const term = search.trim().toLowerCase();
        return assigned.filter(item =>
            (brand === "ALL" || item.brand === brand) &&
            (!term || [
                item.distributorName,
                item.brand,
                item.assignedBrands,
                item.distributorMobile,
                item.distributorWhatsapp,
                item.distributorCity,
                item.distributorState,
                item.distributorGstin,
                item.salespersonName
            ].some(entry => String(entry || "").toLowerCase().includes(term)))
        );
    }, [assigned, brand, search]);

    const submit = async event => {
        event.preventDefault();
        try {
            setError("");
            const missing = [...requiredFields].find(key => !String(form[key] || "").trim());
            if (missing) {
                setActiveRequestTab(groups.find(([, fields]) => fields.includes(missing))?.[0] || groups[0][0]);
                setError(`${labels[missing]} is required.`);
                return;
            }
            await api.post("/api/seller/distributor-requests", form);
            setForm(blank);
            setRequestOpen(false);
            setMessage("Distributor request submitted. Admin will verify its details and then assign it to your shop and permitted brand.");
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Request could not be submitted.");
        }
    };

    return (
        <div className="seller-data-page">
            <SellerSidebar />
            <main className="seller-distributor-page">
                <header className="seller-distributor-head">
                    <div>
                        <span>MY PROCUREMENT DIRECTORY</span>
                        <h1>Assigned distributors</h1>
                        <p>Only ShivHub Admin-approved distributors and permitted product brands are available for your purchases.</p>
                    </div>
                    <button className="request-toggle" onClick={() => setRequestOpen(current => !current)}>
                        {requestOpen ? "Close request" : "+ Request distributor"}
                    </button>
                </header>

                {error && <div className="seller-distributor-alert error">{error}</div>}
                {message && <div className="seller-distributor-alert success">✓ {message}</div>}

                <section className="distributor-stat-row">
                    <article><span>Assigned distributors</span><strong>{assigned.length}</strong></article>
                    <article><span>Permitted brands</span><strong>{brands.length}</strong></article>
                    <article><span>Active for purchase</span><strong>{assigned.filter(item => item.active).length}</strong></article>
                </section>

                {requestOpen && (
                    <section className={`seller-request-card ${activeRequestTab === "Bank Details" ? "bank-tab-active" : ""}`}>
                        <div>
                            <span>NEW DISTRIBUTOR REQUEST</span>
                            <h2>Send complete distributor details for Admin verification</h2>
                            <p>This does not create an assignment automatically. Admin will decide your permitted brand.</p>
                        </div>
                        <form onSubmit={submit}>
                            <div className="distributor-tabs">
                                {groups.map(([title]) => (
                                    <button type="button" key={title} className={activeRequestTab === title ? "active" : ""} onClick={() => setActiveRequestTab(title)}>
                                        {title}
                                    </button>
                                ))}
                            </div>
                            {groups.filter(([title]) => title === activeRequestTab).map(([title, fields]) => (
                                <fieldset className="distributor-field-group" key={title}>
                                    <legend>{title}</legend>
                                    {fields.map(key => (
                                        <label key={key}>
                                            {labels[key] || key}
                                            {multilineFields.has(key)
                                                ? <textarea rows="3" required={requiredFields.has(key)} value={form[key]} placeholder={placeholder(key)} onChange={event => setForm({ ...form, [key]: event.target.value })} />
                                                : <input required={requiredFields.has(key)} value={form[key]} placeholder={placeholder(key)} onChange={event => setForm({ ...form, [key]: event.target.value })} />}
                                        </label>
                                    ))}
                                </fieldset>
                            ))}
                            <button>Submit for Admin review</button>
                        </form>
                    </section>
                )}

                <section className="seller-directory-card">
                    <div className="directory-toolbar">
                        <input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search distributor, brand, phone, city or GSTIN" />
                        <select value={brand} onChange={event => setBrand(event.target.value)}>
                            <option value="ALL">All permitted brands</option>
                            {brands.map(item => <option key={item} value={item}>{item}</option>)}
                        </select>
                        <button onClick={() => void load()}>Refresh</button>
                    </div>

                    {loading ? (
                        <p className="directory-empty">Loading your assignments…</p>
                    ) : visible.length === 0 ? (
                        <div className="directory-empty">
                            <strong>No assigned distributor found.</strong>
                            <p>Use “Request distributor” if you need a new supplier. Only Admin can assign it to your shop.</p>
                        </div>
                    ) : (
                        <div className="assigned-grid">
                            {visible.map(item => (
                                <article className="assigned-card" key={item.id}>
                                    <div className="assigned-card-top">
                                        <div>
                                            <span>PERMITTED BRAND</span>
                                            <h2>{item.distributorName}</h2>
                                        </div>
                                        <b>{item.brand}</b>
                                    </div>

                                    <div className="assigned-details assigned-details-rich">
                                        <Detail label="Phone">{item.distributorMobile}</Detail>
                                        <Detail label="WhatsApp">{item.distributorWhatsapp}</Detail>
                                        <Detail label="Email">{item.distributorEmail}</Detail>
                                        <Detail label="Accounts">{item.distributorAccountsEmail}</Detail>
                                        <Detail label="GSTIN">{item.distributorGstin}</Detail>
                                        <Detail label="Address">{addressLine(item)}</Detail>
                                        <Detail label="Brands">{item.assignedBrands || item.brand}</Detail>
                                        <Detail label="Categories">{item.productCategories}</Detail>
                                        <Detail label="Type">{item.distributorType}</Detail>
                                        <Detail label="Salesman">{[item.salespersonName, item.salespersonMobile].filter(Boolean).join(" · ")}</Detail>
                                        <Detail label="Claims">{item.claimsContact}</Detail>
                                        <Detail label="Payment">{item.paymentTerms}</Detail>
                                        <Detail label="Credit days">{item.creditPeriodDays}</Detail>
                                        <Detail label="Credit limit">{item.creditLimit ? `₹${item.creditLimit}` : ""}</Detail>
                                        <Detail label="Opening">{item.openingBalance ? `₹${item.openingBalance} ${item.openingBalanceType || ""}` : ""}</Detail>
                                        <Detail label="Delivery">{item.usualDeliveryTime || item.deliveryTerms}</Detail>
                                        <Detail label="Notes">{item.sellerNotes}</Detail>
                                    </div>

                                    <footer>
                                        <span>Admin assignment</span>
                                        <strong>{item.active ? "Active" : "Inactive"}</strong>
                                    </footer>
                                </article>
                            ))}
                        </div>
                    )}
                </section>
            </main>
        </div>
    );
}
