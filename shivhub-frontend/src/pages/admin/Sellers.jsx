import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSellers } from "../../services/adminService";
import "./AdminManagement.css";

export default function Sellers() {
  const [sellers, setSellers] = useState([]); const [error, setError] = useState("");
  useEffect(() => { getSellers().then(setSellers).catch(() => setError("Could not load sellers.")); }, []);
  return <section className="admin-management"><header><div><p className="eyebrow">Seller management</p><h1>Sellers</h1><p>Approved, pending and rejected seller accounts.</p></div><Link className="back-link" to="/admin/dashboard">Dashboard</Link></header>{error && <p className="admin-error">{error}</p>}<div className="management-card"><table><thead><tr><th>Seller</th><th>Contact</th><th>Status</th><th /></tr></thead><tbody>{sellers.map(s => <tr key={s.id}><td><strong>{s.name}</strong><small>Seller #{s.id}</small></td><td>{s.email}<small>{s.mobile || "No mobile"}</small></td><td><span className={`status ${s.status?.toLowerCase()}`}>{s.status}</span></td><td><Link className="text-link" to={`/admin/sellers/${s.id}`}>View details</Link></td></tr>)}</tbody></table>{!sellers.length && !error && <p className="empty">No sellers found.</p>}</div></section>;
}
