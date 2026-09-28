import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import api from "../../services/api";
import useAuth from "../../hooks/useAuth";
import "./variants.css";
import { fadeIn, noTransform } from "../../utils/animationVariants";

const isMobileProduct = product => String(product?.categoryEntity?.name || product?.category || "").toLowerCase() === "mobiles";

export default function MobileDeliveryEstimate({ product, addressId, onEstimate, display = true }) {
    const { user } = useAuth();
    const reducedMotion = useReducedMotion();
    const [addresses, setAddresses] = useState([]);
    const [chosen, setChosen] = useState("");
    const [estimate, setEstimate] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const listener = useRef(onEstimate);
    const mobile = isMobileProduct(product);
    const customer = user?.role === "CUSTOMER";
    const selected = addressId === undefined ? chosen : addressId;

    useEffect(() => { listener.current = onEstimate; }, [onEstimate]);

    useEffect(() => {
        let active = true;
        if (!mobile || !customer || addressId !== undefined) return () => { active = false; };
        api.get("/api/customer/addresses")
            .then(({ data }) => {
                if (!active) return;
                const list = Array.isArray(data) ? data : [];
                setAddresses(list);
                setChosen(String((list.find(item => item.isDefault) || list[0])?.id || ""));
            })
            .catch(() => { if (active) setError("Unable to load delivery addresses."); });
        return () => { active = false; };
    }, [mobile, customer, addressId]);

    useEffect(() => {
        let active = true;
        setEstimate(null);
        setLoading(false);
        listener.current?.(null);
        if (!mobile || !customer || !selected) return () => { active = false; };

        setLoading(true);
        setError("");
        api.get(`/api/customer/products/${product.id}/delivery-estimate`, { params: { addressId: selected } })
            .then(({ data }) => {
                if (!active) return;
                setEstimate(data);
                listener.current?.(data);
            })
            .catch(requestError => {
                if (active) setError(requestError.response?.data?.message || "Unable to check delivery time. Seller will confirm delivery.");
            })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [mobile, customer, product?.id, selected]);

    // Checkout uses this component as a data loader and renders one consolidated
    // estimate below the selected address. Keeping the fetch here avoids changing
    // the delivery API flow while preventing a wide card from breaking an item row.
    if (!mobile || !display) return null;

    return (
        <section className="delivery-estimate">
            <strong>Mobile delivery estimate</strong>
            {customer && addressId === undefined && (
                <label>Delivery address
                    <select value={chosen} onChange={event => setChosen(event.target.value)}>
                        <option value="">Select delivery address</option>
                        {addresses.map(address => <option key={address.id} value={address.id}>{address.addressLabel} · {address.city} {address.pincode}</option>)}
                    </select>
                </label>
            )}
            <AnimatePresence initial={false} mode="wait">
                {loading ? (
                    <motion.p key="loading" role="status" variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">Checking delivery time…</motion.p>
                ) : error ? (
                    <motion.p key="error" role="alert" variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">{error}</motion.p>
                ) : estimate ? (
                    <motion.div key={`estimate-${estimate.distanceKm ?? "pending"}`} variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">
                        <p><strong>{estimate.estimatedDeliveryText || estimate.message}</strong></p>
                        {estimate.locationAvailable && <>
                            <p>Distance from {estimate.sellerShopName}: approximately {estimate.distanceKm} km</p>
                            <p>{estimate.message}</p>
                            {estimate.serviceAvailable === false && <p>Delivery is unavailable for this address.</p>}
                            {Number(estimate.deliveryCharge) > 0 && <p>Delivery charge: ₹{estimate.deliveryCharge}</p>}
                        </>}
                        <p>Delivered from: {estimate.sellerShopName}</p>
                    </motion.div>
                ) : (
                    <motion.p key="empty" variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">Add/select your delivery address to check delivery time.</motion.p>
                )}
            </AnimatePresence>
        </section>
    );
}
