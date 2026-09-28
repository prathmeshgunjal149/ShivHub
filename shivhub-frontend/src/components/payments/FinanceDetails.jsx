import { dateText, generateEmis } from "./financeUtils.js";
import { useEffect, useState } from "react";
import api from "../../services/api";
import "./Finance.css";




export default function FinanceDetails({value,onChange,total}) {
    const [companies,setCompanies]=useState([]),[schemes,setSchemes]=useState([]),[error,setError]=useState(""),[busy,setBusy]=useState(false);
    const [start,setStart]=useState(()=>dateText(new Date())),[requesting,setRequesting]=useState(false),[request,setRequest]=useState({name:"",contact:"",remarks:""}),[notice,setNotice]=useState("");
    useEffect(()=>{let current=true;Promise.all([api.get("/api/finance/companies"),api.get("/api/finance/schemes")]).then(([a,b])=>{if(current){setCompanies(a.data);setSchemes(b.data);}}).catch(()=>{if(current)setError("Unable to load finance configuration.");});return()=>{current=false;};},[]);
    const set=(key,data)=>onChange({...value,[key]:data});
    const scheduleTotal=value.installments.reduce((sum,row)=>sum+Number(row.amount||0),0);
    const base=Math.max(0,total-Number(value.downpayment||0)+Number(value.processingCharges||0)+Number(value.dbdCharges||0)+Number(value.otherCharges||0)+Number(value.adjustment||0));
    const selectScheme=id=>{const scheme=schemes.find(row=>String(row.id)===id);onChange({...value,schemeId:id,tenureMonths:scheme?.tenureMonths||value.tenureMonths,advanceMonths:scheme?.advanceMonths||0,installments:[]});};
    const submitRequest=async()=>{setBusy(true);setError("");try{await api.post("/api/finance/company-requests",request);setNotice("Request submitted for admin approval. You can use the provider after approval.");setRequesting(false);}catch(err){setError(err.response?.data?.message||"Unable to submit request.");}finally{setBusy(false);}};
    return <section className="finance-panel"><h3>Finance & EMI details</h3><p className="finance-help">Full invoice value stays ₹{Number(total).toFixed(2)}. EMI details appear in the customer email, not on the invoice.</p>
        {error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}
        <div className="finance-grid"><label>Finance company<select value={requesting?"request":value.companyId} onChange={e=>{setRequesting(e.target.value==="request");onChange({...value,companyId:e.target.value==="request"?"":e.target.value,schemeId:"",installments:[]});}}><option value="">Select approved company</option>{companies.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}<option value="request">Request new finance company…</option></select></label>
        <label>Loan / application number<input maxLength={150} value={value.loanNumber} onChange={e=>set("loanNumber",e.target.value)}/></label></div>
        {requesting&&<div className="finance-request"><label>Company name<input value={request.name} maxLength={150} onChange={e=>setRequest({...request,name:e.target.value})}/></label><label>Contact/reference<input value={request.contact} maxLength={500} onChange={e=>setRequest({...request,contact:e.target.value})}/></label><label>Remarks<input value={request.remarks} maxLength={1000} onChange={e=>setRequest({...request,remarks:e.target.value})}/></label><button type="button" disabled={busy||!request.name.trim()} onClick={submitRequest}>Submit for approval</button></div>}
        <div className="finance-grid"><label>Scheme<select value={value.schemeId} onChange={e=>selectScheme(e.target.value)}><option value="">Custom</option>{schemes.filter(s=>!s.companyId||String(s.companyId)===String(value.companyId)).map(s=><option value={s.id} key={s.id}>{s.name} — {s.tenureMonths} installments</option>)}</select></label>
        <label>EMI start date<input type="date" value={start} onChange={e=>setStart(e.target.value)}/></label>
        <label>Installments<input type="number" min="1" max="120" disabled={!!value.schemeId} value={value.tenureMonths} onChange={e=>set("tenureMonths",Number(e.target.value))}/></label><label>Advance months (metadata)<input type="number" min="0" max="120" disabled={!!value.schemeId} value={value.advanceMonths} onChange={e=>set("advanceMonths",Number(e.target.value))}/></label>
        {[["downpayment","Downpayment collected"],["processingCharges","Processing charges"],["dbdCharges","DBD charges"],["otherCharges","Other charges"],["deduction","Provider deduction"],["adjustment","Adjustment (+/-)"]].map(([key,label])=><label key={key}>{label}<input type="number" step="0.01" min={key==="adjustment"?undefined:0} value={value[key]} onChange={e=>set(key,e.target.value)}/></label>)}</div>
        <button type="button" onClick={()=>set("installments",generateEmis(start,Number(value.tenureMonths),base))}>{value.installments.length?"Regenerate suggested schedule":"Generate suggested schedule"}</button><p className="finance-help">Generating replaces the rows below. Edit them to match the provider agreement.</p>
        <div className="finance-table"><table><thead><tr><th>Month</th><th>EMI amount</th><th>Due date</th></tr></thead><tbody>{value.installments.map((row,index)=><tr key={index}>{["monthLabel","amount","dueDate"].map(key=><td key={key}><input aria-label={`${key} installment ${index+1}`} type={key==="amount"?"number":key==="dueDate"?"date":"text"} min={key==="amount"?0:undefined} step={key==="amount"?"0.01":undefined} value={row[key]} onChange={e=>set("installments",value.installments.map((old,i)=>i===index?{...old,[key]:e.target.value}:old))}/></td>)}</tr>)}</tbody></table></div>
        <p>Scheduled EMI: ₹{scheduleTotal.toFixed(2)} · Suggested base: ₹{base.toFixed(2)} · Difference: ₹{(scheduleTotal-base).toFixed(2)}</p><p>Expected provider settlement: ₹{(total-Number(value.downpayment||0)-Number(value.deduction||0)+Number(value.adjustment||0)).toFixed(2)}</p>
        <label>Internal finance remarks<textarea maxLength={1000} value={value.remarks} onChange={e=>set("remarks",e.target.value)}/></label>
    </section>;
}
