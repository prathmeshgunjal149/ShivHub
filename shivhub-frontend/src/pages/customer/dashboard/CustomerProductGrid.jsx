import { useState } from "react";
import { motion, useReducedMotion } from "framer-motion";
import api from "../../../services/api";

import { getProductImage } from "./helpers";


/*
 * =========================================================
 * CustomerProductGrid
 * =========================================================
 *
 * Displays real products received from:
 *
 * GET /api/products
 *
 * Features:
 *
 * - Product image
 * - Product details
 * - Stock status
 * - View product
 * - Add to cart
 * - Wishlist
 *
 * No dummy buttons.
 *
 * =========================================================
 */

export default function CustomerProductGrid({
    products,
    onOpenProduct,
    onCart,
    onCartUpdated
}) {
    const reducedMotion = useReducedMotion();

    const [cartLoading, setCartLoading] =
        useState({});

    const [wishlistLoading, setWishlistLoading] =
        useState({});

    const [cartItems, setCartItems] =
        useState(new Set());

    const [wishlistItems, setWishlistItems] =
        useState(new Set());


    /*
     * =====================================================
     * ADD TO CART
     * =====================================================
     */

    const handleAddToCart = async (product) => {
        if (product?.variantsEnabled) { onOpenProduct(product.id); return; }

        if (!product?.id) {
            return;
        }


        if (
            Number(product.availableStock ?? product.stock ?? 0) <= 0
        ) {

            alert(
                "This product is currently out of stock."
            );

            return;
        }


        const productId =
            Number(product.id);


        if (cartLoading[productId]) {
            return;
        }


        try {

            setCartLoading(
                previous => ({
                    ...previous,
                    [productId]: true
                })
            );


            /*
             * Existing ShivHub API service.
             *
             * Authentication is handled by api.js.
             */

            await api.post(
                "/api/cart",
                {
                    productId: productId,
                    quantity: 1
                }
            );


            setCartItems(
                previous => {

                    const updated =
                        new Set(previous);

                    updated.add(
                        productId
                    );

                    return updated;
                }
            );

            onCartUpdated?.();


        } catch (error) {

            console.error(
                "Add to cart failed:",
                error
            );


            /*
             * If backend says item already exists,
             * still consider it inside cart.
             */

            if (
                error.response?.status === 409
            ) {

                setCartItems(
                    previous => {

                        const updated =
                            new Set(previous);

                        updated.add(
                            productId
                        );

                        return updated;
                    }
                );

                onCartUpdated?.();

            } else {

                alert(
                    error.response?.data?.message ||
                    "Unable to add product to cart."
                );
            }

        } finally {

            setCartLoading(
                previous => ({
                    ...previous,
                    [productId]: false
                })
            );
        }
    };


    /*
     * =====================================================
     * WISHLIST
     * =====================================================
     */

    const handleWishlist = async (product) => {

        if (!product?.id) {
            return;
        }


        const productId =
            Number(product.id);


        if (
            wishlistLoading[productId]
        ) {

            return;
        }


        const alreadyAdded =
            wishlistItems.has(
                productId
            );


        try {

            setWishlistLoading(
                previous => ({
                    ...previous,
                    [productId]: true
                })
            );


            if (alreadyAdded) {

                /*
                 * Remove from wishlist.
                 */

                await api.delete(
                    `/api/wishlist/${productId}`
                );


                setWishlistItems(
                    previous => {

                        const updated =
                            new Set(previous);

                        updated.delete(
                            productId
                        );

                        return updated;
                    }
                );


            } else {

                /*
                 * Add to wishlist.
                 */

                await api.post(
                    `/api/wishlist/${productId}`
                );


                setWishlistItems(
                    previous => {

                        const updated =
                            new Set(previous);

                        updated.add(
                            productId
                        );

                        return updated;
                    }
                );
            }


        } catch (error) {

            console.error(
                "Wishlist operation failed:",
                error
            );


            if (
                error.response?.status === 401 ||
                error.response?.status === 403
            ) {

                alert(
                    "Please login to use Wishlist."
                );

            } else {

                alert(
                    error.response?.data?.message ||
                    "Unable to update wishlist."
                );
            }

        } finally {

            setWishlistLoading(
                previous => ({
                    ...previous,
                    [productId]: false
                })
            );
        }
    };


    /*
     * =====================================================
     * GO TO CART
     * =====================================================
     */

    const handleGoToCart = () => {

        if (onCart) {

            onCart();

        }

    };


    /*
     * =====================================================
     * PRODUCT GRID
     * =====================================================
     */

    return (

        <div className="marketplace-product-grid">

            {products.map(
                (product, index) => {

                    const productId =
                        Number(product.id);


                    const image =
                        getProductImage(
                            product
                        );


                    const inCart =
                        cartItems.has(
                            productId
                        );


                    const inWishlist =
                        wishlistItems.has(
                            productId
                        );


                    const addingToCart =
                        cartLoading[
                            productId
                        ];


                    const updatingWishlist =
                        wishlistLoading[
                            productId
                        ];


                    const outOfStock =
                        Number(product.availableStock ?? product.stock ?? 0) <= 0;

                    const originalPrice = Number(product.price || 0);
                    const finalPrice = Number(product.finalPrice ?? product.finalSellingPrice ?? originalPrice);
                    const hasOffer = Number(product.offerPercentage || 0) > 0
                        && finalPrice < originalPrice;


                    return (

                        <motion.article
                            className="marketplace-product-card"
                            key={product.id}
                            initial={reducedMotion || index > 11 ? false : { opacity: 0, y: 8 }}
                            animate={{ opacity: 1, y: 0 }}
                            whileHover={reducedMotion ? undefined : { y: -5 }}
                            transition={{ duration: 0.2, delay: Math.min(index, 8) * 0.025 }}
                        >


                            {/* =================================================
                                IMAGE
                            ================================================= */}

                            <button
                                type="button"
                                className="marketplace-image-button"
                                onClick={() =>
                                    onOpenProduct(
                                        product.id
                                    )
                                }
                            >

                                <span className="marketplace-product-badge">

                                    {
                                        outOfStock
                                            ? "Out of stock"
                                            : "In stock"
                                    }

                                </span>


                                {image ? (

                                    <img
                                        src={image}
                                        alt={
                                            product.name
                                        }
                                    />

                                ) : (

                                    <span>
                                        No image available
                                    </span>

                                )}

                            </button>


                            {/* =================================================
                                WISHLIST ICON
                            ================================================= */}

                            <button
                                type="button"
                                className={
                                    inWishlist
                                        ? "marketplace-wishlist marketplace-wishlist-active"
                                        : "marketplace-wishlist"
                                }
                                onClick={() =>
                                    handleWishlist(
                                        product
                                    )
                                }
                                disabled={
                                    updatingWishlist
                                }
                                title={
                                    inWishlist
                                        ? "Remove from wishlist"
                                        : "Add to wishlist"
                                }
                            >

                                {
                                    updatingWishlist
                                        ? "..."
                                        : inWishlist
                                            ? "♥"
                                            : "♡"
                                }

                            </button>


                            {/* =================================================
                                PRODUCT INFORMATION
                            ================================================= */}

                            <div className="marketplace-product-info">

                                <span>
                                    {
                                        product.category ||
                                        "Product"
                                    }
                                </span>


                                <h3>
                                    {
                                        product.name
                                    }
                                </h3>


                                <p>
                                    {
                                        product.description ||
                                        "No description available."
                                    }
                                </p>


                                {hasOffer && (
                                    <span className="marketplace-original-price">
                                        MRP ₹{originalPrice.toLocaleString("en-IN")}
                                    </span>
                                )}


                                <strong>
                                    ₹
                                    {
                                        Number(
                                            product.finalPrice ?? product.finalSellingPrice ?? product.price ?? 0
                                        ).toLocaleString(
                                            "en-IN"
                                        )
                                    }
                                </strong>


                                {hasOffer && (
                                    <span className="marketplace-offer-badge">
                                        {product.offerPercentage}% OFF
                                    </span>
                                )}


                                {/* =================================================
                                    VIEW PRODUCT
                                ================================================= */}

                                <button
                                    type="button"
                                    onClick={() =>
                                        onOpenProduct(
                                            product.id
                                        )
                                    }
                                >
                                    View product
                                </button>


                                {/* =================================================
                                    ADD TO CART
                                ================================================= */}

                                <button
                                    type="button"
                                    className={
                                        inCart
                                            ? "marketplace-cart-button marketplace-cart-added"
                                            : "marketplace-cart-button"
                                    }
                                    onClick={() => {

                                        if (inCart) {

                                            handleGoToCart();

                                        } else {

                                            handleAddToCart(
                                                product
                                            );
                                        }

                                    }}
                                    disabled={
                                        outOfStock ||
                                        addingToCart
                                    }
                                >

                                    {
                                        addingToCart
                                            ? "Adding..."
                                            : outOfStock
                                                ? "Out of stock"
                                                : inCart
                                                    ? "🛒 Go to Cart"
                                                    : "🛒 Add to Cart"
                                    }

                                </button>


                                {/* =================================================
                                    WISHLIST
                                ================================================= */}

                                <button
                                    type="button"
                                    className={
                                        inWishlist
                                            ? "marketplace-wishlist-button marketplace-wishlist-button-active"
                                            : "marketplace-wishlist-button"
                                    }
                                    onClick={() =>
                                        handleWishlist(
                                            product
                                        )
                                    }
                                    disabled={
                                        updatingWishlist
                                    }
                                >

                                    {
                                        updatingWishlist
                                            ? "Updating..."
                                            : inWishlist
                                                ? "♥ Wishlisted"
                                                : "♡ Add to Wishlist"
                                    }

                                </button>

                            </div>

                        </motion.article>
                    );
                }
            )}

        </div>
    );
}
