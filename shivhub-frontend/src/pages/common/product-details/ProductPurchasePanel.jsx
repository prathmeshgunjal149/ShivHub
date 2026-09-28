import { splitOptions } from "./productUtils";
import { motion, useReducedMotion } from "framer-motion";

export default function ProductPurchasePanel({
    product,
    quantity,
    setQuantity,
    availableStock,
    inStock,
    working,
    wishlist,
    addToCart,
    toggleWishlist
}) {
    const reducedMotion = useReducedMotion();
    const colours = product.variantsEnabled ? [] : splitOptions(product.colorOptions);
    const storage = product.variantsEnabled ? [] : splitOptions(product.storage);
    const ram = product.variantsEnabled ? [] : splitOptions(product.ram);

    return (
        <aside className="shp-purchase-panel">
            <p className="shp-panel-label">Seller</p>
            <h2>{product.seller?.businessName || product.seller?.name || "ShivHub seller"}</h2>
            <span className={inStock ? "shp-stock ok" : "shp-stock"}>{inStock ? `${availableStock} available` : "Out of stock"}</span>

            {!!colours.length && (
                <div className="shp-option-group">
                    <span>Colour</span>
                    <div>{colours.map(value => <span className="shp-option-value" key={value}>{value}</span>)}</div>
                </div>
            )}
            {!!ram.length && (
                <div className="shp-option-group">
                    <span>RAM</span>
                    <div>{ram.map(value => <span className="shp-option-value" key={value}>{value}</span>)}</div>
                </div>
            )}
            {!!storage.length && (
                <div className="shp-option-group">
                    <span>Storage</span>
                    <div>{storage.map(value => <span className="shp-option-value" key={value}>{value}</span>)}</div>
                </div>
            )}

            <div className="shp-qty">
                <span>Quantity</span>
                <div className="shp-qty-control">
                    <button type="button" aria-label="Decrease quantity" disabled={quantity <= 1 || working} onClick={() => setQuantity(value => value - 1)}>−</button>
                    <output aria-live="polite">{quantity}</output>
                    <button type="button" aria-label="Increase quantity" disabled={quantity >= availableStock || working} onClick={() => setQuantity(value => value + 1)}>+</button>
                </div>
            </div>

            <div className="shp-actions">
                <motion.button type="button" disabled={!inStock || working} onClick={() => addToCart(false)} whileHover={reducedMotion || !inStock ? undefined : { scale: 1.02 }} whileTap={reducedMotion || !inStock ? undefined : { scale: 0.98 }}>Add to cart</motion.button>
                <motion.button type="button" disabled={!inStock || working} onClick={() => addToCart(true)} whileHover={reducedMotion || !inStock ? undefined : { scale: 1.02 }} whileTap={reducedMotion || !inStock ? undefined : { scale: 0.98 }}>Buy now</motion.button>
                <button type="button" className={wishlist ? "wish active" : "wish"} disabled={working} onClick={toggleWishlist}>
                    {wishlist ? "♥" : "♡"}
                </button>
            </div>
        </aside>
    );
}
