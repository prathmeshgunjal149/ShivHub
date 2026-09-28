import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "./AdminGrowth.css";

const defaults = {
  minimumPurchaseAmount: 0,
  pointsPerPurchaseUnit: 1,
  purchaseUnitInRupees: 100,
  pointValueInRupees: 1,
  maximumPointsPerSale: 0,
  active: true
};

export default function LoyaltyManagement() {
  const [settings, setSettings] = useState(defaults);
  const [adjustment, setAdjustment] = useState({ customerProfileId: "", points: "", reason: "" });
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const load = () => api.get("/api/admin/loyalty/settings")
    .then(({ data }) => setSettings(data.find(item => item.scopeType === "GLOBAL") || defaults))
    .catch(() => setError("Loyalty settings could not be loaded."));

  useEffect(() => { load(); }, []);

  const save = async event => {
    event.preventDefault();
    try {
      await api.put("/api/admin/loyalty/settings", {
        ...settings,
        sellerId: null,
        minimumPurchaseAmount: Number(settings.minimumPurchaseAmount),
        pointsPerPurchaseUnit: Number(settings.pointsPerPurchaseUnit),
        purchaseUnitInRupees: Number(settings.purchaseUnitInRupees),
        // Kept for the migration-compatible API; the server enforces 3 points = Rs. 1.
        pointValueInRupees: 1,
        maximumPointsPerSale: Number(settings.maximumPointsPerSale)
      });
      setNotice("Loyalty settings saved.");
    } catch (err) {
      setError(err.response?.data?.message || "Settings could not be saved.");
    }
  };

  const adjust = async event => {
    event.preventDefault();
    try {
      await api.post("/api/admin/loyalty/adjustments", {
        ...adjustment,
        customerProfileId: Number(adjustment.customerProfileId),
        points: Number(adjustment.points)
      });
      setNotice("Manual adjustment recorded in the audit ledger.");
      setAdjustment({ customerProfileId: "", points: "", reason: "" });
    } catch (err) {
      setError(err.response?.data?.message || "Adjustment could not be recorded.");
    }
  };

  const field = (key, label, min = 0) => (
    <label>
      {label}
      <input
        type="number"
        min={min}
        value={settings[key]}
        onChange={event => setSettings(old => ({ ...old, [key]: event.target.value }))}
        required
      />
    </label>
  );

  return (
    <main className="growth-page">
      <Link className="back-link" to="/admin/dashboard">Back to Dashboard</Link>
      <header className="growth-heading">
        <div>
          <p className="eyebrow">Customer rewards</p>
          <h1>Loyalty & credit points</h1>
          <p>Settings and manual adjustments are applied to the immutable points ledger.</p>
        </div>
      </header>
      {error && <p className="growth-error">{error}</p>}
      {notice && <p className="growth-success">{notice}</p>}

      <div className="detail-columns">
        <form className="growth-card campaign-form" onSubmit={save}>
          <h2>Global earning and redemption rules</h2>
          {field("minimumPurchaseAmount", "Minimum purchase (Rs.)")}
          {field("pointsPerPurchaseUnit", "Points per purchase unit", 1)}
          {field("purchaseUnitInRupees", "Purchase unit (Rs.)", 0.01)}
          <p className="loyalty-fixed-rate">
            <strong>Fixed redemption rate:</strong> 3 ShivHub Points = Rs. 1. Customers can redeem points only in multiples of 3.
          </p>
          {field("maximumPointsPerSale", "Maximum points per bill/order (0 = no cap)")}
          <label className="check-row">
            <input type="checkbox" checked={Boolean(settings.active)} onChange={event => setSettings(old => ({ ...old, active: event.target.checked }))} /> Active
          </label>
          <button className="primary-button">Save settings</button>
        </form>

        <form className="growth-card campaign-form" onSubmit={adjust}>
          <h2>Manual adjustment</h2>
          <label>Customer profile ID<input type="number" min="1" value={adjustment.customerProfileId} onChange={event => setAdjustment(old => ({ ...old, customerProfileId: event.target.value }))} required /></label>
          <label>Points (+ or -)<input type="number" value={adjustment.points} onChange={event => setAdjustment(old => ({ ...old, points: event.target.value }))} required /></label>
          <label>Reason<textarea value={adjustment.reason} onChange={event => setAdjustment(old => ({ ...old, reason: event.target.value }))} required /></label>
          <button className="secondary-button">Record adjustment</button>
        </form>
      </div>
    </main>
  );
}
