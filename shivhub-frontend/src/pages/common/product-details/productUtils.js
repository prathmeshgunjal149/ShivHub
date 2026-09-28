import { API_BASE_URL } from "../../../services/api";

const BACKEND_URL = API_BASE_URL;
const RECENT_KEY = "shivhub_guest_recent_products";

export const money = value => `₹${Number(value || 0).toLocaleString("en-IN", {
    maximumFractionDigits: 2
})}`;

export const imageUrl = value => {
    if (!value) return "";
    return /^https?:\/\//.test(value) ? value : `${BACKEND_URL}/${String(value).replace(/^\//, "")}`;
};

export const stars = rating => {
    const safe = Math.max(0, Math.min(5, Math.round(Number(rating || 0))));
    return "★".repeat(safe) + "☆".repeat(5 - safe);
};

export const errorMessage = error => (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.response?.data ||
    "Something went wrong. Please try again."
);

export const getProductImages = product => {
    const images = product?.images || [];
    // Product and description images are both real seller/admin uploads; show all of them in the gallery.
    const visibleImages = images.filter(image => image?.imageUrl);
    if (visibleImages.length) return visibleImages;
    return product?.imageUrl ? [{ id: "legacy", imageUrl: product.imageUrl }] : [];
};

export const splitOptions = value => String(value || "")
    .split(/[,|/]/)
    .map(item => item.trim())
    .filter(Boolean);

const readGuestRecent = () => {
    try {
        const data = JSON.parse(localStorage.getItem(RECENT_KEY) || "[]");
        return Array.isArray(data) ? data : [];
    } catch {
        return [];
    }
};

const writeGuestRecent = entries => {
    localStorage.setItem(RECENT_KEY, JSON.stringify(entries.slice(0, 20)));
};

export const guestRecentlyViewed = {
    add(productId) {
        const id = Number(productId);
        if (!id) return;
        const entries = readGuestRecent().filter(entry => Number(entry.productId) !== id);
        entries.unshift({ productId: id, lastViewedAt: Date.now() });
        writeGuestRecent(entries);
    },
    get(excludeProductId) {
        const exclude = Number(excludeProductId);
        return readGuestRecent()
            .filter(entry => Number(entry.productId) && Number(entry.productId) !== exclude)
            .sort((first, second) => Number(second.lastViewedAt || 0) - Number(first.lastViewedAt || 0));
    },
    remove(productId) {
        writeGuestRecent(readGuestRecent().filter(entry => Number(entry.productId) !== Number(productId)));
    },
    clear() {
        localStorage.removeItem(RECENT_KEY);
    }
};
