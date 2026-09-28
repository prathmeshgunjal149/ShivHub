// Shared, short motion primitives used by customer, seller and admin views.
// Keep movement small so forms, tables and point-of-sale screens remain fast.
export const motionEase = [0.22, 1, 0.36, 1];

export const fadeIn = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { duration: 0.2, ease: motionEase } },
    exit: { opacity: 0, transition: { duration: 0.14, ease: motionEase } }
};

export const fadeUp = {
    hidden: { opacity: 0, y: 12 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.28, ease: motionEase } },
    exit: { opacity: 0, y: 6, transition: { duration: 0.14, ease: motionEase } }
};

export const fadeDown = {
    hidden: { opacity: 0, y: -8 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.18, ease: motionEase } },
    exit: { opacity: 0, y: -4, transition: { duration: 0.12, ease: motionEase } }
};

export const scaleIn = {
    hidden: { opacity: 0, scale: 0.98 },
    visible: { opacity: 1, scale: 1, transition: { duration: 0.2, ease: motionEase } },
    exit: { opacity: 0, scale: 0.985, transition: { duration: 0.14, ease: motionEase } }
};

export const staggerContainer = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.045, delayChildren: 0.02 } }
};

export const listItem = {
    hidden: { opacity: 0, y: 8 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.2, ease: motionEase } },
    exit: { opacity: 0, x: -8, transition: { duration: 0.14, ease: motionEase } }
};

export const modalAnimation = {
    hidden: { opacity: 0, scale: 0.985, y: 6 },
    visible: { opacity: 1, scale: 1, y: 0, transition: { duration: 0.2, ease: motionEase } },
    exit: { opacity: 0, scale: 0.985, y: 4, transition: { duration: 0.14, ease: motionEase } }
};

export const noTransform = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { duration: 0.12 } },
    exit: { opacity: 0, transition: { duration: 0.1 } }
};
