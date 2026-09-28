import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AddressCard from "../../components/customer/AddressCard";
import AddressForm from "../../components/customer/AddressForm";
import { emptyAddress } from "../../components/customer/addressDefaults.js";
import { createAddress, deleteAddress, getAddresses, setDefaultAddress, updateAddress } from "../../services/addressService";
import "./CustomerAddresses.css";

const valid = address => /^[6-9]\d{9}$/.test(address.mobileNumber || "") && /^[0-9]{6}$/.test(address.pincode || "") && ["recipientName","addressLine1","addressLine2","city","district","state"].every(key => address[key]?.trim());
export default function CustomerAddresses() {
 const [items,setItems]=useState([]),[draft,setDraft]=useState(emptyAddress),[editing,setEditing]=useState(null),[error,setError]=useState(""),[saving,setSaving]=useState(false);
 const load=()=>getAddresses().then(setItems).catch(e=>setError(e.response?.data?.message||"Could not load addresses.")); useEffect(()=>{load();},[]);
 const save=async e=>{e.preventDefault();if(!valid(draft)){setError("Please complete all required fields with a valid mobile number and pincode.");return;}try{setSaving(true);setError("");if(editing)await updateAddress(editing,draft);else await createAddress(draft);setDraft(emptyAddress);setEditing(null);load();}catch(e){setError(e.response?.data?.message||"Could not save address.");}finally{setSaving(false);}};
 const edit=a=>{const {id}=a;const data={...a};for(const key of ["id","isDefault","createdAt","updatedAt","customerId"])delete data[key];setDraft(data);setEditing(id);};
 const remove=async a=>{if(!window.confirm("Delete this saved address?"))return;try{await deleteAddress(a.id);load();}catch(e){setError(e.response?.data?.message||"Could not delete address.");}};
 return <main className="customer-addresses-page"><header><Link to="/customer/profile">← Profile</Link><div><span>ADDRESS BOOK</span><h1>My Addresses</h1><p>Manage delivery addresses for faster checkout.</p></div></header>{error&&<p className="address-page-error">{error}</p>}<section className="customer-address-list">{items.length?items.map(a=><AddressCard key={a.id} address={a} onEdit={edit} onDelete={remove} onDefault={async x=>{await setDefaultAddress(x.id);load();}}/>):<p className="address-empty">No saved address yet. Your first checkout address will be saved here.</p>}</section><section className="customer-address-editor"><h2>{editing?"Edit address":"Add new address"}</h2><form onSubmit={save}><AddressForm value={draft} onChange={setDraft} disabled={saving}/><div><button type="button" onClick={()=>{setDraft(emptyAddress);setEditing(null);}}>Cancel</button><button disabled={saving}>{saving?"Saving…":"Save address"}</button></div></form></section></main>;
}
