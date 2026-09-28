import { motion, useReducedMotion } from "framer-motion";
import { fadeUp, noTransform } from "../../../utils/animationVariants";

/**
 * Lightweight route-entry wrapper. It deliberately has no exit animation so
 * navigation and API-driven pages never flicker or delay a route change.
 */
export default function AnimatedPage({ children, pageKey, disabled = false, className = "" }) {
    const reducedMotion = useReducedMotion();

    if (disabled) return children;

    return (
        <motion.div
            key={pageKey}
            className={className}
            variants={reducedMotion ? noTransform : fadeUp}
            initial="hidden"
            animate="visible"
        >
            {children}
        </motion.div>
    );
}
