import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "../../components/payments/Finance.css";

const money = n => Number(n || 0).toLocaleString("en-IN", { style: "currency", currency: "INR" });

export default function FinanceManagement({ admin = false }) {
  const [rows, setRows] = useState({ content: [], totalPages: 0 });
  const [filters, setFilters] = useState({ q: "", companyId: "", status: "", from: "", to: "" });
  const [page, setPage] = useState(0);
  const [companies, setCompanies] = useState([]);
  const [schemes, setSchemes] = useState([]);
  const [requests, setRequests] = useState({ content: [], totalPages: 0 });
  const [requestPage, setRequestPage] = useState(0);
  const [masterPage, setMasterPage] = useState(0);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [company, setCompany] = useState({ name: "", contact: "", active: true });
  const [scheme, setScheme] = useState({ name: "", companyId: "", tenureMonths: 8, advanceMonths: 2, active: true });
  const [settlement, setSettlement] = useState({ amount: "", date: "", reference: "", status: "RECEIVED", reason: "" });

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const params = Object.fromEntries(Object.entries({ ...filters, page, size: 25 }).filter(([, v]) => v !== ""));
      const [sales, c, s, r] = await Promise.all([
        api.get("/api/finance/sales", { params }),
        api.get(admin ? "/api/finance/admin/companies" : "/api/finance/companies", { params: { page: masterPage } }),
        api.get(admin ? "/api/finance/admin/schemes" : "/api/finance/schemes", { params: { page: masterPage } }),
        api.get("/api/finance/company-requests", { params: { page: requestPage } })
      ]);
      setRows(sales.data);
      setCompanies(admin ? c.data.content : c.data);
      setSchemes(admin ? s.data.content : s.data);
      setRequests(r.data);
    } catch (e) {
      setError(e.response?.data?.message || "Unable to load finance records.");
    } finally {
      setBusy(false);
    }
  }, [admin, filters, page, masterPage, requestPage]);

  useEffect(() => {
    const timer = setTimeout(() => void load(), 300);
    return () => clearTimeout(timer);
  }, [load]);

  const action = async (fn) => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await fn();
      setNotice("Saved successfully.");
      await load();
    } catch (e) {
      setError(e.response?.data?.message || "Unable to save. Check the entered values.");
    } finally {
      setBusy(false);
    }
  };

  const open = async id => {
    try {
      const { data } = await api.get(`/api/finance/sales/${id}`);
      setDetail(data);
      setSettlement({
        amount: data.sale.expectedDisbursement,
        date: new Date().toLocaleDateString("en-CA"),
        reference: "",
        status: "RECEIVED",
        reason: ""
      });
    } catch {
      setError("Unable to load finance details.");
    }
  };

  const reviewCompanyRequest = (id, approve) => {
    const reason = approve
      ? "Company verified and approved by admin"
      : window.prompt("Rejection reason");
    if (!reason?.trim()) return;
    void action(() => api.patch(`/api/finance/admin/company-requests/${id}`, { approve, reason }));
  };

  return (
    <main className="finance-management">
      <header>
        <h1>Finance & EMI management</h1>
        <Link to={admin ? "/admin/settings" : "/seller/billing"}>Back</Link>
        <p>Full invoice sales, provider settlements and customer EMI reminders.</p>
      </header>

      {error && <p role="alert">{error}</p>}
      {notice && <p role="status">{notice}</p>}

      {admin && (
        <section>
          <h2>Finance configuration</h2>
          <div className="finance-grid">
            <form onSubmit={e => {
              e.preventDefault();
              void action(async () => {
                const endpoint = company.id ? `/api/finance/admin/companies/${company.id}` : "/api/finance/admin/companies";
                await api[company.id ? "put" : "post"](endpoint, company);
                setCompany({ name: "", contact: "", active: true });
              });
            }}>
              <h3>{company.id ? "Edit" : "Add approved"} company</h3>
              <label>Name<input required maxLength={150} value={company.name} onChange={e => setCompany({ ...company, name: e.target.value })} /></label>
              <label>Contact/reference<input maxLength={500} value={company.contact || ""} onChange={e => setCompany({ ...company, contact: e.target.value })} /></label>
              <label>Availability<select value={String(company.active)} onChange={e => setCompany({ ...company, active: e.target.value === "true" })}><option value="true">Active</option><option value="false">Inactive</option></select></label>
              <button disabled={busy}>Save company</button>
            </form>

            <form onSubmit={e => {
              e.preventDefault();
              void action(async () => {
                const endpoint = scheme.id ? `/api/finance/admin/schemes/${scheme.id}` : "/api/finance/admin/schemes";
                await api[scheme.id ? "put" : "post"](endpoint, {
                  ...scheme,
                  companyId: scheme.companyId ? Number(scheme.companyId) : null,
                  tenureMonths: Number(scheme.tenureMonths),
                  advanceMonths: Number(scheme.advanceMonths)
                });
                setScheme({ name: "", companyId: "", tenureMonths: 8, advanceMonths: 2, active: true });
              });
            }}>
              <h3>{scheme.id ? "Edit" : "Add"} scheme</h3>
              <label>Name/code<input required maxLength={100} placeholder="8/2" value={scheme.name} onChange={e => setScheme({ ...scheme, name: e.target.value })} /></label>
              <label>Company<select value={scheme.companyId || ""} onChange={e => setScheme({ ...scheme, companyId: e.target.value })}><option value="">All companies</option>{companies.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
              <label>Installment rows<input required type="number" min="1" max="120" value={scheme.tenureMonths} onChange={e => setScheme({ ...scheme, tenureMonths: e.target.value })} /></label>
              <label>Advance months metadata<input required type="number" min="0" max={scheme.tenureMonths} value={scheme.advanceMonths} onChange={e => setScheme({ ...scheme, advanceMonths: e.target.value })} /></label>
              <label>Availability<select value={String(scheme.active)} onChange={e => setScheme({ ...scheme, active: e.target.value === "true" })}><option value="true">Active</option><option value="false">Inactive</option></select></label>
              <button disabled={busy}>Save scheme</button>
            </form>
          </div>

          <div className="finance-grid">
            <div>{companies.map(c => <p key={c.id}>{c.name} · {c.active ? "Active" : "Inactive"}<button onClick={() => setCompany(c)}>Edit</button></p>)}</div>
            <div>{schemes.map(s => <p key={s.id}>{s.name} · {s.tenureMonths} installments · {s.active ? "Active" : "Inactive"}<button onClick={() => setScheme(s)}>Edit</button></p>)}</div>
          </div>
          <button disabled={!masterPage} onClick={() => setMasterPage(masterPage - 1)}>Previous configuration page</button>
          <button disabled={companies.length < 50 && schemes.length < 50} onClick={() => setMasterPage(masterPage + 1)}>Next</button>
        </section>
      )}

      <section>
        <h2>Company requests</h2>
        {requests.content.map(r => (
          <p key={r.id}>
            <strong>{r.name}</strong> · {r.status} · {r.contact} · {r.remarks}
            {admin && r.status === "PENDING" && (
              <>
                <button disabled={busy} onClick={() => reviewCompanyRequest(r.id, true)}>Approve</button>
                <button disabled={busy} onClick={() => reviewCompanyRequest(r.id, false)}>Reject</button>
              </>
            )}
          </p>
        ))}
        {!requests.content.length && <p>No company requests.</p>}
        <button disabled={!requestPage} onClick={() => setRequestPage(requestPage - 1)}>Previous</button>
        <button disabled={requestPage + 1 >= requests.totalPages} onClick={() => setRequestPage(requestPage + 1)}>Next</button>
      </section>

      <section>
        <h2>Finance sales & settlements</h2>
        <div className="finance-toolbar">
          <label>Search<input placeholder="Customer, mobile, invoice, loan…" value={filters.q} onChange={e => { setPage(0); setFilters({ ...filters, q: e.target.value }); }} /></label>
          <label>Company<select value={filters.companyId} onChange={e => { setPage(0); setFilters({ ...filters, companyId: e.target.value }); }}><option value="">All</option>{companies.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
          <label>Status<select value={filters.status} onChange={e => { setPage(0); setFilters({ ...filters, status: e.target.value }); }}><option value="">All</option>{["PENDING", "PARTIALLY_RECEIVED", "RECEIVED", "ADJUSTED"].map(s => <option key={s}>{s}</option>)}</select></label>
          {["from", "to"].map(key => <label key={key}>{key}<input type="date" value={filters[key]} onChange={e => { setPage(0); setFilters({ ...filters, [key]: e.target.value }); }} /></label>)}
        </div>

        <div className="finance-table">
          <table>
            <thead>
              <tr>
                <th>Invoice / customer</th>
                <th>Provider / loan</th>
                <th>Full sale</th>
                <th>Downpayment</th>
                <th>Expected</th>
                <th>Received</th>
                <th>Status</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {rows.content.map(r => (
                <tr key={r.id}>
                  <td>{r.invoice}<br />{r.customer}<br />{r.mobile}</td>
                  <td>{r.company}<br />{r.loanNumber}</td>
                  <td>{money(r.fullSaleAmount)}</td>
                  <td>{money(r.downpayment)}</td>
                  <td>{money(r.expectedDisbursement)}</td>
                  <td>{money(r.actualDisbursement)}</td>
                  <td>{r.active ? r.settlementStatus : "REMINDERS STOPPED"}</td>
                  <td><button onClick={() => void open(r.id)}>View</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!rows.content.length && <p>{busy ? "Loading…" : "No finance sales match these filters."}</p>}
        <button disabled={!page || busy} onClick={() => setPage(page - 1)}>Previous</button>
        <span>Page {page + 1}</span>
        <button disabled={page + 1 >= rows.totalPages || busy} onClick={() => setPage(page + 1)}>Next</button>
      </section>

      {detail && (
        <section className="finance-detail">
          <button onClick={() => setDetail(null)}>Close details</button>
          <h2>{detail.sale.invoice} · {detail.sale.customer}</h2>
          <p>{detail.sale.email} · {detail.sale.mobile} · {detail.sale.company} · {detail.sale.scheme}</p>
          <p>Summary email: {detail.sale.summarySent ? "Sent" : "Pending / retry"}</p>
          <p>Processing: {money(detail.sale.processingCharges)} · DBD: {money(detail.sale.dbdCharges)} · Other: {money(detail.sale.otherCharges)} · Deduction: {money(detail.sale.deduction)} · Adjustment: {money(detail.sale.adjustment)}</p>
          <p>{detail.sale.remarks}</p>

          <div className="finance-table">
            <table>
              <thead>
                <tr>
                  <th>Month</th>
                  <th>Amount</th>
                  <th>Due</th>
                  <th>Reminder</th>
                </tr>
              </thead>
              <tbody>
                {detail.installments.map(r => (
                  <tr key={r.id}>
                    <td>{r.monthLabel}</td>
                    <td>{money(r.amount)}</td>
                    <td>{r.dueDate}</td>
                    <td>{r.reminderSent ? "Sent" : r.failureMessage || r.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <form onSubmit={e => {
            e.preventDefault();
            void action(async () => {
              await api.patch(`/api/finance/sales/${detail.sale.id}/settlement`, { ...settlement, amount: Number(settlement.amount) });
              await open(detail.sale.id);
            });
          }}>
            <h3>Record cumulative provider settlement</h3>
            <div className="finance-grid">
              {["amount", "date", "reference", "reason"].map(key => (
                <label key={key}>
                  {key}
                  <input
                    required={key !== "reason"}
                    type={key === "date" ? "date" : key === "amount" ? "number" : "text"}
                    step={key === "amount" ? "0.01" : undefined}
                    min={key === "amount" ? 0 : undefined}
                    maxLength={key === "reason" ? 700 : 150}
                    value={settlement[key]}
                    onChange={e => setSettlement({ ...settlement, [key]: e.target.value })}
                  />
                </label>
              ))}
              <label>Status<select value={settlement.status} onChange={e => setSettlement({ ...settlement, status: e.target.value })}>{["PENDING", "PARTIALLY_RECEIVED", "RECEIVED", "ADJUSTED"].map(s => <option key={s}>{s}</option>)}</select></label>
            </div>
            <button disabled={busy || !detail.sale.active}>Save settlement</button>
          </form>
        </section>
      )}
    </main>
  );
}
