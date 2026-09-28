import { useState } from "react";
import { useNavigate } from "react-router-dom";
import SellerSidebar from "./SellerSidebar";
import "./SellerReports.css";

const reports = [
    ["Sales Report", "sales", "Combined online and POS sale entries"],
    ["Invoice / Bill Report", "invoice", "Find and reprint saved POS bills"],
    ["Product-wise Sales", "product-sales", "Product quantities and sales value"],
    ["Category-wise Sales", "category-sales", "Sales grouped by product category"],
    ["IMEI / Serial Report", "serial-report", "Traceable physical-unit history"],
    ["Payment Report", "payment-report", "POS collection by payment method"],
    ["Customer Credit / Collection", "customer-credit-report", "Credit sales, money received and outstanding balance"],
    ["GST Report", "gst-report", "Recorded POS GST breakup"],
    ["Stock Report", "stock-report", "Current inventory position"],
    ["Low Stock", "low-stock-report", "Products at or below five units"],
    ["Purchase Report", "purchase-report", "Distributor purchase history"],
    ["Customer Sales", "customer-report", "POS customer purchase activity"]
];

export default function SellerReports() {
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return <div className="seller-reports-page">
        <SellerSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="seller-reports-main">
            <header className="seller-reports-header">
                <button className="reports-menu" type="button" onClick={() => setSidebarOpen(true)}>☰</button>
                <div><span>REPORT CENTRE</span><h1>Business reports</h1><p>Choose a report to open it on its own page.</p></div>
            </header>
            <section className="report-catalogue report-catalogue-alone">
                <div><span>SELLER REPORTS</span><h2>Find the entry you need</h2><p>Each report has its own date range and entry-search filter.</p></div>
                <div className="report-card-grid">
                    {reports.map(([title, key, description]) => <button className="report-live report-link" type="button" key={key} onClick={() => navigate(`/seller/reports/${key}`)}>
                        <div><strong>{title}</strong><small>{description}</small></div><em>Open report →</em>
                    </button>)}
                    <article><div><strong>Profit & Margin</strong><small>Needs sale-to-cost allocation.</small></div><em>Planned</em></article>
                </div>
            </section>
        </main>
    </div>;
}
