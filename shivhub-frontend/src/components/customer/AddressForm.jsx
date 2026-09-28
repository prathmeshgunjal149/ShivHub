import { useEffect, useState } from "react";
import { getCities, getDistricts, getStates } from "../../services/locationService";



export default function AddressForm({ value, onChange, disabled = false }) {
    const [states, setStates] = useState([]), [districts, setDistricts] = useState([]), [cities, setCities] = useState([]);
    useEffect(() => { getStates().then(setStates).catch(() => setStates(["Maharashtra"])); }, []);
    useEffect(() => { getDistricts(value.state).then(setDistricts).catch(() => setDistricts([])); }, [value.state]);
    useEffect(() => { getCities(value.state, value.district).then(setCities).catch(() => setCities([])); }, [value.state, value.district]);
    const change = event => onChange({ ...value, [event.target.name]: event.target.value });
    const stateChange = event => onChange({ ...value, state:event.target.value, district:"", city:"" });
    const districtChange = event => onChange({ ...value, district:event.target.value, city:"" });
    const cityIsOther = value.city === "__OTHER__" || (value.city && !cities.includes(value.city));
    return <div className="address-form-grid">
        <label>Full name*<input required disabled={disabled} name="recipientName" value={value.recipientName} onChange={change} /></label>
        <label>Mobile number*<input required disabled={disabled} name="mobileNumber" inputMode="numeric" maxLength="10" value={value.mobileNumber} onChange={change} /></label>
        <label>Alternate mobile<input disabled={disabled} name="alternateMobileNumber" inputMode="numeric" maxLength="10" value={value.alternateMobileNumber || ""} onChange={change} /></label>
        <label>Address label<select disabled={disabled} name="addressLabel" value={value.addressLabel || "HOME"} onChange={change}><option value="HOME">Home</option><option value="WORK">Work</option><option value="OTHER">Other</option></select></label>
        <label>House / Flat / Building*<input required disabled={disabled} name="addressLine1" value={value.addressLine1} onChange={change} /></label>
        <label>Street / Area / Landmark*<input required disabled={disabled} name="addressLine2" value={value.addressLine2} onChange={change} /></label>
        <label>Landmark (optional)<input disabled={disabled} name="landmark" value={value.landmark || ""} onChange={change} /></label>
        <label>State*<select required disabled={disabled} value={value.state} onChange={stateChange}><option value="">Select state</option>{states.map(item => <option key={item}>{item}</option>)}</select></label>
        <label>District*<select required disabled={disabled || !value.state} value={value.district} onChange={districtChange}><option value="">Select district</option>{districts.map(item => <option key={item}>{item}</option>)}</select></label>
        <label>City*<select required disabled={disabled || !value.district} value={cityIsOther ? "Other" : value.city} onChange={event => onChange({ ...value, city:event.target.value === "Other" ? "__OTHER__" : event.target.value })}><option value="">Select city</option>{cities.map(item => <option key={item}>{item}</option>)}</select></label>
        {cityIsOther && <label>Other city*<input required disabled={disabled} name="city" value={value.city === "__OTHER__" ? "" : value.city} onChange={change} placeholder="Enter your city" /></label>}
        <label>Pincode*<input required disabled={disabled} name="pincode" inputMode="numeric" maxLength="6" value={value.pincode} onChange={change} /></label>
    </div>;
}
