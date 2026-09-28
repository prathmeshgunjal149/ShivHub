import { Link } from "react-router-dom";
import WorkspacePreferences from "./WorkspacePreferences";
import "./AdminFooter.css";

export default function AdminFooter() {
    return <footer className="admin-console-footer">
        <div className="admin-console-footer-inner">
            <div><strong><span>SH</span> ShivHub Admin</strong><p>Secure Admin Console · Marketplace operations workspace</p></div>
            <nav aria-label="Admin footer navigation"><Link to="/admin/dashboard">Overview</Link><Link to="/admin/sellers">Sellers</Link><Link to="/admin/customers">Customers</Link><Link to="/admin/campaigns">Campaigns</Link><Link to="/admin/settings">Settings</Link></nav>
            <div className="admin-console-support"><strong>Support</strong><span>Configure contact details in Admin Settings</span><WorkspacePreferences scope="admin" compact /></div>
        </div>
        <div className="admin-console-footer-bottom"><span>© {new Date().getFullYear()} ShivHub. All rights reserved.</span><span>Version 1.0 · Secure Admin Console</span></div>
    </footer>;
}
