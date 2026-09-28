import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { CheckCircle2, Coins, PackageCheck, ShoppingBag, Sparkles } from "lucide-react";
import "./CustomerActionFeedback.css";

const icons = { cart: ShoppingBag, order: PackageCheck, rewards: Coins };

export default function CustomerActionFeedback() {
    const reducedMotion = useReducedMotion();
    const [feedback, setFeedback] = useState(null);
    const timer = useRef(null);

    useEffect(() => {
        const show = event => {
            const next = event.detail || {};
            window.clearTimeout(timer.current);
            setFeedback(next);
            if (next.duration !== 0) timer.current = window.setTimeout(() => setFeedback(null), next.duration || 3200);
        };
        window.addEventListener("shivhub:customer-feedback", show);
        return () => { window.removeEventListener("shivhub:customer-feedback", show); window.clearTimeout(timer.current); };
    }, []);

    const Icon = icons[feedback?.kind] || CheckCircle2;
    return <AnimatePresence>
        {feedback && <motion.aside className={`customer-action-feedback ${feedback.kind || "success"}`} role="status" aria-live="polite" initial={reducedMotion ? { opacity: 0 } : { opacity: 0, y: 22, scale: .96 }} animate={reducedMotion ? { opacity: 1 } : { opacity: 1, y: 0, scale: 1 }} exit={reducedMotion ? { opacity: 0 } : { opacity: 0, y: 12, scale: .97 }} transition={{ duration: .22 }}>
            {feedback.kind === "rewards" && !reducedMotion && <div className="customer-feedback-coins" aria-hidden="true">{Array.from({ length: 7 }, (_, index) => <span key={index}>●</span>)}</div>}
            <span className="customer-feedback-icon"><Icon aria-hidden="true" />{feedback.kind === "rewards" && <Sparkles aria-hidden="true" />}</span>
            <div><strong>{feedback.title}</strong><p>{feedback.message}</p></div>
        </motion.aside>}
    </AnimatePresence>;
}
