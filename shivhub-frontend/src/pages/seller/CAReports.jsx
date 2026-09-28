import { useMemo, useState } from "react";
import SellerSidebar from "./SellerSidebar";
import { CA_REPORTS, downloadCaBundle, downloadCaCsv } from "../../services/caReportService";
import "./CAReports.css";
import "./PremiumPolish.css";

const today = () => new Date().toISOString().slice(0, 10);
const monthStart = () => {
    const date = new Date();
    date.setDate(1);
    return date.toISOString().slice(0, 10);
};

export default function CAReports() {
    const [range, setRange] = useState({ startDate: monthStart(), endDate: today() });
    const [busy, setBusy] = useState("");
    const [error, setError] = useState("");

    const fyLabel = useMemo(() => {
        const startYear = new Date(range.startDate).getFullYear();
        const endYear = new Date(range.endDate).getFullYear();
        return `${startYear}-${String(endYear).slice(-2)}`;
    }, [range]);

    const download = async reportKey => {
        try {
            setBusy(reportKey);
            setError("");
            await downloadCaCsv(reportKey, range);
        } catch (err) {
            setError(err.response?.data?.message || "Could not download CA report.");
        } finally {
            setBusy("");
        }
    };

    const downloadAll = async () => {
        try {
            setBusy("all");
            setError("");
            for (const report of CA_REPORTS) {
                await downloadCaCsv(report.key, range);
            }
        } catch (err) {
            setError(err.response?.data?.message || "Could not download all CA reports.");
        } finally {
            setBusy("");
        }
    };

    const downloadBundle = async type => {
        try {
            setBusy(type);
            setError("");
            await downloadCaBundle(type, range);
        } catch (err) {
            setError(err.response?.data?.message || "Could not download CA export.");
        } finally {
            setBusy("");
        }
    };

    return <div className="ca-page">
        <SellerSidebar />
        <main className="ca-main">
            <header className="ca-header">
                <div>
                    <span>CA REPORTS</span>
                    <h1>Tax and accounts export</h1>
                    <p>Choose a date range and export seller-scoped records for your CA.</p>
                </div>
                <div className="ca-header-actions">
                    <button type="button" onClick={() => downloadBundle("workbook")} disabled={Boolean(busy)}>{busy === "workbook" ? "Preparing..." : "Download Excel Workbook"}</button>
                    <button type="button" onClick={() => downloadBundle("pdf")} disabled={Boolean(busy)}>{busy === "pdf" ? "Preparing..." : "Download PDF Summary"}</button>
                    <button type="button" onClick={() => downloadBundle("zip")} disabled={Boolean(busy)}>{busy === "zip" ? "Preparing..." : "Download Documents ZIP"}</button>
                    <button type="button" onClick={downloadAll} disabled={Boolean(busy)}>{busy === "all" ? "Preparing..." : "Download all CSV"}</button>
                </div>
            </header>

            {error && <div className="ca-error">{error}</div>}

            <section className="ca-filter-card">
                <div>
                    <span>PERIOD</span>
                    <h2>{fyLabel}</h2>
                    <p>Use financial year dates or any custom audit period.</p>
                </div>
                <label>Start date<input type="date" value={range.startDate} onChange={e => setRange({ ...range, startDate: e.target.value })} /></label>
                <label>End date<input type="date" value={range.endDate} onChange={e => setRange({ ...range, endDate: e.target.value })} /></label>
            </section>

            <section className="ca-grid">
                {CA_REPORTS.map(report => <article key={report.key}>
                    <div>
                        <strong>{report.title}</strong>
                        <small>{report.description}</small>
                    </div>
                    <em className="ready">Available</em>
                    <button type="button" onClick={() => download(report.key)} disabled={Boolean(busy)}>
                        {busy === report.key ? "Preparing..." : "Download CSV"}
                    </button>
                </article>)}
            </section>

            <section className="ca-card">
                <h2>Export roadmap</h2>
                <div className="ca-export-row"><span>Excel workbook</span><b>Available: all reports in one Excel-compatible workbook</b></div>
                <div className="ca-export-row"><span>PDF summary</span><b>Available: P&L and GST summary composer</b></div>
                <div className="ca-export-row"><span>Documents ZIP</span><b>Available: CSV bundle; invoice/receipt file collection can be expanded when storage links are finalized</b></div>
            </section>
        </main>
    </div>;
}
