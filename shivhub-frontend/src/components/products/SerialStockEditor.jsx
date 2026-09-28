export default function SerialStockEditor({ quantity, purchase, onPurchaseChange, units, onUnitsChange, distributors, loading }) {
    const count = Math.max(0, Math.min(500, Number(quantity) || 0));
    const change = (key, value) => onPurchaseChange({ ...purchase, [key]: value });
    const updateUnit = (index, key, value) => onUnitsChange(Array.from({ length: count }, (_, i) =>
        i === index ? { ...units[i], [key]: value } : (units[i] || { imei1: "", imei2: "", serialNumber: "" })));
    return <section className="serial-stock-editor">
        <h2>IMEI / serial stock units</h2>
        <p>Enter one physical unit per stock quantity. A laptop/TV can use its serial number; IMEI is optional unless the device has one. These identifiers are saved to existing purchase stock, not just product specifications.</p>
        {!count ? <p>Stock is zero. Save the product, then register units through Purchases when stock arrives.</p> : <>
            <h3>Actual opening purchase</h3>
            <p>A real distributor invoice and purchase rate are required to preserve stock and GST history. Selling price remains GST-inclusive.</p>
            <div className="add-other-grid">
                <label>Distributor<select required disabled={loading} value={purchase.sellerDistributorId} onChange={event => change("sellerDistributorId", event.target.value)}>
                    <option value="">{loading ? "Loading your distributors…" : "Select linked distributor"}</option>
                    {distributors.map(distributor => <option key={distributor.id} value={distributor.id}>{distributor.distributorName} {distributor.brand ? `— ${distributor.brand}` : ""}</option>)}
                </select></label>
                <label>Distributor invoice number<input required maxLength={100} value={purchase.invoiceNumber} onChange={event => change("invoiceNumber", event.target.value)} /></label>
                <label>Purchase date<input type="datetime-local" required value={purchase.purchaseDate} onChange={event => change("purchaseDate", event.target.value)} /></label>
                <label>Purchase unit rate (excl. GST)<input type="number" required min="0.01" step="0.01" value={purchase.unitPrice} onChange={event => change("unitPrice", event.target.value)} /></label>
                <label>Purchase GST (%)<input type="number" required min="0" max="100" step="0.01" value={purchase.gstRate} onChange={event => change("gstRate", event.target.value)} /></label>
            </div>
            {!loading && !distributors.length && <p role="alert">Link an active distributor first, or save with zero stock and use Purchases later.</p>}
            {Array.from({ length: count }, (_, index) => <fieldset key={index}><legend>Unit {index + 1}</legend><div className="add-other-grid">
                <label>IMEI 1<input inputMode="numeric" pattern="[0-9]{15}" maxLength={15} value={units[index]?.imei1 || ""} onChange={event => updateUnit(index, "imei1", event.target.value)} /></label>
                <label>IMEI 2 (optional)<input inputMode="numeric" pattern="[0-9]{15}" maxLength={15} value={units[index]?.imei2 || ""} onChange={event => updateUnit(index, "imei2", event.target.value)} /></label>
                <label>Serial number<input maxLength={100} value={units[index]?.serialNumber || ""} onChange={event => updateUnit(index, "serialNumber", event.target.value)} /></label>
            </div></fieldset>)}
        </>}
    </section>;
}
