import { getPrimaryProductImage } from "../../../utils/imageUrl";

// Converts backend product-image data into a browser-safe absolute image URL.
export const getProductImage = (product) => {
    return getPrimaryProductImage(product) || null;
};

// Creates the time-based greeting without storing any data in the backend.
export const getCustomerGreeting = () => {
    const hour = new Date().getHours();
    return hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
};
