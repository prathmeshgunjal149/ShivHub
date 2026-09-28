import { Link } from "react-router-dom";
import "./MarketingDashboard.css";

export default function MarketingReports() {
    return <main className="marketing">
        <Link className="back-link" to="/admin/marketing">← Marketing</Link>
        <h1>Marketing reports</h1>
        <p className="lead">Email delivery totals are displayed when a campaign is sent. Coupon redemption and conversion analytics can be added when checkout records are available.</p>
    </main>;
}
