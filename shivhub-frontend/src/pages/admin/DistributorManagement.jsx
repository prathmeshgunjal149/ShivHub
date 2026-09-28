import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import "./DistributorManagement.css";

const blankDistributor = {
    businessName: "", contactPerson: "", distributorCode: "", businessType: "", mobile: "", alternateMobile: "", whatsappNumber: "", email: "", website: "", status: "ACTIVE",
    address: "", city: "", district: "", state: "", pincode: "", stateCode: "", gstRegistrationStatus: "UNREGISTERED", gstin: "", pan: "", billingAddress: "", warehouseAddress: "", accountsEmail: "",
    brands: "", productCategories: "Mobile", distributorType: "", authorizationDetails: "", salespersonName: "", salespersonMobile: "", claimsContact: "",
    accountHolderName: "", bankName: "", accountNumber: "", ifscCode: "", branchName: "", accountType: "", upiId: "", paymentQrUrl: "", bankDetails: "", documentUrls: "",
    returnPolicy: "", replacementTerms: "", shortageReportingPeriod: "", warrantyClaimProcess: "", creditNoteTerms: "", deliveryTerms: "", usualDeliveryTime: "", schemeTerms: ""
};
const blankAssignment = { sellerId: "", distributorId: "", brand: "", assignedBrands: "", paymentTerms: "", creditPeriodDays: "", creditLimit: "", openingBalance: "", openingBalanceDate: "", openingBalanceType: "", preferredPaymentMethod: "", assignedSalesperson: "", sellerNotes: "" };

const distributorGroups = [
    ["Basic Details", ["businessName", "contactPerson", "businessType", "mobile", "alternateMobile", "whatsappNumber", "email", "website", "status"]],
    ["Address & Tax", ["address", "city", "district", "state", "stateCode", "pincode", "gstRegistrationStatus", "gstin", "pan", "billingAddress", "warehouseAddress", "accountsEmail"]],
    ["Brands & Contacts", ["brands", "productCategories", "distributorType", "authorizationDetails", "salespersonName", "salespersonMobile", "claimsContact"]],
    ["Bank Details", ["accountHolderName", "bankName", "accountNumber", "ifscCode", "branchName", "accountType", "upiId", "paymentQrUrl", "bankDetails"]],
    ["Documents & Policies", ["documentUrls", "returnPolicy", "replacementTerms", "shortageReportingPeriod", "warrantyClaimProcess", "creditNoteTerms", "deliveryTerms", "usualDeliveryTime", "schemeTerms"]]
];

const labels = {
    businessName: "Distributor / Firm Name", contactPerson: "Owner / Contact Person", businessType: "Business Type", mobile: "Mobile Number", alternateMobile: "Alternate Mobile", whatsappNumber: "WhatsApp Number", email: "Email", website: "Website", status: "Status",
    address: "Complete Business Address", city: "City", district: "District", state: "State", stateCode: "State Code", pincode: "Pincode", gstRegistrationStatus: "GST Registration Status", gstin: "GSTIN", pan: "PAN", billingAddress: "Billing Address", warehouseAddress: "Dispatch / Warehouse Address", accountsEmail: "Accounts Email",
    brands: "Supplied Brands", productCategories: "Product Categories", distributorType: "Distributor Type", authorizationDetails: "Authorization Details", salespersonName: "Salesperson Name", salespersonMobile: "Salesperson Mobile", claimsContact: "Service / Claims Contact",
    accountHolderName: "Account Holder Name", bankName: "Bank Name", accountNumber: "Account Number", ifscCode: "IFSC Code", branchName: "Branch Name", accountType: "Account Type", bankDetails: "Extra Bank Notes", upiId: "UPI ID", paymentQrUrl: "Payment QR URL", documentUrls: "Document URLs", returnPolicy: "Return Policy", replacementTerms: "DOA / Replacement Terms", shortageReportingPeriod: "Damage / Shortage Reporting Period", warrantyClaimProcess: "Warranty Claim Process", creditNoteTerms: "Credit Note Terms", deliveryTerms: "Delivery Charges / Terms", usualDeliveryTime: "Usual Delivery Time", schemeTerms: "Price Protection / Scheme Terms"
};
const multiline = new Set(["address", "billingAddress", "warehouseAddress", "authorizationDetails", "bankDetails", "documentUrls", "returnPolicy", "replacementTerms", "warrantyClaimProcess", "creditNoteTerms", "deliveryTerms", "schemeTerms", "sellerNotes"]);
const required = new Set(["businessName", "contactPerson", "mobile", "address", "city", "district", "state", "pincode", "brands"]);

const normaliseBrand = value => {
    const brand = String(value || "").trim().toUpperCase();
    if (/REDMI|XIAOMI|POCO/.test(brand)) return "XIAOMI";
    if (/PIXEL|GOOGLE/.test(brand)) return "GOOGLE PIXEL";
    return brand;
};

const money = value => Number(value || 0).toLocaleString("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
});

export default function DistributorManagement() {
    const [form, setForm] = useState(blankDistributor);
    const [assignment, setAssignment] = useState(blankAssignment);
    const [all, setAll] = useState([]);
    const [pending, setPending] = useState([]);
    const [sellers, setSellers] = useState([]);
    const [assignments, setAssignments] = useState([]);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [detail, setDetail] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [activeDistributorTab, setActiveDistributorTab] = useState(distributorGroups[0][0]);

    const load = async () => {
        try {
            setError("");
            const [catalogue, requests, sellerList, assignmentList] = await Promise.all([
                api.get("/api/admin/distributors"),
                api.get("/api/admin/distributors/pending"),
                api.get("/api/admin/sellers", { params: { page: 0, size: 100 } }),
                api.get("/api/admin/distributor-assignments")
            ]);
            setAll(catalogue.data || []);
            setPending(requests.data || []);
            const sellerRows = Array.isArray(sellerList.data)
                ? sellerList.data
                : (sellerList.data?.content || []);
            setSellers(sellerRows.filter(seller => seller.active ?? seller.enabled));
            setAssignments(assignmentList.data || []);
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to load distributor data.");
        }
    };

    useEffect(() => { void load(); }, []);

    const selectedDistributor = useMemo(
        () => all.find(distributor => String(distributor.id) === String(assignment.distributorId)),
        [all, assignment.distributorId]
    );
    const availableBrands = useMemo(() => {
        const raw = selectedDistributor?.brands || "ALL_BRANDS";
        return [...new Set(raw.split(",").map(normaliseBrand).filter(Boolean))];
    }, [selectedDistributor]);

    const addDistributor = async event => {
        event.preventDefault();
        try {
            const missing = [...required].find(key => !String(form[key] || "").trim());
            if (missing) {
                setActiveDistributorTab(distributorGroups.find(([, fields]) => fields.includes(missing))?.[0] || distributorGroups[0][0]);
                setError(`${labels[missing]} is required.`);
                return;
            }
            await api.post("/api/admin/distributors", form);
            setForm(blankDistributor);
            setMessage("Distributor created. Use the assignment section to choose its seller and brand.");
            await load();
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Could not add distributor.");
        }
    };

    const reviewRequest = async (id, action) => {
        const review = window.prompt(action === "reject" ? "Rejection reason:" : "Admin note (optional):");
        if (action === "reject" && !review) return;
        try {
            await api.patch(`/api/admin/distributors/${id}/${action}`, { review });
            setMessage(`Distributor request ${action}d. Assign it to a seller below when ready.`);
            await load();
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Action failed.");
        }
    };

    const assign = async event => {
        event.preventDefault();
        try {
            const payload = {
                ...assignment,
                sellerId: Number(assignment.sellerId),
                distributorId: Number(assignment.distributorId),
                creditPeriodDays: assignment.creditPeriodDays === "" ? null : Number(assignment.creditPeriodDays),
                creditLimit: assignment.creditLimit === "" ? null : Number(assignment.creditLimit),
                openingBalance: assignment.openingBalance === "" ? null : Number(assignment.openingBalance),
                openingBalanceDate: assignment.openingBalanceDate || null
            };
            await api.post("/api/admin/distributor-assignments", payload);
            setAssignment(blankAssignment);
            setMessage("Distributor and brand assigned to the selected seller.");
            await load();
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Could not create assignment.");
        }
    };

    const removeAssignment = async id => {
        if (!window.confirm("Remove this seller distributor assignment?")) return;
        try {
            await api.delete(`/api/admin/distributor-assignments/${id}`);
            setMessage("Assignment removed. The seller can no longer use that distributor for this brand.");
            await load();
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Could not remove assignment.");
        }
    };

    const loadDetails = async id => {
        try {
            setDetailLoading(true);
            setError("");
            const response = await api.get(`/api/admin/distributors/${id}/details`);
            setDetail(response.data);
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to load distributor details.");
        } finally {
            setDetailLoading(false);
        }
    };

    return <main className="dist-admin">
        <header>
            <p>ADMIN MODULE</p>
            <h1>Distributor & seller assignment centre</h1>
            <span>Create or approve the distributor, then explicitly assign its permitted brand to a seller.</span>
        </header>
        {error && <div className="dist-error">{error}</div>}
        {message && <div className="dist-ok">✓ {message}</div>}

        <section className="dist-card">
            <h2>All distributor details <b>{all.length}</b></h2>
            {all.length ? all.map(distributor => <article className="dist-row" key={distributor.id}>
                <div>
                    <strong>{distributor.businessName}</strong>
                    <small>{distributor.mobile} · {distributor.email || "No email"} · GSTIN: {distributor.gstin || "Not added"} · {distributor.status}</small>
                </div>
                <button type="button" onClick={() => loadDetails(distributor.id)}>
                    {detailLoading ? "Loading..." : "View details"}
                </button>
            </article>) : <p className="dist-empty">No distributor records yet.</p>}
        </section>

        {detail && <section className="dist-card distributor-detail-card">
            <div className="dist-detail-head">
                <div>
                    <h2>{detail.distributor?.businessName}</h2>
                    <p>{detail.distributor?.address || "No address"} {detail.distributor?.city || ""} {detail.distributor?.state || ""}</p>
                    <small>{detail.distributor?.contactPerson || "No contact"} · {detail.distributor?.mobile || "-"} · GSTIN {detail.distributor?.gstin || "-"}</small>
                </div>
                <button type="button" className="reject" onClick={() => setDetail(null)}>Close</button>
            </div>
            <div className="dist-money-grid">
                <article><span>Linked sellers</span><strong>{detail.summary?.linkedSellers || 0}</strong></article>
                <article><span>Purchase invoices</span><strong>{detail.summary?.purchaseInvoices || 0}</strong></article>
                <article><span>Total purchase</span><strong>{money(detail.summary?.totalPurchaseAmount)}</strong></article>
                <article><span>Total paid</span><strong>{money(detail.summary?.totalPaidAmount)}</strong></article>
                <article><span>Credit notes</span><strong>{money(detail.summary?.totalCreditNotes)}</strong></article>
                <article><span>Pending payable</span><strong>{money(detail.summary?.pendingPayableAmount)}</strong></article>
            </div>
            <h3>Linked sellers</h3>
            <div className="dist-table-wrap">
                <table><thead><tr><th>Seller</th><th>Brand</th><th>Terms</th><th>Credit limit</th><th>Status</th></tr></thead>
                    <tbody>{(detail.linkedSellers || []).map(row => <tr key={row.assignmentId}>
                        <td><b>{row.sellerName || row.sellerEmail}</b><small>{row.sellerMobile || ""}</small></td>
                        <td>{row.brand || row.assignedBrands || "-"}</td>
                        <td>{row.paymentTerms || "-"}</td>
                        <td>{money(row.creditLimit)}</td>
                        <td>{row.active ? "Active" : "Inactive"}</td>
                    </tr>)}</tbody></table>
            </div>
            <h3>Purchase invoice history</h3>
            <div className="dist-table-wrap">
                <table><thead><tr><th>Invoice</th><th>Date</th><th>Seller</th><th>Brand</th><th>Total</th><th>Paid</th><th>CN</th><th>Pending</th></tr></thead>
                    <tbody>{(detail.purchaseHistory || []).map(row => <tr key={row.purchaseId}>
                        <td><b>{row.invoiceNumber}</b><small>{row.status}</small></td>
                        <td>{row.purchaseDate ? new Date(row.purchaseDate).toLocaleDateString("en-IN") : "-"}</td>
                        <td>{row.sellerName || row.sellerEmail || "-"}</td>
                        <td>{row.brand || "-"}</td>
                        <td>{money(row.grandTotal)}</td>
                        <td>{money(row.paidAmount)}</td>
                        <td>{money(row.creditNoteAmount)}</td>
                        <td><b>{money(row.pendingAmount)}</b></td>
                    </tr>)}</tbody></table>
            </div>
        </section>}

        <section className="dist-card">
            <h2>Add distributor catalogue record</h2>
            <p className="dist-empty">These are the contact and GST details used for purchasing. Creating a record does not expose it to sellers.</p>
            <form onSubmit={addDistributor}>
                <div className="dist-tabs">{distributorGroups.map(([title]) => <button type="button" key={title} className={activeDistributorTab === title ? "active" : ""} onClick={() => setActiveDistributorTab(title)}>{title}</button>)}</div>
                {distributorGroups.filter(([title]) => title === activeDistributorTab).map(([title, fields]) => <fieldset className="dist-field-group" key={title}><legend>{title}</legend>{fields.map(key => <label key={key}>
                    {labels[key] || key.replace(/([A-Z])/g, " $1")}
                    {multiline.has(key) ? <textarea rows="3" required={required.has(key)} name={key} value={form[key]} placeholder={key === "brands" ? "Samsung, Vivo, Oppo or ALL_BRANDS" : ""} onChange={event => setForm({ ...form, [key]: event.target.value })} /> : <input required={required.has(key)} name={key} value={form[key]} placeholder={key === "brands" ? "Samsung, Vivo, Oppo or ALL_BRANDS" : ""} onChange={event => setForm({ ...form, [key]: event.target.value })} />}
                </label>)}</fieldset>)}
                <button>Add distributor</button>
            </form>
        </section>

        <section className="dist-card">
            <h2>Assign distributor to seller</h2>
            <p className="dist-empty">Only this selected seller will see and use this distributor for the selected product brand.</p>
            <form onSubmit={assign}>
                <label>Seller<select required value={assignment.sellerId} onChange={event => setAssignment({ ...assignment, sellerId: event.target.value })}>
                    <option value="">Select approved seller</option>
                    {sellers.map(seller => <option key={seller.id} value={seller.id}>{seller.name || seller.email} · {seller.email}</option>)}
                </select></label>
                <label>Distributor<select required value={assignment.distributorId} onChange={event => setAssignment({ ...assignment, distributorId: event.target.value, brand: "" })}>
                    <option value="">Select active distributor</option>
                    {all.filter(distributor => distributor.status === "ACTIVE").map(distributor => <option key={distributor.id} value={distributor.id}>{distributor.businessName} · {distributor.brands || "ALL_BRANDS"}</option>)}
                </select></label>
                <label>Permitted brand<select required value={assignment.brand} onChange={event => setAssignment({ ...assignment, brand: event.target.value })} disabled={!selectedDistributor}>
                    <option value="">Select brand</option>
                    {availableBrands.map(brand => <option key={brand} value={brand}>{brand}</option>)}
                </select></label>
                <label>Payment terms<input value={assignment.paymentTerms} placeholder="Advance / Cash / Credit" onChange={event => setAssignment({ ...assignment, paymentTerms: event.target.value })} /></label>
                <label>Credit period days<input type="number" min="0" value={assignment.creditPeriodDays} onChange={event => setAssignment({ ...assignment, creditPeriodDays: event.target.value })} /></label>
                <label>Credit limit<input type="number" min="0" step="0.01" value={assignment.creditLimit} onChange={event => setAssignment({ ...assignment, creditLimit: event.target.value })} /></label>
                <label>Opening balance<input type="number" step="0.01" value={assignment.openingBalance} onChange={event => setAssignment({ ...assignment, openingBalance: event.target.value })} /></label>
                <label>Opening balance date<input type="date" value={assignment.openingBalanceDate} onChange={event => setAssignment({ ...assignment, openingBalanceDate: event.target.value })} /></label>
                <label>Opening balance type<input value={assignment.openingBalanceType} placeholder="PAYABLE / ADVANCE" onChange={event => setAssignment({ ...assignment, openingBalanceType: event.target.value })} /></label>
                <label>Preferred payment method<input value={assignment.preferredPaymentMethod} placeholder="Bank / UPI / Cash / Cheque" onChange={event => setAssignment({ ...assignment, preferredPaymentMethod: event.target.value })} /></label>
                <label>Assigned salesperson<input value={assignment.assignedSalesperson} onChange={event => setAssignment({ ...assignment, assignedSalesperson: event.target.value })} /></label>
                <label>Seller notes<textarea rows="3" value={assignment.sellerNotes} onChange={event => setAssignment({ ...assignment, sellerNotes: event.target.value })} /></label>
                <button disabled={!assignment.sellerId || !assignment.distributorId || !assignment.brand}>Assign distributor</button>
            </form>
        </section>

        <section className="dist-card">
            <h2>Current seller assignments <b>{assignments.length}</b></h2>
            {assignments.length ? assignments.map(item => <article className="dist-row" key={item.id}>
                <div><strong>{item.sellerName} → {item.distributorName}</strong><small>{item.sellerEmail} · Brand: {item.brand} · {item.active ? "Active" : "Inactive"}</small></div>
                <button className="reject" onClick={() => removeAssignment(item.id)}>Remove</button>
            </article>) : <p className="dist-empty">No seller assignments yet.</p>}
        </section>

        <section className="dist-card">
            <h2>Pending seller distributor requests <b>{pending.length}</b></h2>
            {pending.length ? pending.map(distributor => <article className="dist-row" key={distributor.id}>
                <div><strong>{distributor.businessName}</strong><small>{distributor.mobile} · {distributor.email || "No email"} · Brands: {distributor.brands || "ALL_BRANDS"}</small></div>
                <div><button onClick={() => reviewRequest(distributor.id, "approve")}>Approve</button><button className="reject" onClick={() => reviewRequest(distributor.id, "reject")}>Reject</button></div>
            </article>) : <p className="dist-empty">No pending requests.</p>}
        </section>
    </main>;
}
