import { useMemo, useState } from "react";
import ProductRecommendationSection from "./ProductRecommendationSection";

export default function ProductRecommendations({
    recommendations,
    fallbackProducts = [],
    recentlyViewed,
    currentProduct,
    money,
    imageUrl,
    working,
    onAddSelected
}) {
    const [selected, setSelected] = useState([]);
    const bundleProducts = recommendations?.frequentlyBoughtTogether || [];
    const compatible = recommendations?.compatibleAccessories || [];
    const similar = recommendations?.similarProducts?.length
        ? recommendations.similarProducts
        : fallbackProducts;

    const toggle = product => {
        setSelected(items => items.some(item => Number(item.id) === Number(product.id))
            ? items.filter(item => Number(item.id) !== Number(product.id))
            : [...items, product]);
    };

    const combinedPrice = useMemo(() => {
        const currentPrice = Number(currentProduct?.finalSellingPrice ?? currentProduct?.price ?? 0);
        return selected.reduce((total, item) => total + Number(item.finalPrice ?? item.finalSellingPrice ?? item.price ?? 0), currentPrice);
    }, [currentProduct, selected]);

    const selectedIds = selected.map(item => Number(item.id));

    return (
        <>
            {!!bundleProducts.length && (
                <section className="shp-bundle">
                    <div>
                        <p>{recommendations?.frequentlyBoughtSource}</p>
                        <h2>{recommendations?.frequentlyBoughtLabel || "Complete your purchase"}</h2>
                    </div>
                    <div className="shp-bundle-total">
                        <span>Combined total</span>
                        <strong>{money(combinedPrice)}</strong>
                        <button type="button" disabled={!selected.length || working} onClick={() => onAddSelected(selected)}>
                            Add selected to cart
                        </button>
                    </div>
                    <ProductRecommendationSection
                        title=""
                        subtitle=""
                        products={bundleProducts}
                        money={money}
                        imageUrl={imageUrl}
                        selectable
                        selectedIds={selectedIds}
                        onToggle={toggle}
                    />
                </section>
            )}

            <ProductRecommendationSection title="Compatible accessories" subtitle="Explicitly linked accessories" products={compatible} money={money} imageUrl={imageUrl} />
            <ProductRecommendationSection
                title={recommendations?.similarProducts?.length ? "Similar products" : "More products from ShivHub"}
                subtitle={recommendations?.similarProducts?.length ? "Matched by category, price and specifications" : "Showing other available approved products"}
                products={similar}
                money={money}
                imageUrl={imageUrl}
            />
            <ProductRecommendationSection title="Recently viewed" subtitle="Your latest browsing" products={recentlyViewed} money={money} imageUrl={imageUrl} />
        </>
    );
}
