import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";

import api from "../../services/api";
import useAuth from "../../hooks/useAuth";
import ProductDetailsSkeleton from "./product-details/ProductDetailsSkeleton";
import ProductGallery from "./product-details/ProductGallery";
import ProductPurchasePanel from "./product-details/ProductPurchasePanel";
import ProductRecommendations from "./product-details/ProductRecommendations";
import ProductReviews from "./product-details/ProductReviews";
import ProductSpecifications from "./product-details/ProductSpecifications";
import ProductInformationTabs from "./product-details/ProductInformationTabs";
import ProductCompare from "./product-details/ProductCompare";
import { ProductDetailHighlights, ProductDetailRail } from "./product-details/ProductDetailSupportPanels";
import {
    errorMessage,
    getProductImages,
    guestRecentlyViewed,
    imageUrl,
    money,
    stars
} from "./product-details/productUtils";
import "./ProductDetails.css";
import VariantSelector from "../../components/products/VariantSelector";
import MobileDeliveryEstimate from "../../components/products/MobileDeliveryEstimate";
import MobileOptionSelector from "../../components/products/MobileOptionSelector";
import CustomerPublicHeader from "../../components/CustomerPublicHeader";
import { fadeIn, noTransform } from "../../utils/animationVariants";
import { showCustomerFeedback } from "../../utils/customerFeedback";

const emptyReviews = {
    averageRating: 0,
    totalReviews: 0,
    ratingBreakdown: [0, 0, 0, 0, 0],
    reviews: []
};

export default function ProductDetails() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user, isAuthenticated } = useAuth();
    const reducedMotion = useReducedMotion();
    const requestId = useRef(0);

    const [product, setProduct] = useState(null);
    const [variants,setVariants]=useState([]),[selection,setSelection]=useState({});
    const [mobileOptions,setMobileOptions]=useState([]);
    const [variantLoading,setVariantLoading]=useState(false),[variantError,setVariantError]=useState("");
    const variantKeys=[...new Set(variants.flatMap(variant=>Object.keys(variant.attributes)))];
    const selectedVariant=variantKeys.length&&variantKeys.every(key=>selection[key])?variants.find(variant=>variantKeys.every(key=>variant.attributes[key]===selection[key])):null;
    const [recommendations, setRecommendations] = useState(null);
    const [fallbackProducts, setFallbackProducts] = useState([]);
    const [guestRecentCards, setGuestRecentCards] = useState([]);
    const [reviewData, setReviewData] = useState(emptyReviews);
    const [selectedImage, setSelectedImage] = useState(null);
    const [quantity, setQuantity] = useState(1);
    const [wishlist, setWishlist] = useState(false);
    const [loading, setLoading] = useState(true);
    const [working, setWorking] = useState(false);
    const [notice, setNotice] = useState("");
    const [error, setError] = useState("");
    const [form, setForm] = useState({ rating: 5, comment: "" });

    const isCustomer = user?.role === "CUSTOMER";

    useEffect(() => {
        document.body.classList.add("product-detail-route");
        return () => document.body.classList.remove("product-detail-route");
    }, []);

    const loadReviews = useCallback(async () => {
        const response = await api.get(`/api/products/${id}/reviews`);
        const data = response.data || emptyReviews;
        setReviewData(data);
        const mine = data.reviews?.find(review => review.mine);
        setForm(mine ? { rating: mine.rating, comment: mine.comment } : { rating: 5, comment: "" });
    }, [id]);

    useEffect(() => {
        let active = true;
        const currentRequest = requestId.current + 1;
        requestId.current = currentRequest;

        const loadGuestRecent = async () => {
            const entries = guestRecentlyViewed.get(Number(id));
            const cards = [];
            for (const entry of entries.slice(0, 8)) {
                try {
                    const response = await api.get(`/api/products/${entry.productId}`);
                    if (Number(response.data?.id) !== Number(id)) cards.push(response.data);
                } catch {
                    guestRecentlyViewed.remove(entry.productId);
                }
            }
            if (active && requestId.current === currentRequest) setGuestRecentCards(cards);
        };

        const load = async () => {
            setLoading(true);
            setError("");
            setNotice("");
            setQuantity(1);
            setRecommendations(null);
            setFallbackProducts([]);
            setGuestRecentCards([]);

            try {
                const [productResponse, recommendationResponse, productListResponse] = await Promise.all([
                    api.get(`/api/products/${id}`),
                    api.get(`/api/products/${id}/recommendations`).catch(() => ({ data: null })),
                    api.get("/api/products").catch(() => ({ data: [] })),
                    loadReviews()
                ]);

                if (!active || requestId.current !== currentRequest) return;

                const loadedProduct = productResponse.data;
                setProduct(loadedProduct);
                const images = getProductImages(loadedProduct);
                setSelectedImage(images[0] || null);
                setRecommendations(recommendationResponse.data);
                const catalogue = (productListResponse.data || []).filter(item => Number(item.id) !== Number(id));
                const sameCategory = catalogue.filter(item => (item.categoryEntity?.id || item.category) === (loadedProduct.categoryEntity?.id || loadedProduct.category));
                setFallbackProducts([...sameCategory, ...catalogue.filter(item => !sameCategory.includes(item))].slice(0, 24));

                if (isCustomer) {
                    await api.post(`/api/customer/recently-viewed/${id}`).catch(() => {});
                    const wishlistResponse = await api.get(`/api/wishlist/check/${id}`).catch(() => ({ data: false }));
                    if (active && requestId.current === currentRequest) setWishlist(Boolean(wishlistResponse.data));
                } else {
                    guestRecentlyViewed.add(Number(id));
                    await loadGuestRecent();
                }
            } catch (requestError) {
                if (active && requestId.current === currentRequest) setError(errorMessage(requestError));
            } finally {
                if (active && requestId.current === currentRequest) setLoading(false);
            }
        };

        load();
        return () => { active = false; };
    }, [id, isCustomer, loadReviews]);

    useEffect(()=>{let active=true;setVariants([]);setSelection({});setVariantError("");if(!product?.variantsEnabled)return;setVariantLoading(true);api.get(`/api/products/${product.id}/variants`).then(({data})=>{if(active)setVariants(data);}).catch(e=>{if(active)setVariantError(errorMessage(e));}).finally(()=>{if(active)setVariantLoading(false);});return()=>{active=false;};},[product?.id,product?.variantsEnabled]);
    useEffect(()=>{let active=true;setMobileOptions([]);const category=String(product?.categoryEntity?.name||product?.category||"").toLowerCase();if(category!=="mobiles")return()=>{active=false;};api.get(`/api/products/${product.id}/mobile-options`).then(({data})=>{if(active)setMobileOptions(Array.isArray(data)?data:[]);}).catch(()=>{if(active)setMobileOptions([]);});return()=>{active=false;};},[product?.id,product?.category,product?.categoryEntity?.name]);
    const productImages = useMemo(() => selectedVariant?.imageUrl ? [{id:`variant-${selectedVariant.id}`,imageUrl:selectedVariant.imageUrl},...getProductImages(product)] : getProductImages(product), [product,selectedVariant]);
    const originalPrice = Number(selectedVariant?.compareAtPrice || product?.price || 0);
    const finalPrice = Number(selectedVariant?.sellingPriceIncludingGst ?? product?.finalSellingPrice ?? originalPrice);
    const offerPercentage = selectedVariant ? Math.max(0,Math.round((1-finalPrice/originalPrice)*100)) : Number(product?.offerPercentage || 0);
    const mine = reviewData.reviews?.find(review => review.mine);
    const availableStock = Number(selectedVariant?.availableStock ?? (product?.variantsEnabled ? 0 : product?.availableStock ?? product?.stock ?? 0));
    const inStock = availableStock > 0;
    const recentlyViewed = isCustomer ? recommendations?.recentlyViewed || [] : guestRecentCards;

    const requireCustomer = () => {
        if (isCustomer) return true;
        setNotice(isAuthenticated ? "This action is available only for customer accounts." : "Please login as a customer to continue.");
        if (!isAuthenticated) navigate("/login");
        return false;
    };

    const addToCart = async (buyNow = false) => {
        if(product?.variantsEnabled&&!selectedVariant){setError(`Please select ${variantKeys.map(key=>key.replaceAll("_"," ")).join(" and ")||"all product options"}.`);return;}
        if (!isCustomer && !isAuthenticated && inStock) {
            sessionStorage.setItem("shivhub_pending_customer_action", JSON.stringify({
                type: "ADD_TO_CART",
                productId: Number(id),
                quantity,
                variantId:selectedVariant?.id,
                buyNow,
                returnTo: `/product/${id}`
            }));
        }
        if (!requireCustomer() || !inStock) return;
        setWorking(true);
        showCustomerFeedback({ kind: "cart", title: buyNow ? "Preparing checkout" : "Adding to cart", message: "Please wait while we update your cart.", duration: 0 });
        setNotice("");
        setError("");
        try {
            await api.post("/api/cart", { productId: Number(id), quantity, variantId:selectedVariant?.id });
            if (buyNow) {
                showCustomerFeedback({ kind: "cart", title: "Ready for checkout", message: "Your selected product is in the checkout flow." });
                navigate("/customer/checkout");
            } else {
                setNotice("Product added to your cart.");
                showCustomerFeedback({ kind: "cart", title: "Added to cart", message: "Your product is ready whenever you are." });
            }
        } catch (requestError) {
            const message = errorMessage(requestError);
            setError(message);
            showCustomerFeedback({ kind: "cart", title: "Couldn’t update cart", message });
        } finally {
            setWorking(false);
        }
    };

    const addSelectedAccessories = async selectedItems => {
        if (!requireCustomer() || !selectedItems.length) return;
        setWorking(true);
        showCustomerFeedback({ kind: "cart", title: "Adding selected items", message: "Please wait while we update your cart.", duration: 0 });
        setNotice("");
        setError("");
        try {
            await api.post("/api/cart/bulk", {
                items: selectedItems.map(item => ({ productId: Number(item.id), quantity: 1 }))
            });
            setNotice("Selected products added to your cart.");
            showCustomerFeedback({ kind: "cart", title: "Products added to cart", message: "Your selected products are ready whenever you are." });
        } catch (requestError) {
            const message = errorMessage(requestError);
            setError(message);
            showCustomerFeedback({ kind: "cart", title: "Couldn’t update cart", message });
        } finally {
            setWorking(false);
        }
    };

    const toggleWishlist = async () => {
        if (!requireCustomer()) return;
        setWorking(true);
        setError("");
        try {
            if (wishlist) await api.delete(`/api/wishlist/${id}`);
            else await api.post(`/api/wishlist/${id}`);
            setWishlist(value => !value);
            setNotice(wishlist ? "Removed from wishlist." : "Added to wishlist.");
        } catch (requestError) {
            setError(errorMessage(requestError));
        } finally {
            setWorking(false);
        }
    };

    const submitReview = async event => {
        event.preventDefault();
        if (!requireCustomer()) return;
        setWorking(true);
        setError("");
        setNotice("");
        try {
            const payload = { rating: Number(form.rating), comment: form.comment };
            if (mine) await api.put(`/api/products/${id}/reviews/mine`, payload);
            else await api.post(`/api/products/${id}/reviews`, payload);
            await loadReviews();
            setNotice(mine ? "Your review was updated." : "Thank you for your review.");
        } catch (requestError) {
            setError(errorMessage(requestError));
        } finally {
            setWorking(false);
        }
    };

    const deleteReview = async () => {
        if (!window.confirm("Delete your review?")) return;
        setWorking(true);
        setError("");
        try {
            await api.delete(`/api/products/${id}/reviews/mine`);
            setForm({ rating: 5, comment: "" });
            await loadReviews();
            setNotice("Your review was deleted.");
        } catch (requestError) {
            setError(errorMessage(requestError));
        } finally {
            setWorking(false);
        }
    };

    if (loading) return <><CustomerPublicHeader /><ProductDetailsSkeleton /></>;

    if (error && !product) {
        return (
            <><CustomerPublicHeader /><main className="shp-product-page">
                <div className="shp-product-state">
                    <p>{error}</p>
                    <button onClick={() => navigate(-1)}>Go back</button>
                    <button onClick={() => window.location.reload()}>Retry</button>
                </div>
            </main></>
        );
    }

    if (!product) {
        return <><CustomerPublicHeader /><main className="shp-product-page"><p className="shp-product-state">Product not found.</p></main></>;
    }

    return (
        <>
            <CustomerPublicHeader />
        <main className="shp-product-page">
            <nav className="shp-breadcrumb" aria-label="Breadcrumb">
                <Link to="/customer/products">Products</Link>
                <span>/</span>
                <span>{product.categoryEntity?.name || product.category || "Catalogue"}</span>
                {product.subCategory?.name && <><span>/</span><span>{product.subCategory.name}</span></>}
            </nav>

            {(notice || error) && <p className={error ? "shp-message error" : "shp-message"}>{error || notice}</p>}

            <section className="shp-product-shell">
                <div className="shp-media-column">
                    <ProductGallery
                        product={product}
                        images={productImages}
                        selectedImage={selectedImage}
                        setSelectedImage={setSelectedImage}
                        imageUrl={imageUrl}
                    />
                    <ProductDetailHighlights product={product} variants={variants} />
                </div>

                <section className="shp-product-info">
                    <p className="shp-kicker">{product.brand || "ShivHub verified"}</p>
                    <h1>{product.name}</h1>
                    <div className="shp-rating-line">
                        <span>{stars(reviewData.averageRating)}</span>
                        <strong>{Number(reviewData.averageRating || 0).toFixed(1)}</strong>
                        <small>{reviewData.totalReviews} real review{reviewData.totalReviews === 1 ? "" : "s"}</small>
                    </div>
                    <AnimatePresence initial={false} mode="wait">
                        <motion.div key={`${selectedVariant?.id || product.id}-${finalPrice}-${availableStock}`} className="shp-price-block" variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">
                            <strong>{money(finalPrice)}</strong>
                            {offerPercentage > 0 && <><del>{money(originalPrice)}</del><span>{offerPercentage}% off</span></>}
                            <small>Selling price includes GST{product.gstRate != null ? ` (${product.gstRate}%)` : ""}. Tax is recalculated on the server during billing/checkout.</small>
                        </motion.div>
                    </AnimatePresence>
                    <p className="shp-description">{product.description}</p>
                    {product.variantsEnabled&&<>{variantLoading&&<p role="status">Loading product options…</p>}{variantError&&<p role="alert">{variantError}</p>}{!variantLoading&&!variantError&&<VariantSelector variants={variants} selection={selection} onChange={value=>{setSelection(value);setQuantity(1);const match=variants.find(variant=>variantKeys.every(key=>variant.attributes[key]===value[key]));if(match?.imageUrl)setSelectedImage({id:`variant-${match.id}`,imageUrl:match.imageUrl});}} />}{selectedVariant&&<p>{availableStock===0?"Out of stock":availableStock<=2?`Only ${availableStock} left`:"Available"}</p>}</>}
                    {!product.variantsEnabled&&<MobileOptionSelector options={mobileOptions} currentProductId={product.id} onSelect={option=>{if(Number(option.productId)!==Number(product.id))navigate(`/product/${option.productId}`);}} />}
                    <MobileDeliveryEstimate product={product} />
                    <ProductSpecifications product={product} compact />
                </section>

                <div className="shp-detail-rail">
                    <ProductPurchasePanel
                        product={product}
                        quantity={quantity}
                        setQuantity={setQuantity}
                        availableStock={availableStock}
                        inStock={inStock}
                        working={working}
                        wishlist={wishlist}
                        addToCart={addToCart}
                        toggleWishlist={toggleWishlist}
                    />
                    <ProductCompare product={product} candidates={fallbackProducts} onMessage={setNotice} />
                    <ProductDetailRail product={product} inStock={inStock} />
                </div>
            </section>

            <ProductRecommendations
                recommendations={recommendations}
                fallbackProducts={fallbackProducts}
                recentlyViewed={recentlyViewed}
                currentProduct={product}
                money={money}
                imageUrl={imageUrl}
                working={working}
                onAddSelected={addSelectedAccessories}
            />

            <ProductInformationTabs product={product} onShowcase={() => document.querySelector(".shp-gallery")?.scrollIntoView({ behavior: "smooth", block: "start" })} />

            <ProductReviews
                reviewData={reviewData}
                form={form}
                setForm={setForm}
                mine={mine}
                working={working}
                submitReview={submitReview}
                deleteReview={deleteReview}
                stars={stars}
            />
        </main>
        </>
    );
}
