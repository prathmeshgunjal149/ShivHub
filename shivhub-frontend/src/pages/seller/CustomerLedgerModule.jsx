import { useEffect, useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import api from "../../services/api";
import "./PaymentModule.css";
import "./PremiumPolish.css";

const money = value =>
    `₹${Number(value || 0).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;

const emptyEntry = () => ({
    customerName: "",
    customerMobile: "",
    customerEmail: "",
    invoiceNumber: "",
    productDetails: "",
    imei: "",
    saleAmount: "",
    paidAmount: "",
    dueDate: ""
});

export default function CustomerLedgerModule() {
    const [rows, setRows] = useState([]);
    const [selected, setSelected] = useState("");
    const [history, setHistory] = useState([]);
    const [error, setError] = useState("");
    const [showEntry, setShowEntry] = useState(false);
    const [customerSearch, setCustomerSearch] = useState("");
    const [entry, setEntry] = useState(emptyEntry());

    const load = () =>
        api.get("/api/customer-receivables")
            .then(response => setRows(response.data || []))
            .catch(err => setError(err.response?.data?.message || "Unable to load customer ledger."));

    useEffect(() => {
        void load();
    }, []);

    const customers = useMemo(
        () => [...new Map(rows.map(row => [
            `${row.customerName}|${row.customerMobile || ""}`,
            { name: row.customerName, mobile: row.customerMobile || "" }
        ])).values()],
        [rows]
    );

    const filteredCustomers = useMemo(() => {
        const search = customerSearch.trim().toLowerCase();
        if (!search) return customers;
        return customers.filter(customer =>
            `${customer.name} ${customer.mobile}`.toLowerCase().includes(search)
        );
    }, [customers, customerSearch]);

    const customerRows = useMemo(
        () => rows.filter(row => `${row.customerName}|${row.customerMobile || ""}` === selected),
        [rows, selected]
    );

    const choose = async value => {
        setSelected(value);
        setHistory([]);
        if (!value) return;

        try {
            const matching = rows.filter(row => `${row.customerName}|${row.customerMobile || ""}` === value);
            const result = await Promise.all(
                matching.map(row =>
                    api.get(`/api/customer-receivables/${row.id}/payments`).then(response => response.data || [])
                )
            );
            setHistory(result.flat().sort((a, b) => String(b.paymentDate).localeCompare(String(a.paymentDate))));
        } catch (err) {
            setError(err.response?.data?.message || "Unable to load payment history.");
        }
    };

    const saveEntry = async event => {
        event.preventDefault();

        try {
            const saleAmount = Number(entry.saleAmount);
            const paidAmount = Number(entry.paidAmount || 0);

            if (paidAmount > saleAmount) {
                setError("Received amount cannot be more than sale amount.");
                return;
            }

            const created = await api.post("/api/customer-receivables", {
                ...entry,
                saleAmount,
                saleDate: new Date().toISOString().slice(0, 10)
            });

            if (paidAmount > 0) {
                await api.post(`/api/customer-receivables/${created.data.id}/payments`, {
                    amount: paidAmount,
                    paymentMethod: "CASH",
                    notes: "Initial payment at customer credit-sale entry"
                });
            }

            setEntry(emptyEntry());
            setShowEntry(false);
            await load();
        } catch (err) {
            setError(err.response?.data?.message || "Customer entry could not be saved.");
        }
    };

    const totalSale = customerRows.reduce((sum, row) => sum + Number(row.saleAmount || 0), 0);
    const totalPaid = customerRows.reduce((sum, row) => sum + Number(row.paidAmount || 0), 0);
    const balance = customerRows.reduce((sum, row) => sum + Number(row.remainingAmount || 0), 0);

    return (
        <div className="payment-page">
            <SellerSidebar />
            <main className="payment-main">
                <header>
                    <div>
                        <span>CUSTOMER LEDGER</span>
                        <h1>Customer full details</h1>
                        <p>Search customer by name or mobile and see all phones, IMEIs, bills and payments.</p>
                    </div>
                    <button onClick={() => setShowEntry(!showEntry)}>
                        {showEntry ? "Close entry" : "+ Add Customer Due"}
                    </button>
                </header>

                {error && <div className="payment-error">{error}</div>}

                {showEntry && (
                    <section className="payment-entry">
                        <form onSubmit={saveEntry}>
                            <h2>Customer credit-sale entry</h2>
                            <input required placeholder="Customer name" value={entry.customerName} onChange={e => setEntry({ ...entry, customerName: e.target.value })} />
                            <input placeholder="Mobile number" value={entry.customerMobile} onChange={e => setEntry({ ...entry, customerMobile: e.target.value })} />
                            <input type="email" placeholder="Email" value={entry.customerEmail} onChange={e => setEntry({ ...entry, customerEmail: e.target.value })} />
                            <input placeholder="Invoice / bill number" value={entry.invoiceNumber} onChange={e => setEntry({ ...entry, invoiceNumber: e.target.value })} />
                            <input required placeholder="Mobile / product, RAM, storage, colour" value={entry.productDetails} onChange={e => setEntry({ ...entry, productDetails: e.target.value })} />
                            <input placeholder="IMEI for mobile" value={entry.imei} onChange={e => setEntry({ ...entry, imei: e.target.value })} />
                            <input required type="number" min="0.01" placeholder="Total sale amount" value={entry.saleAmount} onChange={e => setEntry({ ...entry, saleAmount: e.target.value })} />
                            <input type="number" min="0" placeholder="Paid now (optional)" value={entry.paidAmount} onChange={e => setEntry({ ...entry, paidAmount: e.target.value })} />
                            <input type="date" title="Promise date for remaining balance" value={entry.dueDate} onChange={e => setEntry({ ...entry, dueDate: e.target.value })} />
                            <button>Save customer entry</button>
                        </form>
                    </section>
                )}

                <section className="payment-entry">
                    <form>
                        <h2>Find existing customer</h2>
                        <input placeholder="Search customer name or mobile" value={customerSearch} onChange={e => setCustomerSearch(e.target.value)} />
                        <select value={selected} onChange={e => choose(e.target.value)}>
                            <option value="">Select customer name / mobile</option>
                            {filteredCustomers.map(customer => (
                                <option key={`${customer.name}|${customer.mobile}`} value={`${customer.name}|${customer.mobile}`}>
                                    {customer.name}{customer.mobile ? ` — ${customer.mobile}` : ""}
                                </option>
                            ))}
                        </select>
                    </form>
                </section>

                {selected && (
                    <>
                        <section className="payment-kpis">
                            <article><small>Total mobile / product sale</small><b>{money(totalSale)}</b></article>
                            <article><small>Total money received</small><b>{money(totalPaid)}</b></article>
                            <article><small>Remaining balance</small><b>{money(balance)}</b></article>
                            <article><small>Purchase / credit bills</small><b>{customerRows.length}</b></article>
                        </section>

                        <section className="payment-table">
                            <h2>All mobiles, products and bills</h2>
                            <table>
                                <thead>
                                    <tr><th>Bill / date</th><th>Product / Mobile</th><th>IMEI</th><th>Sale total</th><th>Paid</th><th>Balance</th><th>Promise / status</th></tr>
                                </thead>
                                <tbody>
                                    {customerRows.map(row => (
                                        <tr key={row.id}>
                                            <td><b>{row.invoiceNumber || "No bill"}</b><small>{row.saleDate}</small></td>
                                            <td>{row.productDetails || "—"}</td>
                                            <td>{row.imei || "—"}</td>
                                            <td>{money(row.saleAmount)}</td>
                                            <td>{money(row.paidAmount)}</td>
                                            <td><b>{money(row.remainingAmount)}</b></td>
                                            <td>{row.dueDate || "—"}<small>{row.paymentStatus}</small></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </section>

                        <section className="payment-table">
                            <h2>Complete payment history</h2>
                            <table>
                                <thead>
                                    <tr><th>Payment date</th><th>Amount received</th><th>Mode</th><th>Reference</th><th>Notes</th></tr>
                                </thead>
                                <tbody>
                                    {history.length ? history.map(payment => (
                                        <tr key={payment.id}>
                                            <td>{String(payment.paymentDate || "").replace("T", " ")}</td>
                                            <td><b>{money(payment.amount)}</b></td>
                                            <td>{payment.paymentMethod}</td>
                                            <td>{payment.transactionReference || "—"}</td>
                                            <td>{payment.notes || "—"}</td>
                                        </tr>
                                    )) : <tr><td colSpan="5">No payment received yet.</td></tr>}
                                </tbody>
                            </table>
                        </section>
                    </>
                )}
            </main>
        </div>
    );
}
