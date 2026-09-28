import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getPendingSellers } from "../../services/adminService";
import "./AdminManagement.css";
export default function SellerRequests() {
 const [items,setItems]=useState([]); const [error,setError]=useState("");
 useEffect(()=>{getPendingSellers().then(setItems).catch(()=>setError("Could not load seller requests."));},[]);
 return <section className="admin-management"><header><div><p className="eyebrow">Review queue</p><h1>Seller Requests</h1><p>Review seller applications and notify applicants of your decision.</p></div><Link className="back-link" to="/admin/sellers">All sellers</Link></header>{error&&<p className="admin-error">{error}</p>}<div className="management-card"><table><thead><tr><th>Applicant</th><th>Email</th><th>Mobile</th><th /></tr></thead><tbody>{items.map(s=><tr key={s.id}><td><strong>{s.name}</strong></td><td>{s.email}</td><td>{s.mobile||"—"}</td><td><Link className="primary-link" to={`/admin/seller-requests/${s.id}`}>Review request</Link></td></tr>)}</tbody></table>{!items.length&&!error&&<p className="empty">No pending seller requests.</p>}</div></section>;
}
