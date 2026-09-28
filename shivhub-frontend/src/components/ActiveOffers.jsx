import { useEffect, useState } from "react";
import { getActiveOffers } from "../services/marketingService";
import "./ActiveOffers.css";
export default function ActiveOffers({ audience }) { const [offers,setOffers]=useState([]); useEffect(()=>{getActiveOffers(audience).then(setOffers).catch(()=>setOffers([]));},[audience]); if(!offers.length)return null; return <section className="active-offers"><div><p>EXCLUSIVE FOR {audience}S</p><h2>Latest offers from ShivHub</h2></div><div className="offer-cards">{offers.map(item=><article key={item.id}><strong>{item.title}</strong><span>{item.description}</span>{item.discountPercent&&<b>{item.discountPercent}% OFF</b>}{item.couponCode&&<code>{item.couponCode}</code>}</article>)}</div></section>; }
