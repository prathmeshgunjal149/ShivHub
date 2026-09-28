import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { fadeIn, modalAnimation, noTransform } from "../../../utils/animationVariants";

/** Reusable overlay that preserves the parent form's submit and validation logic. */
export default function AnimatedModal({
    open,
    onBackdropMouseDown,
    children,
    backdropClassName = "dialog-backdrop",
    panelClassName = "dialog",
    ariaLabel = "Dialog"
}) {
    const reducedMotion = useReducedMotion();
    const panelVariants = reducedMotion ? noTransform : modalAnimation;

    return (
        <AnimatePresence initial={false}>
            {open && (
                <motion.div
                    className={backdropClassName}
                    role="presentation"
                    variants={fadeIn}
                    initial="hidden"
                    animate="visible"
                    exit="exit"
                    onMouseDown={onBackdropMouseDown}
                >
                    <motion.div
                        className={panelClassName}
                        role="dialog"
                        aria-modal="true"
                        aria-label={ariaLabel}
                        variants={panelVariants}
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                        onMouseDown={event => event.stopPropagation()}
                    >
                        {children}
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
